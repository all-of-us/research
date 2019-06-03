package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.Cohort;
import org.springframework.data.repository.CrudRepository;

public interface CohortDao extends CrudRepository<Cohort, Long> {

  /** Returns the cohort in the workspace with the specified name, or null if there is none. */
  Cohort findCohortByNameAndWorkspaceId(String name, long workspaceId);

  List<Cohort> findAllByCohortIdIn(Collection<Long> cohortIds);
}
