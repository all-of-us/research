package org.pmiops.workbench.publicapi;

import org.pmiops.workbench.cdr.dao.*;
import org.pmiops.workbench.cdr.model.*;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.ConceptAnalysisListResponse;
import org.pmiops.workbench.model.ConceptAnalysis;
import org.pmiops.workbench.model.DbDomainListResponse;
import org.pmiops.workbench.model.QuestionConceptListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.MatchType;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Slice;

import com.google.common.collect.ImmutableMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class DataBrowserController implements DataBrowserApiDelegate {

    @Autowired
    private ConceptDao conceptDao;
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    @Autowired
    private DbDomainDao dbDomainDao;
    @Autowired
    private ConceptSynonymDao conceptSynonymDao;

    private ConceptService conceptService;

    @Autowired
    public DataBrowserController(ConceptService conceptService, ConceptDao conceptDao, DbDomainDao dbDomainDao, AchillesResultDao achillesResultDao,AchillesAnalysisDao achillesAnalysisDao, ConceptSynonymDao conceptSynonymDao) {
        this.conceptService = conceptService;
        this.conceptDao = conceptDao;
        this.dbDomainDao = dbDomainDao;
        this.achillesResultDao = achillesResultDao;
        this.achillesAnalysisDao = achillesAnalysisDao;
        this.conceptSynonymDao = conceptSynonymDao;
    }


    // TODO: consider putting this in CDM config, fetching it from there
    private static final ImmutableMultimap<Domain, String> DOMAIN_MAP =
            ImmutableMultimap.<Domain, String>builder()
                    .put(Domain.CONDITION, "Condition")
                    .put(Domain.CONDITION, "Condition/Meas")
                    .put(Domain.CONDITION, "Condition/Device")
                    .put(Domain.CONDITION, "Condition/Procedure")
                    .put(Domain.DEVICE, "Device")
                    .put(Domain.DEVICE, "Condition/Device")
                    .put(Domain.DRUG, "Drug")
                    .put(Domain.ETHNICITY, "Ethnicity")
                    .put(Domain.GENDER, "Gender")
                    .put(Domain.MEASUREMENT, "Measurement")
                    .put(Domain.MEASUREMENT, "Meas/Procedure")
                    .put(Domain.OBSERVATION, "Observation")
                    .put(Domain.PROCEDURE, "Procedure")
                    .put(Domain.PROCEDURE, "Meas/Procedure")
                    .put(Domain.PROCEDURE, "Condition/Procedure")
                    .put(Domain.RACE, "Race")
                    .build();

    public static final long PARTICIPANT_COUNT_ANALYSIS_ID = 1;
    public static final long COUNT_ANALYSIS_ID = 3000;
    public static final long GENDER_ANALYSIS_ID = 3101;
    public static final long AGE_ANALYSIS_ID = 3102;

    public static final long RACE_ANALYSIS_ID = 3103;
    public static final long ETHNICITY_ANALYSIS_ID = 3104;

    public static final long MEASUREMENT_GENDER_ANALYSIS_ID = 1900;
    public static final long MEASUREMENT_AGE_ANALYSIS_ID = 1901;

    public static final String MALE_CONCEPT_ID = "8507";
    public static final String FEMALE_CONCEPT_ID = "8532";

    public static final long GENDER_ANALYSIS = 2;
    public static final long RACE_ANALYSIS = 4;
    public static final long ETHNICITY_ANALYSIS = 5;

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
                            .countValue(concept.getCountValue())
                            .sourceCountValue(concept.getSourceCountValue())
                            .prevalence(concept.getPrevalence())
                            .conceptSynonyms(concept.getSynonyms().stream().distinct().map(ConceptSynonym::getConceptSynonymName).collect(Collectors.toList()));
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<QuestionConcept, org.pmiops.workbench.model.QuestionConcept>
            TO_CLIENT_QUESTION_CONCEPT =
            new Function<QuestionConcept, org.pmiops.workbench.model.QuestionConcept>() {
                @Override
                public org.pmiops.workbench.model.QuestionConcept apply(QuestionConcept concept) {
                    org.pmiops.workbench.model.Analysis countAnalysis=null;
                    org.pmiops.workbench.model.Analysis genderAnalysis=null;
                    org.pmiops.workbench.model.Analysis ageAnalysis=null;
                    if(concept.getCountAnalysis() != null){
                        countAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getCountAnalysis());
                    }
                    if(concept.getGenderAnalysis() != null){
                        genderAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getGenderAnalysis());
                    }
                    if(concept.getAgeAnalysis() != null){
                        ageAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getAgeAnalysis());
                    }


                    return new org.pmiops.workbench.model.QuestionConcept()
                            .conceptId(concept.getConceptId())
                            .conceptName(concept.getConceptName())
                            .conceptCode(concept.getConceptCode())
                            .domainId(concept.getDomainId())
                            .countValue(concept.getCountValue())
                            .prevalence(concept.getPrevalence())
                            .countAnalysis(countAnalysis)
                            .genderAnalysis(genderAnalysis)
                            .ageAnalysis(ageAnalysis);
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
                public org.pmiops.workbench.model.Analysis apply(AchillesAnalysis cdr) {
                    List<org.pmiops.workbench.model.AchillesResult> results = new ArrayList<>();
                    if (!cdr.getResults().isEmpty()) {
                        results = cdr.getResults().stream().map(TO_CLIENT_ACHILLES_RESULT).collect(Collectors.toList());
                    }

                    return new org.pmiops.workbench.model.Analysis()
                            .analysisId(cdr.getAnalysisId())
                            .analysisName(cdr.getAnalysisName())
                            .stratum1Name(cdr.getStratum1Name())
                            .stratum2Name(cdr.getStratum2Name())
                            .stratum3Name(cdr.getStratum3Name())
                            .stratum4Name(cdr.getStratum4Name())
                            .stratum5Name(cdr.getStratum5Name())
                            .chartType(cdr.getChartType())
                            .dataType(cdr.getDataType())
                            .results(results);

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<DbDomain, org.pmiops.workbench.model.DbDomain>
            TO_CLIENT_DBDOMAIN =
            new Function<DbDomain, org.pmiops.workbench.model.DbDomain>() {
                @Override
                public org.pmiops.workbench.model.DbDomain apply(DbDomain cdr) {
                    return new org.pmiops.workbench.model.DbDomain()
                            .domainId(cdr.getDomainId())
                            .domainDisplay(cdr.getDomainDisplay())
                            .domainDesc(cdr.getDomainDesc())
                            .dbType(cdr.getDbType())
                            .domainRoute(cdr.getDomainRoute())
                            .conceptId(cdr.getConceptId())
                            .countValue(cdr.getCountValue())
                            .participantCount(cdr.getParticipantCount());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<ConceptAnalysis, ConceptAnalysis>
            TO_CLIENT_CONCEPTANALYSIS=
            new Function<ConceptAnalysis, ConceptAnalysis>() {
                @Override
                public ConceptAnalysis apply(ConceptAnalysis ca) {
                    return new ConceptAnalysis()
                            .conceptId(ca.getConceptId())
                            .genderAnalysis(ca.getGenderAnalysis())
                            .ageAnalysis(ca.getAgeAnalysis())
                            .raceAnalysis(ca.getRaceAnalysis())
                            .ethnicityAnalysis(ca.getEthnicityAnalysis())
                            .measurementValueGenderAnalysis(ca.getMeasurementValueGenderAnalysis())
                            .measurementValueMaleAnalysis(ca.getMeasurementValueMaleAnalysis())
                            .measurementValueFemaleAnalysis(ca.getMeasurementValueFemaleAnalysis())
                            .measurementValueOtherGenderAnalysis(ca.getMeasurementValueOtherGenderAnalysis())
                            .measurementValueAgeAnalysis(ca.getMeasurementValueAgeAnalysis());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesResult, org.pmiops.workbench.model.AchillesResult>
            TO_CLIENT_ACHILLES_RESULT =
            new Function<AchillesResult, org.pmiops.workbench.model.AchillesResult>() {
                @Override
                public org.pmiops.workbench.model.AchillesResult apply(AchillesResult o) {

                    return new org.pmiops.workbench.model.AchillesResult()
                            .id(o.getId())
                            .analysisId(o.getAnalysisId())
                            .stratum1(o.getStratum1())
                            .stratum2(o.getStratum2())
                            .stratum3(o.getStratum3())
                            .stratum4(o.getStratum4())
                            .stratum5(o.getStratum5())
                            .analysisStratumName(o.getAnalysisStratumName())
                            .countValue(o.getCountValue())
                            .sourceCountValue(o.getSourceCountValue());
                }
            };


    @Override
    public ResponseEntity<DbDomainListResponse> getDomainFilters() {
        List<DbDomain> domains=dbDomainDao.findByDbType("domain_filter");
        DbDomainListResponse resp=new DbDomainListResponse();
        resp.setItems(domains.stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<DbDomainListResponse> getSurveyList() {
        List<DbDomain> domains=dbDomainDao.findByDbTypeAndConceptIdNot("survey",0L);
        DbDomainListResponse resp=new DbDomainListResponse();
        resp.setItems(domains.stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<DbDomainListResponse> getDomainSearchResults(String keyword){

        keyword = ConceptService.modifyMultipleMatchKeyword(keyword);
        List<DbDomain> domains = dbDomainDao.findDomainSearchResults(keyword);
        DbDomainListResponse resp = new DbDomainListResponse();
        resp.setItems(domains.stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<ConceptListResponse> searchConcepts(SearchConceptsRequest searchConceptsRequest){

        Integer maxResults = searchConceptsRequest.getMaxResults();
        if(maxResults == null || maxResults == 0){
            maxResults = Integer.MAX_VALUE;
        }

        Integer minCount = searchConceptsRequest.getMinCount();
        if(minCount == null){
            minCount = 1;
        }

        StandardConceptFilter standardConceptFilter = searchConceptsRequest.getStandardConceptFilter();

        if(searchConceptsRequest.getQuery() == null || searchConceptsRequest.getQuery().isEmpty()){
            if(standardConceptFilter == null || standardConceptFilter == StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH){
                standardConceptFilter = StandardConceptFilter.STANDARD_CONCEPTS;
            }
        }else{
            if(standardConceptFilter == null){
                standardConceptFilter = StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH;
            }
        }


        List<String> domainIds = null;
        if (searchConceptsRequest.getDomain() != null) {
            domainIds = DOMAIN_MAP.get(searchConceptsRequest.getDomain()).asList();
        }

        ConceptService.StandardConceptFilter convertedConceptFilter = ConceptService.StandardConceptFilter.valueOf(standardConceptFilter.name());


        List<Concept> conceptSynonymList = null;
        List<Long> synonymConceptIds = new ArrayList<>();
        if(searchConceptsRequest.getQuery() != null && !searchConceptsRequest.getQuery().isEmpty()){
            conceptSynonymList = conceptDao.findConceptSynonyms(ConceptService.modifyMultipleMatchKeyword(searchConceptsRequest.getQuery()));
            for(Concept c:conceptSynonymList){
                synonymConceptIds.add(c.getConceptId());
            }
        }

        Slice<Concept> concepts =
                    conceptService.searchConcepts(searchConceptsRequest.getQuery(), convertedConceptFilter,
                            searchConceptsRequest.getVocabularyIds(), domainIds, maxResults, minCount, synonymConceptIds);
            ConceptListResponse response = new ConceptListResponse();
            List<Concept> matchedConcepts = concepts.getContent();
            List<String> conceptSynonymNames = new ArrayList<>();

            for(Concept con : matchedConcepts){
                String conceptCode = con.getConceptCode();
                String conceptId = String.valueOf(con.getConceptId());

                if(con.getSynonyms() != null){
                    response.setMatchType(MatchType.SYNONYM);
                    for(ConceptSynonym conceptSynonym:con.getSynonyms()){
                        if(!conceptSynonymNames.contains(conceptSynonym.getConceptSynonymName())){
                            conceptSynonymNames.add(conceptSynonym.getConceptSynonymName());
                        }
                    }
                }


                if((con.getStandardConcept() == null || !con.getStandardConcept().equals("S") ) && (searchConceptsRequest.getQuery().equals(conceptCode) || searchConceptsRequest.getQuery().equals(conceptId))){
                    response.setMatchType(conceptCode.equals(searchConceptsRequest.getQuery()) ? MatchType.CODE : MatchType.ID );

                    List<Concept> std_concepts = conceptDao.findStandardConcepts(con.getConceptId());
                    response.setStandardConcepts(std_concepts.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
                }

            }

            if(response.getMatchType() == null && response.getStandardConcepts() == null){
                response.setMatchType(MatchType.NAME);
            }

            response.setItems(matchedConcepts.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
            return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DbDomainListResponse> getDomainTotals(){
        List<DbDomain> domains = dbDomainDao.findDomainTotals();
        List<Concept> concepts = conceptDao.findDbDomainParticpantCounts();
        DbDomain.mapConceptCounts(concepts);
        for(DbDomain dbd : domains){
            if(dbd.getParticipantCount() == 0){
                Long participantCount = DbDomain.conceptCountMap.get(dbd.getConceptId());
                if(participantCount != null){
                    dbd.setParticipantCount(participantCount);
                }
            }
        }
        DbDomainListResponse resp=new DbDomainListResponse();
        resp.setItems(domains.stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getGenderAnalysis(){
        AchillesAnalysis genderAnalysis = achillesAnalysisDao.findAnalysisById(GENDER_ANALYSIS);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(genderAnalysis));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getRaceAnalysis(){
        AchillesAnalysis raceAnalysis = achillesAnalysisDao.findAnalysisById(RACE_ANALYSIS);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(raceAnalysis));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getEthnicityAnalysis(){
        AchillesAnalysis ethnicityAnalysis = achillesAnalysisDao.findAnalysisById(ETHNICITY_ANALYSIS);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(ethnicityAnalysis));
    }

    @Override
    public ResponseEntity<QuestionConceptListResponse> getSurveyResults(String surveyConceptId) {
        /* Set up the age and gender names */
        // Too slow and concept names wrong so we hardcode list
        // List<Concept> genders = conceptDao.findByConceptClassId("Gender");

        long longSurveyConceptId = Long.parseLong(surveyConceptId);

        // Get questions for survey
        List<QuestionConcept> questions = questionConceptDao.findSurveyQuestions(longSurveyConceptId);

        // Get survey definition
        QuestionConceptListResponse resp = new QuestionConceptListResponse();
        DbDomain survey = dbDomainDao.findByConceptId(longSurveyConceptId);
        resp.setSurvey(TO_CLIENT_DBDOMAIN.apply(survey));
        // Get all analyses for question list and put the analyses on the question objects
        if (!questions.isEmpty()) {
            // Put ids in array for query to get all results at once
            List<String> qlist = new ArrayList();
            for (QuestionConcept q : questions) {
                qlist.add(String.valueOf(q.getConceptId()));
            }

            List<AchillesAnalysis> analyses = achillesAnalysisDao.findSurveyAnalysisResults(surveyConceptId, qlist);
            QuestionConcept.mapAnalysesToQuestions(questions, analyses);
        }

        resp.setItems(questions.stream().map(TO_CLIENT_QUESTION_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<ConceptAnalysisListResponse> getConceptAnalysisResults(List<String> conceptIds){
        ConceptAnalysisListResponse resp=new ConceptAnalysisListResponse();
        List<ConceptAnalysis> conceptAnalysisList=new ArrayList<>();
        List<Long> analysisIds  = new ArrayList<>();
        analysisIds.add(GENDER_ANALYSIS_ID);
        analysisIds.add(AGE_ANALYSIS_ID);
        analysisIds.add(RACE_ANALYSIS_ID);
        analysisIds.add(ETHNICITY_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_GENDER_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_AGE_ANALYSIS_ID);

        for(String conceptId: conceptIds){
            ConceptAnalysis conceptAnalysis=new ConceptAnalysis();

            List<AchillesAnalysis> analysisList = achillesAnalysisDao.findConceptAnalysisResults(conceptId,analysisIds);

            conceptAnalysis.setConceptId(conceptId);

            for(AchillesAnalysis aa:analysisList){
                    if(aa.getAnalysisId() == GENDER_ANALYSIS_ID){
                        for(AchillesResult ar: aa.getResults()){
                            String analysisStratumName =ar.getAnalysisStratumName();
                            if (analysisStratumName == null || analysisStratumName.equals("")) {
                                ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum2()));
                            }
                        }
                        conceptAnalysis.setGenderAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                    }else if(aa.getAnalysisId() == AGE_ANALYSIS_ID){
                        for(AchillesResult ar: aa.getResults()){
                            String analysisStratumName=ar.getAnalysisStratumName();
                            if (analysisStratumName == null || analysisStratumName.equals("")) {
                                ar.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(ar.getStratum2()));
                            }
                        }
                        conceptAnalysis.setAgeAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                    }else if(aa.getAnalysisId() == RACE_ANALYSIS_ID){
                        for(AchillesResult ar: aa.getResults()){
                            String analysisStratumName=ar.getAnalysisStratumName();
                            if (analysisStratumName == null || analysisStratumName.equals("")) {
                                ar.setAnalysisStratumName(QuestionConcept.raceStratumNameMap.get(ar.getStratum2()));
                            }
                        }
                        conceptAnalysis.setRaceAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                    }else if(aa.getAnalysisId() == ETHNICITY_ANALYSIS_ID){
                        for(AchillesResult ar: aa.getResults()){
                            String analysisStratumName=ar.getAnalysisStratumName();
                            if (analysisStratumName == null || analysisStratumName.equals("")) {
                                ar.setAnalysisStratumName(QuestionConcept.ethnicityStratumNameMap.get(ar.getStratum2()));
                            }
                        }
                        conceptAnalysis.setEthnicityAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                    }else if(aa.getAnalysisId() == MEASUREMENT_GENDER_ANALYSIS_ID){

                        List<AchillesResult> maleResults = new ArrayList<>();
                        List<AchillesResult> femaleResults = new ArrayList<>();
                        List<AchillesResult> otherResults = new ArrayList<>();

                        for(AchillesResult ar: aa.getResults()){
                            String analysisStratumName=ar.getAnalysisStratumName();
                            String stratum2 = ar.getStratum2();
                            if (analysisStratumName == null || analysisStratumName.equals("")) {
                                ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum2()));
                                if(stratum2.equals(MALE_CONCEPT_ID)){
                                    maleResults.add(ar);
                                }else if(stratum2.equals(FEMALE_CONCEPT_ID)){
                                    femaleResults.add(ar);
                                }else{
                                    otherResults.add(ar);
                                }
                            }
                        }

                        AchillesAnalysis maleAnalysis = new AchillesAnalysis(aa);
                        AchillesAnalysis femaleAnalysis = new AchillesAnalysis(aa);
                        AchillesAnalysis otherAnalysis = new AchillesAnalysis(aa);

                        maleAnalysis.setResults(maleResults);
                        femaleAnalysis.setResults(femaleResults);
                        otherAnalysis.setResults(otherResults);

                        conceptAnalysis.setMeasurementValueGenderAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                        conceptAnalysis.setMeasurementValueMaleAnalysis(TO_CLIENT_ANALYSIS.apply(maleAnalysis));
                        conceptAnalysis.setMeasurementValueFemaleAnalysis(TO_CLIENT_ANALYSIS.apply(femaleAnalysis));
                        conceptAnalysis.setMeasurementValueOtherGenderAnalysis(TO_CLIENT_ANALYSIS.apply(otherAnalysis));

                    }else if(aa.getAnalysisId() == MEASUREMENT_AGE_ANALYSIS_ID){

                        for(AchillesResult ar: aa.getResults()){
                            String analysisStratumName=ar.getAnalysisStratumName();
                            if (analysisStratumName == null || analysisStratumName.equals("")) {
                                ar.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(ar.getStratum2()));
                            }
                        }
                        conceptAnalysis.setMeasurementValueAgeAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                    }

            }
            conceptAnalysisList.add(conceptAnalysis);
        }

        resp.setItems(conceptAnalysisList.stream().map(TO_CLIENT_CONCEPTANALYSIS).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }


    /**
     * This method gets concepts with maps to relationship in concept relationship table
     *
     * @param conceptId
     * @return
     */
    @Override
    public ResponseEntity<ConceptListResponse> getSourceConcepts(Long conceptId,Integer minCount) {
        Integer count=minCount;
        if(count == null){
            count = 0;
        }
        List<Concept> conceptList = conceptDao.findSourceConcepts(conceptId,count);
        ConceptListResponse resp = new ConceptListResponse();
        resp.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    /**
     * This method gets concepts with maps to relationship in concept relationship table
     *
     * @param conceptId
     * @return
     */
    @Override
    public ResponseEntity<ConceptListResponse> getParentConcepts(Long conceptId) {
        List<Concept> conceptList = conceptDao.findConceptsMapsToParents(conceptId);
        ConceptListResponse resp = new ConceptListResponse();
        resp.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.AchillesResult> getParticipantCount() {
        AchillesResult result = achillesResultDao.findAchillesResultByAnalysisId(PARTICIPANT_COUNT_ANALYSIS_ID);
        return ResponseEntity.ok(TO_CLIENT_ACHILLES_RESULT.apply(result));
    }

    @Override
    public ResponseEntity<DbDomainListResponse> getDbDomains() {
        List<DbDomain> resultList = dbDomainDao.findByConceptIdNotNull();
        DbDomainListResponse resp = new DbDomainListResponse();
        resp.setItems(resultList.stream().map(TO_CLIENT_DBDOMAIN).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }


}
