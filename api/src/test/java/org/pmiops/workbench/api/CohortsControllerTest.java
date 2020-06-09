package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.api.ConceptsControllerTest.makeConcept;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdrselector.WorkspaceResourcesServiceImpl;
import org.pmiops.workbench.cohortreview.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.BillingConfig;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.WorkspacesController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CohortsControllerTest {

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String CDR_VERSION_NAME = "cdrVersion";
  private static final String WORKSPACE_NAME = "workspace";
  private static final String WORKSPACE_NAME_2 = "workspace2";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String COHORT_NAME = "cohort";
  private static final String CONCEPT_SET_NAME = "concept_set";
  private static final String CREATOR_EMAIL = "bob@gmail.com";

  private static final Concept CLIENT_CONCEPT_1 =
      new Concept()
          .conceptId(123L)
          .conceptName("a concept")
          .standardConcept(true)
          .conceptCode("conceptA")
          .conceptClassId("classId")
          .vocabularyId("V1")
          .domainId("Condition")
          .countValue(123L)
          .prevalence(0.2F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_2 =
      new Concept()
          .conceptId(789L)
          .standardConcept(false)
          .conceptName("multi word concept")
          .conceptCode("conceptC")
          .conceptClassId("classId3")
          .vocabularyId("V3")
          .domainId("Condition")
          .countValue(789L)
          .prevalence(0.4F)
          .conceptSynonyms(new ArrayList<>());

  private static final DbConcept CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1);
  private static final DbConcept CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2);
  private static DbUser currentUser;

  @Autowired WorkspacesController workspacesController;
  @Autowired CohortsController cohortsController;
  @Autowired ConceptSetsController conceptSetsController;
  @Autowired BillingProjectBufferService billingProjectBufferService;

  Workspace workspace;
  Workspace workspace2;
  DbCdrVersion cdrVersion;
  SearchRequest searchRequest;
  String cohortCriteria;
  String badCohortCriteria;
  private TestMockFactory testMockFactory;
  @Autowired WorkspaceService workspaceService;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortDao cohortDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired ConceptDao conceptDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired DataSetService dataSetService;
  @Autowired UserRecentResourceService userRecentResourceService;
  @Autowired UserDao userDao;
  @Autowired CohortMaterializationService cohortMaterializationService;
  @Autowired FireCloudService fireCloudService;
  @Autowired UserService userService;
  @Autowired CloudStorageService cloudStorageService;
  @Autowired CdrVersionService cdrVersionService;
  @Autowired ComplianceService complianceService;

  @TestConfiguration
  @Import({
    CohortCloningService.class,
    CohortFactoryImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortsController.class,
    CommonMappers.class,
    ConceptService.class,
    ConceptSetMapperImpl.class,
    ConceptSetsController.class,
    ConceptSetService.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    LogsBasedMetricServiceFakeImpl.class,
    NotebooksServiceImpl.class,
    UserMapperImpl.class,
    UserServiceTestConfiguration.class,
    WorkspaceMapperImpl.class,
    WorkspaceResourcesServiceImpl.class,
    WorkspacesController.class,
    WorkspaceServiceImpl.class,
  })
  @MockBean({
    BillingProjectBufferService.class,
    CdrVersionService.class,
    CloudStorageService.class,
    CohortMaterializationService.class,
    ComplianceService.class,
    ConceptBigQueryService.class,
    DataSetService.class,
    DirectoryService.class,
    FireCloudService.class,
    FreeTierBillingService.class,
    LeonardoNotebooksClient.class,
    MonitoringService.class,
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class
  })
  static class Configuration {

    @Bean
    Cloudbilling cloudbilling() {
      return TestMockFactory.createMockedCloudbilling();
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.billing = new BillingConfig();
      workbenchConfig.billing.accountId = "free-tier";
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() throws Exception {
    testMockFactory = new TestMockFactory();
    testMockFactory.stubBufferBillingProject(billingProjectBufferService);
    testMockFactory.stubCreateFcWorkspace(fireCloudService);
    DbUser user = new DbUser();
    user.setUsername(CREATOR_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    currentUser = user;

    cdrVersion = new DbCdrVersion();
    cdrVersion.setName(CDR_VERSION_NAME);
    cdrVersionDao.save(cdrVersion);

    searchRequest = SearchRequests.males();
    cohortCriteria = new Gson().toJson(searchRequest);
    badCohortCriteria =
        "{\n"
            + "  \"includes\": [\n"
            + "    {\n"
            + "      \"id\": \"includes_7dhyhvbch\",\n"
            + "      \"items\": [\n"
            + "        {\n"
            + "          \"id\": \"items_38kgpbn2e\",\n"
            + "          \"type\": \"DRUG\",\n"
            + "          \"searchParameters\": [\n"
            + "            {\n"
            + "              \"parameterId\": \"param11332011819\",\n"
            + "              \"name\": \"Buprenorphine\",\n"
            + "              \"domain\": \"DRUG\",\n"
            + "              \"type\": \"RXNORM\",\n"
            + "              \"group\": false,\n"
            + "              \"attributes\": [],\n"
            + "              \"ancestorData\": true,\n"
            + "              \"standard\": true,\n"
            + "              \"conceptId\": 1133201,\n"
            + "              \"value\": \"1819\"\n"
            + "            }\n"
            + "          ],\n"
            + "          \"modifiers\": [\n"
            + "            {\n"
            + "              \"name\": \"AGE_AT_EVENT\",\n"
            + "              \"operator\": {\n"
            + "                \"label\": \"Any\"\n"
            + "              },\n"
            + "              \"operands\": [\n"
            + "                \"1\"\n"
            + "              ]\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      ],\n"
            + "      \"temporal\": false\n"
            + "    }\n"
            + "  ],\n"
            + "  \"excludes\": [],\n"
            + "  \"dataFilters\": []\n"
            + "}";

    workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace.setBillingAccountName("billing-account");

    workspace2 = new Workspace();
    workspace2.setName(WORKSPACE_NAME_2);
    workspace2.setNamespace(WORKSPACE_NAMESPACE);
    workspace2.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace2.setResearchPurpose(new ResearchPurpose());
    workspace2.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace2.setBillingAccountName("billing-account");

    CLOCK.setInstant(NOW);

    Cohort cohort = new Cohort();
    cohort.setName("demo");
    cohort.setDescription("demo");
    cohort.setType("demo");
    cohort.setCriteria(createDemoCriteria().toString());

    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspace2 = workspacesController.createWorkspace(workspace2).getBody();

    stubGetWorkspace(
        workspace.getNamespace(), WORKSPACE_NAME, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspaceAcl(
        workspace.getNamespace(), WORKSPACE_NAME, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(
        workspace2.getNamespace(), WORKSPACE_NAME_2, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspaceAcl(
        workspace2.getNamespace(), WORKSPACE_NAME_2, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER);
  }

  private JSONObject createDemoCriteria() {
    JSONObject criteria = new JSONObject();
    criteria.append("includes", new JSONArray());
    criteria.append("excludes", new JSONArray());
    return criteria;
  }

  private void stubGetWorkspace(String ns, String name, String creator, WorkspaceAccessLevel access)
      throws Exception {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
    stubGetWorkspaceAcl(ns, name, creator, access);
  }

  private void stubGetWorkspaceAcl(
      String ns, String name, String creator, WorkspaceAccessLevel access) {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(access.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(creator, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, name))
        .thenReturn(workspaceAccessLevelResponse);
  }

  public Cohort createDefaultCohort() {
    Cohort cohort = new Cohort();
    cohort.setName(COHORT_NAME);
    cohort.setCriteria(cohortCriteria);
    return cohort;
  }

  @Test
  public void testGetCohortsInWorkspace() throws Exception {
    Cohort c1 = createDefaultCohort();
    c1.setName("c1");
    c1 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c1).getBody();
    Cohort c2 = createDefaultCohort();
    c2.setName("c2");
    c2 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c2).getBody();

    List<Cohort> cohorts =
        cohortsController
            .getCohortsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();
    assertThat(cohorts).containsAllOf(c1, c2);
    assertThat(cohorts.size()).isEqualTo(2);
  }

  @Test(expected = ServerErrorException.class)
  public void testCreateCohortBadCriteria() {
    Cohort cohort = createDefaultCohort();
    cohort.setCriteria(badCohortCriteria);
    cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();
  }

  @Test
  public void testUpdateCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    cohort.setName("updated-name");
    Cohort updated =
        cohortsController
            .updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort)
            .getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    cohort.setName("updated-name2");
    updated =
        cohortsController
            .updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort)
            .getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    Cohort got =
        cohortsController
            .getCohort(workspace.getNamespace(), workspace.getId(), cohort.getId())
            .getBody();
    assertThat(got).isEqualTo(cohort);
  }

  @Test
  public void testDuplicateCohort() {
    Cohort originalCohort = createDefaultCohort();
    originalCohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), originalCohort)
            .getBody();

    DuplicateCohortRequest params = new DuplicateCohortRequest();
    params.setNewName("New Cohort Name");
    params.setOriginalCohortId(originalCohort.getId());

    Cohort newCohort =
        cohortsController
            .duplicateCohort(workspace.getNamespace(), workspace.getId(), params)
            .getBody();
    newCohort =
        cohortsController
            .getCohort(workspace.getNamespace(), workspace.getId(), newCohort.getId())
            .getBody();

    assertThat(newCohort.getName()).isEqualTo(params.getNewName());
    assertThat(newCohort.getCriteria()).isEqualTo(originalCohort.getCriteria());
    assertThat(newCohort.getType()).isEqualTo(originalCohort.getType());
    assertThat(newCohort.getDescription()).isEqualTo(originalCohort.getDescription());
  }

  @Test(expected = NotFoundException.class)
  public void testGetCohortWrongWorkspace() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    cohortsController.getCohort(workspace2.getNamespace(), WORKSPACE_NAME_2, cohort.getId());
  }

  @Test(expected = ConflictException.class)
  public void testUpdateCohortStaleThrows() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    cohortsController
        .updateCohort(
            workspace.getNamespace(),
            workspace.getId(),
            cohort.getId(),
            new Cohort().name("updated-name").etag(cohort.getEtag()))
        .getBody();

    // Still using the initial etag.
    cohortsController
        .updateCohort(
            workspace.getNamespace(),
            workspace.getId(),
            cohort.getId(),
            new Cohort().name("updated-name2").etag(cohort.getEtag()))
        .getBody();
  }

  @Test
  public void testUpdateCohortInvalidEtagsThrow() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    // TODO: Refactor to be a @Parameterized test case.
    List<String> cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"");
    for (String etag : cases) {
      try {
        cohortsController.updateCohort(
            workspace.getNamespace(),
            workspace.getId(),
            cohort.getId(),
            new Cohort().name("updated-name").etag(etag));
        fail(String.format("expected BadRequestException for etag: %s", etag));
      } catch (BadRequestException e) {
        // expected
      }
    }
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortWorkspaceNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    WorkspaceAccessLevel owner = WorkspaceAccessLevel.OWNER;
    String workspaceName = "badWorkspace";
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setAccessLevel(owner.toString());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, workspaceName)).thenReturn(fcResponse);
    stubGetWorkspaceAcl(
        WORKSPACE_NAMESPACE, workspaceName, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER);
    when(workspaceService.getWorkspaceAccessLevel(WORKSPACE_NAMESPACE, workspaceName))
        .thenThrow(new NotFoundException());
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, workspaceName, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortCdrVersionNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setCdrVersionName("badCdrVersion");
    cohortsController.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortCohortNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName("badCohort");
    cohortsController.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortNoSpecOrCohortName() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    cohortsController.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortPageSizeTooSmall() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(-1);
    cohortsController.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test
  public void testMaterializeCohortPageSizeZero() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(0);
    MaterializeCohortRequest adjustedRequest = new MaterializeCohortRequest();
    adjustedRequest.setCohortName(cohort.getName());
    adjustedRequest.setPageSize(CohortsController.DEFAULT_PAGE_SIZE);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            null, cohortCriteria, null, adjustedRequest))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortPageSizeTooLarge() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(CohortsController.MAX_PAGE_SIZE + 1);
    MaterializeCohortRequest adjustedRequest = new MaterializeCohortRequest();
    adjustedRequest.setCohortName(cohort.getName());
    adjustedRequest.setPageSize(CohortsController.MAX_PAGE_SIZE);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            null, cohortCriteria, null, adjustedRequest))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortNamedCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(null, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortNamedCohortWithConceptSet() throws Exception {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    ConceptSet conceptSet = new ConceptSet().domain(Domain.CONDITION).name(CONCEPT_SET_NAME);
    conceptSet =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(conceptSet)
                    .addedIds(
                        ImmutableList.of(
                            CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    TableQuery tableQuery =
        new TableQuery().tableName("condition_occurrence").conceptSetName(CONCEPT_SET_NAME);
    request.setFieldSet(new FieldSet().tableQuery(tableQuery));
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            null,
            cohortCriteria,
            ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId()),
            request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortNamedCohortWithConceptSetWrongTable() throws Exception {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    ConceptSet conceptSet = new ConceptSet().domain(Domain.CONDITION).name(CONCEPT_SET_NAME);
    conceptSet =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(conceptSet)
                    .addedIds(
                        ImmutableList.of(
                            CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    TableQuery tableQuery =
        new TableQuery().tableName("observation").conceptSetName(CONCEPT_SET_NAME);
    request.setFieldSet(new FieldSet().tableQuery(tableQuery));
    cohortsController.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortNamedCohortWithConceptSetNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    TableQuery tableQuery =
        new TableQuery().tableName("condition_occurrence").conceptSetName(CONCEPT_SET_NAME);
    request.setFieldSet(new FieldSet().tableQuery(tableQuery));
    cohortsController.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test
  public void testMaterializeCohortNamedCohortWithReview() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    DbCohortReview cohortReview = new DbCohortReview();
    cohortReview.setCohortId(cohort.getId());
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setReviewSize(2);
    cohortReview.setReviewedCount(2);
    cohortReviewDao.save(cohortReview);

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            cohortReview, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortWithSpec() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortSpec(cohort.getCriteria());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(null, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortWithEverything() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(123);
    request.setPageToken("token");
    request.setCdrVersionName(CDR_VERSION_NAME);
    List<CohortStatus> statuses =
        ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED);
    request.setStatusFilter(statuses);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(null, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }
}
