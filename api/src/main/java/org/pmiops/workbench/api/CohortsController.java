package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortsController implements CohortsApiDelegate {

  private static final int MAX_PAGE_SIZE = 10000;
  private static final int DEFAULT_PAGE_SIZE = 1000;
  private static final Logger log = Logger.getLogger(CohortsController.class.getName());

  /**
   * Converter function from backend representation (used with Hibernate) to
   * client representation (generated by Swagger).
   */
  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          Cohort result = new Cohort()
              .etag(Etags.fromVersion(cohort.getVersion()))
              .lastModifiedTime(cohort.getLastModifiedTime().getTime())
              .creationTime(cohort.getCreationTime().getTime())
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

  private final WorkspaceService workspaceService;
  private final CohortDao cohortDao;
  private final CdrVersionDao cdrVersionDao;
  private final CohortMaterializationService cohortMaterializationService;
  private final Provider<User> userProvider;
  private final Clock clock;

  @Autowired
  CohortsController(
      WorkspaceService workspaceService,
      CohortDao cohortDao,
      CdrVersionDao cdrVersionDao,
      CohortMaterializationService cohortMaterializationService,
      Provider<User> userProvider,
      Clock clock) {
    this.workspaceService = workspaceService;
    this.cohortDao = cohortDao;
    this.cdrVersionDao = cdrVersionDao;
    this.cohortMaterializationService = cohortMaterializationService;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<Cohort> createCohort(String workspaceNamespace, String workspaceId,
      Cohort cohort) {
    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    org.pmiops.workbench.db.model.Cohort dbCohort = FROM_CLIENT_COHORT.apply(cohort);
    dbCohort.setCreator(userProvider.get());
    dbCohort.setWorkspaceId(workspace.getWorkspaceId());
    dbCohort.setCreationTime(now);
    dbCohort.setLastModifiedTime(now);
    dbCohort.setVersion(1);
    try {
      // TODO Make this a pre-check within a transaction?
      dbCohort = cohortDao.save(dbCohort);
    } catch (DataIntegrityViolationException e) {
      // TODO The exception message doesn't show up anywhere; neither logged nor returned to the
      // client by Spring (the client gets a default reason string).
      throw new BadRequestException(String.format(
          "Cohort \"/%s/%s/%s\" already exists.",
          workspaceNamespace, workspaceId, dbCohort.getCohortId()));
    }
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohort(String workspaceNamespace, String workspaceId,
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
    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);
    CohortListResponse response = new CohortListResponse();
    List<org.pmiops.workbench.db.model.Cohort> cohorts = workspace.getCohorts();
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
    if(Strings.isNullOrEmpty(cohort.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(cohort.getEtag());
    if (dbCohort.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated cohort version");
    }
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
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbCohort.setLastModifiedTime(now);
    try {
      // The version asserted on save is the same as the one we read via
      // getRequired() above, see RW-215 for details.
      dbCohort = cohortDao.save(dbCohort);
    } catch (OptimisticLockException e) {
      log.log(Level.WARNING, "version conflict for cohort update", e);
      throw new ConflictException("Failed due to concurrent cohort modification");
    }
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  @Override
  public ResponseEntity<MaterializeCohortResponse> materializeCohort(String workspaceNamespace,
      String workspaceId, MaterializeCohortRequest request) {
    // TODO(danrodney): get list of participant IDs by:
    // 1. Retrieve participant cohort statuses matching the status filter.
    // 2. If the status filter does not contain NOT_REVIEWED, or the cohort review contains
    // all participant IDs in the cohort, return the IDs directly (subject to pagination.)
    // 3. Otherwise, query BigQuery for participant IDs using SQL constructed from the cohort
    // criteria, subject to pagination; remove IDs for participants that had a cohort status not
    // included in the status filter.

    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);
    CdrVersion cdrVersion = workspace.getCdrVersion();
    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new BadRequestException(String.format("Couldn't find CDR version with name %s",
            request.getCdrVersionName()));
      }
    }
    String cohortSpec;
    if (request.getCohortName() != null) {
      org.pmiops.workbench.db.model.Cohort cohort =
          cohortDao.findCohortByNameAndWorkspaceId(request.getCohortName(), workspace.getWorkspaceId());
      if (cohort == null) {
        throw new BadRequestException(
            String.format("Couldn't find cohort with name %s in workspace %s/%s",
                request.getCohortName(), workspaceNamespace, workspaceId));
      }
      cohortSpec = cohort.getCriteria();
    } else if (request.getCohortSpec() != null) {
      cohortSpec = request.getCohortSpec();
    } else {
      throw new BadRequestException("Must specify either cohortName or cohortSpec");
    }
    Integer pageSize = request.getPageSize();
    if (pageSize == null) {
      pageSize = DEFAULT_PAGE_SIZE;
    } else {
      if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
        throw new BadRequestException(
            String.format("Invalid page size: %s; must be between 1 and %d", pageSize,
                MAX_PAGE_SIZE));
      }
    }

    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(
        cdrVersion, cohortSpec, request.getStatusFilter(), pageSize,
        request.getPageToken());
    return ResponseEntity.ok(response);
  }

  private org.pmiops.workbench.db.model.Cohort getDbCohort(String workspaceNamespace,
      String workspaceId, String cohortId) {
    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    org.pmiops.workbench.db.model.Cohort cohort =
        cohortDao.findOne(convertCohortId(cohortId));
    if (cohort == null) {
      throw new NotFoundException(String.format(
          "No cohort with name %s in workspace %s.", cohortId, workspace.getFirecloudName()));
    }
    return cohort;
  }

  private static long convertCohortId(String cohortId) {
    try {
      return Long.parseLong(cohortId);
    } catch (NumberFormatException e) {
      throw new BadRequestException(String.format("Invalid cohort ID: %s", cohortId));
    }
  }
}
