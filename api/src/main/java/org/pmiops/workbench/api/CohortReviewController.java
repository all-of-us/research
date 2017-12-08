package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class CohortReviewController implements CohortReviewApiDelegate {

    public static final String STATUS = "status";
    public static final String PARTICIPANT_ID = "participantKey.participantId";
    public static final Integer PAGE = 0;
    public static final Integer LIMIT = 25;
    public static final Integer MAX_REVIEW_SIZE = 10000;

    private CohortReviewDao cohortReviewDao;
    private CohortDao cohortDao;
    private ParticipantCohortStatusDao participantCohortStatusDao;
    private BigQueryService bigQueryService;
    private CodeDomainLookupService codeDomainLookupService;
    private ParticipantCounter participantCounter;
    private WorkspaceDao workspaceDao;

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
                           ParticipantCohortStatusDao participantCohortStatusDao,
                           BigQueryService bigQueryService,
                           CodeDomainLookupService codeDomainLookupService,
                           ParticipantCounter participantCounter,
                           WorkspaceDao workspaceDao) {
        this.cohortReviewDao = cohortReviewDao;
        this.cohortDao = cohortDao;
        this.participantCohortStatusDao = participantCohortStatusDao;
        this.bigQueryService = bigQueryService;
        this.codeDomainLookupService = codeDomainLookupService;
        this.participantCounter = participantCounter;
        this.workspaceDao = workspaceDao;
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
            throw new BadRequestException("Invalid Request: Cohort Review size must be between 0 and " + MAX_REVIEW_SIZE);
        }
        CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        if (cohortReview == null) {
            throw new BadRequestException("Invalid Request: Cohort Review does not exist for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId);
        }
        if(cohortReview.getReviewSize() > 0) {
            throw new BadRequestException("Invalid Request: Cohort Review already created for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId);
        }

        Cohort cohort = cohortDao.findOne(cohortId);
        String definition = cohort.getCriteria();
        Workspace workspace = workspaceDao.findOne(cohort.getWorkspaceId());

        if (!workspace.getWorkspaceNamespace().equals(workspaceNamespace) ||
                !workspace.getFirecloudName().equals(workspaceId)) {
            throw new BadRequestException("Invalid Request: No workspace matching "
                    + "workspaceNamespace: " + workspaceNamespace
                    + ", workspaceId: " + workspaceId);
        }

        if (definition == null) {
            throw new BadRequestException("Invalid Request: No Cohort definition matching cohortId: "
                    + cohortId
                    + ", workspaceNamespace: " + workspaceNamespace
                    + ", workspaceId: " + workspaceId);
        }

        SearchRequest searchRequest = new Gson().fromJson(definition, SearchRequest.class);

        /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
        codeDomainLookupService.findCodesForEmptyDomains(searchRequest.getIncludes());
        codeDomainLookupService.findCodesForEmptyDomains(searchRequest.getExcludes());

        QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
                participantCounter.buildParticipantIdQuery(searchRequest, request.getSize())));
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
                .limit(LIMIT)
                .collect(Collectors.toList());

        cohortReviewDao.save(cohortReview);
        participantCohortStatusDao.save(participantCohortStatuses);

        org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview);
        responseReview.setParticipantCohortStatuses(paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

        return ResponseEntity.ok(responseReview);
    }

    @Override
    public ResponseEntity<CohortSummaryListResponse> getCohortSummary(String workspaceNamespace,
                                                                      String workspaceId,
                                                                      Long cohortId,
                                                                      Long cdrVersionId,
                                                                      String domain) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortSummaryListResponse());
    }

    /**
     * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
     * based on page, limit, order and column.
     *
     * @param cohortId
     * @param cdrVersionId
     * @param page
     * @param limit
     * @param order
     * @param column
     * @return
     */
    @Override
    public ResponseEntity<org.pmiops.workbench.model.CohortReview>
        getParticipantCohortStatuses(String workspaceNamespace,
                                 String workspaceId,
                                 Long cohortId,
                                 Long cdrVersionId,
                                 Integer page,
                                 Integer limit,
                                 String order,
                                 String column) {

        CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

        if (cohortReview == null) {

            Cohort cohort = cohortDao.findOne(cohortId);
            String definition = cohort.getCriteria();
            Workspace workspace = workspaceDao.findOne(cohort.getWorkspaceId());

            if (!workspace.getWorkspaceNamespace().equals(workspaceNamespace) ||
                    !workspace.getFirecloudName().equals(workspaceId)) {
                throw new BadRequestException("Invalid Request: No workspace matching "
                        + "workspaceNamespace: " + workspaceNamespace
                        + ", workspaceId: " + workspaceId);
            }

            if (definition == null) {
                throw new BadRequestException("Invalid Request: No Cohort definition matching cohortId: "
                        + cohortId
                        + ", workspaceNamespace: " + workspaceNamespace
                        + ", workspaceId: " + workspaceId);
            }

            SearchRequest request = new Gson().fromJson(definition, SearchRequest.class);

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
        int limitParam = Optional.ofNullable(limit).orElse(LIMIT);
        Sort.Direction orderParam = Sort.Direction.fromString(Optional.ofNullable(order)
                .filter(o -> o.equalsIgnoreCase("DESC")).orElse("ASC"));
        String columnParam = Optional.ofNullable(column)
                .filter(o -> o.equalsIgnoreCase(STATUS)).orElse(PARTICIPANT_ID);

        final Sort sort = (columnParam.equals(PARTICIPANT_ID))
                ? new Sort(orderParam, columnParam)
                : new Sort(orderParam, columnParam, PARTICIPANT_ID);
        final PageRequest pageRequest = new PageRequest(pageParam, limitParam, sort);

        final List<ParticipantCohortStatus> participantCohortStatuses =
                participantCohortStatusDao.findByParticipantKey_CohortReviewId(
                        cohortReview.getCohortReviewId(),
                        pageRequest)
                        .getContent();

        org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview);
        responseReview.setParticipantCohortStatuses(participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

        return ResponseEntity.ok(responseReview);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus>
    updateParticipantCohortStatus(String workspaceNamespace,
                                  String workspaceId,
                                  Long cohortId,
                                  Long cdrVersionId,
                                  ModifyCohortStatusRequest cohortStatusRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new org.pmiops.workbench.model.ParticipantCohortStatus());
    }
}
