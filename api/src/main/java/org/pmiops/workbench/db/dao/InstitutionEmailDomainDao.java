package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionEmailDomainDao extends CrudRepository<DbInstitutionEmailDomain, Long> {

  List<DbInstitutionEmailDomain> findAllByInstitution(DbInstitution institution);

  default void deleteAllByInstitution(DbInstitution institution) {
    delete(findAllByInstitution(institution));
  }
}
