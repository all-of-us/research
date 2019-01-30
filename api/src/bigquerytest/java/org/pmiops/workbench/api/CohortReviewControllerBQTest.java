package org.pmiops.workbench.api;

import com.google.gson.Gson;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.TemporalQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.time.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.junit.Assert.fail;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class})
@ComponentScan(basePackages = {"org.pmiops.workbench.cohortreview.*","org.pmiops.workbench.cohortbuilder.*"})
public class CohortReviewControllerBQTest extends BigQueryBaseTest {

  private static final String NAMESPACE = "aou-test";
  private static final String NAME = "test";
  private static final Long PARTICIPANT_ID = 102246L;
  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private ParticipantData expectedCondition1;
  private ParticipantData expectedCondition2;
  private ParticipantData expectedPhysicalMeasure1;
  private ParticipantData expectedPhysicalMeasure2;
  private ParticipantData expectedMeasurement1;
  private ParticipantData expectedMeasurement2;
  private ParticipantData expectedProcedure1;
  private ParticipantData expectedProcedure2;
  private ParticipantData expectedObservation1;
  private ParticipantData expectedObservation2;
  private ParticipantData expectedDrug1;
  private ParticipantData expectedDrug2;
  private ParticipantData expectedAllEvents1;
  private ParticipantData expectedAllEvents2;
  private ParticipantData expectedAllEvents3;
  private ParticipantData expectedAllEvents4;
  private ParticipantData expectedAllEvents5;
  private ParticipantData expectedAllEvents6;
  private ParticipantData expectedAllEvents7;
  private ParticipantData expectedAllEvents8;
  private CdrVersion cdrVersion;
  private Workspace workspace;

  @Autowired
  private CohortReviewController controller;

  @Autowired
  private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private FireCloudService mockFireCloudService;

  private Cohort cohort;

  @TestConfiguration
  @Import({
    WorkspaceServiceImpl.class,
    CohortReviewServiceImpl.class,
    CohortReviewController.class,
    BigQueryService.class,
    ReviewQueryBuilder.class,
    CohortService.class,
    ParticipantCounter.class,
    CohortQueryBuilder.class,
    TemporalQueryBuilder.class,
    QueryBuilderFactory.class,
    ParticipantCounter.class
  })
  @MockBean({
    FireCloudService.class,
    UserRecentResourceService.class,
    ConceptSetService.class
  })
  static class Configuration {
    @Bean
    public GenderRaceEthnicityConcept getGenderRaceEthnicityConcept() {
      Map<String, Map<Long, String>> concepts = new HashMap<>();
      concepts.put(ParticipantCohortStatusColumns.RACE.name(), new HashMap<>());
      concepts.put(ParticipantCohortStatusColumns.GENDER.name(), new HashMap<>());
      concepts.put(ParticipantCohortStatusColumns.ETHNICITY.name(), new HashMap<>());
      return new GenderRaceEthnicityConcept(concepts);
    }

    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
      "p_all_events",
      "p_condition",
      "p_procedure",
      "p_observation",
      "p_measurement",
      "p_drug",
      "p_physical_measure",
      "person",
      "search_person",
      "search_codes",
      "criteria"
    );
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {

    expectedAllEvents1 = new AllEvents()
      .dataId(12751440L)
      .domain("Condition")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .sourceValue("0020")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .numMentions("2")
      .ageAtEvent(28)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2008-07-22 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents2 = new AllEvents()
      .dataId(12751441L)
      .domain("Condition")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .sourceValue("0021")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .ageAtEvent(28)
      .numMentions("2")
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents3 = new AllEvents()
      .dataId(12751446L)
      .domain("Observation")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents4 = new AllEvents()
      .dataId(12751447L)
      .domain("Observation")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents5 = new AllEvents()
      .dataId(12751444L)
      .domain("Procedure")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceValue("val")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents6 = new AllEvents()
      .dataId(12751445L)
      .domain("Procedure")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceValue("val")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents7 = new AllEvents()
      .dataId(12751442L)
      .domain("Condition")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(21)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2001-12-03 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents8 = new AllEvents()
      .dataId(12751443L)
      .domain("Drug")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(21)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2001-12-04 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedCondition1 = new Condition()
      .visitId(1L)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2008-07-22 05:00:00 UTC")
      .numMentions("1")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("name1")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.CONDITION);
    expectedCondition2 = new Condition()
      .visitId(1L)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2008-07-22 05:00:00 UTC")
      .numMentions("1")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("name2")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.CONDITION);
    expectedPhysicalMeasure1 = new PhysicalMeasurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("nits")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.PHYSICAL_MEASURE);
    expectedPhysicalMeasure2 = new PhysicalMeasurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("nits")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.PHYSICAL_MEASURE);
    expectedMeasurement1 = new Measurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("units")
      .labRefRange("range")
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.MEASUREMENT);
    expectedMeasurement2 = new Measurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("units")
      .labRefRange("range")
      .visitId(1L)
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.MEASUREMENT);
    expectedProcedure1 = new Procedure()
      .visitId(1L)
      .firstMention("2009-12-03 05:00:00 UTC")
      .lastMention("2009-12-03 05:00:00 UTC")
      .numMentions("2")
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.PROCEDURE);
    expectedProcedure2 = new Procedure()
      .visitId(1L)
      .firstMention("2009-12-03 05:00:00 UTC")
      .lastMention("2009-12-03 05:00:00 UTC")
      .numMentions("2")
      .ageAtEvent(29)
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.PROCEDURE);
    expectedObservation1 = new Observation()
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.OBSERVATION);
    expectedObservation2 = new Observation()
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.OBSERVATION);
    expectedDrug1 = new Drug()
      .visitId(1L)
      .route("route")
      .strength("str")
      .refills("1")
      .quantity("1.0")
      .numMentions("2")
      .firstMention("2001-12-03 05:00:00 UTC")
      .lastMention("2001-12-03 05:00:00 UTC")
      .ageAtEvent(21)
      .sourceVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2001-12-03 05:00:00 UTC")
      .domainType(DomainType.DRUG);
    expectedDrug2 = new Drug()
      .visitId(1L)
      .route("route")
      .strength("str")
      .refills("1")
      .quantity("1.0")
      .numMentions("2")
      .firstMention("2001-12-03 05:00:00 UTC")
      .lastMention("2001-12-03 05:00:00 UTC")
      .ageAtEvent(21)
      .sourceVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2001-12-04 05:00:00 UTC")
      .domainType(DomainType.DRUG);

    cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersionDao.save(cdrVersion);

    workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setWorkspaceNamespace(NAMESPACE);
    workspace.setFirecloudName(NAME);
    workspaceDao.save(workspace);

    Gson gson = new Gson();

    cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setCriteria(gson.toJson(SearchRequests.males()));
    cohortDao.save(cohort);

    CohortReview review = new CohortReview()
      .cdrVersionId(cdrVersion.getCdrVersionId())
      .matchedParticipantCount(212)
      .cohortId(cohort.getCohortId());
    cohortReviewDao.save(review);

    ParticipantCohortStatusKey key = new ParticipantCohortStatusKey()
      .participantId(PARTICIPANT_ID)
      .cohortReviewId(review.getCohortReviewId());
    ParticipantCohortStatus participantCohortStatus = new ParticipantCohortStatus()
      .participantKey(key);
    participantCohortStatusDao.save(participantCohortStatus);
  }

  @After
  public void tearDown() {
    workspaceDao.delete(workspace.getWorkspaceId());
    cdrVersionDao.delete(cdrVersion.getCdrVersionId());
  }

  @Test
  public void createCohortReview() throws Exception {
    stubMockFirecloudGetWorkspace();

    Cohort cohortWithoutReview = new Cohort();
    cohortWithoutReview.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithoutReview.setCriteria("{\"includes\":[{\"id\":\"includes_9bdr91i2t\",\"items\":[{\"id\":\"items_r0tsp87r4\",\"type\":\"CONDITION\",\"searchParameters\":[{\"parameterId\":\"param25164\"," +
      "\"name\":\"Malignant neoplasm of bronchus and lung\",\"value\":\"C34\",\"type\":\"ICD10\",\"subtype\":\"CM\",\"group\":false,\"domainId\":\"Condition\",\"conceptId\":\"1\"}],\"modifiers\":[]}]}],\"excludes\":[]}");
    cohortDao.save(cohortWithoutReview);

    org.pmiops.workbench.model.CohortReview cohortReview =
      controller.createCohortReview(NAMESPACE,
        NAME,
        cohortWithoutReview.getCohortId(),
        cdrVersion.getCdrVersionId(),
        new CreateReviewRequest()
          .size(1)).getBody();

    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus()).isEqualTo(CohortStatus.NOT_REVIEWED);
  }

  @Test
  public void getParticipantConditionsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.CONDITION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition1, expectedCondition2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition2, expectedCondition1), 2);
  }

  @Test
  public void getParticipantConditionsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.CONDITION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition2), 2);
  }

  @Test
  public void getParticipantPhysicalMeasureSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PHYSICAL_MEASURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure1, expectedPhysicalMeasure2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure2, expectedPhysicalMeasure1), 2);
  }

  @Test
  public void getParticipantPhysicalMeasurePagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PHYSICAL_MEASURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure2), 2);
  }

  @Test
  public void getParticipantMeasurementsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.MEASUREMENT);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement1, expectedMeasurement2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement2, expectedMeasurement1), 2);
  }

  @Test
  public void getParticipantMeasurementsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.MEASUREMENT);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement2), 2);
  }

  @Test
  public void getParticipantProceduresSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PROCEDURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure1, expectedProcedure2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure2, expectedProcedure1), 2);
  }

  @Test
  public void getParticipantProceduresPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PROCEDURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure2), 2);
  }

  @Test
  public void getParticipantObservationsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.OBSERVATION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation1, expectedObservation2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation2, expectedObservation1), 2);
  }

  @Test
  public void getParticipantObservationsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.OBSERVATION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation2), 2);
  }

  @Test
  public void getParticipantDrugsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.DRUG);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug1, expectedDrug2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug2, expectedDrug1), 2);
  }

  @Test
  public void getParticipantDrugsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.DRUG);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug2), 2);
  }

  @Test
  public void getParticipantAllEventsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.ALL_EVENTS);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedAllEvents7), 8);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedAllEvents8), 8);
  }

  @Test
  public void getParticipantAllEventsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.ALL_EVENTS);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest,
      Arrays.asList(expectedAllEvents7,
        expectedAllEvents8,
        expectedAllEvents1,
        expectedAllEvents2,
        expectedAllEvents5,
        expectedAllEvents3,
        expectedAllEvents6,
        expectedAllEvents4), 8);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest,
      Arrays.asList(expectedAllEvents6,
        expectedAllEvents4,
        expectedAllEvents5,
        expectedAllEvents3,
        expectedAllEvents2,
        expectedAllEvents1,
        expectedAllEvents8,
        expectedAllEvents7), 8);
  }

  @Test
  public void getParticipantChartData() throws Exception {
    stubMockFirecloudGetWorkspace();

    ParticipantChartDataListResponse response =
    controller
      .getParticipantChartData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        DomainType.CONDITION.name(),
        null)
      .getBody();

    ParticipantChartData expectedData1 = new ParticipantChartData().ageAtEvent(28).rank(1).standardName("name1").standardVocabulary("SNOMED").startDate("2008-07-22");
    ParticipantChartData expectedData2 = new ParticipantChartData().ageAtEvent(28).rank(1).standardName("name2").standardVocabulary("SNOMED").startDate("2008-08-01");
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(expectedData1).isIn(response.getItems());
    assertThat(expectedData2).isIn(response.getItems());
  }

  @Test
  public void getParticipantChartDataBadCohortId() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller
        .getParticipantChartData(
          NAMESPACE,
          NAME,
          99L,
          cdrVersion.getCdrVersionId(),
          PARTICIPANT_ID,
          DomainType.CONDITION.name(),
          null);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      //Success
      assertThat(nfe.getMessage()).isEqualTo("Not Found: No Cohort exists for cohortId: 99");
    }
  }

  @Test
  public void getParticipantChartDataBadCdrVersionId() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller
        .getParticipantChartData(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          99L,
          PARTICIPANT_ID,
          DomainType.CONDITION.name(),
          null);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      //Success
      assertThat(nfe.getMessage())
        .isEqualTo("Not Found: Cohort Review does not exist for cohortId: "
          + cohort.getCohortId() + ", cdrVersionId: 99");
    }
  }

  @Test
  public void getParticipantChartDataBadLimit() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller
        .getParticipantChartData(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          99L,
          PARTICIPANT_ID,
          DomainType.CONDITION.name(),
          -1);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getParticipantChartDataBadLimitOverHundred() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller
        .getParticipantChartData(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          99L,
          PARTICIPANT_ID,
          DomainType.CONDITION.name(),
          101);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataBadLimit() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller
        .getCohortChartData(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          cdrVersion.getCdrVersionId(),
          DomainType.CONDITION.name(),
          -1);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataBadLimitOverHundred() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller
        .getCohortChartData(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          cdrVersion.getCdrVersionId(),
          DomainType.CONDITION.name(),
          101);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataLab() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response = controller.getCohortChartData(NAMESPACE,
      NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      DomainType.LAB.name(),
      10).getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(new CohortChartData().name("name10").conceptId(10L).count(1L), response.getItems().get(0));
    assertEquals(new CohortChartData().name("name3").conceptId(3L).count(1L), response.getItems().get(1));
    assertEquals(new CohortChartData().name("name9").conceptId(9L).count(1L), response.getItems().get(2));
  }

  @Test
  public void getCohortChartDataDrug() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response = controller.getCohortChartData(NAMESPACE,
      NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      DomainType.DRUG.name(),
      10).getBody();
    assertEquals(1, response.getItems().size());
    assertEquals(new CohortChartData().name("name11").conceptId(1L).count(1L), response.getItems().get(0));
  }

  @Test
  public void getCohortChartDataCondition() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response = controller.getCohortChartData(NAMESPACE,
      NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      DomainType.CONDITION.name(),
      10).getBody();
    assertEquals(2, response.getItems().size());
    assertEquals(new CohortChartData().name("name1").conceptId(1L).count(1L), response.getItems().get(0));
    assertEquals(new CohortChartData().name("name7").conceptId(7L).count(1L), response.getItems().get(1));
  }

  @Test
  public void getCohortChartDataProcedure() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response = controller.getCohortChartData(NAMESPACE,
      NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      DomainType.PROCEDURE.name(),
      10).getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(new CohortChartData().name("name2").conceptId(2L).count(1L), response.getItems().get(0));
    assertEquals(new CohortChartData().name("name4").conceptId(4L).count(1L), response.getItems().get(1));
    assertEquals(new CohortChartData().name("name8").conceptId(8L).count(1L), response.getItems().get(2));
  }

  private void assertResponse(ParticipantDataListResponse response, PageRequest expectedPageRequest, List<ParticipantData> expectedData, int totalCount) {
    List<ParticipantData> data = response.getItems();
    assertThat(response.getCount()).isEqualTo(totalCount);
    assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
    assertThat(data.size()).isEqualTo(expectedData.size());
    int i = 0;
    for (ParticipantData actualData : data) {
      ParticipantData expected = expectedData.get(i++);
      if (expected instanceof Drug) {
        assertThat((Drug) actualData).isEqualTo((Drug) expected);
      } else if (expected instanceof Observation) {
        assertThat((Observation) actualData).isEqualTo((Observation) expected);
      } else if (expected instanceof Condition) {
        assertThat((Condition) actualData).isEqualTo((Condition) expected);
      } else if (expected instanceof Procedure) {
        assertThat((Procedure) actualData).isEqualTo((Procedure) expected);
      } else if (expected instanceof AllEvents) {
        assertThat((AllEvents) actualData).isEqualTo((AllEvents) expected);
      } else if (expected instanceof Measurement) {
        assertThat((Measurement) actualData).isEqualTo((Measurement) expected);
      } else if (expected instanceof PhysicalMeasurement) {
        assertThat((PhysicalMeasurement) actualData).isEqualTo((PhysicalMeasurement) expected);
      }
      assertThat(actualData.getDomainType()).isEqualTo(expected.getDomainType());
      assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate());
    }
  }

  private void stubMockFirecloudGetWorkspace() throws ApiException {
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
    when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
  }
}
