package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
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
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilderOld;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AllEvents;
import org.pmiops.workbench.model.AllEventsOld;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Condition;
import org.pmiops.workbench.model.ConditionOld;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Drug;
import org.pmiops.workbench.model.DrugOld;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.Lab;
import org.pmiops.workbench.model.MeasurementOld;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.Observation;
import org.pmiops.workbench.model.ObservationOld;
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
import org.pmiops.workbench.model.ParticipantDataListResponseOld;
import org.pmiops.workbench.model.ParticipantDataOld;
import org.pmiops.workbench.model.PhysicalMeasurement;
import org.pmiops.workbench.model.PhysicalMeasurementOld;
import org.pmiops.workbench.model.Procedure;
import org.pmiops.workbench.model.ProcedureOld;
import org.pmiops.workbench.model.ReviewColumns;
import org.pmiops.workbench.model.ReviewFilter;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Survey;
import org.pmiops.workbench.model.Vital;
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
    ImmutableList.of(ParticipantCohortStatusColumns.ETHNICITY.name(),
      ParticipantCohortStatusColumns.GENDER.name(),
      ParticipantCohortStatusColumns.RACE.name());

  private CohortReviewService cohortReviewService;
  private BigQueryService bigQueryService;
  private ParticipantCounter participantCounter;
  private ReviewQueryBuilderOld reviewQueryBuilderOld;
  private ReviewQueryBuilder reviewQueryBuilder;
  private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;
  private UserRecentResourceService userRecentResourceService;
  private Provider<User> userProvider;
  private final Clock clock;

  /**
   * Converter function from backend representation (used with Hibernate) to
   * client representation (generated by Swagger).
   */
  private static final Function<ParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>
    TO_CLIENT_PARTICIPANT =
    new Function<ParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>() {
      @Override
      public org.pmiops.workbench.model.ParticipantCohortStatus apply(ParticipantCohortStatus participant) {
        return new org.pmiops.workbench.model.ParticipantCohortStatus()
          .participantId(participant.getParticipantKey().getParticipantId())
          .status(participant.getStatusEnum())
          .birthDate(participant.getBirthDate().toString())
          .ethnicityConceptId(participant.getEthnicityConceptId())
          .ethnicity(participant.getEthnicity())
          .genderConceptId(participant.getGenderConceptId())
          .gender(participant.getGender())
          .raceConceptId(participant.getRaceConceptId())
          .race(participant.getRace());
      }
    };

  /**
   * Converter function from backend representation (used with Hibernate) to
   * client representation (generated by Swagger).
   */
  private static final BiFunction<CohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>
    TO_CLIENT_COHORTREVIEW =
    new BiFunction<CohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>() {
      @Override
      public org.pmiops.workbench.model.CohortReview apply(CohortReview cohortReview, PageRequest pageRequest) {
        return new org.pmiops.workbench.model.CohortReview()
          .cohortReviewId(cohortReview.getCohortReviewId())
          .cohortId(cohortReview.getCohortId())
          .cdrVersionId(cohortReview.getCdrVersionId())
          .creationTime(cohortReview.getCreationTime().toString())
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

  private static final Function<ParticipantCohortAnnotation, org.pmiops.workbench.db.model.ParticipantCohortAnnotation>
    FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION =
    new Function<ParticipantCohortAnnotation, org.pmiops.workbench.db.model.ParticipantCohortAnnotation>() {
      @Override
      public org.pmiops.workbench.db.model.ParticipantCohortAnnotation apply(ParticipantCohortAnnotation participantCohortAnnotation) {
        return new org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
          .annotationId(participantCohortAnnotation.getAnnotationId())
          .cohortAnnotationDefinitionId(participantCohortAnnotation.getCohortAnnotationDefinitionId())
          .cohortReviewId(participantCohortAnnotation.getCohortReviewId())
          .participantId(participantCohortAnnotation.getParticipantId())
          .annotationValueString(participantCohortAnnotation.getAnnotationValueString())
          .annotationValueEnum(participantCohortAnnotation.getAnnotationValueEnum())
          .annotationValueDateString(participantCohortAnnotation.getAnnotationValueDate())
          .annotationValueBoolean(participantCohortAnnotation.getAnnotationValueBoolean())
          .annotationValueInteger(participantCohortAnnotation.getAnnotationValueInteger());
      }
    };

  private static final Function<org.pmiops.workbench.db.model.ParticipantCohortAnnotation, ParticipantCohortAnnotation>
    TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION =
    new Function<org.pmiops.workbench.db.model.ParticipantCohortAnnotation, ParticipantCohortAnnotation>() {
      @Override
      public ParticipantCohortAnnotation apply(org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantCohortAnnotation) {
        String date = participantCohortAnnotation.getAnnotationValueDate() == null ? null :
          participantCohortAnnotation.getAnnotationValueDate().toString();
        String enumValue = participantCohortAnnotation.getCohortAnnotationEnumValue() == null ?
          null : participantCohortAnnotation.getCohortAnnotationEnumValue().getName();
        return new ParticipantCohortAnnotation()
          .annotationId(participantCohortAnnotation.getAnnotationId())
          .cohortAnnotationDefinitionId(participantCohortAnnotation.getCohortAnnotationDefinitionId())
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
  CohortReviewController(CohortReviewService cohortReviewService,
                         BigQueryService bigQueryService,
                         ParticipantCounter participantCounter,
                         ReviewQueryBuilderOld reviewQueryBuilderOld,
                         ReviewQueryBuilder reviewQueryBuilder,
                         Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider,
                         UserRecentResourceService userRecentResourceService,
                         Provider<User> userProvider,
                         Clock clock) {
    this.cohortReviewService = cohortReviewService;
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
    this.reviewQueryBuilderOld = reviewQueryBuilderOld;
    this.reviewQueryBuilder = reviewQueryBuilder;
    this.genderRaceEthnicityConceptProvider = genderRaceEthnicityConceptProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  /**
   * Create a cohort review per the specified workspaceId, cohortId, cdrVersionId and size. If participant cohort status
   * data exists for a review or no cohort review exists for cohortReviewId then throw a
   * {@link BadRequestException}.
   *
   * @param workspaceNamespace
   * @param workspaceId
   * @param cohortId
   * @param cdrVersionId
   * @param request
   */
  @Override
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> createCohortReview(String workspaceNamespace,
                                                                                    String workspaceId,
                                                                                    Long cohortId,
                                                                                    Long cdrVersionId,
                                                                                    CreateReviewRequest request) {
    if (request.getSize() <= 0 || request.getSize() > MAX_REVIEW_SIZE) {
      throw new BadRequestException(
        String.format("Bad Request: Cohort Review size must be between %s and %s", 0, MAX_REVIEW_SIZE));
    }

    Cohort cohort = cohortReviewService.findCohort(cohortId);
    //this validates that the user is in the proper workspace
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(workspaceNamespace,
        workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);
    CohortReview cohortReview = null;
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort)
        .reviewStatusEnum(ReviewStatus.NONE)
        .reviewSize(0L);
      cohortReviewService.saveCohortReview(cohortReview);
    }
    if (cohortReview.getReviewSize() > 0) {
      throw new BadRequestException(
        String.format("Bad Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
          cohortId, cdrVersionId));
    }

    SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      participantCounter.buildParticipantIdQuery(new ParticipantCriteria(searchRequest),
        request.getSize(), 0L)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<ParticipantCohortStatus> participantCohortStatuses =
      createParticipantCohortStatusesList(cohortReview.getCohortReviewId(), result, rm);

    cohortReview
      .reviewSize(participantCohortStatuses.size())
      .reviewStatusEnum(ReviewStatus.CREATED);

    //when saving ParticipantCohortStatuses to the database the long value of birthdate is mutated.
    cohortReviewService.saveFullCohortReview(cohortReview, participantCohortStatuses);

    PageRequest pageRequest = new PageRequest()
      .page(PAGE)
      .pageSize(PAGE_SIZE)
      .sortOrder(SortOrder.ASC)
      .sortColumn(ParticipantCohortStatusColumns.PARTICIPANTID.toString());

    List<ParticipantCohortStatus> paginatedPCS =
      cohortReviewService.findAll(cohortReview.getCohortReviewId(),
        Collections.<Filter>emptyList(),
        pageRequest);
    lookupGenderRaceEthnicityValues(paginatedPCS);

    org.pmiops.workbench.model.CohortReview responseReview =
      TO_CLIENT_COHORTREVIEW.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));
    return ResponseEntity.ok(responseReview);
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(String workspaceNamespace,
                                                                                       String workspaceId,
                                                                                       Long cohortId,
                                                                                       Long cdrVersionId,
                                                                                       Long participantId,
                                                                                       ParticipantCohortAnnotation request) {

    if (request.getCohortAnnotationDefinitionId() == null) {
      throw new BadRequestException("Bad Request: Please provide a valid cohort annotation definition id.");
    }
    if (request.getCohortReviewId() == null) {
      throw new BadRequestException("Bad Request: Please provide a valid cohort review id.");
    }
    if (request.getParticipantId() == null) {
      throw new BadRequestException("Bad Request: Please provide a valid participant id.");
    }

    validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantCohortAnnotation =
      FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(request);

    participantCohortAnnotation = cohortReviewService.saveParticipantCohortAnnotation(request.getCohortReviewId(), participantCohortAnnotation);

    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(String workspaceNamespace,
                                                                         String workspaceId,
                                                                         Long cohortId,
                                                                         Long cdrVersionId,
                                                                         Long participantId,
                                                                         Long annotationId) {

    if (annotationId == null) {
      throw new BadRequestException("Bad Request: Please provide a valid cohort annotation definition id.");
    }
    if (participantId == null) {
      throw new BadRequestException("Bad Request: Please provide a valid participant id.");
    }

    CohortReview cohortReview = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.WRITER);

    //will throw a NotFoundException if participant does not exist
    cohortReviewService.findParticipantCohortStatus(cohortReview.getCohortReviewId(), participantId);

    //will throw a NotFoundException if participant cohort annotation does not exist
    cohortReviewService.deleteParticipantCohortAnnotation(annotationId, cohortReview.getCohortReviewId(), participantId);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<CohortChartDataListResponse> getCohortChartData(String workspaceNamespace,
                                                                        String workspaceId,
                                                                        Long cohortId,
                                                                        Long cdrVersionId,
                                                                        String domain,
                                                                        Integer limit) {
    int chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
      throw new BadRequestException(
        String.format("Bad Request: Please provide a chart limit between %d and %d.", MIN_LIMIT, MAX_LIMIT));
    }
    Cohort cohort = cohortReviewService.findCohort(cohortId);
    CohortReview cohortReview = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

    SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      participantCounter.buildDomainChartInfoCounterQuery(new ParticipantCriteria(searchRequest), DomainType.fromValue(domain), chartLimit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    CohortChartDataListResponse response = new CohortChartDataListResponse();
    response.count(cohortReview.getMatchedParticipantCount());
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(new CohortChartData()
        .name(bigQueryService.getString(row, rm.get("name")))
        .conceptId(bigQueryService.getLong(row, rm.get("conceptId")))
        .count(bigQueryService.getLong(row, rm.get("count"))));
    }

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantChartDataListResponse> getParticipantChartData(String workspaceNamespace,
                                                                                  String workspaceId,
                                                                                  Long cohortId,
                                                                                  Long cdrVersionId,
                                                                                  Long participantId,
                                                                                  String domain,
                                                                                  Integer limit) {
    int chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
      throw new BadRequestException(
        String.format("Bad Request: Please provide a chart limit between %d and %d.", MIN_LIMIT, MAX_LIMIT));
    }
    validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      reviewQueryBuilder.buildChartDataQuery(participantId, DomainType.fromValue(domain), chartLimit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantChartDataListResponse response = new ParticipantChartDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToChartData(rm, row));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(String workspaceNamespace,
                                                                                                 String workspaceId,
                                                                                                 Long cohortId,
                                                                                                 Long cdrVersionId,
                                                                                                 Long participantId) {
    CohortReview review = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

    List<org.pmiops.workbench.db.model.ParticipantCohortAnnotation> annotations =
      cohortReviewService.findParticipantCohortAnnotations(review.getCohortReviewId(), participantId);

    ParticipantCohortAnnotationListResponse response = new ParticipantCohortAnnotationListResponse();
    response.setItems(annotations.stream().map(TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> getParticipantCohortStatus(String workspaceNamespace,
                                                                                                       String workspaceId,
                                                                                                       Long cohortId,
                                                                                                       Long cdrVersionId,
                                                                                                       Long participantId) {
    CohortReview review = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

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
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> getParticipantCohortStatuses(String workspaceNamespace,
                                                                                              String workspaceId,
                                                                                              Long cohortId,
                                                                                              Long cdrVersionId,
                                                                                              PageFilterRequest request) {
    CohortReview cohortReview = null;
    Cohort cohort = cohortReviewService.findCohort(cohortId);

    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(workspaceNamespace, workspaceId,
        cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort);
    }

    PageRequest pageRequest = createPageRequest(request);

    List<Filter> filters = request.getFilters() == null ? Collections.<Filter>emptyList() : request.getFilters().getItems();
    List<ParticipantCohortStatus> participantCohortStatuses =
      cohortReviewService.findAll(cohortReview.getCohortReviewId(),
        convertGenderRaceEthnicityFilters(filters),
        convertGenderRaceEthnicitySortOrder(pageRequest));
    lookupGenderRaceEthnicityValues(participantCohortStatuses);

    Long queryResultSize = filters.isEmpty() ? cohortReview.getReviewSize() :
      cohortReviewService.findCount(cohortReview.getCohortReviewId(),
        convertGenderRaceEthnicityFilters(filters),
        convertGenderRaceEthnicitySortOrder(pageRequest));

    org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(
      participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));
    responseReview.setQueryResultSize(queryResultSize);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    userRecentResourceService.updateCohortEntry(cohort.getWorkspaceId(), userProvider.get().getUserId(), cohortId, now );
    return ResponseEntity.ok(responseReview);
  }

  /**
   * TODO: delete this when UI work is done.
   * @param workspaceNamespace
   * @param workspaceId
   * @param cohortId
   * @param cdrVersionId
   * @param participantId
   * @param request
   * @return
   */
  @Override
  public ResponseEntity<ParticipantDataListResponseOld> getParticipantDataOld(String workspaceNamespace, String workspaceId, Long cohortId, Long cdrVersionId, Long participantId, PageFilterRequest request) {
    CohortReview review = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

    //this validates that the participant is in the requested review.
    cohortReviewService.findParticipantCohortStatus(review.getCohortReviewId(), participantId);

    DomainType domain = ((ReviewFilter) request).getDomain();
    PageRequest pageRequest = createPageRequest(request);

    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      reviewQueryBuilderOld.buildQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantDataListResponseOld response = new ParticipantDataListResponseOld();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToParticipantDataOld(rm, row, domain));
    }

    if (result.getTotalRows() == pageRequest.getPageSize()) {
      result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
        reviewQueryBuilderOld.buildCountQuery(participantId, domain)));
      rm = bigQueryService.getResultMapper(result);
      response.count(bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count")));
    } else {
      response.count(result.getTotalRows());
    }

    response.setPageRequest(pageRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantDataListResponse> getParticipantData(String workspaceNamespace,
                                                                        String workspaceId,
                                                                        Long cohortId,
                                                                        Long cdrVersionId,
                                                                        Long participantId,
                                                                        PageFilterRequest request) {
    CohortReview review = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

    //this validates that the participant is in the requested review.
    cohortReviewService.findParticipantCohortStatus(review.getCohortReviewId(), participantId);

    DomainType domain = ((ReviewFilter) request).getDomain();
    PageRequest pageRequest = createPageRequest(request);

    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      reviewQueryBuilder.buildQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantDataListResponse response = new ParticipantDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToParticipantData(rm, row, domain));
    }

    if (result.getTotalRows() == pageRequest.getPageSize()) {
      result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
        reviewQueryBuilder.buildCountQuery(participantId, domain)));
      rm = bigQueryService.getResultMapper(result);
      response.count(bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count")));
    } else {
      response.count(result.getTotalRows());
    }

    response.setPageRequest(pageRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(String workspaceNamespace,
                                                                                       String workspaceId,
                                                                                       Long cohortId,
                                                                                       Long cdrVersionId,
                                                                                       Long participantId,
                                                                                       Long annotationId,
                                                                                       ModifyParticipantCohortAnnotationRequest request) {
    CohortReview cohortReview = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantCohortAnnotation =
      cohortReviewService.updateParticipantCohortAnnotation(annotationId, cohortReview.getCohortReviewId(), participantId, request);

    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation));
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> updateParticipantCohortStatus(String workspaceNamespace,
                                                                                                          String workspaceId,
                                                                                                          Long cohortId,
                                                                                                          Long cdrVersionId,
                                                                                                          Long participantId,
                                                                                                          ModifyCohortStatusRequest cohortStatusRequest) {
    CohortReview cohortReview = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.WRITER);

    ParticipantCohortStatus participantCohortStatus
      = cohortReviewService.findParticipantCohortStatus(cohortReview.getCohortReviewId(), participantId);

    participantCohortStatus.setStatusEnum(cohortStatusRequest.getStatus());
    cohortReviewService.saveParticipantCohortStatus(participantCohortStatus);
    lookupGenderRaceEthnicityValues(Arrays.asList(participantCohortStatus));

    cohortReview.lastModifiedTime(new Timestamp(System.currentTimeMillis()));
    cohortReview.incrementReviewedCount();
    cohortReviewService.saveCohortReview(cohortReview);

    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus));
  }

  private CohortReview validateRequestAndSetCdrVersion(String workspaceNamespace,
                                                       String workspaceId,
                                                       Long cohortId,
                                                       Long cdrVersionId,
                                                       WorkspaceAccessLevel level) {
    Cohort cohort = cohortReviewService.findCohort(cohortId);
    //this validates that the user is in the proper workspace
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(workspaceNamespace,
        workspaceId, cohort.getWorkspaceId(), level);

    return cohortReviewService.findCohortReview(cohort.getCohortId(), cdrVersionId);
  }

  /**
   * Helper method to create a new {@link CohortReview}.
   *
   * @param cdrVersionId
   * @param cohort
   */
  private CohortReview initializeCohortReview(Long cdrVersionId, Cohort cohort) {
    SearchRequest request = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result = bigQueryService.executeQuery(
      bigQueryService.filterBigQueryConfig(participantCounter.buildParticipantCounterQuery(
        new ParticipantCriteria(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    long cohortCount = bigQueryService.getLong(row, rm.get("count"));

    return createNewCohortReview(cohort.getCohortId(), cdrVersionId, cohortCount);
  }

  /**
   * Helper method that builds a list of {@link ParticipantCohortStatus} from BigQuery results.
   *
   * @param cohortReviewId
   * @param result
   * @param rm
   * @return
   */
  private List<ParticipantCohortStatus> createParticipantCohortStatusesList(Long cohortReviewId,
                                                                            TableResult result,
                                                                            Map<String, Integer> rm) {
    List<ParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      String birthDateTimeString = bigQueryService.getString(row, rm.get("birth_datetime"));
      if (birthDateTimeString == null) {
        throw new BigQueryException(500, "birth_datetime is null at position: " + rm.get("birth_datetime"));
      }
      java.util.Date birthDate = Date.from(Instant.ofEpochMilli(Double.valueOf(birthDateTimeString).longValue() * 1000));
      participantCohortStatuses.add(
        new ParticipantCohortStatus()
          .participantKey(new ParticipantCohortStatusKey(cohortReviewId, bigQueryService.getLong(row, rm.get("person_id"))))
          .statusEnum(CohortStatus.NOT_REVIEWED)
          .birthDate(new Date(birthDate.getTime()))
          .genderConceptId(bigQueryService.getLong(row, rm.get("gender_concept_id")))
          .raceConceptId(bigQueryService.getLong(row, rm.get("race_concept_id")))
          .ethnicityConceptId(bigQueryService.getLong(row, rm.get("ethnicity_concept_id"))));
    }
    return participantCohortStatuses;
  }

  /**
   * Helper to method that consolidates access to Cohort Definition. Will throw a
   * {@link NotFoundException} if {@link Cohort#getCriteria()} return null.
   *
   * @param cohort
   * @return
   */
  private String getCohortDefinition(Cohort cohort) {
    String definition = cohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
        String.format("Not Found: No Cohort definition matching cohortId: %s", cohort.getCohortId()));
    }
    return definition;
  }

  /**
   * Helper method that constructs a {@link CohortReview} with the specified ids and count.
   *
   * @param cohortId
   * @param cdrVersionId
   * @param cohortCount
   * @return
   */
  private CohortReview createNewCohortReview(Long cohortId, Long cdrVersionId, long cohortCount) {
    CohortReview cohortReview = new CohortReview();
    cohortReview.setCohortId(cohortId);
    cohortReview.setCdrVersionId(cdrVersionId);
    cohortReview.matchedParticipantCount(cohortCount);
    cohortReview.setCreationTime(new Timestamp(System.currentTimeMillis()));
    cohortReview.reviewedCount(0L);
    cohortReview.reviewStatusEnum(ReviewStatus.NONE);
    return cohortReview;
  }

  /**
   * Helper method that will populate all gender, race and ethnicity per the spcecified list of
   * {@link ParticipantCohortStatus}.
   *
   * @param participantCohortStatuses
   */
  private void lookupGenderRaceEthnicityValues(List<ParticipantCohortStatus> participantCohortStatuses) {
    Map<String, Map<Long, String>> concepts = genderRaceEthnicityConceptProvider.get().getConcepts();
    participantCohortStatuses.forEach(pcs -> {
      pcs.setRace(concepts.get(ParticipantCohortStatusColumns.RACE.name()).get(pcs.getRaceConceptId()));
      pcs.setGender(concepts.get(ParticipantCohortStatusColumns.GENDER.name()).get(pcs.getGenderConceptId()));
      pcs.setEthnicity(concepts.get(ParticipantCohortStatusColumns.ETHNICITY.name()).get(pcs.getEthnicityConceptId()));
    });
  }

  /**
   * Helper method that generates a list of concept ids per demo
   * @param filters
   * @return
   */
  private List<Filter> convertGenderRaceEthnicityFilters(List<Filter> filters) {
    return filters.stream()
      .map(filter -> {
        if (GENDER_RACE_ETHNICITY_TYPES.contains(filter.getProperty().name())) {
          Map<Long, String> possibleConceptIds =
            genderRaceEthnicityConceptProvider.get().getConcepts().get(filter.getProperty().name());
          List<String> values = possibleConceptIds.entrySet().stream()
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
   * @param pageRequest
   * @return
   */
  private PageRequest convertGenderRaceEthnicitySortOrder(PageRequest pageRequest) {
    String sortColumn = pageRequest.getSortColumn();
    if (GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
      Map<String, Map<Long, String>> concepts = genderRaceEthnicityConceptProvider.get().getConcepts();
      List<String> demoList =
        concepts.get(sortColumn).entrySet().stream()
          .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
          .sorted(Comparator.comparing(ConceptIdName::getConceptName))
          .map(c -> c.getConceptId().toString())
          .collect(Collectors.toList());
      if (!demoList.isEmpty()) {
        pageRequest.setSortColumn("FIELD(" + ParticipantCohortStatusDbInfo.fromName(sortColumn).getDbName() + ","
          + String.join(",", demoList) + ") " + pageRequest.getSortOrder().name());
      }
    }
    return pageRequest;
  }

  private PageRequest createPageRequest(PageFilterRequest request) {
    String sortColumn = "";
    if (request instanceof ParticipantCohortStatuses) {
      sortColumn = Optional.ofNullable(((ParticipantCohortStatuses) request).getSortColumn())
        .orElse(ParticipantCohortStatusColumns.PARTICIPANTID).toString();
    } else if (request instanceof ReviewFilter) {
      sortColumn = Optional.ofNullable(((ReviewFilter) request).getSortColumn())
        .orElse(ReviewColumns.STARTDATE).toString();
    }
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
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
  private ParticipantData convertRowToParticipantData(Map<String, Integer> rm,
                                                      List<FieldValue> row,
                                                      DomainType domain) {
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
        .visitId(bigQueryService.getLong(row, rm.get("visitId")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.OBSERVATION);
    } else if (domain.equals(DomainType.LAB)) {
      return new Lab()
        .value(bigQueryService.getFloat(row, rm.get("value")))
        .unit(bigQueryService.getString(row, rm.get("unit")))
        .refRange(bigQueryService.getString(row, rm.get("refRange")))
        .visitType(bigQueryService.getString(row, rm.get("visitType")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .domainType(DomainType.MEASUREMENT);
    } else if (domain.equals(DomainType.VITAL)) {
      return new Vital()
        .value(bigQueryService.getFloat(row, rm.get("value")))
        .unit(bigQueryService.getString(row, rm.get("unit")))
        .refRange(bigQueryService.getString(row, rm.get("refRange")))
        .visitType(bigQueryService.getString(row, rm.get("visitType")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .domainType(DomainType.MEASUREMENT);
    } else if(domain.equals(DomainType.PHYSICAL_MEASURE)) {
      return new PhysicalMeasurement()
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .value(bigQueryService.getFloat(row, rm.get("value")))
        .unit(bigQueryService.getString(row, rm.get("unit")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .domainType(DomainType.PHYSICAL_MEASURE);
    } else if (domain.equals(DomainType.SURVEY)) {
      return new Survey()
        .survey(bigQueryService.getString(row, rm.get("survey")))
        .question(bigQueryService.getString(row, rm.get("question")))
        .answer(bigQueryService.getString(row, rm.get("answer")))
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
          .firstMention(row.get(rm.get("firstMention")).isNull() ? "" : bigQueryService.getDateTime(row, rm.get("firstMention")))
          .lastMention(row.get(rm.get("lastMention")).isNull() ? "" : bigQueryService.getDateTime(row, rm.get("lastMention")))
          .visitType(bigQueryService.getString(row, rm.get("visitType")))
          .route(bigQueryService.getString(row, rm.get("route")))
          .dose(bigQueryService.getString(row, rm.get("dose")))
          .strength(bigQueryService.getString(row, rm.get("strength")))
          .value(bigQueryService.getFloat(row, rm.get("value")))
          .unit(bigQueryService.getString(row, rm.get("unit")))
          .refRange(bigQueryService.getString(row, rm.get("refRange")))
          .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
          .standardName(bigQueryService.getString(row, rm.get("standardName")))
          .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
          .domainType(DomainType.ALL_EVENTS);
      }
  }

  /**
   * TODO: delete when UI design is done.
   * @param rm
   * @param row
   * @param domain
   * @return
   */
  private ParticipantDataOld convertRowToParticipantDataOld(Map<String, Integer> rm,
                                                            List<FieldValue> row,
                                                            DomainType domain) {
    if (domain.equals(DomainType.DRUG)) {
      return new DrugOld()
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
        .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
        .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .numMentions(bigQueryService.getString(row, rm.get("numMentions")))
        .firstMention(bigQueryService.getDateTime(row, rm.get("firstMention")))
        .lastMention(bigQueryService.getDateTime(row, rm.get("lastMention")))
        .quantity(bigQueryService.getString(row, rm.get("quantity")))
        .refills(bigQueryService.getString(row, rm.get("refills")))
        .strength(bigQueryService.getString(row, rm.get("strength")))
        .route(bigQueryService.getString(row, rm.get("route")))
        .visitId(bigQueryService.getLong(row, rm.get("visitId")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.DRUG);
    } else if (domain.equals(DomainType.CONDITION)) {
      return new ConditionOld()
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
        .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
        .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .numMentions(bigQueryService.getString(row, rm.get("numMentions")))
        .firstMention(bigQueryService.getDateTime(row, rm.get("firstMention")))
        .lastMention(bigQueryService.getDateTime(row, rm.get("lastMention")))
        .visitId(bigQueryService.getLong(row, rm.get("visitId")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.CONDITION);
    } else if (domain.equals(DomainType.PROCEDURE)) {
      return new ProcedureOld()
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
        .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
        .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .numMentions(bigQueryService.getString(row, rm.get("numMentions")))
        .firstMention(bigQueryService.getDateTime(row, rm.get("firstMention")))
        .lastMention(bigQueryService.getDateTime(row, rm.get("lastMention")))
        .visitId(bigQueryService.getLong(row, rm.get("visitId")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.PROCEDURE);
    } else if (domain.equals(DomainType.OBSERVATION)) {
      return new ObservationOld()
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
        .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
        .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .visitId(bigQueryService.getLong(row, rm.get("visitId")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.OBSERVATION);
    } else if (domain.equals(DomainType.MEASUREMENT)) {
      return new MeasurementOld()
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
        .sourceCode(bigQueryService.getString(row, rm.get("sourceCode")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .valueConcept(bigQueryService.getString(row, rm.get("valueConcept")))
        .valueSource(bigQueryService.getString(row, rm.get("valueSourceValue")))
        .valueNumber(bigQueryService.getString(row, rm.get("valueAsNumber")))
        .units(bigQueryService.getString(row, rm.get("units")))
        .labRefRange(bigQueryService.getString(row, rm.get("refRange")))
        .visitId(bigQueryService.getLong(row, rm.get("visitId")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.MEASUREMENT);
    } else if(domain.equals(DomainType.PHYSICAL_MEASURE)) {
      return new PhysicalMeasurementOld()
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .valueConcept(bigQueryService.getString(row, rm.get("valueConcept")))
        .valueSource(bigQueryService.getString(row, rm.get("valueSourceValue")))
        .valueNumber(bigQueryService.getString(row, rm.get("valueAsNumber")))
        .units(bigQueryService.getString(row, rm.get("units")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.PHYSICAL_MEASURE);
    } else {
      return new AllEventsOld()
        .dataId(bigQueryService.getLong(row, rm.get("dataId")))
        .domain(bigQueryService.getString(row, rm.get("domain")))
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardCode(bigQueryService.getString(row, rm.get("standardCode")))
        .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
        .sourceName(bigQueryService.getString(row, rm.get("sourceName")))
        .sourceValue(bigQueryService.getString(row, rm.get("sourceValue")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .numMentions(bigQueryService.getString(row, rm.get("numMentions")))
        .firstMention(row.get(rm.get("firstMention")).isNull() ? "" : bigQueryService.getDateTime(row, rm.get("firstMention")))
        .lastMention(row.get(rm.get("lastMention")).isNull() ? "" : bigQueryService.getDateTime(row, rm.get("lastMention")))
        .visitType(bigQueryService.getString(row, rm.get("visitType")))
        .itemDate(bigQueryService.getDateTime(row, rm.get("startDate")))
        .domainType(DomainType.ALL_EVENTS);
    }
  }

  private ParticipantChartData convertRowToChartData(Map<String, Integer> rm,
                                                     List<FieldValue> row) {
    return new ParticipantChartData()
      .standardName(bigQueryService.getString(row, rm.get("standardName")))
      .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
      .startDate(bigQueryService.getDate(row, rm.get("startDate")))
      .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
      .rank(bigQueryService.getLong(row, rm.get("rank")).intValue());
  }

}
