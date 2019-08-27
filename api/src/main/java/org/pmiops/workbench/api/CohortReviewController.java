package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AllEvents;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReviewListResponse;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Condition;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Drug;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.Lab;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.Observation;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.PageRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantCohortStatuses;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.PhysicalMeasurement;
import org.pmiops.workbench.model.Procedure;
import org.pmiops.workbench.model.ReviewColumns;
import org.pmiops.workbench.model.ReviewFilter;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Survey;
import org.pmiops.workbench.model.Vital;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortReviewController implements CohortReviewApiDelegate {

  public static final Integer PAGE = 0;
  public static final Integer PAGE_SIZE = 25;
  public static final Integer MAX_REVIEW_SIZE = 10000;
  public static final Integer MIN_LIMIT = 1;
  public static final Integer MAX_LIMIT = 20;
  public static final Integer DEFAULT_LIMIT = 5;
  public static final List<String> GENDER_RACE_ETHNICITY_TYPES =
      ImmutableList.of(
          ParticipantCohortStatusColumns.ETHNICITY.name(),
          ParticipantCohortStatusColumns.GENDER.name(),
          ParticipantCohortStatusColumns.RACE.name());

  private CohortReviewService cohortReviewService;
  private BigQueryService bigQueryService;
  private CohortQueryBuilder cohortQueryBuilder;
  private ReviewQueryBuilder reviewQueryBuilder;
  private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;
  private UserRecentResourceService userRecentResourceService;
  private Provider<User> userProvider;
  private final Clock clock;
  private Provider<WorkbenchConfig> configProvider;
  private static final Logger log = Logger.getLogger(CohortReviewController.class.getName());

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final Function<
          ParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>
      TO_CLIENT_PARTICIPANT =
          new Function<
              ParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>() {
            @Override
            public org.pmiops.workbench.model.ParticipantCohortStatus apply(
                ParticipantCohortStatus participant) {
              return new org.pmiops.workbench.model.ParticipantCohortStatus()
                  .participantId(participant.getParticipantKey().getParticipantId())
                  .status(participant.getStatusEnum())
                  .birthDate(participant.getBirthDate().toString())
                  .ethnicityConceptId(participant.getEthnicityConceptId())
                  .ethnicity(participant.getEthnicity())
                  .genderConceptId(participant.getGenderConceptId())
                  .gender(participant.getGender())
                  .raceConceptId(participant.getRaceConceptId())
                  .race(participant.getRace())
                  .deceased(participant.getDeceased());
            }
          };

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final BiFunction<
          CohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>
      TO_CLIENT_COHORTREVIEW_WITH_PAGING =
          new BiFunction<CohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>() {
            @Override
            public org.pmiops.workbench.model.CohortReview apply(
                CohortReview cohortReview, PageRequest pageRequest) {
              return new org.pmiops.workbench.model.CohortReview()
                  .cohortReviewId(cohortReview.getCohortReviewId())
                  .cohortId(cohortReview.getCohortId())
                  .cdrVersionId(cohortReview.getCdrVersionId())
                  .creationTime(cohortReview.getCreationTime().toString())
                  .cohortDefinition(cohortReview.getCohortDefinition())
                  .cohortName(cohortReview.getCohortName())
                  .description(cohortReview.getDescription())
                  .matchedParticipantCount(cohortReview.getMatchedParticipantCount())
                  .reviewedCount(cohortReview.getReviewedCount())
                  .reviewStatus(cohortReview.getReviewStatusEnum())
                  .reviewSize(cohortReview.getReviewSize())
                  .page(pageRequest.getPage())
                  .pageSize(pageRequest.getPageSize())
                  .sortOrder(pageRequest.getSortOrder().toString())
                  .sortColumn(pageRequest.getSortColumn());
            }
          };

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final Function<CohortReview, org.pmiops.workbench.model.CohortReview>
      TO_CLIENT_COHORTREVIEW =
          new Function<CohortReview, org.pmiops.workbench.model.CohortReview>() {
            @Override
            public org.pmiops.workbench.model.CohortReview apply(CohortReview cohortReview) {
              return new org.pmiops.workbench.model.CohortReview()
                  .cohortReviewId(cohortReview.getCohortReviewId())
                  .etag(Etags.fromVersion(cohortReview.getVersion()))
                  .cohortId(cohortReview.getCohortId())
                  .cdrVersionId(cohortReview.getCdrVersionId())
                  .creationTime(cohortReview.getCreationTime().toString())
                  .lastModifiedTime(cohortReview.getLastModifiedTime().getTime())
                  .cohortDefinition(cohortReview.getCohortDefinition())
                  .cohortName(cohortReview.getCohortName())
                  .description(cohortReview.getDescription())
                  .matchedParticipantCount(cohortReview.getMatchedParticipantCount())
                  .reviewedCount(cohortReview.getReviewedCount())
                  .reviewStatus(cohortReview.getReviewStatusEnum())
                  .reviewSize(cohortReview.getReviewSize());
            }
          };

  private static final Function<
          ParticipantCohortAnnotation, org.pmiops.workbench.db.model.ParticipantCohortAnnotation>
      FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION =
          new Function<
              ParticipantCohortAnnotation,
              org.pmiops.workbench.db.model.ParticipantCohortAnnotation>() {
            @Override
            public org.pmiops.workbench.db.model.ParticipantCohortAnnotation apply(
                ParticipantCohortAnnotation participantCohortAnnotation) {
              return new org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
                  .annotationId(participantCohortAnnotation.getAnnotationId())
                  .cohortAnnotationDefinitionId(
                      participantCohortAnnotation.getCohortAnnotationDefinitionId())
                  .cohortReviewId(participantCohortAnnotation.getCohortReviewId())
                  .participantId(participantCohortAnnotation.getParticipantId())
                  .annotationValueString(participantCohortAnnotation.getAnnotationValueString())
                  .annotationValueEnum(participantCohortAnnotation.getAnnotationValueEnum())
                  .annotationValueDateString(participantCohortAnnotation.getAnnotationValueDate())
                  .annotationValueBoolean(participantCohortAnnotation.getAnnotationValueBoolean())
                  .annotationValueInteger(participantCohortAnnotation.getAnnotationValueInteger());
            }
          };

  private static final Function<
          org.pmiops.workbench.db.model.ParticipantCohortAnnotation, ParticipantCohortAnnotation>
      TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION =
          new Function<
              org.pmiops.workbench.db.model.ParticipantCohortAnnotation,
              ParticipantCohortAnnotation>() {
            @Override
            public ParticipantCohortAnnotation apply(
                org.pmiops.workbench.db.model.ParticipantCohortAnnotation
                    participantCohortAnnotation) {
              String date =
                  participantCohortAnnotation.getAnnotationValueDate() == null
                      ? null
                      : participantCohortAnnotation.getAnnotationValueDate().toString();
              String enumValue =
                  participantCohortAnnotation.getCohortAnnotationEnumValue() == null
                      ? null
                      : participantCohortAnnotation.getCohortAnnotationEnumValue().getName();
              return new ParticipantCohortAnnotation()
                  .annotationId(participantCohortAnnotation.getAnnotationId())
                  .cohortAnnotationDefinitionId(
                      participantCohortAnnotation.getCohortAnnotationDefinitionId())
                  .cohortReviewId(participantCohortAnnotation.getCohortReviewId())
                  .participantId(participantCohortAnnotation.getParticipantId())
                  .annotationValueString(participantCohortAnnotation.getAnnotationValueString())
                  .annotationValueEnum(enumValue)
                  .annotationValueDate(date)
                  .annotationValueBoolean(participantCohortAnnotation.getAnnotationValueBoolean())
                  .annotationValueInteger(participantCohortAnnotation.getAnnotationValueInteger());
            }
          };

  @Autowired
  CohortReviewController(
      CohortReviewService cohortReviewService,
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      ReviewQueryBuilder reviewQueryBuilder,
      Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider,
      UserRecentResourceService userRecentResourceService,
      Provider<User> userProvider,
      Clock clock,
      Provider<WorkbenchConfig> configProvider) {
    this.cohortReviewService = cohortReviewService;
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.reviewQueryBuilder = reviewQueryBuilder;
    this.genderRaceEthnicityConceptProvider = genderRaceEthnicityConceptProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.configProvider = configProvider;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  @VisibleForTesting
  public void setConfigProvider(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  /**
   * Create a cohort review per the specified workspaceId, cohortId, cdrVersionId and size. If
   * participant cohort status data exists for a review or no cohort review exists for
   * cohortReviewId then throw a {@link BadRequestException}.
   *
   * @param workspaceNamespace
   * @param workspaceId
   * @param cohortId
   * @param cdrVersionId
   * @param request
   */
  @Override
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> createCohortReview(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      Long cdrVersionId,
      CreateReviewRequest request) {
    if (request.getSize() <= 0 || request.getSize() > MAX_REVIEW_SIZE) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Cohort Review size must be between %s and %s", 0, MAX_REVIEW_SIZE));
    }

    Cohort cohort = cohortReviewService.findCohort(cohortId);
    // this validates that the user is in the proper workspace
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);
    CohortReview cohortReview = null;
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort, userProvider.get());
      cohortReviewService.saveCohortReview(cohortReview);
    }
    if (cohortReview.getReviewSize() > 0) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
              cohortId, cdrVersionId));
    }

    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildRandomParticipantQuery(
                    new ParticipantCriteria(searchRequest), request.getSize(), 0L)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<ParticipantCohortStatus> participantCohortStatuses =
        createParticipantCohortStatusesList(cohortReview.getCohortReviewId(), result, rm);

    cohortReview
        .reviewSize(participantCohortStatuses.size())
        .reviewStatusEnum(ReviewStatus.CREATED);

    // when saving ParticipantCohortStatuses to the database the long value of birthdate is mutated.
    cohortReviewService.saveFullCohortReview(cohortReview, participantCohortStatuses);

    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(ParticipantCohortStatusColumns.PARTICIPANTID.toString());

    List<ParticipantCohortStatus> paginatedPCS =
        cohortReviewService.findAll(
            cohortReview.getCohortReviewId(), Collections.<Filter>emptyList(), pageRequest);
    lookupGenderRaceEthnicityValues(paginatedPCS);

    org.pmiops.workbench.model.CohortReview responseReview =
        TO_CLIENT_COHORTREVIEW_WITH_PAGING.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(
        paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));
    return ResponseEntity.ok(responseReview);
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      ParticipantCohortAnnotation request) {

    if (request.getCohortReviewId() != cohortReviewId) {
      throw new BadRequestException(
          "Bad Request: request cohort review id must equal path parameter cohort review id.");
    }

    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantCohortAnnotation =
        FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(request);

    participantCohortAnnotation =
        cohortReviewService.saveParticipantCohortAnnotation(
            request.getCohortReviewId(), participantCohortAnnotation);

    return ResponseEntity.ok(
        TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohortReview(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    CohortReview dbCohortReview =
        cohortReviewService.findCohortReview(workspaceNamespace, workspaceId, cohortReviewId);
    cohortReviewService.deleteCohortReview(dbCohortReview);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId) {

    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    // will throw a NotFoundException if participant cohort annotation does not exist
    cohortReviewService.deleteParticipantCohortAnnotation(
        annotationId, cohortReviewId, participantId);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<CohortChartDataListResponse> getCohortChartData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      String domain,
      Integer limit) {
    int chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a chart limit between %d and %d.",
              MIN_LIMIT, MAX_LIMIT));
    }

    CohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    Cohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildDomainChartInfoCounterQuery(
                    new ParticipantCriteria(searchRequest),
                    DomainType.fromValue(domain),
                    chartLimit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    CohortChartDataListResponse response = new CohortChartDataListResponse();
    response.count(cohortReview.getMatchedParticipantCount());
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(
          new CohortChartData()
              .name(bigQueryService.getString(row, rm.get("name")))
              .conceptId(bigQueryService.getLong(row, rm.get("conceptId")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CohortReviewListResponse> getCohortReviewsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    // This also enforces registered auth domain.
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<CohortReview> reviews =
        cohortReviewService.getRequiredWithCohortReviews(workspaceNamespace, workspaceId);
    CohortReviewListResponse response = new CohortReviewListResponse();
    response.setItems(reviews.stream().map(TO_CLIENT_COHORTREVIEW).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantChartDataListResponse> getParticipantChartData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      String domain,
      Integer limit) {
    int chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a chart limit between %d and %d.",
              MIN_LIMIT, MAX_LIMIT));
    }
    CohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    Cohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildChartDataQuery(
                    participantId, DomainType.fromValue(domain), chartLimit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantChartDataListResponse response = new ParticipantChartDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToChartData(rm, row));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(
      String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<org.pmiops.workbench.db.model.ParticipantCohortAnnotation> annotations =
        cohortReviewService.findParticipantCohortAnnotations(cohortReviewId, participantId);

    ParticipantCohortAnnotationListResponse response =
        new ParticipantCohortAnnotationListResponse();
    response.setItems(
        annotations.stream()
            .map(TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION)
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus>
      getParticipantCohortStatus(
          String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    CohortReview review = cohortReviewService.findCohortReview(cohortReviewId);
    Cohort cohort = cohortReviewService.findCohort(review.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    ParticipantCohortStatus status =
        cohortReviewService.findParticipantCohortStatus(review.getCohortReviewId(), participantId);
    lookupGenderRaceEthnicityValues(Arrays.asList(status));
    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(status));
  }

  /**
   * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
   * based on page, pageSize, sortOrder and sortColumn.
   */
  @Override
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> getParticipantCohortStatuses(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      Long cdrVersionId,
      PageFilterRequest request) {
    CohortReview cohortReview = null;
    Cohort cohort = cohortReviewService.findCohort(cohortId);

    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort, userProvider.get());
    }

    PageRequest pageRequest = createPageRequest(request);

    List<Filter> filters =
        request.getFilters() == null
            ? Collections.<Filter>emptyList()
            : request.getFilters().getItems();
    List<ParticipantCohortStatus> participantCohortStatuses =
        cohortReviewService.findAll(
            cohortReview.getCohortReviewId(),
            convertGenderRaceEthnicityFilters(filters),
            convertGenderRaceEthnicitySortOrder(pageRequest));
    lookupGenderRaceEthnicityValues(participantCohortStatuses);

    Long queryResultSize =
        filters.isEmpty()
            ? cohortReview.getReviewSize()
            : cohortReviewService.findCount(
                cohortReview.getCohortReviewId(),
                convertGenderRaceEthnicityFilters(filters),
                convertGenderRaceEthnicitySortOrder(pageRequest));

    org.pmiops.workbench.model.CohortReview responseReview =
        TO_CLIENT_COHORTREVIEW_WITH_PAGING.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(
        participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));
    responseReview.setQueryResultSize(queryResultSize);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    userRecentResourceService.updateCohortEntry(
        cohort.getWorkspaceId(), userProvider.get().getUserId(), cohortId, now);
    return ResponseEntity.ok(responseReview);
  }

  @Override
  public ResponseEntity<ParticipantDataListResponse> getParticipantData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      PageFilterRequest request) {
    CohortReview review = cohortReviewService.findCohortReview(cohortReviewId);
    Cohort cohort = cohortReviewService.findCohort(review.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    // this validates that the participant is in the requested review.
    cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

    DomainType domain = ((ReviewFilter) request).getDomain();
    PageRequest pageRequest = createPageRequest(request);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantDataListResponse response = new ParticipantDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToParticipantData(rm, row, domain));
    }

    if (result.getTotalRows() == pageRequest.getPageSize()) {
      result =
          bigQueryService.executeQuery(
              bigQueryService.filterBigQueryConfig(
                  reviewQueryBuilder.buildCountQuery(participantId, domain)));
      rm = bigQueryService.getResultMapper(result);
      response.count(
          bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count")));
    } else {
      response.count(result.getTotalRows());
    }

    response.setPageRequest(pageRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<VocabularyListResponse> getVocabularies(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    CohortReview review = cohortReviewService.findCohortReview(cohortReviewId);
    Cohort cohort = cohortReviewService.findCohort(review.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(reviewQueryBuilder.buildVocabularyDataQuery()));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    VocabularyListResponse response = new VocabularyListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(
          new Vocabulary()
              .domain(bigQueryService.getString(row, rm.get("domain")))
              .type(bigQueryService.getString(row, rm.get("type")))
              .vocabulary(bigQueryService.getString(row, rm.get("vocabulary"))));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> updateCohortReview(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      org.pmiops.workbench.model.CohortReview cohortReview) {
    // This also enforces registered auth domain.
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    CohortReview dbCohortReview =
        cohortReviewService.findCohortReview(workspaceNamespace, workspaceId, cohortReviewId);
    if (Strings.isNullOrEmpty(cohortReview.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(cohortReview.getEtag());
    if (dbCohortReview.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated cohort review version");
    }
    if (cohortReview.getCohortName() != null) {
      dbCohortReview.setCohortName(cohortReview.getCohortName());
    }
    if (cohortReview.getDescription() != null) {
      dbCohortReview.setDescription(cohortReview.getDescription());
    }
    dbCohortReview.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    try {
      cohortReviewService.saveCohortReview(dbCohortReview);
    } catch (OptimisticLockException e) {
      log.log(Level.WARNING, "version conflict for cohort review update", e);
      throw new ConflictException("Failed due to concurrent cohort review modification");
    }
    return ResponseEntity.ok(TO_CLIENT_COHORTREVIEW.apply(dbCohortReview));
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId,
      ModifyParticipantCohortAnnotationRequest request) {
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantCohortAnnotation =
        cohortReviewService.updateParticipantCohortAnnotation(
            annotationId, cohortReviewId, participantId, request);

    return ResponseEntity.ok(
        TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation));
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus>
      updateParticipantCohortStatus(
          String workspaceNamespace,
          String workspaceId,
          Long cohortReviewId,
          Long participantId,
          ModifyCohortStatusRequest cohortStatusRequest) {
    CohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    Cohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);

    ParticipantCohortStatus participantCohortStatus =
        cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

    participantCohortStatus.setStatusEnum(cohortStatusRequest.getStatus());
    cohortReviewService.saveParticipantCohortStatus(participantCohortStatus);
    lookupGenderRaceEthnicityValues(Arrays.asList(participantCohortStatus));

    cohortReview.lastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    cohortReview.incrementReviewedCount();
    cohortReviewService.saveCohortReview(cohortReview);

    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus));
  }

  /**
   * Helper method to create a new {@link CohortReview}.
   *
   * @param cdrVersionId
   * @param cohort
   * @param creator
   */
  private CohortReview initializeCohortReview(Long cdrVersionId, Cohort cohort, User creator) {
    SearchRequest request = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildParticipantCounterQuery(new ParticipantCriteria(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    long cohortCount = bigQueryService.getLong(row, rm.get("count"));

    return createNewCohortReview(cohort, cdrVersionId, cohortCount, creator);
  }

  /**
   * Helper method that builds a list of {@link ParticipantCohortStatus} from BigQuery results.
   *
   * @param cohortReviewId
   * @param result
   * @param rm
   * @return
   */
  private List<ParticipantCohortStatus> createParticipantCohortStatusesList(
      Long cohortReviewId, TableResult result, Map<String, Integer> rm) {
    List<ParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      String birthDateTimeString = bigQueryService.getString(row, rm.get("birth_datetime"));
      if (birthDateTimeString == null) {
        throw new BigQueryException(
            500, "birth_datetime is null at position: " + rm.get("birth_datetime"));
      }
      java.util.Date birthDate =
          Date.from(Instant.ofEpochMilli(Double.valueOf(birthDateTimeString).longValue() * 1000));
      participantCohortStatuses.add(
          new ParticipantCohortStatus()
              .participantKey(
                  new ParticipantCohortStatusKey(
                      cohortReviewId, bigQueryService.getLong(row, rm.get("person_id"))))
              .statusEnum(CohortStatus.NOT_REVIEWED)
              .birthDate(new Date(birthDate.getTime()))
              .genderConceptId(bigQueryService.getLong(row, rm.get("gender_concept_id")))
              .raceConceptId(bigQueryService.getLong(row, rm.get("race_concept_id")))
              .ethnicityConceptId(bigQueryService.getLong(row, rm.get("ethnicity_concept_id")))
              .deceased(bigQueryService.getBoolean(row, rm.get("deceased"))));
    }
    return participantCohortStatuses;
  }

  /**
   * Helper to method that consolidates access to Cohort Definition. Will throw a {@link
   * NotFoundException} if {@link Cohort#getCriteria()} return null.
   *
   * @param cohort
   * @return
   */
  private String getCohortDefinition(Cohort cohort) {
    String definition = cohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort definition matching cohortId: %s", cohort.getCohortId()));
    }
    return definition;
  }

  /**
   * Helper method that constructs a {@link CohortReview} with the specified ids and count.
   *
   * @param cohort
   * @param cdrVersionId
   * @param cohortCount
   * @param creator
   * @return
   */
  private CohortReview createNewCohortReview(
      Cohort cohort, Long cdrVersionId, Long cohortCount, User creator) {
    return new CohortReview()
        .cohortId(cohort.getCohortId())
        .cohortDefinition(getCohortDefinition(cohort))
        .cohortName(cohort.getName())
        .description(cohort.getDescription())
        .cdrVersionId(cdrVersionId)
        .matchedParticipantCount(cohortCount)
        .creationTime(new Timestamp(clock.instant().toEpochMilli()))
        .lastModifiedTime(new Timestamp(clock.instant().toEpochMilli()))
        .reviewedCount(0L)
        .reviewSize(0L)
        .reviewStatusEnum(ReviewStatus.NONE)
        .creator(creator);
  }

  /**
   * Helper method that will populate all gender, race and ethnicity per the spcecified list of
   * {@link ParticipantCohortStatus}.
   *
   * @param participantCohortStatuses
   */
  private void lookupGenderRaceEthnicityValues(
      List<ParticipantCohortStatus> participantCohortStatuses) {
    Map<String, Map<Long, String>> concepts =
        genderRaceEthnicityConceptProvider.get().getConcepts();
    participantCohortStatuses.forEach(
        pcs -> {
          pcs.setRace(
              concepts.get(ParticipantCohortStatusColumns.RACE.name()).get(pcs.getRaceConceptId()));
          pcs.setGender(
              concepts
                  .get(ParticipantCohortStatusColumns.GENDER.name())
                  .get(pcs.getGenderConceptId()));
          pcs.setEthnicity(
              concepts
                  .get(ParticipantCohortStatusColumns.ETHNICITY.name())
                  .get(pcs.getEthnicityConceptId()));
        });
  }

  /**
   * Helper method that generates a list of concept ids per demo
   *
   * @param filters
   * @return
   */
  private List<Filter> convertGenderRaceEthnicityFilters(List<Filter> filters) {
    return filters.stream()
        .map(
            filter -> {
              if (GENDER_RACE_ETHNICITY_TYPES.contains(filter.getProperty().name())) {
                Map<Long, String> possibleConceptIds =
                    genderRaceEthnicityConceptProvider
                        .get()
                        .getConcepts()
                        .get(filter.getProperty().name());
                List<String> values =
                    possibleConceptIds.entrySet().stream()
                        .filter(entry -> filter.getValues().contains(entry.getValue()))
                        .map(entry -> entry.getKey().toString())
                        .collect(Collectors.toList());
                return new Filter()
                    .property(filter.getProperty())
                    .operator(filter.getOperator())
                    .values(values);
              }
              return filter;
            })
        .collect(Collectors.toList());
  }

  /**
   * Helper method that converts sortOrder if gender, race or ethnicity.
   *
   * @param pageRequest
   * @return
   */
  private PageRequest convertGenderRaceEthnicitySortOrder(PageRequest pageRequest) {
    String sortColumn = pageRequest.getSortColumn();
    if (GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
      Map<String, Map<Long, String>> concepts =
          genderRaceEthnicityConceptProvider.get().getConcepts();
      List<String> demoList =
          concepts.get(sortColumn).entrySet().stream()
              .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
              .sorted(Comparator.comparing(ConceptIdName::getConceptName))
              .map(c -> c.getConceptId().toString())
              .collect(Collectors.toList());
      if (!demoList.isEmpty()) {
        pageRequest.setSortColumn(
            "FIELD("
                + ParticipantCohortStatusDbInfo.fromName(sortColumn).getDbName()
                + ","
                + String.join(",", demoList)
                + ") "
                + pageRequest.getSortOrder().name());
      }
    }
    return pageRequest;
  }

  private PageRequest createPageRequest(PageFilterRequest request) {
    String sortColumn = "";
    if (request instanceof ParticipantCohortStatuses) {
      sortColumn =
          Optional.ofNullable(((ParticipantCohortStatuses) request).getSortColumn())
              .orElse(ParticipantCohortStatusColumns.PARTICIPANTID)
              .toString();
    } else if (request instanceof ReviewFilter) {
      sortColumn =
          Optional.ofNullable(((ReviewFilter) request).getSortColumn())
              .orElse(ReviewColumns.STARTDATE)
              .toString();
    }
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam =
        Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest()
        .page(pageParam)
        .pageSize(pageSizeParam)
        .sortOrder(sortOrderParam)
        .sortColumn(sortColumn);
  }

  /**
   * Helper method to convert a collection of {@link FieldValue} to {@link ParticipantData}.
   *
   * @param rm
   * @param row
   * @param domain
   */
  private ParticipantData convertRowToParticipantData(
      Map<String, Integer> rm, List<FieldValue> row, DomainType domain) {
    if (domain.equals(DomainType.DRUG)) {
      return new Drug()
          .numMentions(bigQueryService.getString(row, rm.get("numMentions")))
          .firstMention(bigQueryService.getDateTime(row, rm.get("firstMention")))
          .lastMention(bigQueryService.getDateTime(row, rm.get("lastMention")))
          .dose(bigQueryService.getString(row, rm.get("dose")))
          .strength(bigQueryService.getString(row, rm.get("strength")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .route(bigQueryService.getString(row, rm.get("route")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.DRUG);
    } else if (domain.equals(DomainType.CONDITION)) {
      return new Condition()
          .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
          .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
          .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
          .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
          .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.CONDITION);
    } else if (domain.equals(DomainType.PROCEDURE)) {
      return new Procedure()
          .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
          .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
          .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
          .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
          .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.PROCEDURE);
    } else if (domain.equals(DomainType.OBSERVATION)) {
      return new Observation()
          .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
          .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
          .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
          .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.OBSERVATION);
    } else if (domain.equals(DomainType.LAB)) {
      return new Lab()
          .value(bigQueryService.getString(row, rm.get("value")))
          .unit(bigQueryService.getString(row, rm.get("unit")))
          .refRange(bigQueryService.getString(row, rm.get("refRange")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.LAB);
    } else if (domain.equals(DomainType.VITAL)) {
      return new Vital()
          .value(bigQueryService.getString(row, rm.get("value")))
          .unit(bigQueryService.getString(row, rm.get("unit")))
          .refRange(bigQueryService.getString(row, rm.get("refRange")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.VITAL);
    } else if (domain.equals(DomainType.PHYSICAL_MEASUREMENT)) {
      return new PhysicalMeasurement()
          .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
          .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
          .value(bigQueryService.getString(row, rm.get("value")))
          .unit(bigQueryService.getString(row, rm.get("unit")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.PHYSICAL_MEASUREMENT);
    } else if (domain.equals(DomainType.SURVEY)) {
      return new Survey()
          .survey(bigQueryService.getString(row, rm.get("survey")))
          .question(bigQueryService.getString(row, rm.get("question")))
          .answer(bigQueryService.getString(row, rm.get("answer")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .domainType(DomainType.SURVEY);
    } else {
      return new AllEvents()
          .domain(bigQueryService.getString(row, rm.get("domain")))
          .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
          .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
          .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
          .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
          .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
          .numMentions(bigQueryService.getString(row, rm.get("numMentions")))
          .firstMention(
              row.get(rm.get("firstMention")).isNull()
                  ? ""
                  : bigQueryService.getDateTime(row, rm.get("firstMention")))
          .lastMention(
              row.get(rm.get("lastMention")).isNull()
                  ? ""
                  : bigQueryService.getDateTime(row, rm.get("lastMention")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .route(bigQueryService.getString(row, rm.get("route")))
          .dose(bigQueryService.getString(row, rm.get("dose")))
          .strength(bigQueryService.getString(row, rm.get("strength")))
          .value(bigQueryService.getString(row, rm.get("value")))
          .unit(bigQueryService.getString(row, rm.get("unit")))
          .refRange(bigQueryService.getString(row, rm.get("refRange")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get("standardConceptId")))
          .sourceConceptId(bigQueryService.getLong(row, rm.get("sourceConceptId")))
          .domainType(DomainType.ALL_EVENTS);
    }
  }

  private ParticipantChartData convertRowToChartData(
      Map<String, Integer> rm, List<FieldValue> row) {
    return new ParticipantChartData()
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .startDate(bigQueryService.getDate(row, rm.get("startDate")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .rank(bigQueryService.getLong(row, rm.get("rank")).intValue());
  }
}
