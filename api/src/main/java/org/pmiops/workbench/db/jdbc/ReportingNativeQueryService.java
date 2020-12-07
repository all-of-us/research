package org.pmiops.workbench.db.jdbc;

import java.util.List;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;

/** Expose handy, performant queries that don't require Dao, Entity, or Projection classes. */
public interface ReportingNativeQueryService {
  List<ReportingDatasetCohort> getReportingDatasetCohorts();

  List<ReportingDatasetConceptSet> getReportingDatasetConceptSets();

  List<ReportingDatasetDomainIdValue> getReportingDatasetDomainIdValues();
}