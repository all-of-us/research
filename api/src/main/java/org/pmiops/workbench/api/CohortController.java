package org.pmiops.workbench.api;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortController implements CohortsApiDelegate {

  private static final Logger log = Logger.getLogger(CohortController.class.getName());

  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          Cohort result = new Cohort();
          result.setLastModifiedTime(new DateTime(cohort.getLastModifiedTime(), DateTimeZone.UTC));
          if (cohort.getCreator() != null) {
            result.setCreator(cohort.getCreator().getEmail());
          }
          result.setCriteria(cohort.getCriteria());
          result.setCreationTime(new DateTime(cohort.getCreationTime(), DateTimeZone.UTC));
          result.setDescription(cohort.getDescription());
          result.setId(cohort.getExternalId());
          result.setName(cohort.getName());
          result.setType(cohort.getType());
          return result;
        }
      };

  private static final Function<Cohort, org.pmiops.workbench.db.model.Cohort> FROM_CLIENT_COHORT =
      new Function<Cohort, org.pmiops.workbench.db.model.Cohort>() {
        @Override
        public org.pmiops.workbench.db.model.Cohort apply(Cohort cohort) {
          org.pmiops.workbench.db.model.Cohort result = new org.pmiops.workbench.db.model.Cohort();
          result.setCriteria(cohort.getCriteria());
          result.setDescription(cohort.getDescription());
          result.setExternalId(cohort.getId());
          result.setName(cohort.getName());
          result.setType(cohort.getType());
          return result;
        }
      };

  private final WorkspaceDao workspaceDao;
  private final CohortDao cohortDao;
  private final Provider<User> userProvider;
  private final Clock clock;

  @Autowired
  CohortController(WorkspaceDao workspaceDao, CohortDao cohortDao, Provider<User> userProvider,
      Clock clock) {
    this.workspaceDao = workspaceDao;
    this.cohortDao = cohortDao;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<Cohort> createCohort(String workspaceNamespace, String workspaceId,
      Cohort cohort) {
    Workspace workspace = getDbWorkspace(workspaceNamespace, workspaceId);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    org.pmiops.workbench.db.model.Cohort dbCohort = FROM_CLIENT_COHORT.apply(cohort);
    dbCohort.setCreator(userProvider.get());
    dbCohort.setWorkspaceId(workspace.getWorkspaceId());
    dbCohort.setCreationTime(now);
    dbCohort.setLastModifiedTime(now);
    dbCohort = cohortDao.save(dbCohort);
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  @Override
  public ResponseEntity<Void> deleteCohort(String workspaceNamespace, String workspaceId,
      String cohortId) {
    org.pmiops.workbench.db.model.Cohort dbCohort = getDbCohort(workspaceNamespace, workspaceId,
        cohortId);
    cohortDao.delete(dbCohort);
    return ResponseEntity.ok(null);
  }

  @Override
  public ResponseEntity<Cohort> getCohort(String workspaceNamespace, String workspaceId,
      String cohortId) {
    org.pmiops.workbench.db.model.Cohort dbCohort = getDbCohort(workspaceNamespace, workspaceId,
        cohortId);
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  @Override
  public ResponseEntity<CohortListResponse> getCohortsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    Workspace workspace = getDbWorkspace(workspaceNamespace, workspaceId);
    CohortListResponse response = new CohortListResponse();
    Set<org.pmiops.workbench.db.model.Cohort> cohorts = workspace.getCohorts();
    if (cohorts != null) {
      response.setItems(cohorts.stream().map(TO_CLIENT_COHORT).collect(Collectors.toList()));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Cohort> updateCohort(String workspaceNamespace, String workspaceId,
      String cohortId, Cohort cohort) {
    org.pmiops.workbench.db.model.Cohort dbCohort = getDbCohort(workspaceNamespace, workspaceId,
        cohortId);
    if (cohort.getType() != null) {
      dbCohort.setType(cohort.getType());
    }
    if (cohort.getName() != null) {
      dbCohort.setName(cohort.getName());
    }
    if (cohort.getDescription() != null) {
      dbCohort.setDescription(cohort.getDescription());
    }
    if (cohort.getCriteria() != null) {
      dbCohort.setCriteria(cohort.getCriteria());
    }
    // TODO: add version, check it here
    dbCohort = cohortDao.save(dbCohort);
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  private Workspace getDbWorkspace(String workspaceNamespace, String workspaceId) {
    String firecloudName = Workspace.toFirecloudName(workspaceNamespace, workspaceId);
    Workspace workspace = workspaceDao.findByFirecloudName(firecloudName);
    if (workspace == null) {
      // Create a workspace if it doesn't already exist.
      // TODO: get rid of this after creating workspace API
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      workspace = new Workspace();
      workspace.setCreationTime(now);
      workspace.setCreator(userProvider.get());
      workspace.setDataAccessLevel(DataAccessLevel.REGISTERED);
      workspace.setLastModifiedTime(now);
      workspace.setFirecloudName(firecloudName);
      workspace.setName(workspaceId);

      workspaceDao.save(workspace);

      //throw new NotFoundException("No workspace with name {0}".format(firecloudName));
    }
    return workspace;
  }

  private org.pmiops.workbench.db.model.Cohort getDbCohort(String workspaceName,
      String workspaceId, String cohortId) {
    Workspace workspace = getDbWorkspace(workspaceName, workspaceId);
    org.pmiops.workbench.db.model.Cohort cohort =
        cohortDao.findByWorkspaceIdAndExternalId(workspace.getWorkspaceId(), cohortId);
    if (cohort == null) {
      throw new NotFoundException("No cohort with name {0} in workspace {0}".format(cohortId,
          workspace.getFirecloudName()));
    }
    return cohort;
  }
}
