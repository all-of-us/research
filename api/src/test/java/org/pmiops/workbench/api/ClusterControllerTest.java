package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.Date;
import com.google.common.collect.ImmutableList;
import java.util.Base64;
import java.util.Map;
import javax.inject.Provider;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
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
public class ClusterControllerTest {
  private static final String WORKSPACE_NS = "proj";
  private static final String WORKSPACE_ID = "wsid";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String BUCKET_NAME = "workspace-bucket";

  @TestConfiguration
  @Import({
    ClusterController.class
  })
  @MockBean({
    FireCloudService.class,
    NotebooksService.class,
    WorkspaceService.class,
    UserService.class,
  })
  static class Configuration {
    @Bean
    @Qualifier("apiHostName")
    String apiHostName() {
      return "https://api.blah.com";
    }

    @Bean
    User user() {
      // Allows for wiring of the initial Provider<User>; actual mocking of the
      // user is achieved via setUserProvider().
      return null;
    }
  }

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @Autowired
  NotebooksService notebookService;
  @Mock
  private AdminActionHistoryDao adminActionHistoryDao;
  @Autowired
  FireCloudService fireCloudService;
  @Autowired
  UserDao userDao;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Autowired
  WorkspaceService workspaceService;
  @Mock
  Provider<User> userProvider;
  @Autowired
  ClusterController clusterController;
  @Mock
  private Provider<WorkbenchConfig> configProvider;

  private CdrVersion cdrVersion;
  private FakeClock clock;
  private org.pmiops.workbench.notebooks.model.Cluster testFcCluster;
  private Cluster testCluster;

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail(LOGGED_IN_USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName(WORKSPACE_NS);
    user.setFreeTierBillingProjectStatus(
        StorageEnums.billingProjectStatusToStorage(BillingProjectStatus.READY));
    when(userProvider.get()).thenReturn(user);
    clusterController.setUserProvider(userProvider);

    UserService userService = new UserService(userProvider, userDao, adminActionHistoryDao, clock, fireCloudService, configProvider);
    clusterController.setUserService(userService);

    cdrVersion = new CdrVersion();
    cdrVersion.setName("1");
    //set the db name to be empty since test cases currently
    //run in the workbench schema only.
    cdrVersion.setCdrDbName("");

    String createdDate = Date.fromYearMonthDay(1988, 12, 26).toString();
    testFcCluster = new org.pmiops.workbench.notebooks.model.Cluster()
        .clusterName("all-of-us")
        .googleProject(WORKSPACE_NS)
        .status(org.pmiops.workbench.notebooks.model.ClusterStatus.DELETING)
        .createdDate(createdDate);
    testCluster = new Cluster()
        .clusterName("all-of-us")
        .clusterNamespace(WORKSPACE_NS)
        .status(ClusterStatus.DELETING)
        .createdDate(createdDate);
  }

  private org.pmiops.workbench.firecloud.model.Workspace createFcWorkspace(
      String ns, String name, String creator) {
    return new org.pmiops.workbench.firecloud.model.Workspace()
        .namespace(ns)
        .name(name)
        .createdBy(creator)
        .bucketName(BUCKET_NAME);
  }

  private void stubGetWorkspace(String ns, String name, String creator) throws Exception {
    Workspace w = new Workspace();
    w.setWorkspaceNamespace(ns);
    w.setFirecloudName(name);
    w.setCdrVersion(cdrVersion);
    when(workspaceService.getRequired(ns, name)).thenReturn(w);
    stubGetFcWorkspace(createFcWorkspace(ns, name, creator));
  }

  private void stubGetFcWorkspace(org.pmiops.workbench.firecloud.model.Workspace fcWorkspace)
      throws Exception {
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    when(fireCloudService.getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName()))
        .thenReturn(fcResponse);
  }

  private JSONObject dataUriToJson(String dataUri) {
    String b64 = dataUri.substring(dataUri.indexOf(',') + 1);
    byte[] raw = Base64.getUrlDecoder().decode(b64);
    return new JSONObject(new String(raw));
  }

  @Test
  public void testListClusters() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenReturn(testFcCluster);

    assertThat(clusterController.listClusters().getBody().getDefaultCluster())
        .isEqualTo(testCluster);
  }

  @Test
  public void testListClustersUnknownStatus() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenReturn(
        testFcCluster.status(null));

    assertThat(clusterController.listClusters().getBody().getDefaultCluster().getStatus())
        .isEqualTo(ClusterStatus.UNKNOWN);
  }

  @Test
  public void testListClustersLazyCreate() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenThrow(new NotFoundException());
    when(notebookService.createCluster(eq(WORKSPACE_NS), eq("all-of-us"), any()))
        .thenReturn(testFcCluster);

    assertThat(clusterController.listClusters().getBody().getDefaultCluster())
        .isEqualTo(testCluster);
  }

  @Test
  public void testDeleteCluster() throws Exception {
    clusterController.deleteCluster(WORKSPACE_NS, "cluster");
    verify(notebookService).deleteCluster(WORKSPACE_NS, "cluster");
  }

  @Test
  public void testLocalize() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setNotebookNames(ImmutableList.of("foo.ipynb"));
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(WORKSPACE_NS, "cluster", req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid");

    verify(notebookService).localize(eq(WORKSPACE_NS), eq("cluster"), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap).containsEntry(
        "~/workspaces/wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    JSONObject delocJson = dataUriToJson(localizeMap.get("~/workspaces/wsid/.delocalize.json"));
    assertThat(delocJson.getString("destination")).isEqualTo("gs://workspace-bucket/notebooks");
    JSONObject aouJson = dataUriToJson(localizeMap.get("~/workspaces/wsid/.all_of_us_config.json"));
    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
  }

  @Test
  public void testLocalize_differentNamespace() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setNotebookNames(ImmutableList.of("foo.ipynb"));
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize("other-proj", "cluster", req).getBody();
    verify(notebookService).localize(eq("other-proj"), eq("cluster"), mapCaptor.capture());

    assertThat(mapCaptor.getValue()).containsEntry(
        "~/workspaces/proj:wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/proj:wsid");
  }

  @Test
  public void testLocalize_noNotebooks() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(WORKSPACE_NS, "cluster", req).getBody();
    verify(notebookService).localize(eq(WORKSPACE_NS), eq("cluster"), mapCaptor.capture());

    // Config files only.
    assertThat(mapCaptor.getValue().size()).isEqualTo(2);
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid");
  }}
