package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.AllEvents;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Condition;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.PageFilterType;
import org.pmiops.workbench.model.PageRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.ReviewFilter;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.workspaces.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class})
@ComponentScan(
    basePackages = {"org.pmiops.workbench.cohortreview.*", "org.pmiops.workbench.cohortbuilder.*"})
public class CohortReviewControllerBQTest extends BigQueryBaseTest {

  private static final String NAMESPACE = "aou-test";
  private static final String NAME = "test";
  private static final Long PARTICIPANT_ID = 102246L;
  private static final Long PARTICIPANT_ID2 = 102247L;
  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private CdrVersion cdrVersion;
  private Workspace workspace;

  @Autowired private CohortReviewController controller;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired private CohortDao cohortDao;

  @Autowired private CohortReviewDao cohortReviewDao;

  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired private FireCloudService mockFireCloudService;

  @Autowired private UserDao userDao;

  @Mock private Provider<User> userProvider;

  private Cohort cohort;
  private CohortReview review;

  @TestConfiguration
  @Import({
    WorkspaceServiceImpl.class,
    WorkspaceMapper.class,
    CohortReviewServiceImpl.class,
    CohortReviewController.class,
    BigQueryTestService.class,
    ReviewQueryBuilder.class,
    CohortCloningService.class,
    CohortQueryBuilder.class,
    SearchGroupItemQueryBuilder.class
  })
  @MockBean({
    FireCloudService.class,
    UserRecentResourceService.class,
    CohortFactory.class,
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
        "cb_review_all_events", "person", "cb_search_person", "cb_search_all_events", "death");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    controller.setUserProvider(userProvider);

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

    review =
        new CohortReview()
            .cdrVersionId(cdrVersion.getCdrVersionId())
            .matchedParticipantCount(212)
            .creationTime(new Timestamp(new Date().getTime()))
            .lastModifiedTime(new Timestamp(new Date().getTime()))
            .cohortId(cohort.getCohortId());
    cohortReviewDao.save(review);

    ParticipantCohortStatusKey key =
        new ParticipantCohortStatusKey()
            .participantId(PARTICIPANT_ID)
            .cohortReviewId(review.getCohortReviewId());
    ParticipantCohortStatus participantCohortStatus =
        new ParticipantCohortStatus().participantKey(key);
    participantCohortStatusDao.save(participantCohortStatus);

    ParticipantCohortStatusKey key2 =
        new ParticipantCohortStatusKey()
            .participantId(PARTICIPANT_ID2)
            .cohortReviewId(review.getCohortReviewId());
    ParticipantCohortStatus participantCohortStatus2 =
        new ParticipantCohortStatus().participantKey(key2);
    participantCohortStatusDao.save(participantCohortStatus2);
  }

  @After
  public void tearDown() {
    workspaceDao.delete(workspace.getWorkspaceId());
    cdrVersionDao.delete(cdrVersion.getCdrVersionId());
  }

  private static ParticipantData expectedAllEvents1() {
    return new AllEvents()
        .domain("Condition")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0020")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .route("route")
        .dose("1.0")
        .strength("str")
        .unit("unit")
        .refRange("range")
        .numMentions("2")
        .firstMention("2008-07-22 05:00:00 UTC")
        .lastMention("2008-07-22 05:00:00 UTC")
        .visitType("visit")
        .value("1.0")
        .itemDate("2008-07-22 05:00:00 UTC")
        .standardName("SNOMED")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L)
        .domainType(DomainType.ALL_EVENTS);
  }

  private static ParticipantData expectedAllEvents2() {
    return new AllEvents()
        .domain("Condition")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0021")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .route("route")
        .dose("1.0")
        .strength("str")
        .unit("unit")
        .refRange("range")
        .numMentions("2")
        .firstMention("2008-08-01 05:00:00 UTC")
        .lastMention("2008-08-01 05:00:00 UTC")
        .visitType("visit")
        .value("1.0")
        .itemDate("2008-08-01 05:00:00 UTC")
        .standardName("SNOMED")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L)
        .domainType(DomainType.ALL_EVENTS);
  }

  private static ParticipantData expectedCondition1() {
    return new Condition()
        .visitType("visit")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0020")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .itemDate("2008-07-22 05:00:00 UTC")
        .standardName("SNOMED")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L)
        .domainType(DomainType.CONDITION);
  }

  private static ParticipantData expectedCondition2() {
    return new Condition()
        .visitType("visit")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0021")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .itemDate("2008-08-01 05:00:00 UTC")
        .standardName("SNOMED")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L)
        .domainType(DomainType.CONDITION);
  }

  @Test
  public void getCohortReviewsInWorkspace() throws Exception {
    stubMockFirecloudGetWorkspace();
    org.pmiops.workbench.model.CohortReview expectedReview =
        new org.pmiops.workbench.model.CohortReview()
            .cohortReviewId(review.getCohortReviewId())
            .reviewSize(review.getReviewSize())
            .reviewStatus(review.getReviewStatusEnum())
            .cdrVersionId(review.getCdrVersionId())
            .cohortDefinition(review.getCohortDefinition())
            .cohortName(review.getCohortName())
            .cohortId(review.getCohortId())
            .creationTime(review.getCreationTime().toString())
            .lastModifiedTime(review.getLastModifiedTime().getTime())
            .matchedParticipantCount(review.getMatchedParticipantCount())
            .reviewedCount(review.getReviewedCount())
            .etag(Etags.fromVersion(review.getVersion()));
    assertEquals(
        expectedReview,
        controller.getCohortReviewsInWorkspace(NAMESPACE, NAME).getBody().getItems().get(0));
  }

  @Test
  public void createCohortReview() throws Exception {
    stubMockFirecloudGetWorkspace();

    Cohort cohortWithoutReview = new Cohort();
    cohortWithoutReview.setWorkspaceId(workspace.getWorkspaceId());
    String criteria =
        "{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
            + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
            + "\"ICD10CM\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1,\"value\":\"C34\"}],"
            + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[]}";
    cohortWithoutReview.setCriteria(criteria);
    cohortDao.save(cohortWithoutReview);

    org.pmiops.workbench.model.CohortReview cohortReview =
        controller
            .createCohortReview(
                NAMESPACE,
                NAME,
                cohortWithoutReview.getCohortId(),
                cdrVersion.getCdrVersionId(),
                new CreateReviewRequest().size(1))
            .getBody();

    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
        .isEqualTo(CohortStatus.NOT_REVIEWED);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getDeceased()).isEqualTo(false);
  }

  @Test
  public void getParticipantConditionsSorting() throws Exception {
    PageRequest expectedPageRequest =
        new PageRequest().page(0).pageSize(25).sortOrder(SortOrder.ASC).sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.CONDITION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    // no sort order or column
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID, testFilter)
            .getBody();

    assertResponse(
        response,
        expectedPageRequest,
        Arrays.asList(expectedCondition1(), expectedCondition2()),
        2);

    // added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID, testFilter)
            .getBody();

    assertResponse(
        response,
        expectedPageRequest,
        Arrays.asList(expectedCondition2(), expectedCondition1()),
        2);
  }

  @Test
  public void getParticipantConditionsPagination() throws Exception {
    PageRequest expectedPageRequest =
        new PageRequest().page(0).pageSize(1).sortOrder(SortOrder.ASC).sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.CONDITION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    // page 1 should have 1 item
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID, testFilter)
            .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition1()), 2);

    // page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID, testFilter)
            .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition2()), 2);
  }

  @Test
  public void getParticipantAllEventsPagination() throws Exception {
    PageRequest expectedPageRequest =
        new PageRequest().page(0).pageSize(1).sortOrder(SortOrder.ASC).sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.ALL_EVENTS);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    // page 1 should have 1 item
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID2, testFilter)
            .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedAllEvents1()), 2);

    // page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID2, testFilter)
            .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedAllEvents2()), 2);
  }

  @Test
  public void getParticipantAllEventsSorting() throws Exception {
    PageRequest expectedPageRequest =
        new PageRequest().page(0).pageSize(25).sortOrder(SortOrder.ASC).sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.ALL_EVENTS);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    // no sort order or column
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID2, testFilter)
            .getBody();

    assertResponse(
        response,
        expectedPageRequest,
        Arrays.asList(expectedAllEvents1(), expectedAllEvents2()),
        2);

    // added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response =
        controller
            .getParticipantData(
                NAMESPACE, NAME, review.getCohortReviewId(), PARTICIPANT_ID2, testFilter)
            .getBody();

    assertResponse(
        response,
        expectedPageRequest,
        Arrays.asList(expectedAllEvents2(), expectedAllEvents1()),
        2);
  }

  @Test
  public void getParticipantChartData() throws Exception {
    stubMockFirecloudGetWorkspace();

    ParticipantChartDataListResponse response =
        controller
            .getParticipantChartData(
                NAMESPACE,
                NAME,
                review.getCohortReviewId(),
                PARTICIPANT_ID,
                DomainType.CONDITION.name(),
                null)
            .getBody();

    ParticipantChartData expectedData1 =
        new ParticipantChartData()
            .ageAtEvent(28)
            .rank(1)
            .standardName("SNOMED")
            .standardVocabulary("SNOMED")
            .startDate("2008-07-22");
    ParticipantChartData expectedData2 =
        new ParticipantChartData()
            .ageAtEvent(28)
            .rank(1)
            .standardName("SNOMED")
            .standardVocabulary("SNOMED")
            .startDate("2008-08-01");
    ParticipantChartData expectedData3 =
        new ParticipantChartData()
            .ageAtEvent(21)
            .rank(1)
            .standardName("name")
            .standardVocabulary("CPT4")
            .startDate("2001-12-03");
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(expectedData1).isIn(response.getItems());
    assertThat(expectedData2).isIn(response.getItems());
  }

  @Test
  public void getParticipantChartDataBadLimit() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller.getParticipantChartData(
          NAMESPACE,
          NAME,
          review.getCohortReviewId(),
          PARTICIPANT_ID,
          DomainType.CONDITION.name(),
          -1);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getParticipantChartDataBadLimitOverHundred() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller.getParticipantChartData(
          NAMESPACE,
          NAME,
          review.getCohortReviewId(),
          PARTICIPANT_ID,
          DomainType.CONDITION.name(),
          101);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataBadLimit() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller.getCohortChartData(
          NAMESPACE, NAME, review.getCohortReviewId(), DomainType.CONDITION.name(), -1);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataBadLimitOverHundred() throws Exception {
    stubMockFirecloudGetWorkspace();

    try {
      controller.getCohortChartData(
          NAMESPACE, NAME, review.getCohortReviewId(), DomainType.CONDITION.name(), 101);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataLab() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, review.getCohortReviewId(), DomainType.LAB.name(), 10)
            .getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name10").conceptId(10L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name3").conceptId(3L).count(1L), response.getItems().get(1));
    assertEquals(
        new CohortChartData().name("name9").conceptId(9L).count(1L), response.getItems().get(2));
  }

  @Test
  public void getCohortChartDataDrug() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, review.getCohortReviewId(), DomainType.DRUG.name(), 10)
            .getBody();
    assertEquals(1, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name11").conceptId(1L).count(1L), response.getItems().get(0));
  }

  @Test
  public void getCohortChartDataCondition() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, review.getCohortReviewId(), DomainType.CONDITION.name(), 10)
            .getBody();
    assertEquals(2, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name1").conceptId(1L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name7").conceptId(7L).count(1L), response.getItems().get(1));
  }

  @Test
  public void getCohortChartDataProcedure() throws Exception {
    stubMockFirecloudGetWorkspace();

    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, review.getCohortReviewId(), DomainType.PROCEDURE.name(), 10)
            .getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name2").conceptId(2L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name4").conceptId(4L).count(1L), response.getItems().get(1));
    assertEquals(
        new CohortChartData().name("name8").conceptId(8L).count(1L), response.getItems().get(2));
  }

  @Test
  public void getVocabularies() throws Exception {
    stubMockFirecloudGetWorkspace();

    VocabularyListResponse response =
        controller.getVocabularies(NAMESPACE, NAME, review.getCohortReviewId()).getBody();
    assertEquals(20, response.getItems().size());
    assertEquals(
        new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("CPT4"),
        response.getItems().get(0));
    assertEquals(
        new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD10CM"),
        response.getItems().get(1));
    assertEquals(
        new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD9CM"),
        response.getItems().get(2));
  }

  private void assertResponse(
      ParticipantDataListResponse response,
      PageRequest expectedPageRequest,
      List<ParticipantData> expectedData,
      int totalCount) {
    List<ParticipantData> data = response.getItems();
    assertThat(response.getCount()).isEqualTo(totalCount);
    assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
    assertThat(data.size()).isEqualTo(expectedData.size());
    int i = 0;
    for (ParticipantData actualData : data) {
      ParticipantData expected = expectedData.get(i++);
      assertThat(actualData).isEqualTo(expected);
      assertThat(actualData.getDomainType()).isEqualTo(expected.getDomainType());
      assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate());
    }
  }

  private void stubMockFirecloudGetWorkspace() {
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
    when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
  }
}
