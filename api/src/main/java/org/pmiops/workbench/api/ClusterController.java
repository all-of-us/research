package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUser.ClusterConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListClusterDeleteRequest;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.utils.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {

  // This file is used by the All of Us libraries to access workspace/CDR metadata.
  private static final String AOU_CONFIG_FILENAME = ".all_of_us_config.json";
  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_ID_KEY = "WORKSPACE_ID";
  private static final String API_HOST_KEY = "API_HOST";
  private static final String BUCKET_NAME_KEY = "BUCKET_NAME";
  private static final String CDR_VERSION_CLOUD_PROJECT = "CDR_VERSION_CLOUD_PROJECT";
  private static final String CDR_VERSION_BIGQUERY_DATASET = "CDR_VERSION_BIGQUERY_DATASET";
  // The billing project to use for the analysis.
  private static final String BILLING_CLOUD_PROJECT = "BILLING_CLOUD_PROJECT";
  private static final String DATA_URI_PREFIX = "data:application/json;base64,";
  private static final String DELOC_PATTERN = "\\.ipynb$";

  private static final Logger log = Logger.getLogger(ClusterController.class.getName());

  private final LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceService workspaceService;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;
  private final UserRecentResourceService userRecentResourceService;
  private final UserDao userDao;
  private final LeonardoMapper leonardoMapper;
  private final Clock clock;

  @Autowired
  ClusterController(
      LeonardoRuntimeAuditor leonardoRuntimeAuditor,
      LeonardoNotebooksClient leonardoNotebooksClient,
      Provider<DbUser> userProvider,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      UserRecentResourceService userRecentResourceService,
      UserDao userDao,
      LeonardoMapper leonardoMapper,
      Clock clock) {
    this.leonardoRuntimeAuditor = leonardoRuntimeAuditor;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
    this.userRecentResourceService = userRecentResourceService;
    this.userDao = userDao;
    this.leonardoMapper = leonardoMapper;
    this.clock = clock;
  }

  private Stream<org.pmiops.workbench.leonardo.model.ListRuntimeResponse> filterByRuntimesInList(
      Stream<org.pmiops.workbench.leonardo.model.ListRuntimeResponse> clustersToFilter,
      List<String> runtimeNames) {
    // Null means keep all clusters.
    return clustersToFilter.filter(
        cluster -> runtimeNames == null || runtimeNames.contains(cluster.getRuntimeName()));
  }

  @Override
  @AuthorityRequired(Authority.SECURITY_ADMIN)
  public ResponseEntity<List<ListClusterResponse>> deleteClustersInProject(
      String billingProjectId, ListClusterDeleteRequest clusterNamesToDelete) {
    if (billingProjectId == null) {
      throw new BadRequestException("Must specify billing project");
    }
    List<org.pmiops.workbench.leonardo.model.ListRuntimeResponse> runtimesToDelete =
        filterByRuntimesInList(
                leonardoNotebooksClient.listRuntimesByProjectAsService(billingProjectId).stream(),
                clusterNamesToDelete.getClustersToDelete())
            .collect(Collectors.toList());

    runtimesToDelete.forEach(
        runtime ->
            leonardoNotebooksClient.deleteRuntimeAsService(
                runtime.getGoogleProject(), runtime.getRuntimeName()));
    List<ListClusterResponse> runtimesInProjectAffected =
        filterByRuntimesInList(
                leonardoNotebooksClient.listRuntimesByProjectAsService(billingProjectId).stream(),
                clusterNamesToDelete.getClustersToDelete())
            .map(leonardoMapper::toApiListClusterResponse)
            .collect(Collectors.toList());
    // DELETED is an acceptable status from an implementation standpoint, but we will never
    // receive runtimes with that status from Leo. We don't want to because we reuse runtime
    // names and thus could have >1 deleted runtimes with the same name in the project.
    List<ClusterStatus> acceptableStates =
        ImmutableList.of(ClusterStatus.DELETING, ClusterStatus.ERROR);
    runtimesInProjectAffected.stream()
        .filter(cluster -> !acceptableStates.contains(cluster.getStatus()))
        .forEach(
            clusterInBadState ->
                log.log(
                    Level.SEVERE,
                    String.format(
                        "Cluster %s/%s is not in a deleting state",
                        clusterInBadState.getGoogleProject(), clusterInBadState.getClusterName())));
    leonardoRuntimeAuditor.fireDeleteRuntimesInProject(
        billingProjectId,
        runtimesToDelete.stream()
            .map(org.pmiops.workbench.leonardo.model.ListRuntimeResponse::getRuntimeName)
            .collect(Collectors.toList()));
    return ResponseEntity.ok(runtimesInProjectAffected);
  }

  private DbWorkspace lookupWorkspace(String workspaceNamespace) throws NotFoundException {
    return workspaceService
        .getByNamespace(workspaceNamespace)
        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
  }

  @Override
  public ResponseEntity<Cluster> getCluster(String workspaceNamespace) {
    String firecloudWorkspaceName = lookupWorkspace(workspaceNamespace).getFirecloudName();
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    org.pmiops.workbench.leonardo.model.GetRuntimeResponse leoRuntime =
        leonardoNotebooksClient.getRuntime(workspaceNamespace, userProvider.get().getClusterName());

    return ResponseEntity.ok(leonardoMapper.toApiCluster(leoRuntime));
  }

  @Override
  public ResponseEntity<Cluster> createCluster(String workspaceNamespace) {
    String firecloudWorkspaceName = lookupWorkspace(workspaceNamespace).getFirecloudName();
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    leonardoNotebooksClient.createRuntime(
        workspaceNamespace, userProvider.get().getClusterName(), firecloudWorkspaceName);

    org.pmiops.workbench.leonardo.model.GetRuntimeResponse leoRuntime =
        leonardoNotebooksClient.getRuntime(workspaceNamespace, userProvider.get().getClusterName());

    return ResponseEntity.ok(leonardoMapper.toApiCluster(leoRuntime));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCluster(String workspaceNamespace) {
    String firecloudWorkspaceName = lookupWorkspace(workspaceNamespace).getFirecloudName();
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);

    leonardoNotebooksClient.deleteRuntime(workspaceNamespace, userProvider.get().getClusterName());
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ClusterLocalizeResponse> localize(
      String workspaceNamespace, ClusterLocalizeRequest body) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    workspaceService.validateActiveBilling(
        dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    final FirecloudWorkspace firecloudWorkspace;
    try {
      firecloudWorkspace =
          fireCloudService
              .getWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
              .getWorkspace();
    } catch (NotFoundException e) {
      throw new NotFoundException(
          String.format(
              "workspace %s/%s not found or not accessible",
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName()));
    }
    DbCdrVersion cdrVersion = dbWorkspace.getCdrVersion();

    // For the common case where the notebook cluster matches the workspace
    // namespace, simply name the directory as the workspace ID; else we
    // include the namespace in the directory name to avoid possible conflicts
    // in workspace IDs.
    String gcsNotebooksDir = "gs://" + firecloudWorkspace.getBucketName() + "/notebooks";
    long workspaceId = dbWorkspace.getWorkspaceId();

    body.getNotebookNames()
        .forEach(
            notebookName ->
                userRecentResourceService.updateNotebookEntry(
                    workspaceId,
                    userProvider.get().getUserId(),
                    gcsNotebooksDir + "/" + notebookName));

    String workspacePath = dbWorkspace.getFirecloudName();
    String editDir = "workspaces/" + workspacePath;
    String playgroundDir = "workspaces_playground/" + workspacePath;
    String targetDir = body.getPlaygroundMode() ? playgroundDir : editDir;

    leonardoNotebooksClient.createStorageLink(
        workspaceNamespace,
        userProvider.get().getClusterName(),
        new StorageLink()
            .cloudStorageDirectory(gcsNotebooksDir)
            .localBaseDirectory(editDir)
            .localSafeModeBaseDirectory(playgroundDir)
            .pattern(DELOC_PATTERN));

    // Always localize config files; usually a no-op after the first call.
    Map<String, String> localizeMap = new HashMap<>();

    // The Welder extension offers direct links to/from playground mode; write the AoU config file
    // to both locations so notebooks will work in either directory.
    String aouConfigUri = aouConfigDataUri(firecloudWorkspace, cdrVersion, workspaceNamespace);
    localizeMap.put(editDir + "/" + AOU_CONFIG_FILENAME, aouConfigUri);
    localizeMap.put(playgroundDir + "/" + AOU_CONFIG_FILENAME, aouConfigUri);

    // Localize the requested notebooks, if any.
    if (body.getNotebookNames() != null) {
      localizeMap.putAll(
          body.getNotebookNames().stream()
              .collect(
                  Collectors.toMap(
                      name -> targetDir + "/" + name, name -> gcsNotebooksDir + "/" + name)));
    }
    log.info(localizeMap.toString());
    leonardoNotebooksClient.localize(
        workspaceNamespace, userProvider.get().getClusterName(), localizeMap);

    // This is the Jupyer-server-root-relative path, the style used by the Jupyter REST API.
    return ResponseEntity.ok(new ClusterLocalizeResponse().clusterLocalDirectory(targetDir));
  }

  @Override
  @AuthorityRequired({Authority.DEVELOPER})
  public ResponseEntity<EmptyResponse> updateClusterConfig(UpdateClusterConfigRequest body) {
    DbUser user = userService.getByUsernameOrThrow(body.getUserEmail());
    String oldOverride = user.getClusterConfigDefaultRaw();

    final ClusterConfig override = body.getClusterConfig() != null ? new ClusterConfig() : null;
    if (override != null) {
      override.masterDiskSize = body.getClusterConfig().getMasterDiskSize();
      override.machineType = body.getClusterConfig().getMachineType();
    }
    userService.updateUserWithRetries(
        (u) -> {
          u.setClusterConfigDefault(override);
          return u;
        },
        user,
        Agent.asAdmin(userProvider.get()));
    userService.logAdminUserAction(
        user.getUserId(), "cluster config override", oldOverride, new Gson().toJson(override));
    return ResponseEntity.ok(new EmptyResponse());
  }

  private String jsonToDataUri(JSONObject json) {
    return DATA_URI_PREFIX + Base64.getUrlEncoder().encodeToString(json.toString().getBytes());
  }

  private String aouConfigDataUri(
      FirecloudWorkspace fcWorkspace, DbCdrVersion cdrVersion, String cdrBillingCloudProject) {
    JSONObject config = new JSONObject();

    String host = null;
    try {
      host = new URL(workbenchConfigProvider.get().server.apiBaseUrl).getHost();
    } catch (MalformedURLException e) {
      log.log(Level.SEVERE, "bad apiBaseUrl config value; failing", e);
      throw new ServerErrorException("Failed to generate AoU notebook config");
    }
    config.put(WORKSPACE_NAMESPACE_KEY, fcWorkspace.getNamespace());
    config.put(WORKSPACE_ID_KEY, fcWorkspace.getName());
    config.put(BUCKET_NAME_KEY, fcWorkspace.getBucketName());
    config.put(API_HOST_KEY, host);
    config.put(CDR_VERSION_CLOUD_PROJECT, cdrVersion.getBigqueryProject());
    config.put(CDR_VERSION_BIGQUERY_DATASET, cdrVersion.getBigqueryDataset());
    config.put(BILLING_CLOUD_PROJECT, cdrBillingCloudProject);
    return jsonToDataUri(config);
  }
}
