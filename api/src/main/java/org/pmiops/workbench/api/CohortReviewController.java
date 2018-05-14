package org.pmiops.workbench.api;

import com.google.cloud.bigquery.*;
import com.google.gson.Gson;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.ReviewTabQueryBuilder;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class CohortReviewController implements CohortReviewApiDelegate {

  public static final Integer PAGE = 0;
  public static final Integer PAGE_SIZE = 25;
  public static final Integer MAX_REVIEW_SIZE = 10000;

  private CohortReviewService cohortReviewService;
  private BigQueryService bigQueryService;
  private ParticipantCounter participantCounter;
  private ReviewTabQueryBuilder reviewTabQueryBuilder;
  private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

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
          .status(participant.getStatus())
          .birthDate(participant.getBirthDate().getTime())
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
          .reviewStatus(cohortReview.getReviewStatus())
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
                         ReviewTabQueryBuilder reviewTabQueryBuilder,
                         Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider) {
    this.cohortReviewService = cohortReviewService;
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
    this.reviewTabQueryBuilder = reviewTabQueryBuilder;
    this.genderRaceEthnicityConceptProvider = genderRaceEthnicityConceptProvider;
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
        String.format("Invalid Request: Cohort Review size must be between %s and %s", 0, MAX_REVIEW_SIZE));
    }

    Cohort cohort = cohortReviewService.findCohort(cohortId);
    //this validates that the user is in the proper workspace
    Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace,
      workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);

    CdrVersionContext.setCdrVersion(workspace.getCdrVersion());

    CohortReview cohortReview = null;
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort)
        .reviewStatus(ReviewStatus.NONE)
        .reviewSize(0L);
      cohortReviewService.saveCohortReview(cohortReview);
    }
    if (cohortReview.getReviewSize() > 0) {
      throw new BadRequestException(
        String.format("Invalid Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
          cohortId, cdrVersionId));
    }

    SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      participantCounter.buildParticipantIdQuery(new ParticipantCriteria(searchRequest),
        request.getSize(), 0L)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<ParticipantCohortStatus> participantCohortStatuses =
      createParticipantCohortStatusesList(cohortReview.getCohortReviewId(), result, rm);

    cohortReview
      .reviewSize(participantCohortStatuses.size())
      .reviewStatus(ReviewStatus.CREATED);

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
      throw new BadRequestException("Invalid Request: Please provide a valid cohort annotation definition id.");
    }

    CohortReview cohortReview = validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantCohortAnnotation =
      FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(request);

    participantCohortAnnotation = cohortReviewService.saveParticipantCohortAnnotation(cohortReview.getCohortReviewId(), participantCohortAnnotation);

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
      throw new BadRequestException("Invalid Request: Please provide a valid cohort annotation definition id.");
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
  public ResponseEntity<CohortSummaryListResponse> getCohortSummary(String workspaceNamespace,
                                                                    String workspaceId,
                                                                    Long cohortId,
                                                                    Long cdrVersionId,
                                                                    String domain) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortSummaryListResponse());
  }

  @Override
  public ResponseEntity<ParticipantData> getDetailParticipantData(String workspaceNamespace,
                                                                  String workspaceId,
                                                                  Long cohortId,
                                                                  Long cdrVersionId,
                                                                  Long dataId,
                                                                  String domain) {
    validateRequestAndSetCdrVersion(workspaceNamespace, workspaceId,
      cohortId, cdrVersionId, WorkspaceAccessLevel.READER);

    QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      reviewTabQueryBuilder.buildDetailsQuery(dataId)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantData response =
      convertRowToParticipantData(rm, result.getValues().iterator().next(), domain);

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

    Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceId,
      cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
    CdrVersionContext.setCdrVersion(workspace.getCdrVersion());
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort);
    }

    String sortColumn = Optional.ofNullable(((ParticipantCohortStatuses) request).getSortColumn())
      .orElse(ParticipantCohortStatusColumns.PARTICIPANTID).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    PageRequest pageRequest = new PageRequest()
      .page(pageParam)
      .pageSize(pageSizeParam)
      .sortOrder(sortOrderParam)
      .sortColumn(sortColumn);

    List<Filter> filters = request.getFilters() == null ? Collections.<Filter>emptyList() : request.getFilters().getItems();
    List<ParticipantCohortStatus> participantCohortStatuses =
      cohortReviewService.findAll(cohortReview.getCohortReviewId(), filters, pageRequest);

    org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(
      participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

    return ResponseEntity.ok(responseReview);
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

    boolean invalidDomain = true;
    String domain = ((ReviewFilter) request).getDomain();
    for (DomainType domainType : DomainType.values()) {
      if (domainType.toString().equals(domain)) {
        invalidDomain = false;
      }
    }
    if (invalidDomain) {
      throw new BadRequestException("Invalid Domain: " + domain +
        " Please provide a valid Domain.");
    }
    String sortColumn = Optional.ofNullable(((ReviewFilter) request).getSortColumn())
      .orElse(ReviewColumns.ITEMDATE).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    PageRequest pageRequest = new PageRequest()
      .page(pageParam)
      .pageSize(pageSizeParam)
      .sortOrder(sortOrderParam)
      .sortColumn(sortColumn);

    QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
      reviewTabQueryBuilder.buildQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantDataListResponse response = new ParticipantDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToParticipantData(rm, row, domain));
    }

    if (result.getTotalRows() == pageSizeParam) {
      result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
        reviewTabQueryBuilder.buildCountQuery(participantId, domain)));
      rm = bigQueryService.getResultMapper(result);
      response.count(bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count")));
    } else {
      response.count(result.getTotalRows());
    }

    response.setPageRequest(new PageRequest()
      .page(pageParam)
      .pageSize(pageSizeParam)
      .sortOrder(sortOrderParam)
      .sortColumn(sortColumn));

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

    participantCohortStatus.setStatus(cohortStatusRequest.getStatus());
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
    Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace,
      workspaceId, cohort.getWorkspaceId(), level);

    CdrVersionContext.setCdrVersion(workspace.getCdrVersion());

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

    QueryResult result = bigQueryService.executeQuery(
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
                                                                            QueryResult result,
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
          .status(CohortStatus.NOT_REVIEWED)
          .birthDate(new java.sql.Date(birthDate.getTime()))
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
    cohortReview.reviewStatus(ReviewStatus.NONE);
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
      pcs.setRace(concepts.get(GenderRaceEthnicityType.RACE.name()).get(pcs.getRaceConceptId()));
      pcs.setGender(concepts.get(GenderRaceEthnicityType.GENDER.name()).get(pcs.getGenderConceptId()));
      pcs.setEthnicity(concepts.get(GenderRaceEthnicityType.ETHNICITY.name()).get(pcs.getEthnicityConceptId()));
    });
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
                                                      String domain) {
    ParticipantData participantData = null;
    if (domain.equals(DomainType.DRUG.toString())) {
      participantData = new Drug()
        .signature(bigQueryService.getString(row, rm.get("signature")))
        .age(bigQueryService.getLong(row, rm.get("age")).intValue())
        .domainType(DomainType.DRUG);
    } else if (domain.equals(DomainType.CONDITION.toString())) {
      participantData = new Condition()
        .age(bigQueryService.getLong(row, rm.get("age")).intValue())
        .domainType(DomainType.CONDITION);
    } else if (domain.equals(DomainType.PROCEDURE.toString())) {
      participantData = new Procedure()
        .age(bigQueryService.getLong(row, rm.get("age")).intValue())
        .domainType(DomainType.PROCEDURE);;
    } else if (domain.equals(DomainType.OBSERVATION.toString())) {
      participantData = new Observation()
        .age(bigQueryService.getLong(row, rm.get("age")).intValue())
        .domainType(DomainType.OBSERVATION);;
    } else if (domain.equals(DomainType.VISIT.toString())) {
      try {
        participantData = new Visit()
          .age(bigQueryService.getLong(row, rm.get("age")).intValue())
          .endDate(bigQueryService.getDateTime(row, rm.get("itemEndDate")))
          .domainType(DomainType.VISIT);;
      } catch (BigQueryException e) {
        //do nothing for now.
      }
    } else if (domain.equals(DomainType.MEASUREMENT.toString())) {
      participantData = new Measurement()
        .age(bigQueryService.getLong(row, rm.get("age")).intValue())
        .domainType(DomainType.MEASUREMENT);;
    } else if (domain.equals(DomainType.MASTER.toString())) {
      participantData = new Master()
        .dataId(bigQueryService.getLong(row, rm.get("dataId")))
        .domain(bigQueryService.getString(row, rm.get("domain")))
        .domainType(DomainType.MASTER);;
    }
    return participantData.itemDate(bigQueryService.getDateTime(row, rm.get("itemDate")))
      .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
      .standardName(bigQueryService.getString(row, rm.get("standardName")))
      .sourceValue(bigQueryService.getString(row, rm.get("sourceValue")))
      .sourceVocabulary(bigQueryService.getString(row, rm.get("sourceVocabulary")))
      .sourceName(bigQueryService.getString(row, rm.get("sourceName")));
  }

}
