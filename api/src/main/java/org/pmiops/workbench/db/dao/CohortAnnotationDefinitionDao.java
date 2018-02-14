package org.pmiops.workbench.db.dao;


import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CohortAnnotationDefinitionDao extends JpaRepository<CohortAnnotationDefinition, Long> {

    CohortAnnotationDefinition findByCohortIdAndColumnName(@Param("cohortId") long cohortId, @Param("ColumnName") String ColumnName);

    List<CohortAnnotationDefinition> findByCohortId(@Param("cohortId") long cohortId);

    CohortAnnotationDefinition findByCohortIdAndCohortAnnotationDefinitionId( @Param("cohortId") long cohortId, @Param("cohortAnnotationDefinitionId") long cohortAnnotationDefinitionId);
}
