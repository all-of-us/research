package org.pmiops.workbench.db.jdbc;

import java.util.List;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;

/** Expose handy, performant queries that don't require Dao, Entity, or Projection classes. */
public interface ReportingQueryService {
  List<ReportingCohort> getCohorts();

  List<ReportingDataset> getDatasets();

  List<ReportingDatasetCohort> getDatasetCohorts();

  List<ReportingDatasetConceptSet> getDatasetConceptSets();

  List<ReportingDatasetDomainIdValue> getDatasetDomainIdValues();

  List<ReportingInstitution> getInstitutions();

  List<ReportingUser> getUsers();

  List<ReportingWorkspace> getWorkspaces();
}
