package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.data.repository.CrudRepository;

public interface CohortDao extends CrudRepository<DbCohort, Long> {

  /** Returns the cohort in the workspace with the specified name, or null if there is none. */
  DbCohort findCohortByNameAndWorkspaceId(String name, long workspaceId);

  DbCohort findCohortByWorkspaceIdAndCohortId(long workspaceId, long cohortId);

  default DbCohort getRequiredByWorkspaceIdAndCohortId(long workspaceId, long cohortId) {
    final DbCohort cohort = findCohortByWorkspaceIdAndCohortId(workspaceId, cohortId);
    if (cohort == null) {
      throw new NotFoundException("Resource does not belong to specified workspace");
    }
    return cohort;
  }

  List<DbCohort> findAllByCohortIdIn(Collection<Long> cohortIds);

  List<DbCohort> findByWorkspaceId(long workspaceId);

  int countByWorkspaceId(long workspaceId);
}
