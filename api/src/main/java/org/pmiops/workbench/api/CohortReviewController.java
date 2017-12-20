package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CohortSummaryListResponse;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class CohortReviewController implements CohortReviewApiDelegate {

    public static final String STATUS = "status";
    public static final String PARTICIPANT_ID = "participantKey.participantId";
    public static final Integer PAGE = 0;
    public static final Integer PAGE_SIZE = 25;
    public static final Integer MAX_REVIEW_SIZE = 10000;

    private CohortReviewDao cohortReviewDao;
    private CohortDao cohortDao;
    private WorkspaceService workspaceService;
    private ParticipantCohortStatusDao participantCohortStatusDao;
    private BigQueryService bigQueryService;
    private CodeDomainLookupService codeDomainLookupService;
    private ParticipantCounter participantCounter;

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
                            .status(participant.getStatus());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<CohortReview, org.pmiops.workbench.model.CohortReview>
            TO_CLIENT_COHORTREVIEW =
            new Function<CohortReview, org.pmiops.workbench.model.CohortReview>() {
                @Override
                public org.pmiops.workbench.model.CohortReview apply(CohortReview cohortReview) {
                    return new org.pmiops.workbench.model.CohortReview()
                            .cohortReviewId(cohortReview.getCohortReviewId())
                            .cohortId(cohortReview.getCohortId())
                            .cdrVersionId(cohortReview.getCdrVersionId())
                            .creationTime(cohortReview.getCreationTime().toString())
                            .matchedParticipantCount(cohortReview.getMatchedParticipantCount())
                            .reviewedCount(cohortReview.getReviewedCount())
                            .reviewStatus(cohortReview.getReviewStatus())
                            .reviewSize(cohortReview.getReviewSize());
                }
            };

    @Autowired
    CohortReviewController(CohortReviewDao cohortReviewDao,
                           CohortDao cohortDao,
                           WorkspaceService workspaceService,
                           ParticipantCohortStatusDao participantCohortStatusDao,
                           BigQueryService bigQueryService,
                           CodeDomainLookupService codeDomainLookupService,
                           ParticipantCounter participantCounter) {
        this.cohortReviewDao = cohortReviewDao;
        this.cohortDao = cohortDao;
        this.workspaceService = workspaceService;
        this.participantCohortStatusDao = participantCohortStatusDao;
        this.bigQueryService = bigQueryService;
        this.codeDomainLookupService = codeDomainLookupService;
        this.participantCounter = participantCounter;
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
     * @return
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
        CohortReview cohortReview = findCohortReview(cohortId, cdrVersionId);
        if(cohortReview.getReviewSize() > 0) {
            throw new BadRequestException(
                    String.format("Invalid Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
                            cohortId, cdrVersionId));
        }

        Cohort cohort = findCohort(cohortId);
        //this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId());

        SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

        /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
        codeDomainLookupService.findCodesForEmptyDomains(searchRequest.getIncludes());
        codeDomainLookupService.findCodesForEmptyDomains(searchRequest.getExcludes());

        QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
                participantCounter.buildParticipantIdQuery(searchRequest, request.getSize(), 0L)));
        Map<String, Integer> rm = bigQueryService.getResultMapper(result);

        List<ParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
        for (List<FieldValue> row : result.iterateAll()) {
            participantCohortStatuses.add(
                    new ParticipantCohortStatus()
                            .participantKey(
                                    new ParticipantCohortStatusKey(
                                            cohortReview.getCohortReviewId(),
                                            bigQueryService.getLong(row, rm.get("person_id"))))
                            .status(CohortStatus.NOT_REVIEWED));
        }

        cohortReview
                .reviewSize(participantCohortStatuses.size())
                .reviewedCount(0L)
                .reviewStatus(ReviewStatus.CREATED);
        List<ParticipantCohortStatus> paginatedPCS = participantCohortStatuses
                .stream()
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        cohortReviewDao.save(cohortReview);
        participantCohortStatusDao.save(participantCohortStatuses);

        org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview);
        responseReview.setParticipantCohortStatuses(paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

        responseReview.setPage(PAGE);
        responseReview.setPageSize(PAGE_SIZE);
        responseReview.setSortOrder("ASC");
        responseReview.setSortColumn(PARTICIPANT_ID);

        return ResponseEntity.ok(responseReview);
    }

    @Override
    public ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(String workspaceNamespace,
                                                                                         String workspaceId,
                                                                                         Long cohortReviewId,
                                                                                         Long participantId,
                                                                                         ParticipantCohortAnnotation request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ParticipantCohortAnnotation());
    }

    @Override
    public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(String workspaceNamespace,
                                                                           String workspaceId,
                                                                           Long cohortReviewId,
                                                                           Long participantId,
                                                                           Long annotationId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new EmptyResponse());
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
    public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(String workspaceNamespace,
                                                                                                   String workspaceId,
                                                                                                   Long cohortReviewId,
                                                                                                   Long participantId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ParticipantCohortAnnotationListResponse());
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> getParticipantCohortStatus(String workspaceNamespace,
                                                                                                         String workspaceId,
                                                                                                         Long cohortReviewId,
                                                                                                         Long participantId) {
        CohortReview cohortReview = findCohortReview(cohortReviewId);

        Cohort cohort = findCohort(cohortReview.getCohortId());
        //this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId());

        ParticipantCohortStatus participantCohortStatus = participantCohortStatusDao.findOne(participantId);

        return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus));
    }

    /**
     * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
     * based on page, pageSize, sortOrder and sortColumn.
     *
     * @param cohortId
     * @param cdrVersionId
     * @param page
     * @param pageSize
     * @param sortOrder
     * @param sortColumn
     * @return
     */
    @Override
    public ResponseEntity<org.pmiops.workbench.model.CohortReview>
        getParticipantCohortStatuses(String workspaceNamespace,
                                 String workspaceId,
                                 Long cohortId,
                                 Long cdrVersionId,
                                 Integer page,
                                 Integer pageSize,
                                 String sortOrder,
                                 String sortColumn) {

        CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

        if (cohortReview == null) {
            Cohort cohort = findCohort(cohortId);
            //this validates that the user is in the proper workspace
            validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId());

            SearchRequest request = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

            /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
            codeDomainLookupService.findCodesForEmptyDomains(request.getIncludes());
            codeDomainLookupService.findCodesForEmptyDomains(request.getExcludes());

            QueryResult result = bigQueryService.executeQuery(
                    bigQueryService.filterBigQueryConfig(participantCounter.buildParticipantCounterQuery(request)));
            Map<String, Integer> rm = bigQueryService.getResultMapper(result);
            List<FieldValue> row = result.iterateAll().iterator().next();
            long cohortCount = bigQueryService.getLong(row, rm.get("count"));

            cohortReview = new CohortReview();
            cohortReview.setCohortId(cohortId);
            cohortReview.setCdrVersionId(cdrVersionId);
            cohortReview.matchedParticipantCount(cohortCount);
            cohortReview.setCreationTime(new Timestamp(System.currentTimeMillis()));
            cohortReview.reviewedCount(0L);
            cohortReview.reviewStatus(ReviewStatus.NONE);
            cohortReviewDao.save(cohortReview);
        }

        int pageParam = Optional.ofNullable(page).orElse(PAGE);
        int pageSizeParam = Optional.ofNullable(pageSize).orElse(PAGE_SIZE);
        Sort.Direction orderParam = Sort.Direction.fromString(Optional.ofNullable(sortOrder)
                .filter(o -> o.equalsIgnoreCase("DESC")).orElse("ASC"));
        String columnParam = Optional.ofNullable(sortColumn)
                .filter(o -> o.equalsIgnoreCase(STATUS)).orElse(PARTICIPANT_ID);

        final Sort sort = (columnParam.equals(PARTICIPANT_ID))
                ? new Sort(orderParam, columnParam)
                : new Sort(orderParam, columnParam, PARTICIPANT_ID);
        final PageRequest pageRequest = new PageRequest(pageParam, pageSizeParam, sort);

        final List<ParticipantCohortStatus> participantCohortStatuses =
                participantCohortStatusDao.findByParticipantKey_CohortReviewId(
                        cohortReview.getCohortReviewId(),
                        pageRequest)
                        .getContent();

        org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview);
        responseReview.setParticipantCohortStatuses(participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

        responseReview.setPage(pageParam);
        responseReview.setPageSize(pageSizeParam);
        responseReview.setSortOrder(orderParam.toString());
        responseReview.setSortColumn(columnParam);

        return ResponseEntity.ok(responseReview);
    }

    @Override
    public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId, Long annotationId, ModifyParticipantCohortAnnotationRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ParticipantCohortAnnotation());
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus>
    updateParticipantCohortStatus(String workspaceNamespace,
                                  String workspaceId,
                                  Long cohortId,
                                  Long cdrVersionId,
                                  Long participantId,
                                  ModifyCohortStatusRequest cohortStatusRequest) {

        Cohort cohort = findCohort(cohortId);
        //this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId());

        CohortReview cohortReview = findCohortReview(cohortId, cdrVersionId);

        ParticipantCohortStatus participantCohortStatus = participantCohortStatusDao.
                findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReview.getCohortReviewId(), participantId);
        if (participantCohortStatus == null) {
            throw new NotFoundException(
                    String.format("Not Found: No participant exists for participantId: %s", participantId));
        }

        participantCohortStatus.setStatus(cohortStatusRequest.getStatus());
        participantCohortStatusDao.save(participantCohortStatus);

        cohortReview.lastModifiedTime(new Timestamp(System.currentTimeMillis()));
        cohortReview.incrementReviewedCount();
        cohortReviewDao.save(cohortReview);

        return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus));
    }

    private Cohort findCohort(long cohortId) {
        Cohort cohort = cohortDao.findOne(cohortId);
        if (cohort == null) {
            throw new NotFoundException(
                    String.format("Not Found: No Cohort exists for cohortId: %s", cohortId));
        }
        return cohort;
    }

    private void validateMatchingWorkspace(String workspaceNamespace, String workspaceName, long workspaceId) {
        Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceName);
        if (workspace.getWorkspaceId() != workspaceId) {
            throw new NotFoundException(
                    String.format("Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                            workspaceNamespace, workspaceName));
        }
    }

    private CohortReview findCohortReview(Long cohortId, Long cdrVersionId) {
        CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

        if (cohortReview == null) {
            throw new NotFoundException(
                    String.format("Not Found: Cohort Review does not exist for cohortId: %s, cdrVersionId: %s",
                            cohortId, cdrVersionId));
        }
        return cohortReview;
    }

    private CohortReview findCohortReview(Long cohortReviewId) {
        CohortReview cohortReview = cohortReviewDao.findOne(cohortReviewId);
        
        if (cohortReview == null) {
            throw new NotFoundException(
                    String.format("Not Found: Cohort Review does not exist for cohortReviewId: %s",
                            cohortReviewId));
        }
        return cohortReview;
    }

    private String getCohortDefinition(Cohort cohort) {
        String definition = cohort.getCriteria();
        if (definition == null) {
            throw new NotFoundException(
                    String.format("Not Found: No Cohort definition matching cohortId: %s", cohort.getCohortId()));
        }
        return definition;
    }

}
