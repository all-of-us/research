package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.cdr.model.DbPerson;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface PersonDao extends CrudRepository<DbPerson, Long> {

  @Query(
      value =
          "SELECT * FROM \n"
              + "  (\n"
              + "    SELECT 'AGE' as ageType, DATE_FORMAT(NOW(), '%Y') - DATE_FORMAT(dob, '%Y') - (DATE_FORMAT(NOW(), '00-%m-%d') < DATE_FORMAT(dob, '00-%m-%d')) as age, count(*) as count \n"
              + "    FROM cb_person where is_deceased = 0\n"
              + "    GROUP BY ageType, age \n"
              + "    UNION \n"
              + "    SELECT 'AGE_AT_CONSENT' as ageType, age_at_consent as age, count(*) as count \n"
              + "    FROM cb_person \n"
              + "    WHERE age_at_consent != 0 \n"
              + "    GROUP BY ageType, age \n"
              + "    UNION \n"
              + "    SELECT 'AGE_AT_CDR' as ageType, age_at_cdr as age, count(*) as count \n"
              + "    FROM cb_person \n"
              + "    WHERE age_at_cdr != 0 \n"
              + "    AND is_deceased = 0 \n"
              + "    GROUP BY ageType, age\n"
              + "  ) a \n"
              + "ORDER BY ageType, age",
      nativeQuery = true)
  List<DbAgeTypeCount> findAgeTypeCounts();
}
