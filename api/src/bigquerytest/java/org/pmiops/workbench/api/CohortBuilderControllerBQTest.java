package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultAccessTier;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.cdr.model.DbPerson;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaMenuOption;
import org.pmiops.workbench.model.CriteriaMenuSubOption;
import org.pmiops.workbench.model.CriteriaRequest;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.StandardFlag;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(BeforeAfterSpringTestRunner.class)
// Note: normally we shouldn't need to explicitly import our own @TestConfiguration. This might be
// a bad interaction with BeforeAfterSpringTestRunner.
@Import({
  TestJpaConfig.class,
  CohortBuilderControllerBQTest.Configuration.class,
  StorageConfig.class
})
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

  @TestConfiguration
  @Import({
    BigQueryTestService.class,
    CloudStorageClientImpl.class,
    CohortQueryBuilder.class,
    CohortBuilderServiceImpl.class,
    SearchGroupItemQueryBuilder.class,
    CdrVersionService.class,
    CohortBuilderMapperImpl.class
  })
  @MockBean({
    FireCloudService.class,
    AccessTierService.class,
    CdrVersionService.class,
  })
  static class Configuration {
    @Bean
    public DbUser user() {
      DbUser user = new DbUser();
      user.setUsername("bob@gmail.com");
      return user;
    }
  }

  private CohortBuilderController controller;

  private DbCdrVersion cdrVersion;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CloudStorageClient cloudStorageClient;

  @Autowired private CohortBuilderService cohortBuilderService;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private CdrVersionService cdrVersionService;

  @Autowired private FireCloudService firecloudService;

  @Autowired private PersonDao personDao;

  @Autowired private CBDataFilterDao cbDataFilterDao;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired private AccessTierDao accessTierDao;

  @Mock private Provider<WorkbenchConfig> configProvider;

  private DbCriteria drugNode1;
  private DbCriteria drugNode2;
  private DbCriteria criteriaParent;
  private DbCriteria criteriaChild;
  private DbCriteria icd9;
  private DbCriteria icd10;
  private DbCriteria snomedSource;
  private DbCriteria snomedStandard;
  private DbCriteria cpt4;
  private DbCriteria temporalParent1;
  private DbCriteria temportalChild1;
  private DbCriteria procedureParent1;
  private DbCriteria procedureChild1;
  private DbCriteria surveyNode;
  private DbCriteria questionNode;
  private DbCriteria answerNode;
  private DbPerson dbPerson1;
  private DbPerson dbPerson2;
  private DbPerson dbPerson3;

  @Override
  public List<String> getTableNames() {
    return ImmutableList.of(
        "person",
        "death",
        "cb_search_person",
        "cb_search_all_events",
        "cb_criteria",
        "cb_criteria_ancestor");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    WorkbenchConfig testConfig = WorkbenchConfig.createEmptyConfig();
    testConfig.elasticsearch.enableElasticsearchBackend = false;

    when(configProvider.get()).thenReturn(testConfig);

    when(firecloudService.isUserMemberOfGroup(anyString(), anyString())).thenReturn(true);

    ElasticSearchService elasticSearchService =
        new ElasticSearchService(cbCriteriaDao, cloudStorageClient, configProvider);

    controller =
        new CohortBuilderController(
            cdrVersionService, elasticSearchService, configProvider, cohortBuilderService);

    cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1L);
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersion.setAccessTier(createDefaultAccessTier(accessTierDao));
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    cdrVersion = cdrVersionDao.save(cdrVersion);

    drugNode1 = saveCriteriaWithPath("0", drugCriteriaParent());
    drugNode2 = saveCriteriaWithPath(drugNode1.getPath(), drugCriteriaChild(drugNode1.getId()));

    criteriaParent = saveCriteriaWithPath("0", icd9CriteriaParent());
    criteriaChild =
        saveCriteriaWithPath(criteriaParent.getPath(), icd9CriteriaChild(criteriaParent.getId()));

    icd9 =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD9CM.toString())
                .addStandard(false)
                .addConceptId("44823922")
                .addFullText("+[CONDITION_rank1]")
                .build());
    icd10 =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD10CM.toString())
                .addStandard(false)
                .build());
    snomedStandard =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(true)
                .addConceptId("44823923")
                .addFullText("+[CONDITION_rank1]")
                .build());
    snomedSource =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(false)
                .build());
    cpt4 =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PROCEDURE.toString())
                .addType(CriteriaType.CPT4.toString())
                .addStandard(false)
                .build());

    temporalParent1 =
        saveCriteriaWithPath(
            "0",
            DbCriteria.builder()
                .addAncestorData(false)
                .addCode("001")
                .addConceptId("0")
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD10CM.toString())
                .addGroup(true)
                .addSelectable(true)
                .addStandard(false)
                .addSynonyms("+[CONDITION_rank1]")
                .build());
    temportalChild1 =
        saveCriteriaWithPath(
            temporalParent1.getPath(),
            temportalChild1 =
                DbCriteria.builder()
                    .addParentId(temporalParent1.getId())
                    .addAncestorData(false)
                    .addCode("001.1")
                    .addConceptId("1")
                    .addDomainId(Domain.CONDITION.toString())
                    .addType(CriteriaType.ICD10CM.toString())
                    .addGroup(false)
                    .addSelectable(true)
                    .addStandard(false)
                    .addSynonyms("+[CONDITION_rank1]")
                    .build());

    procedureParent1 =
        saveCriteriaWithPath(
            "0",
            DbCriteria.builder()
                .addParentId(99999)
                .addDomainId(Domain.PROCEDURE.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(true)
                .addCode("386637004")
                .addName("Obstetric procedure")
                .addCount(36673L)
                .addGroup(true)
                .addSelectable(true)
                .addAncestorData(false)
                .addConceptId("4302541")
                .addSynonyms("+[PROCEDURE_rank1]")
                .build());
    procedureChild1 =
        saveCriteriaWithPath(
            procedureParent1.getPath(),
            DbCriteria.builder()
                .addParentId(procedureParent1.getId())
                .addDomainId(Domain.PROCEDURE.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(true)
                .addCode("386639001")
                .addName("Termination of pregnancy")
                .addCount(50L)
                .addGroup(false)
                .addSelectable(true)
                .addAncestorData(false)
                .addConceptId("4")
                .addSynonyms("+[PROCEDURE_rank1]")
                .build());

    surveyNode =
        saveCriteriaWithPath(
            "0",
            DbCriteria.builder()
                .addParentId(0)
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.SURVEY.toString())
                .addGroup(true)
                .addSelectable(true)
                .addStandard(false)
                .addConceptId("22")
                .build());
    questionNode =
        saveCriteriaWithPath(
            surveyNode.getPath(),
            DbCriteria.builder()
                .addParentId(surveyNode.getId())
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.QUESTION.toString())
                .addGroup(true)
                .addSelectable(true)
                .addStandard(false)
                .addName("In what country were you born?")
                .addConceptId("1585899")
                .addSynonyms("[SURVEY_rank1]")
                .build());
    answerNode =
        saveCriteriaWithPath(
            questionNode.getPath(),
            DbCriteria.builder()
                .addParentId(questionNode.getId())
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.ANSWER.toString())
                .addGroup(false)
                .addSelectable(true)
                .addStandard(false)
                .addName("USA")
                .addConceptId("5")
                .addSynonyms("[SURVEY_rank1]")
                .build());

    dbPerson1 = personDao.save(DbPerson.builder().addAgeAtConsent(55).addAgeAtCdr(56).build());
    dbPerson2 = personDao.save(DbPerson.builder().addAgeAtConsent(22).addAgeAtCdr(22).build());
    dbPerson3 = personDao.save(DbPerson.builder().addAgeAtConsent(34).addAgeAtCdr(35).build());
    cbDataFilterDao.save(
        DbDataFilter.builder()
            .addDataFilterId(1L)
            .addDisplayName("displayName1")
            .addName("name1")
            .build());
    cbDataFilterDao.save(
        DbDataFilter.builder()
            .addDataFilterId(2L)
            .addDisplayName("displayName2")
            .addName("name2")
            .build());
  }

  @After
  public void tearDown() {
    delete(
        drugNode1,
        drugNode2,
        criteriaParent,
        criteriaChild,
        icd9,
        icd10,
        snomedSource,
        snomedStandard,
        cpt4,
        temporalParent1,
        temportalChild1,
        procedureParent1,
        procedureChild1,
        surveyNode,
        questionNode,
        answerNode);
    personDao.delete(dbPerson1.getPersonId());
    personDao.delete(dbPerson2.getPersonId());
    personDao.delete(dbPerson3.getPersonId());
  }

  private static SearchParameter icd9() {
    return new SearchParameter()
        .domain(Domain.CONDITION.toString())
        .type(CriteriaType.ICD9CM.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(1L);
  }

  private static SearchParameter icd10() {
    return new SearchParameter()
        .domain(Domain.CONDITION.toString())
        .type(CriteriaType.ICD10CM.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(9L);
  }

  private static SearchParameter snomed() {
    return new SearchParameter()
        .domain(Domain.CONDITION.toString())
        .type(CriteriaType.SNOMED.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(4L);
  }

  private static SearchParameter drug() {
    return new SearchParameter()
        .domain(Domain.DRUG.toString())
        .type(CriteriaType.ATC.toString())
        .group(false)
        .ancestorData(true)
        .standard(true)
        .conceptId(11L);
  }

  private static SearchParameter measurement() {
    return new SearchParameter()
        .domain(Domain.MEASUREMENT.toString())
        .type(CriteriaType.LOINC.toString())
        .subtype(CriteriaSubType.LAB.toString())
        .group(false)
        .ancestorData(false)
        .standard(true)
        .conceptId(3L);
  }

  private static SearchParameter visit() {
    return new SearchParameter()
        .domain(Domain.VISIT.toString())
        .type(CriteriaType.VISIT.toString())
        .group(false)
        .ancestorData(false)
        .standard(true)
        .conceptId(1L);
  }

  private static SearchParameter procedure() {
    return new SearchParameter()
        .domain(Domain.PROCEDURE.toString())
        .type(CriteriaType.CPT4.toString())
        .group(false)
        .ancestorData(false)
        .standard(false)
        .conceptId(10L);
  }

  private static SearchParameter bloodPressure() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BP.toString())
        .ancestorData(false)
        .standard(false)
        .group(false);
  }

  private static SearchParameter hrDetail() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HR_DETAIL.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(903126L);
  }

  private static SearchParameter hr() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HR.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(1586218L);
  }

  private static SearchParameter height() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HEIGHT.toString())
        .group(false)
        .conceptId(903133L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter weight() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WEIGHT.toString())
        .group(false)
        .conceptId(903121L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter bmi() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BMI.toString())
        .group(false)
        .conceptId(903124L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter waistCircumference() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WC.toString())
        .group(false)
        .conceptId(903135L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter hipCircumference() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HC.toString())
        .group(false)
        .conceptId(903136L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter pregnancy() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.PREG.toString())
        .group(false)
        .conceptId(903120L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter wheelchair() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WHEEL.toString())
        .group(false)
        .conceptId(903111L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter age() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.AGE.toString())
        .group(false)
        .ancestorData(false)
        .standard(true);
  }

  private static SearchParameter male() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.GENDER.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(8507L);
  }

  private static SearchParameter race() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.RACE.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(1L);
  }

  private static SearchParameter ethnicity() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.ETHNICITY.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(9898L);
  }

  private static SearchParameter deceased() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.DECEASED.toString())
        .group(false)
        .standard(true)
        .ancestorData(false);
  }

  private static SearchParameter fitbit() {
    return new SearchParameter()
        .domain(Domain.FITBIT.toString())
        .group(false)
        .standard(true)
        .ancestorData(false);
  }

  private static SearchParameter survey() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.SURVEY.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(22L);
  }

  private static SearchParameter surveyAnswer() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.ANSWER.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(5L);
  }

  private static SearchParameter copeSurveyQuestion() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101"))));
  }

  private static SearchParameter copeSurveyQuestionAnyValue() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(ImmutableList.of(new Attribute().name(AttrName.ANY)));
  }

  private static SearchParameter copeSurveyQuestionVersionAndAnyValue() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute().name(AttrName.ANY),
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101"))));
  }

  private static SearchParameter copeSurveyAnswer() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101")),
                new Attribute()
                    .name(AttrName.CAT)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("10"))));
  }

  private static SearchParameter copeSurveyCatAndNum() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101")),
                new Attribute()
                    .name(AttrName.CAT)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("10")),
                new Attribute()
                    .name(AttrName.NUM)
                    .operator(Operator.EQUAL)
                    .operands(ImmutableList.of("10"))));
  }

  private static Modifier ageModifier() {
    return new Modifier()
        .name(ModifierType.AGE_AT_EVENT)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(ImmutableList.of("25"));
  }

  private static Modifier visitModifier() {
    return new Modifier()
        .name(ModifierType.ENCOUNTERS)
        .operator(Operator.IN)
        .operands(ImmutableList.of("1"));
  }

  private static Modifier occurrencesModifier() {
    return new Modifier()
        .name(ModifierType.NUM_OF_OCCURRENCES)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(ImmutableList.of("1"));
  }

  private static Modifier eventDateModifier() {
    return new Modifier()
        .name(ModifierType.EVENT_DATE)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(ImmutableList.of("2009-12-03"));
  }

  private static List<Attribute> wheelchairAttributes() {
    return ImmutableList.of(
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(ImmutableList.of("4023190")));
  }

  private static List<Attribute> bpAttributes() {
    return ImmutableList.of(
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.LESS_THAN_OR_EQUAL_TO)
            .operands(ImmutableList.of("90"))
            .conceptId(903118L),
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.BETWEEN)
            .operands(ImmutableList.of("60", "80"))
            .conceptId(903115L));
  }

  private static DbCriteria drugCriteriaParent() {
    return DbCriteria.builder()
        .addParentId(99999)
        .addDomainId(Domain.DRUG.toString())
        .addType(CriteriaType.ATC.toString())
        .addConceptId("21600932")
        .addGroup(true)
        .addStandard(true)
        .addSelectable(true)
        .build();
  }

  private static DbCriteria drugCriteriaChild(long parentId) {
    return DbCriteria.builder()
        .addParentId(parentId)
        .addDomainId(Domain.DRUG.toString())
        .addType(CriteriaType.RXNORM.toString())
        .addConceptId("1520218")
        .addGroup(false)
        .addSelectable(true)
        .build();
  }

  private static DbCriteria icd9CriteriaParent() {
    return DbCriteria.builder()
        .addParentId(99999)
        .addDomainId(Domain.CONDITION.toString())
        .addType(CriteriaType.ICD9CM.toString())
        .addStandard(false)
        .addCode("001")
        .addName("Cholera")
        .addCount(19L)
        .addGroup(true)
        .addSelectable(true)
        .addAncestorData(false)
        .addConceptId("2")
        .addSynonyms("[CONDITION_rank1]")
        .build();
  }

  private static DbCriteria icd9CriteriaChild(long parentId) {
    return DbCriteria.builder()
        .addParentId(parentId)
        .addDomainId(Domain.CONDITION.toString())
        .addType(CriteriaType.ICD9CM.toString())
        .addStandard(false)
        .addCode("001.1")
        .addName("Cholera")
        .addCount(19L)
        .addGroup(false)
        .addSelectable(true)
        .addAncestorData(false)
        .addConceptId("1")
        .build();
  }

  @Test
  public void findCriteriaMenuOptions() {
    ImmutableList<StandardFlag> standardFlag = ImmutableList.of(new StandardFlag().standard(true));
    ImmutableList<StandardFlag> sourceFlag = ImmutableList.of(new StandardFlag().standard(false));
    ImmutableList<StandardFlag> sourceAndStandardFlags =
        ImmutableList.of(new StandardFlag().standard(false), new StandardFlag().standard(true));
    List<CriteriaMenuOption> options =
        controller.findCriteriaMenuOptions(cdrVersion.getCdrVersionId()).getBody().getItems();

    assertThat(options.size()).isEqualTo(4);
    CriteriaMenuOption option1 =
        new CriteriaMenuOption()
            .domain(Domain.CONDITION.toString())
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.ICD10CM.toString())
                    .standardFlags(sourceFlag))
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.ICD9CM.toString())
                    .standardFlags(sourceFlag))
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.SNOMED.toString())
                    .standardFlags(sourceAndStandardFlags));
    CriteriaMenuOption option2 =
        new CriteriaMenuOption()
            .domain(Domain.DRUG.toString())
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.ATC.toString())
                    .standardFlags(standardFlag))
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.RXNORM.toString())
                    .standardFlags(sourceFlag));
    CriteriaMenuOption option3 =
        new CriteriaMenuOption()
            .domain(Domain.PROCEDURE.toString())
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.CPT4.toString())
                    .standardFlags(sourceFlag))
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.SNOMED.toString())
                    .standardFlags(sourceAndStandardFlags));
    CriteriaMenuOption option4 =
        new CriteriaMenuOption()
            .domain(Domain.SURVEY.toString())
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.PPI.toString())
                    .standardFlags(sourceFlag));
    assertThat(options.contains(option1)).isTrue();
    assertThat(options.contains(option2)).isTrue();
    assertThat(options.contains(option3)).isTrue();
    assertThat(options.contains(option4)).isTrue();
  }

  @Test
  public void findDataFilters() {
    List<DataFilter> filters =
        controller.findDataFilters(cdrVersion.getCdrVersionId()).getBody().getItems();
    assertThat(
            filters.contains(
                new DataFilter().dataFilterId(1L).displayName("displayName1").name("name1")))
        .isTrue();
    assertThat(
            filters.contains(
                new DataFilter().dataFilterId(2L).displayName("displayName2").name("name2")))
        .isTrue();
  }

  @Test
  public void validateAttribute() {
    SearchParameter demo = age();
    Attribute attribute = new Attribute().name(AttrName.NUM);
    demo.attributes(ImmutableList.of(attribute));
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(demo), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo("Bad Request: attribute operator null is not valid.");
    }

    attribute.operator(Operator.BETWEEN);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo("Bad Request: attribute operands are empty.");
    }

    attribute.operands(ImmutableList.of("20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: attribute NUM can only have 2 operands when using the BETWEEN operator");
    }

    attribute.operands(ImmutableList.of("s", "20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: attribute NUM operands must be numeric.");
    }

    attribute.operands(ImmutableList.of("10", "20"));
    attribute.operator(Operator.EQUAL);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: attribute NUM must have one operand when using the EQUAL operator.");
    }
  }

  @Test
  public void validateModifiers() {
    Modifier modifier = ageModifier().operator(null).operands(new ArrayList<>());
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), ImmutableList.of(modifier));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo("Bad Request: modifier operator null is not valid.");
    }

    modifier.operator(Operator.BETWEEN);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo("Bad Request: modifier operands are empty.");
    }

    modifier.operands(ImmutableList.of("20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: modifier AGE_AT_EVENT can only have 2 operands when using the BETWEEN operator");
    }

    modifier.operands(ImmutableList.of("s", "20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: modifier AGE_AT_EVENT operands must be numeric.");
    }

    modifier.operands(ImmutableList.of("10", "20"));
    modifier.operator(Operator.EQUAL);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: modifier AGE_AT_EVENT must have one operand when using the EQUAL operator.");
    }

    modifier.name(ModifierType.EVENT_DATE);
    modifier.operands(ImmutableList.of("10"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: modifier EVENT_DATE must be a valid date.");
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasEHRData() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    searchRequest.addDataFiltersItem("HAS_EHR_DATA");
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasPMData() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    searchRequest.addDataFiltersItem("HAS_PHYSICAL_MEASUREMENT_DATA");
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasPPISurveyData() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    searchRequest.addDataFiltersItem("HAS_PPI_SURVEY_DATA");
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasEHRAndPPISurveyData() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    searchRequest.addDataFiltersItem("HAS_EHR_DATA").addDataFiltersItem("HAS_PPI_SURVEY_DATA");
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void temporalGroupExceptions() {
    SearchGroupItem icd9SGI =
        new SearchGroupItem().type(Domain.CONDITION.toString()).addSearchParametersItem(icd9());

    SearchGroup temporalGroup = new SearchGroup().items(ImmutableList.of(icd9SGI)).temporal(true);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: search group item temporal group null is not valid.");
    }

    icd9SGI.temporalGroup(0);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).");
    }
  }

  @Test
  public void firstMentionOfICD9WithModifiersOrSnomed5DaysAfterICD10WithModifiers() {
    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd9())
            .temporalGroup(0)
            .addModifiersItem(ageModifier());
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1)
            .addModifiersItem(visitModifier());
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(snomed())
            .temporalGroup(0);

    // First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(icd9SGI, snomedSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    // matches icd9SGI in group 0 and icd10SGI in group 1
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsForSurveyWithAgeModifiers() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(),
            ImmutableList.of(survey().conceptId(1585899L)),
            ImmutableList.of(ageModifier()));

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfDrug5DaysBeforeICD10WithModifiers() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug().conceptId(21600932L))
            .temporalGroup(0);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1)
            .addModifiersItem(visitModifier());

    // First Mention Of Drug 5 Days Before ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_BEFORE)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void anyMentionOfCPT5DaysAfterICD10Child() {
    SearchGroupItem cptSGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(
                icd9().type(CriteriaType.CPT4.toString()).group(false).conceptId(1L))
            .temporalGroup(0)
            .addModifiersItem(visitModifier());
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1);

    // Any Mention Of CPT 5 Days After ICD10
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(cptSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.ANY_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void anyMentionOfCPTWithIn5DaysOfVisit() {
    SearchGroupItem cptSGI =
        new SearchGroupItem()
            .type(Domain.PROCEDURE.toString())
            .addSearchParametersItem(procedure())
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(Domain.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(1);

    // Any Mention Of ICD10 Parent within 5 Days of visit
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(visitSGI, cptSGI))
            .temporal(true)
            .mention(TemporalMention.ANY_MENTION)
            .time(TemporalTime.WITHIN_X_DAYS_OF)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfDrugDuringSameEncounterAsMeasurement() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);

    // First Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurement() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);

    // Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurementOrVisit() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(Domain.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(1);

    // Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI, visitSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void lastMentionOfMeasurementOrVisit5DaysAfterDrug() {
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(Domain.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(0);
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug().group(true).conceptId(21600932L))
            .temporalGroup(1);

    // Last Mention Of Measurement or Visit 5 days after Drug
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI, visitSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEvent() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), ImmutableList.of(ageModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEncounter() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(visitModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEventBetween() {
    Modifier modifier =
        ageModifier().operator(Operator.BETWEEN).operands(ImmutableList.of("37", "39"));

    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), ImmutableList.of(modifier));

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(ageModifier(), occurrencesModifier().operands(ImmutableList.of("2"))));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditioChildAgeAtEventAndOccurrencesAndEventDate() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9(), icd9().conceptId(2L)),
            ImmutableList.of(ageModifier(), occurrencesModifier(), eventDateModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEventDate() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(eventDateModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionNumOfOccurrences() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(occurrencesModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9Child() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionParent() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9().group(true).conceptId(44823922L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDemoGender() {
    SearchRequest searchRequest =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(male()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoRace() {
    SearchRequest searchRequest =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(race()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PERSON.toString(), ImmutableList.of(ethnicity()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoDec() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PERSON.toString(), ImmutableList.of(deceased()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countParticipants() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.FITBIT.toString(), ImmutableList.of(fitbit()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoAge() {
    int lo = getTestPeriod().getYears() - 1;
    int hi = getTestPeriod().getYears() + 1;
    SearchParameter demo = age();
    demo.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of(String.valueOf(lo), String.valueOf(hi)))));
    SearchRequest searchRequests =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 2);
  }

  @Test
  public void countSubjectsDemoAgeAtConsent() {
    SearchParameter demo = age();
    demo.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE_AT_CONSENT)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of("29", "30"))));
    SearchRequest searchRequests =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 3);
  }

  @Test
  public void countSubjectsDemoAgeAtCdr() {
    SearchParameter demo = age();
    demo.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE_AT_CDR)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of("29", "30"))));
    SearchRequest searchRequests =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 2);
  }

  @Test
  public void countSubjectsICD9AndDemo() {
    SearchParameter demoAgeSearchParam = age();
    int lo = getTestPeriod().getYears() - 1;
    int hi = getTestPeriod().getYears() + 1;

    demoAgeSearchParam.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of(String.valueOf(lo), String.valueOf(hi)))));

    SearchRequest searchRequests =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(male()), new ArrayList<>());

    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .searchParameters(ImmutableList.of(icd9().conceptId(3L)))
            .modifiers(new ArrayList<>());

    SearchGroupItem anotherNewSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PERSON.toString())
            .searchParameters(ImmutableList.of(demoAgeSearchParam))
            .modifiers(new ArrayList<>());

    searchRequests.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);
    searchRequests.getIncludes().get(0).addItemsItem(anotherNewSearchGroupItem);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 2);
  }

  @Test
  public void countSubjectsDemoExcluded() {
    SearchGroupItem excludeSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PERSON.toString())
            .searchParameters(ImmutableList.of(male()));
    SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

    SearchRequest searchRequests =
        createSearchRequests(Domain.PERSON.toString(), ImmutableList.of(male()), new ArrayList<>());
    searchRequests.getExcludes().add(excludeSearchGroup);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 0);
  }

  @Test
  public void countSubjectsICD9ParentAndICD10ChildCondition() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9().group(true).conceptId(2L), icd10().conceptId(6L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 2);
  }

  @Test
  public void countSubjectsCPTProcedure() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PROCEDURE.toString(),
            ImmutableList.of(procedure().conceptId(4L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsSnomedChildCondition() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.CONDITION.toString(),
            ImmutableList.of(snomed().standard(true).conceptId(6L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsSnomedParentProcedure() {
    SearchParameter snomed = snomed().group(true).standard(true).conceptId(4302541L);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PROCEDURE.toString(), ImmutableList.of(snomed), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsVisit() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.VISIT.toString(), ImmutableList.of(visit().conceptId(10L)), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsVisitModifiers() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.VISIT.toString(),
            ImmutableList.of(visit().conceptId(10L)),
            ImmutableList.of(occurrencesModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsDrugChild() {
    SearchRequest searchRequest =
        createSearchRequests(Domain.DRUG.toString(), ImmutableList.of(drug()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugParent() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.DRUG.toString(),
            ImmutableList.of(drug().group(true).conceptId(21600932L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugParentAndChild() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.DRUG.toString(),
            ImmutableList.of(drug().group(true).conceptId(21600932L), drug()),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugChildEncounter() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.DRUG.toString(), ImmutableList.of(drug()), ImmutableList.of(visitModifier()));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugChildAgeAtEvent() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.DRUG.toString(), ImmutableList.of(drug()), ImmutableList.of(ageModifier()));
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsLabEncounter() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.MEASUREMENT.toString(),
            ImmutableList.of(measurement()),
            ImmutableList.of(visitModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalBetween() {
    SearchParameter lab = measurement();
    lab.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of("0", "1"))));
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.MEASUREMENT.toString(), ImmutableList.of(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalIn() {
    SearchParameter lab = measurement();
    lab.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("1"))));
    SearchRequest searchRequest =
        createSearchRequests(lab.getDomain(), ImmutableList.of(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategorical() {
    SearchParameter lab = measurement();
    Attribute numerical =
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.EQUAL)
            .operands(ImmutableList.of("0.1"));
    Attribute categorical =
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(ImmutableList.of("1", "2"));
    lab.attributes(ImmutableList.of(numerical, categorical));
    SearchRequest searchRequest =
        createSearchRequests(lab.getDomain(), ImmutableList.of(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalAgeAtEvent() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.MEASUREMENT.toString(),
            ImmutableList.of(measurement()),
            ImmutableList.of(ageModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameter() {
    SearchParameter lab1 = measurement();
    SearchParameter lab2 = measurement().conceptId(9L);
    SearchParameter lab3 = measurement().conceptId(11L);
    Attribute labCategorical =
        new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(ImmutableList.of("77"));
    lab3.attributes(ImmutableList.of(labCategorical));
    SearchRequest searchRequest =
        createSearchRequests(
            lab1.getDomain(), ImmutableList.of(lab1, lab2, lab3), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameterSourceAndStandard() {
    SearchParameter icd9 = icd9();
    SearchParameter snomed = snomed().standard(true);
    SearchRequest searchRequest =
        createSearchRequests(icd9.getDomain(), ImmutableList.of(icd9, snomed), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressure() {
    SearchParameter pm = bloodPressure().attributes(bpAttributes());
    SearchRequest searchRequest =
        createSearchRequests(pm.getDomain(), ImmutableList.of(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureAny() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903118L),
            new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903115L));
    SearchParameter pm = bloodPressure().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetail() {
    SearchParameter bpPm = bloodPressure().attributes(bpAttributes());

    List<Attribute> hrAttributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("71")));
    SearchParameter hrPm = hrDetail().attributes(hrAttributes);

    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(),
            ImmutableList.of(bpPm, hrPm),
            new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() {
    SearchParameter bpPm = bloodPressure().attributes(bpAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(bpPm), new ArrayList<>());

    List<Attribute> hrAttributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("71")));
    SearchParameter hrPm = hrDetail().attributes(hrAttributes);
    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PHYSICAL_MEASUREMENT.toString())
            .searchParameters(ImmutableList.of(hrPm))
            .modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    List<Attribute> irrAttributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("4262985")));
    SearchParameter hrIrrPm = hr().attributes(irrAttributes);
    SearchGroupItem heartRateIrrSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PHYSICAL_MEASUREMENT.toString())
            .searchParameters(ImmutableList.of(hrIrrPm))
            .modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsHeartRateAny() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(),
            ImmutableList.of(hrDetail().conceptId(1586218L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsHeartRate() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("45")));
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(),
            ImmutableList.of(hrDetail().conceptId(1586218L).attributes(attributes)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsHeight() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("168")));
    SearchParameter pm = height().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsWeight() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("201")));
    SearchParameter pm = weight().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBMI() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("263")));
    SearchParameter pm = bmi().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWaistCircumferenceAndHipCircumference() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("31")));
    SearchParameter pm = waistCircumference().attributes(attributes);
    SearchParameter pm1 = hipCircumference().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm, pm1), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectPregnant() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("45877994")));
    SearchParameter pm = pregnancy().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWheelChairUser() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsSurvey() {
    // Survey
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(),
            ImmutableList.of(survey().conceptId(1585899L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyQuestion() {
    // Cope Survey
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(), ImmutableList.of(copeSurveyQuestion()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyQuestionVersionAndAnyValue() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(),
            ImmutableList.of(copeSurveyQuestionVersionAndAnyValue()),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyQuestionAnyValue() {
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(),
            ImmutableList.of(copeSurveyQuestionAnyValue()),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyAnswer() {
    // Cope Survey
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(), ImmutableList.of(copeSurveyAnswer()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyCatAndNum() {
    // Cope Survey
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.SURVEY.toString(), ImmutableList.of(copeSurveyCatAndNum()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsQuestion() {
    // Question
    SearchParameter ppiQuestion =
        survey().subtype(CriteriaSubType.QUESTION.toString()).conceptId(1585899L);
    SearchRequest searchRequest =
        createSearchRequests(
            ppiQuestion.getDomain(), ImmutableList.of(ppiQuestion), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsSurveyValueSourceConceptId() {
    // value source concept id
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("7")));
    SearchParameter ppiValueAsConceptId = surveyAnswer().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            ppiValueAsConceptId.getDomain(),
            ImmutableList.of(ppiValueAsConceptId),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsSurveyValueAsNumber() {
    // value as number
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("7")));
    SearchParameter ppiValueAsNumer = surveyAnswer().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            ppiValueAsNumer.getDomain(), ImmutableList.of(ppiValueAsNumer), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
    assertParticipants(response, 1);
  }

  @Test
  public void findDemoChartInfo() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                cdrVersion.getCdrVersionId(),
                GenderOrSexType.GENDER.toString(),
                AgeType.AGE.toString(),
                searchRequest)
            .getBody();
    assertDemographics(response);
  }

  @Test
  public void findDemoChartInfoGenderAgeAtConsentWithEHRData() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());
    searchRequest.addDataFiltersItem("HAS_EHR_DATA");

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                cdrVersion.getCdrVersionId(),
                GenderOrSexType.GENDER.toString(),
                AgeType.AGE_AT_CONSENT.toString(),
                searchRequest)
            .getBody();
    assertDemographics(response);
  }

  @Test
  public void findDemoChartInfoSexAtBirthAgeAtCdr() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                cdrVersion.getCdrVersionId(),
                GenderOrSexType.SEX_AT_BIRTH.toString(),
                AgeType.AGE_AT_CDR.toString(),
                searchRequest)
            .getBody();
    assertDemographics(response);
  }

  @Test
  public void findCriteriaForCohortEdit() {
    ImmutableList<Long> sourceConceptIds = ImmutableList.of(44823922L);
    ImmutableList<Long> standardConceptIds = ImmutableList.of(44823923L);
    CriteriaRequest request =
        new CriteriaRequest()
            .sourceConceptIds(sourceConceptIds)
            .standardConceptIds(standardConceptIds);
    List<Criteria> criteriaList =
        controller
            .findCriteriaForCohortEdit(
                cdrVersion.getCdrVersionId(), Domain.CONDITION.toString(), request)
            .getBody()
            .getItems();
    assertThat(criteriaList).hasSize(2);
    assertThat(criteriaList.get(0).getId()).isEqualTo(icd9.getId());
    assertThat(criteriaList.get(1).getId()).isEqualTo(snomedStandard.getId());
  }

  @Test
  public void filterBigQueryConfig_WithoutTableName() {
    final String statement = "my statement ${projectId}.${dataSetId}.myTableName";
    QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build();
    final String expectedResult = "my statement " + getTablePrefix() + ".myTableName";
    assertThat(expectedResult)
        .isEqualTo(bigQueryService.filterBigQueryConfig(queryJobConfiguration).getQuery());
  }

  protected String getTablePrefix() {
    DbCdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
    return cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset();
  }

  private Period getTestPeriod() {
    DateTime birthDate = new DateTime(1980, 8, 1, 0, 0, 0, 0);
    DateTime now = new DateTime();
    return new Period(birthDate, now);
  }

  private SearchRequest createSearchRequests(
      String type, List<SearchParameter> parameters, List<Modifier> modifiers) {
    final SearchGroupItem searchGroupItem =
        new SearchGroupItem().type(type).searchParameters(parameters).modifiers(modifiers);

    final SearchGroup searchGroup = new SearchGroup().addItemsItem(searchGroupItem);

    List<SearchGroup> groups = new ArrayList<>();
    groups.add(searchGroup);
    return new SearchRequest().includes(groups);
  }

  private void assertParticipants(ResponseEntity response, Integer expectedCount) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long participantCount = (Long) response.getBody();
    assertThat(participantCount).isEqualTo(expectedCount);
  }

  private DbCriteria saveCriteriaWithPath(String path, DbCriteria criteria) {
    criteria = cbCriteriaDao.save(criteria);
    String pathEnd = String.valueOf(criteria.getId());
    criteria.setPath(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    return cbCriteriaDao.save(criteria);
  }

  private void delete(DbCriteria... criteriaList) {
    Arrays.stream(criteriaList).forEach(c -> cbCriteriaDao.delete(c.getId()));
  }

  private void assertDemographics(DemoChartInfoListResponse response) {
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(new DemoChartInfo().name("MALE").race("Asian").ageRange("45-64").count(1L))
        .isEqualTo(response.getItems().get(0));
    assertThat(new DemoChartInfo().name("MALE").race("Caucasian").ageRange("18-44").count(1L))
        .isEqualTo(response.getItems().get(1));
  }
}
