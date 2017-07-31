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
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortController implements CohortsApiDelegate {

  private static final Logger log = Logger.getLogger(CohortController.class.getName());

  // Converter functions between backend representation (used with Hibernate) and
  // client representation (generated by Swagger).
  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          Cohort result = new Cohort()
              .lastModifiedTime(new DateTime(cohort.getLastModifiedTime(), DateTimeZone.UTC))
              .creationTime(new DateTime(cohort.getCreationTime(), DateTimeZone.UTC))
              .criteria(cohort.getCriteria())
              .description(cohort.getDescription())
              .id(String.valueOf(cohort.getCohortId()))
              .name(cohort.getName())
              .type(cohort.getType());
          if (cohort.getCreator() != null) {
            result.setCreator(cohort.getCreator().getEmail());
          }
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
    try {
      // TODO Make this a pre-check within a transaction?
      dbCohort = cohortDao.save(dbCohort);
    } catch (DataIntegrityViolationException e) {
      // TODO The exception message doesn't show up anywhere; neither logged nor returned to the
      // client by Spring (the client gets a default reason string).
      throw new BadRequestException(
          "Cohort \"/%s/%s/%s\" already exists.".format(
              workspaceNamespace, workspaceId, dbCohort.getCohortId()));
    }
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

  /**
   * Gets or creates a workspace with the given namespace and ID.
   * (In future it will throw NotFoundException if the workspace wasn't created previously.)
   */
  private Workspace getDbWorkspace(String workspaceNamespace, String workspaceId) {
    String firecloudName = Workspace.toFirecloudName(workspaceNamespace, workspaceId);
    Workspace workspace = workspaceDao.findByFirecloudName(firecloudName);
    if (workspace == null) {
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
        cohortDao.findOne(convertCohortId(cohortId));
    if (cohort == null) {
      throw new NotFoundException("No cohort with name {0} in workspace {0}".format(cohortId,
          workspace.getFirecloudName()));
    }
    return cohort;
  }

  private static long convertCohortId(String cohortId) {
    try {
      return Long.parseLong(cohortId);
    } catch (NumberFormatException e) {
      throw new BadRequestException("Invalid cohort ID: {0}".format(cohortId));
    }
  }
}
