package org.pmiops.workbench.api;


import io.swagger.annotations.ApiParam;
import org.pmiops.workbench.cdr.dao.AchillesAnalysisDao;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.dao.AnalysisResultDao;
import org.pmiops.workbench.cdr.model.AnalysisResult;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.SubjectCounter;
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;



@RestController
public class DataBrowserController implements DataBrowserApiDelegate {


    @Autowired
    private ConceptDao conceptDao;

    @Autowired
    private AnalysisResultDao analysisResultDao;

    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;


    private static final Logger log = Logger.getLogger(DataBrowserController.class.getName());

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<Concept, org.pmiops.workbench.model.Concept>
            TO_CLIENT_CONCEPT =
            new Function<Concept, org.pmiops.workbench.model.Concept>() {
                @Override
                public org.pmiops.workbench.model.Concept apply(Concept concept) {
                    return new org.pmiops.workbench.model.Concept()
                                .conceptId(concept.getConceptId())
                                .conceptName(concept.getConceptName())
                                .standardConcept(concept.getStandardConcept())
                                .conceptCode(concept.getConceptCode())
                                .conceptClassId(concept.getConceptClassId())
                                .vocabularyId(concept.getVocabularyId())
                                .domainId(concept.getDomainId())
                                .count(concept.getCount())
                                .prevalence(concept.getPrevalence());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AnalysisResult, org.pmiops.workbench.model.AnalysisResult>
            TO_CLIENT_ANALYSIS_RESULT =
            new Function<AnalysisResult, org.pmiops.workbench.model.AnalysisResult>() {
                @Override
                public org.pmiops.workbench.model.AnalysisResult apply(org.pmiops.workbench.cdr.model.AnalysisResult cdr) {
                    return new org.pmiops.workbench.model.AnalysisResult()
                            .id(cdr.getId())
                            .analysisId( cdr.getAnalysisId())
                            .stratum1(cdr.getStratum1())
                            .stratum1Name(cdr.getStratum1Name())
                            .stratum2(cdr.getStratum2())
                            .stratum2Name(cdr.getStratum2Name())
                            .stratum3(cdr.getStratum3())
                            .stratum3Name(cdr.getStratum3Name())
                            .stratum4(cdr.getStratum4())
                            .stratum4Name(cdr.getStratum4Name())
                            .stratum5(cdr.getStratum5Name())
                            .countValue(cdr.getCountValue());

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesAnalysis, org.pmiops.workbench.model.Analysis>
            TO_CLIENT_ANALYSIS =
            new Function<AchillesAnalysis, org.pmiops.workbench.model.Analysis>() {
                @Override
                public org.pmiops.workbench.model.Analysis apply(org.pmiops.workbench.cdr.model.AchillesAnalysis cdr) {
                    return new org.pmiops.workbench.model.Analysis()
                            .analysisId( cdr.getAnalysisId())
                            .analysisName(cdr.getAnalysisName())
                            .stratum1Name(cdr.getStratum1Name())
                            .stratum2Name(cdr.getStratum2Name())
                            .stratum3Name(cdr.getStratum3Name())
                            .stratum4Name(cdr.getStratum4Name())
                            .stratum5Name(cdr.getStratum5Name())
                            .chartType(cdr.getChartType())
                            .dataType(cdr.getDataType());

                }
            };



    /**
     * This method searches concepts
     *
     * @param conceptName
     * @param standardConcept
     * @param concept_code
     * @param vocabulary_id
     * @param domain_id
     * @return
     */
    @Override
    public ResponseEntity<ConceptListResponse> getConceptsSearch(
            String conceptName,
            String standardConcept,
            String concept_code,
            String vocabulary_id,
            String domain_id) {

        final List<Concept> conceptList = conceptDao.findConceptLikeName(conceptName);
        ConceptListResponse resp = new ConceptListResponse();
        resp.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));

        return ResponseEntity.ok(resp);
    }

    /**
     * This method searches concepts
     *
     * @param analysisId
     * @param stratum1
     * @param stratum2
     * @param stratum3
     * @param stratum4
     * @param stratum5
     * @return
     */
    @Override
    public ResponseEntity<AnalysisResultListResponse> getAnalysisResults(
            Long analysisId,
            String stratum1,
            String stratum2,
            String stratum3,
            String stratum4,
            String stratum5) {

        final List<AnalysisResult> resultList;

        if ( (stratum1 != null && !stratum1.isEmpty()) && (stratum2 != null && !stratum2.isEmpty()))  {
             resultList = analysisResultDao.findAnalysisResultsByAnalysisIdAndStratum1AndStratum2(analysisId, stratum1, stratum2);
        }
        else if ((stratum1 != null && !stratum1.isEmpty())) {
            resultList = analysisResultDao.findAnalysisResultsByAnalysisIdAndStratum1(analysisId, stratum1);
        }
        else {
            resultList = analysisResultDao.findAnalysisResultsByAnalysisId(analysisId);
        }

        AnalysisResultListResponse resp = new AnalysisResultListResponse();
        resp.setItems(resultList.stream().map(TO_CLIENT_ANALYSIS_RESULT).collect(Collectors.toList()));


        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.AnalysisResult> getParticipantCount() {
        long aid =  1; // partipant count analysis id
        final List<AnalysisResult> resultList  =  analysisResultDao.findAnalysisResultsByAnalysisId(aid);
        AnalysisResultListResponse resp = new AnalysisResultListResponse();
        resp.setItems(resultList.stream().map(TO_CLIENT_ANALYSIS_RESULT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp.getItems().get(0));
    }

    @Override
    public ResponseEntity<AnalysisResultListResponse> getConceptCount(String conceptId) {
        long aid = 3000;
        return this.getAnalysisResults(aid, conceptId, null, null , null, null);
    }

    @Override
    public ResponseEntity<AnalysisResultListResponse> getConceptCountByGender(String conceptId) {
        long aid = 3101;
        return this.getAnalysisResults(aid, conceptId, null, null , null, null);
    }

    @Override
    public ResponseEntity<AnalysisResultListResponse> getConceptCountByAge(String conceptId) {
        long aid = 3102;
        return this.getAnalysisResults(aid, conceptId, null, null , null, null);
    }

    @Override
    public ResponseEntity<AnalysisListResponse> getAnalyses() {
        long num = 1;
        final List<AchillesAnalysis> resultList = achillesAnalysisDao.findAllByAnalysisIdIsGreaterThanEqual((Long)num);
        AnalysisListResponse resp = new AnalysisListResponse();
        resp.setItems(resultList.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);

    }


}
