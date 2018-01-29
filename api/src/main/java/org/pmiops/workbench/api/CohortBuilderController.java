package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.annotations.UsesDefaultCdr;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.model.ChartInfo;
import org.pmiops.workbench.model.ChartInfoListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@UsesDefaultCdr
// TODO: don't use the default CDR version, let the client specify what version to use
public class CohortBuilderController implements CohortBuilderApiDelegate {

    private BigQueryService bigQueryService;
    private CodeDomainLookupService codeDomainLookupService;
    private ParticipantCounter participantCounter;
    private CriteriaDao criteriaDao;
    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<Criteria, org.pmiops.workbench.model.Criteria>
            TO_CLIENT_CRITERIA =
            new Function<Criteria, org.pmiops.workbench.model.Criteria>() {
                @Override
                public org.pmiops.workbench.model.Criteria apply(Criteria criteria) {
                    return new org.pmiops.workbench.model.Criteria()
                                .id(criteria.getId())
                                .type(criteria.getType())
                                .subtype(criteria.getSubtype())
                                .code(criteria.getCode())
                                .name(criteria.getName())
                                .count(StringUtils.isEmpty(criteria.getCount()) ? null : new Long(criteria.getCount()))
                                .group(criteria.getGroup())
                                .selectable(criteria.getSelectable())
                                .conceptId(StringUtils.isEmpty(criteria.getConceptId()) ? null : new Long(criteria.getConceptId()))
                                .domainId(criteria.getDomainId());
                }
            };

    @Autowired
    CohortBuilderController(BigQueryService bigQueryService,
                            CodeDomainLookupService codeDomainLookupService,
                            ParticipantCounter participantCounter,
                            CriteriaDao criteriaDao) {
        this.bigQueryService = bigQueryService;
        this.codeDomainLookupService = codeDomainLookupService;
        this.participantCounter = participantCounter;
        this.criteriaDao = criteriaDao;
    }

    /**
     * This method list any of the criteria trees.
     *
     * @param type
     * @param parentId
     * @return
     */
    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(Long cdrVersionId, String type, Long parentId) {

        final List<Criteria> criteriaList = criteriaDao.findCriteriaByTypeAndParentIdOrderByCodeAsc(type, parentId);

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        criteriaResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));

        return ResponseEntity.ok(criteriaResponse);
    }

    /**
     * This method will return a count of unique subjects
     * defined by the provided {@link SearchRequest}.
     *
     * @param request
     * @return
     */
    @Override
    public ResponseEntity<Long> countParticipants(Long cdrVersionId, SearchRequest request) {

        codeDomainLookupService.findCodesForEmptyDomains(request.getIncludes());
        codeDomainLookupService.findCodesForEmptyDomains(request.getExcludes());

        QueryJobConfiguration qjc = bigQueryService.filterBigQueryConfig(participantCounter.buildParticipantCounterQuery(request));
        QueryResult result = bigQueryService.executeQuery(qjc);
        Map<String, Integer> rm = bigQueryService.getResultMapper(result);

        List<FieldValue> row = result.iterateAll().iterator().next();
        return ResponseEntity.ok(bigQueryService.getLong(row, rm.get("count")));
    }

    @Override
    public ResponseEntity<ChartInfoListResponse> getChartInfo(Long cdrVersionId, SearchRequest request) {

        ChartInfoListResponse response = new ChartInfoListResponse();

        codeDomainLookupService.findCodesForEmptyDomains(request.getIncludes());
        codeDomainLookupService.findCodesForEmptyDomains(request.getExcludes());

        QueryJobConfiguration qjc = bigQueryService.filterBigQueryConfig(participantCounter.buildChartInfoCounterQuery(request));
        QueryResult result = bigQueryService.executeQuery(qjc);
        Map<String, Integer> rm = bigQueryService.getResultMapper(result);

        for (List<FieldValue> row : result.iterateAll()) {
            response.addItemsItem(new ChartInfo()
                    .gender(bigQueryService.getString(row, rm.get("gender")))
                    .race(bigQueryService.getString(row, rm.get("race")))
                    .ageRange(bigQueryService.getString(row, rm.get("ageRange")))
                    .count(bigQueryService.getLong(row, rm.get("count"))));
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaTreeQuickSearch(Long cdrVersionId, String type, String value) {
        String nameOrCode = value + "*";
        final List<Criteria> criteriaList = criteriaDao.findCriteriaByTypeAndNameOrCode(type, nameOrCode);

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        criteriaResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));

        return ResponseEntity.ok(criteriaResponse);
    }

}
