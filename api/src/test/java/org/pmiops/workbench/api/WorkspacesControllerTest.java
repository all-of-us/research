package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static junit.framework.TestCase.fail;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.BigQueryConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ResearchPurposeReviewRequest;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.ShareWorkspaceResponse;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkspacesControllerTest {
  private static final Instant NOW = Instant.now();
  private static final long NOW_TIME = Timestamp.from(NOW).getTime();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";

  @TestConfiguration
  @Import({
    WorkspacesController.class,
    WorkspaceServiceImpl.class,
    CohortsController.class,
    CohortService.class,
    CohortReviewController.class,
    CohortReviewServiceImpl.class
  })
  @MockBean({
    FireCloudService.class,
    CohortMaterializationService.class,
    CloudStorageService.class,
    BigQueryService.class,
    CodeDomainLookupService.class,
    ParticipantCounter.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    @Qualifier("apiHostName")
    String apiHostName() {
      return "https://api.blah.com";
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = new WorkbenchConfig();
      config.bigquery = new BigQueryConfig();
      config.bigquery.projectId = "project";
      config.bigquery.dataSetId = "dataset";
      return config;
    }

    @Bean
    User user() {
      // Allows for wiring of the initial Provider<User>; actual mocking of the
      // user is achieved via setUserProvider().
      return null;
    }
  }

  @Autowired
  FireCloudService fireCloudService;
  @Autowired
  BigQueryService bigQueryService;
  @Autowired
  WorkspaceDao workspaceDao;
  @Mock
  WorkspaceService workspaceService;
  @Autowired
  UserDao userDao;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Mock
  Provider<User> userProvider;
  @Autowired
  CohortsController cohortsController;
  @Autowired
  CohortReviewController cohortReviewController;
  @Autowired
  WorkspacesController workspacesController;

  private CdrVersion cdrVersion;
  private String cdrVersionId;

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail(LOGGED_IN_USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName("TestBillingProject1");
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    workspacesController.setUserProvider(userProvider);

    cdrVersion = new CdrVersion();
    cdrVersion.setName("1");
    cdrVersion = cdrVersionDao.save(cdrVersion);
    cdrVersionId = Long.toString(cdrVersion.getCdrVersionId());

    CLOCK.setInstant(NOW);
  }

  private void stubGetWorkspace(String ns, String name, String creator,
      WorkspaceAccessLevel access) throws Exception {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(
      fcResponse
    );
  }

  private void stubBigQueryCohortCalls() {
    QueryResult queryResult = mock(QueryResult.class);
    Iterable testIterable = new Iterable() {
        @Override
        public Iterator iterator() {
          List<FieldValue> list = new ArrayList<>();
          list.add(null);
          return list.iterator();
        }
      };
    Map<String, Integer> rm = ImmutableMap.<String, Integer>builder()
        .put("person_id", 0)
        .put("birth_datetime", 1)
        .put("gender_concept_id", 2)
        .put("race_concept_id", 3)
        .put("ethnicity_concept_id", 4)
        .put("count", 5)
        .build();

    when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
    when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
    when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
    when(queryResult.iterateAll()).thenReturn(testIterable);
    when(bigQueryService.getLong(null, 0)).thenReturn(0L);
    when(bigQueryService.getString(null, 1)).thenReturn("1");
    when(bigQueryService.getLong(null, 2)).thenReturn(0L);
    when(bigQueryService.getLong(null, 3)).thenReturn(0L);
    when(bigQueryService.getLong(null, 4)).thenReturn(0L);
    when(bigQueryService.getLong(null, 5)).thenReturn(0L);
  }

  public Workspace createDefaultWorkspace() throws Exception {
    ResearchPurpose researchPurpose = new ResearchPurpose();
    researchPurpose.setDiseaseFocusedResearch(true);
    researchPurpose.setDiseaseOfFocus("cancer");
    researchPurpose.setMethodsDevelopment(true);
    researchPurpose.setControlSet(true);
    researchPurpose.setAggregateAnalysis(true);
    researchPurpose.setAncestry(true);
    researchPurpose.setCommercialPurpose(true);
    researchPurpose.setPopulation(true);
    researchPurpose.setPopulationOfFocus("population");
    researchPurpose.setAdditionalNotes("additional notes");
    researchPurpose.setTimeRequested(new Long(1000));
    researchPurpose.setTimeReviewed(new Long(1500));
    researchPurpose.setReviewRequested(true);
    researchPurpose.setApproved(false);
    Workspace workspace = new Workspace();
    workspace.setId("name");
    workspace.setName("name");
    workspace.setNamespace("namespace");
    workspace.setDescription("description");
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(researchPurpose);
    workspace.setUserRoles(new ArrayList<UserRole>());
    workspace.setCdrVersionId(cdrVersionId);
    stubGetWorkspace("namespace", "name", LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    return workspace;
  }

  public Cohort createDefaultCohort(String name) {
    Cohort cohort = new Cohort();
    cohort.setName(name);
    cohort.setCriteria(new Gson().toJson(SearchRequests.males()));
    return cohort;
  }

  @Test
  public void testCreateWorkspace() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    verify(fireCloudService).createWorkspace(workspace.getNamespace(), workspace.getName());

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    Workspace workspace2 =
        workspacesController.getWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody().getWorkspace();
    assertThat(workspace2.getCreationTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getLastModifiedTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getCdrVersionId()).isEqualTo(cdrVersionId);
    assertThat(workspace2.getCreator()).isEqualTo(LOGGED_IN_USER_EMAIL);
    assertThat(workspace2.getDataAccessLevel()).isEqualTo(DataAccessLevel.PROTECTED);
    assertThat(workspace2.getDescription()).isEqualTo("description");
    assertThat(workspace2.getId()).isEqualTo("name");
    assertThat(workspace2.getName()).isEqualTo("name");
    assertThat(workspace2.getResearchPurpose().getDiseaseFocusedResearch()).isTrue();
    assertThat(workspace2.getResearchPurpose().getDiseaseOfFocus()).isEqualTo("cancer");
    assertThat(workspace2.getResearchPurpose().getMethodsDevelopment()).isTrue();
    assertThat(workspace2.getResearchPurpose().getControlSet()).isTrue();
    assertThat(workspace2.getResearchPurpose().getAggregateAnalysis()).isTrue();
    assertThat(workspace2.getResearchPurpose().getAncestry()).isTrue();
    assertThat(workspace2.getResearchPurpose().getCommercialPurpose()).isTrue();
    assertThat(workspace2.getResearchPurpose().getPopulation()).isTrue();
    assertThat(workspace2.getResearchPurpose().getPopulationOfFocus()).isEqualTo("population");
    assertThat(workspace2.getResearchPurpose().getAdditionalNotes()).isEqualTo("additional notes");
    assertThat(workspace2.getNamespace()).isEqualTo("namespace");
    assertThat(workspace2.getResearchPurpose().getReviewRequested()).isTrue();
    assertThat(workspace2.getResearchPurpose().getTimeRequested()).isEqualTo(NOW_TIME);

    //Test that the correct owner is added.
    assertThat(workspace2.getUserRoles().size()).isEqualTo(1);
    assertThat(workspace2.getUserRoles().get(0).getRole()).isEqualTo(WorkspaceAccessLevel.OWNER);
  }

  @Test
  public void testCreateWorkspaceAlreadyApproved() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspace.getResearchPurpose().setApproved(true);
    workspacesController.createWorkspace(workspace);

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    Workspace workspace2 =
        workspacesController.getWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody().getWorkspace();
    assertThat(workspace2.getResearchPurpose().getApproved()).isNotEqualTo(true);
  }

  @Test
  public void testCreateMultipleFirecloudSameName() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);

    Workspace workspace2 = createDefaultWorkspace();
    workspace2.setName(workspace2.getName() + ' ');
    doThrow(new ConflictException("Conflict")).when(fireCloudService)
        .createWorkspace(workspace2.getNamespace(), workspace2.getId());
    stubGetWorkspace(workspace2.getNamespace(), workspace2.getId() + '0',
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    Workspace workspaceCreated =
        workspacesController.createWorkspace(workspace2).getBody();

    assertThat(workspaceCreated.getId()).isEqualTo(workspace2.getId() + '0');

  }

  @Test(expected = NotFoundException.class)
  public void testDeleteWorkspace() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    verify(fireCloudService).createWorkspace(workspace.getNamespace(), workspace.getId());

    workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getName());

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    Workspace workspace2 =
        workspacesController.getWorkspace(workspace.getNamespace(), workspace.getName())
            .getBody().getWorkspace();
  }

  @Test
  public void testApproveWorkspace() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ResearchPurpose researchPurpose = ws.getResearchPurpose();
    researchPurpose.setApproved(null);
    researchPurpose.setTimeReviewed(null);
    workspacesController.createWorkspace(ws);

    ResearchPurposeReviewRequest request = new ResearchPurposeReviewRequest();
    request.setApproved(true);
    workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request);
    stubGetWorkspace(ws.getNamespace(), ws.getName(), ws.getCreator(), WorkspaceAccessLevel.OWNER);
    ws = workspacesController.getWorkspace(ws.getNamespace(), ws.getName()).getBody().getWorkspace();
    researchPurpose = ws.getResearchPurpose();

    assertThat(researchPurpose.getApproved()).isTrue();
    assertThat(researchPurpose.getTimeReviewed()).isEqualTo(NOW_TIME);
  }

  @Test
  public void testUpdateWorkspace() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();

    ws.setName("updated-name");
    stubGetWorkspace(ws.getNamespace(), ws.getId(),
        ws.getCreator(), WorkspaceAccessLevel.OWNER);
    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    request.setWorkspace(ws);
    Workspace updated =
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).getBody();
    ws.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(ws);

    ws.setName("updated-name2");
    stubGetWorkspace(ws.getNamespace(), ws.getId(),
        ws.getCreator(), WorkspaceAccessLevel.OWNER);
    updated = workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).getBody();
    ws.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(ws);
    stubGetWorkspace(ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.OWNER);
    Workspace got = workspacesController.getWorkspace(ws.getNamespace(), ws.getId()).getBody().getWorkspace();
    assertThat(got).isEqualTo(ws);
  }

  @Test(expected = ForbiddenException.class)
  public void testReaderUpdateWorkspaceThrows() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();

    ws.setName("updated-name");
    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    request.setWorkspace(ws);
    stubGetWorkspace(ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.READER);
    Workspace updated =
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).getBody();
  }

  @Test(expected = ConflictException.class)
  public void testUpdateWorkspaceStaleThrows() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    request.setWorkspace(new Workspace().name("updated-name").etag(ws.getEtag()));
    stubGetWorkspace(ws.getNamespace(), ws.getId(),
        ws.getCreator(), WorkspaceAccessLevel.OWNER);
    workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(),
        request).getBody();

    // Still using the initial now-stale etag; this should throw.
    stubGetWorkspace(ws.getNamespace(), ws.getId(),
        ws.getCreator(), WorkspaceAccessLevel.OWNER);
    request.setWorkspace(new Workspace().name("updated-name2").etag(ws.getEtag()));
    workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(),
        request).getBody();
  }

  @Test
  public void testUpdateWorkspaceInvalidEtagsThrow() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();

    // TODO: Refactor to be a @Parameterized test case.
    List<String> cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"");
    for (String etag : cases) {
      try {
        stubGetWorkspace(ws.getNamespace(), ws.getId(),
            ws.getCreator(), WorkspaceAccessLevel.OWNER);
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        request.setWorkspace(new Workspace().name("updated-name").etag(etag));
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(),
            request);
        fail(String.format("expected BadRequestException for etag: %s", etag));
      } catch(BadRequestException e) {
        // expected
      }
    }
  }

  @Test(expected = BadRequestException.class)
  public void testRejectAfterApproveThrows() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ResearchPurpose researchPurpose = ws.getResearchPurpose();
    workspacesController.createWorkspace(ws);

    ResearchPurposeReviewRequest request = new ResearchPurposeReviewRequest();
    request.setApproved(true);
    workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request);

    request.setApproved(false);
    workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request);
  }

  @Test
  public void testListForApproval() throws Exception {
    List<Workspace> forApproval =
        workspacesController.getWorkspacesForReview().getBody().getItems();
    assertThat(forApproval).isEmpty();

    Workspace ws;
    ResearchPurpose researchPurpose;
    String nameForRequested = "requestedButNotApprovedYet";
    // requested approval, but not approved
    ws = createDefaultWorkspace();
    ws.setName(nameForRequested);
    researchPurpose = ws.getResearchPurpose();
    researchPurpose.setApproved(null);
    researchPurpose.setTimeReviewed(null);
    stubGetWorkspace(ws.getNamespace(), ws.getName().toLowerCase(), LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    workspacesController.createWorkspace(ws);
    // already approved
    ws = createDefaultWorkspace();
    ws.setName("alreadyApproved");
    stubGetWorkspace(ws.getNamespace(), ws.getName().toLowerCase(), LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    researchPurpose = ws.getResearchPurpose();
    ws = workspacesController.createWorkspace(ws).getBody();
    ResearchPurposeReviewRequest request = new ResearchPurposeReviewRequest();
    request.setApproved(true);
    workspacesController.reviewWorkspace(ws.getNamespace(), ws.getId(), request);

    // no approval requested
    ws = createDefaultWorkspace();
    ws.setName("noApprovalRequested");
    researchPurpose = ws.getResearchPurpose();
    researchPurpose.setReviewRequested(false);
    researchPurpose.setTimeRequested(null);
    researchPurpose.setApproved(null);
    researchPurpose.setTimeReviewed(null);
    stubGetWorkspace(ws.getNamespace(), ws.getName().toLowerCase(), LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    ws = workspacesController.createWorkspace(ws).getBody();

    forApproval = workspacesController.getWorkspacesForReview().getBody().getItems();
    assertThat(forApproval.size()).isEqualTo(1);
    ws = forApproval.get(0);
    assertThat(ws.getName()).isEqualTo(nameForRequested);
  }

  @Test
  public void testCloneWorkspace() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    // The original workspace is shared with one other user.
    User writerUser = new User();
    writerUser.setEmail("writerfriend@gmail.com");
    writerUser.setUserId(124L);
    writerUser.setFreeTierBillingProjectName("TestBillingProject2");

    writerUser = userDao.save(writerUser);
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    UserRole creator = new UserRole();
    creator.setEmail(LOGGED_IN_USER_EMAIL);
    creator.setRole(WorkspaceAccessLevel.OWNER);
    shareWorkspaceRequest.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail(writerUser.getEmail());
    writer.setRole(WorkspaceAccessLevel.WRITER);
    shareWorkspaceRequest.addItemsItem(writer);

    when(fireCloudService.updateWorkspaceACL(anyString(), anyString(),
        anyListOf(WorkspaceACLUpdate.class))).thenReturn(new WorkspaceACLUpdateResponseList());
    workspacesController.shareWorkspace(
        workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    req.setWorkspace(modWorkspace);
    Workspace workspace2 =
        workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody().getWorkspace();

    stubGetWorkspace(workspace2.getNamespace(), workspace2.getId(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    assertWithMessage("get and clone responses are inconsistent")
        .that(workspace2)
        .isEqualTo(
          workspacesController.getWorkspace(workspace2.getNamespace(),
              workspace2.getId()).getBody().getWorkspace());

    assertThat(workspace2.getName()).isEqualTo(modWorkspace.getName());
    assertThat(workspace2.getNamespace()).isEqualTo(modWorkspace.getNamespace());
    assertThat(workspace2.getResearchPurpose()).isEqualTo(modPurpose);

    // Original description should have been copied.
    assertThat(workspace2.getDescription()).isEqualTo(workspace.getDescription());

    // User roles should *not* be copied.
    assertThat(workspace2.getUserRoles().size()).isEqualTo(1);
    assertThat(workspace2.getUserRoles().get(0).getRole()).isEqualTo(WorkspaceAccessLevel.OWNER);
  }

  @Test
  public void testCloneWorkspaceWithCohorts() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    Cohort c1 = createDefaultCohort("c1");
    c1 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c1).getBody();
    Cohort c2 = createDefaultCohort("c2");
    c2 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c2).getBody();

    stubBigQueryCohortCalls();
    CreateReviewRequest reviewReq = new CreateReviewRequest();
    reviewReq.setSize(1);
    CohortReview cr1 = cohortReviewController.createCohortReview(
        workspace.getNamespace(), workspace.getId(), c1.getId(),
        cdrVersion.getCdrVersionId(), reviewReq).getBody();
    reviewReq.setSize(2);
    CohortReview cr2 = cohortReviewController.createCohortReview(
        workspace.getNamespace(), workspace.getId(), c2.getId(),
        cdrVersion.getCdrVersionId(), reviewReq).getBody();

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");

    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    req.setWorkspace(modWorkspace);
    Workspace cloned = workspacesController.cloneWorkspace(
        workspace.getNamespace(), workspace.getId(), req).getBody().getWorkspace();

    stubGetWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    List<Cohort> cohorts = cohortsController
        .getCohortsInWorkspace(cloned.getNamespace(), cloned.getId()).getBody().getItems();
    Map<String, Cohort> cohortsByName = Maps.uniqueIndex(cohorts, c -> c.getName());
    assertThat(cohortsByName.keySet()).containsExactlyElementsIn(ImmutableSet.of("c1", "c2"));
    assertThat(cohorts.stream().map(c -> c.getId()).collect(Collectors.toList()))
        .containsNoneOf(c1.getId(), c2.getId());

    CohortReview gotCr1 = cohortReviewController.getParticipantCohortStatuses(
        cloned.getNamespace(), cloned.getId(), cohortsByName.get("c1").getId(),
        cdrVersion.getCdrVersionId(), null, null, null, null, null, null).getBody();
    assertThat(gotCr1.getReviewSize()).isEqualTo(cr1.getReviewSize());
    assertThat(gotCr1.getParticipantCohortStatuses())
        .isEqualTo(cr1.getParticipantCohortStatuses());

    CohortReview gotCr2 = cohortReviewController.getParticipantCohortStatuses(
        cloned.getNamespace(), cloned.getId(), cohortsByName.get("c2").getId(),
        cdrVersion.getCdrVersionId(), null, null, null, null, null, null).getBody();
    assertThat(gotCr2.getReviewSize()).isEqualTo(cr2.getReviewSize());
    assertThat(gotCr2.getParticipantCohortStatuses())
        .isEqualTo(cr2.getParticipantCohortStatuses());

    assertThat(ImmutableSet.of(gotCr1.getCohortReviewId(), gotCr2.getCohortReviewId()))
        .containsNoneOf(cr1.getCohortReviewId(), cr2.getCohortId());
  }

  @Test
  public void testCloneWorkspaceDifferentOwner() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    User cloner = new User();
    cloner.setEmail("cloner@gmail.com");
    cloner.setUserId(456L);
    cloner.setFreeTierBillingProjectName("TestBillingProject1");
    cloner = userDao.save(cloner);
    when(userProvider.get()).thenReturn(cloner);

    stubGetWorkspace(workspace.getNamespace(), workspace.getId(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.READER);
    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    req.setWorkspace(modWorkspace);
    Workspace workspace2 =
        workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody().getWorkspace();

    assertThat(workspace2.getCreator()).isEqualTo(cloner.getEmail());

    assertThat(workspace2.getUserRoles().size()).isEqualTo(1);
    assertThat(workspace2.getUserRoles().get(0).getRole()).isEqualTo(WorkspaceAccessLevel.OWNER);
    assertThat(workspace2.getUserRoles().get(0).getEmail()).isEqualTo(cloner.getEmail());
  }

  @Test(expected = BadRequestException.class)
  public void testCloneWorkspaceBadRequest() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    req.setWorkspace(modWorkspace);
    // Missing research purpose.
    workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req);
  }

  @Test(expected = NotFoundException.class)
  public void testCloneNotFound() throws Exception {
    doThrow(new ApiException(404, "")).when(fireCloudService).getWorkspace("doesnot", "exist");
    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    req.setWorkspace(modWorkspace);
    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    workspacesController.cloneWorkspace("doesnot", "exist", req);
  }

  @Test(expected = ForbiddenException.class)
  public void testClonePermissionDenied() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    // Clone with a different user.
    User cloner = new User();
    cloner.setEmail("cloner@gmail.com");
    cloner.setUserId(456L);
    cloner.setFreeTierBillingProjectName("TestBillingProject1");
    cloner = userDao.save(cloner);
    when(userProvider.get()).thenReturn(cloner);

    stubGetWorkspace(workspace.getNamespace(), workspace.getName(),
        LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.NO_ACCESS);
    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    req.setWorkspace(modWorkspace);
    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req);
  }

  @Test
  public void testShareWorkspace() throws Exception{
    User writerUser = new User();
    writerUser.setEmail("writerfriend@gmail.com");
    writerUser.setUserId(124L);
    writerUser.setFreeTierBillingProjectName("TestBillingProject2");

    writerUser = userDao.save(writerUser);
    User readerUser = new User();
    readerUser.setEmail("readerfriend@gmail.com");
    readerUser.setUserId(125L);
    readerUser.setFreeTierBillingProjectName("TestBillingProject3");

    readerUser = userDao.save(readerUser);
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    UserRole creator = new UserRole();
    creator.setEmail(LOGGED_IN_USER_EMAIL);
    creator.setRole(WorkspaceAccessLevel.OWNER);
    shareWorkspaceRequest.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    writer.setRole(WorkspaceAccessLevel.WRITER);
    shareWorkspaceRequest.addItemsItem(writer);
    UserRole reader = new UserRole();
    reader.setEmail("readerfriend@gmail.com");
    reader.setRole(WorkspaceAccessLevel.READER);
    shareWorkspaceRequest.addItemsItem(reader);

    // Simulate time between API calls to trigger last-modified/@Version changes.
    CLOCK.increment(1000);
    WorkspaceACLUpdateResponseList responseValue = new WorkspaceACLUpdateResponseList();
    when(fireCloudService.updateWorkspaceACL(anyString(), anyString(), anyListOf(WorkspaceACLUpdate.class))).thenReturn(responseValue);
    ShareWorkspaceResponse shareResp = workspacesController.shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest).getBody();
    stubGetWorkspace(workspace.getNamespace(), workspace.getName(), LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    Workspace workspace2 =
        workspacesController.getWorkspace(workspace.getNamespace(), workspace.getName())
            .getBody().getWorkspace();
    assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag());

    assertThat(workspace2.getUserRoles().size()).isEqualTo(3);
    int numOwners = 0;
    int numWriters = 0;
    int numReaders = 0;
    for (UserRole userRole : workspace2.getUserRoles()) {
      if (userRole.getRole().equals(WorkspaceAccessLevel.OWNER)) {
        assertThat(userRole.getEmail()).isEqualTo(LOGGED_IN_USER_EMAIL);
        numOwners++;
      } else if (userRole.getRole().equals(WorkspaceAccessLevel.WRITER)) {
        assertThat(userRole.getEmail()).isEqualTo("writerfriend@gmail.com");
        numWriters++;
      } else {
        assertThat(userRole.getEmail()).isEqualTo("readerfriend@gmail.com");
        numReaders++;
      }
    }
    assertThat(numOwners).isEqualTo(1);
    assertThat(numWriters).isEqualTo(1);
    assertThat(numReaders).isEqualTo(1);
    assertThat(workspace.getEtag()).isNotEqualTo(workspace2.getEtag());
  }

  @Test
  public void testUnshareWorkspace() throws Exception {
    User writerUser = new User();
    writerUser.setEmail("writerfriend@gmail.com");
    writerUser.setUserId(124L);
    writerUser.setFreeTierBillingProjectName("TestBillingProject2");
    writerUser = userDao.save(writerUser);
    User readerUser = new User();
    readerUser.setEmail("readerfriend@gmail.com");
    readerUser.setUserId(125L);
    readerUser.setFreeTierBillingProjectName("TestBillingProject3");
    readerUser = userDao.save(readerUser);
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    UserRole creator = new UserRole();
    creator.setEmail(LOGGED_IN_USER_EMAIL);
    creator.setRole(WorkspaceAccessLevel.OWNER);
    shareWorkspaceRequest.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    writer.setRole(WorkspaceAccessLevel.WRITER);
    shareWorkspaceRequest.addItemsItem(writer);
    UserRole reader = new UserRole();
    reader.setEmail("readerfriend@gmail.com");
    reader.setRole(WorkspaceAccessLevel.READER);
    shareWorkspaceRequest.addItemsItem(reader);

    WorkspaceACLUpdateResponseList responseValue = new WorkspaceACLUpdateResponseList();
    responseValue.setUsersNotFound(new ArrayList<WorkspaceACLUpdate>());

    // Simulate time between API calls to trigger last-modified/@Version changes.
    CLOCK.increment(1000);
    when(fireCloudService.updateWorkspaceACL(anyString(), anyString(), anyListOf(WorkspaceACLUpdate.class))).thenReturn(responseValue);
    ShareWorkspaceResponse shareResp = workspacesController.shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest).getBody();
    stubGetWorkspace(workspace.getNamespace(), workspace.getId(),
        workspace.getCreator(), WorkspaceAccessLevel.OWNER);
    Workspace workspace2 = workspacesController.getWorkspace(workspace.getNamespace(), workspace.getId()).getBody().getWorkspace();
    assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag());

    CLOCK.increment(1000);
    shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace2.getEtag());
    shareWorkspaceRequest.addItemsItem(creator);
    shareWorkspaceRequest.addItemsItem(writer);

    shareResp = workspacesController.shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest).getBody();
    Workspace workspace3 =
        workspacesController.getWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody().getWorkspace();
    assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace3.getEtag());

    assertThat(workspace3.getUserRoles().size()).isEqualTo(2);
    int numOwners = 0;
    int numWriters = 0;
    int numReaders = 0;
    for (UserRole userRole : workspace3.getUserRoles()) {
      if (userRole.getRole().equals(WorkspaceAccessLevel.OWNER)) {
        assertThat(userRole.getEmail()).isEqualTo(LOGGED_IN_USER_EMAIL);
        numOwners++;
      } else if (userRole.getRole().equals(WorkspaceAccessLevel.WRITER)) {
        assertThat(userRole.getEmail()).isEqualTo("writerfriend@gmail.com");
        numWriters++;
      } else {
        assertThat(userRole.getEmail()).isEqualTo("readerfriend@gmail.com");
        numReaders++;
      }
    }
    assertThat(numOwners).isEqualTo(1);
    assertThat(numWriters).isEqualTo(1);
    assertThat(numReaders).isEqualTo(0);
    assertThat(workspace.getEtag()).isNotEqualTo(workspace2.getEtag());
    assertThat(workspace2.getEtag()).isNotEqualTo(workspace3.getEtag());
  }

  @Test
  public void testStaleShareWorkspace() throws Exception{
    Workspace workspace = createDefaultWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    UserRole creator = new UserRole();
    creator.setEmail(LOGGED_IN_USER_EMAIL);
    creator.setRole(WorkspaceAccessLevel.OWNER);
    shareWorkspaceRequest.addItemsItem(creator);

    // Simulate time between API calls to trigger last-modified/@Version changes.
    CLOCK.increment(1000);
    WorkspaceACLUpdateResponseList responseValue = new WorkspaceACLUpdateResponseList();
    when(fireCloudService.updateWorkspaceACL(anyString(), anyString(), anyListOf(WorkspaceACLUpdate.class))).thenReturn(responseValue);
    workspacesController.shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);


    // Simulate time between API calls to trigger last-modified/@Version changes.
    CLOCK.increment(1000);
    shareWorkspaceRequest = new ShareWorkspaceRequest();
    // Use the initial etag, not the updated value from shareWorkspace.
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    try {
      workspacesController.shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);
      fail("expected conflict exception when sharing with stale etag");
    } catch(ConflictException e) {
      // Expected
    }
  }

  @Test(expected = BadRequestException.class)
  public void testUnableToShareWithNonExistentUser() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    UserRole creator = new UserRole();
    creator.setEmail(LOGGED_IN_USER_EMAIL);
    creator.setRole(WorkspaceAccessLevel.OWNER);
    shareWorkspaceRequest.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    writer.setRole(WorkspaceAccessLevel.WRITER);
    shareWorkspaceRequest.addItemsItem(writer);
    workspacesController.shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);
  }
}
