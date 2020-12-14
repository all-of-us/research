package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import java.time.Clock;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingSnapshotServiceImpl implements ReportingSnapshotService {
  private static final Logger log = Logger.getLogger(ReportingSnapshotServiceImpl.class.getName());

  private final Clock clock;
  private final ReportingQueryService reportingQueryService;
  private final Provider<Stopwatch> stopwatchProvider;

  public ReportingSnapshotServiceImpl(
      Clock clock,
      ReportingQueryService reportingQueryService,
      Provider<Stopwatch> stopwatchProvider) {
    this.clock = clock;
    this.reportingQueryService = reportingQueryService;
    this.stopwatchProvider = stopwatchProvider;
  }

  // Retrieve all the data we need from the MySQL database in a single transaction for
  // consistency.
  @Transactional(readOnly = true)
  @Override
  public ReportingSnapshot takeSnapshot() {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final ReportingSnapshot result =
        new ReportingSnapshot()
            .cohorts(reportingQueryService.getCohorts())
            .datasets(reportingQueryService.getDatasets())
            .datasetCohorts(reportingQueryService.getDatasetCohorts())
            .datasetConceptSets(reportingQueryService.getDatasetConceptSets())
            .datasetDomainIdValues(reportingQueryService.getDatasetDomainIdValues())
            .institutions(reportingQueryService.getInstitutions())
            .users(reportingQueryService.getUsers())
            .workspaces(reportingQueryService.getWorkspaces());
    stopwatch.stop();
    log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()));
    return result;
  }
}
