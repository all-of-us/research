package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

    List<Criteria> findCriteriaByTypeAndParentIdOrderByCodeAsc(@Param("type") String type, @Param("parentId") Long parentId);

    @Query("select c from Criteria c where c.type like :type and (lower(c.name) like lower(:name) or lower(c.code) like lower(:code)) order by c.code asc")
    List<Criteria> findCriteriaByTypeAndNameOrCode(@Param("type") String type, @Param("name") String name, @Param("code") String code);

}
