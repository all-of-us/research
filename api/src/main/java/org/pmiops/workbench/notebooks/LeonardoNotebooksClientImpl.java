package org.pmiops.workbench.notebooks;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUser.ClusterConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.pmiops.workbench.notebooks.api.StatusApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterRequest;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.notebooks.model.MachineConfig;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.notebooks.model.UserJupyterExtensionConfig;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoNotebooksClientImpl implements LeonardoNotebooksClient {

  private static final String CLUSTER_LABEL_AOU = "all-of-us";
  private static final String CLUSTER_LABEL_CREATED_BY = "created-by";
  private static final String WORKSPACE_CDR = "WORKSPACE_CDR";

  private static final Logger log = Logger.getLogger(LeonardoNotebooksClientImpl.class.getName());

  private final Provider<ClusterApi> clusterApiProvider;
  private final Provider<ClusterApi> serviceClusterApiProvider;
  private final Provider<NotebooksApi> notebooksApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final NotebooksRetryHandler retryHandler;
  private final WorkspaceService workspaceService;

  @Autowired
  public LeonardoNotebooksClientImpl(
      @Qualifier(NotebooksConfig.USER_CLUSTER_API) Provider<ClusterApi> clusterApiProvider,
      @Qualifier(NotebooksConfig.SERVICE_CLUSTER_API)
          Provider<ClusterApi> serviceClusterApiProvider,
      Provider<NotebooksApi> notebooksApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      NotebooksRetryHandler retryHandler,
      WorkspaceService workspaceService) {
    this.clusterApiProvider = clusterApiProvider;
    this.serviceClusterApiProvider = serviceClusterApiProvider;
    this.notebooksApiProvider = notebooksApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.retryHandler = retryHandler;
    this.workspaceService = workspaceService;
  }

  private ClusterRequest createFirecloudClusterRequest(
      String userEmail,
      @Nullable ClusterConfig clusterOverride,
      Map<String, String> customClusterEnvironmentVariables) {
    if (clusterOverride == null) {
      clusterOverride = new ClusterConfig();
    }

    WorkbenchConfig config = workbenchConfigProvider.get();
    String assetsBaseUrl = config.server.apiBaseUrl + "/static";

    Map<String, String> nbExtensions = new HashMap<>();
    nbExtensions.put("aou-snippets-menu", assetsBaseUrl + "/aou-snippets-menu.js");
    nbExtensions.put("aou-download-extension", assetsBaseUrl + "/aou-download-policy-extension.js");
    nbExtensions.put(
        "aou-activity-checker-extension", assetsBaseUrl + "/activity-checker-extension.js");
    nbExtensions.put(
        "aou-upload-policy-extension", assetsBaseUrl + "/aou-upload-policy-extension.js");

    return new ClusterRequest()
        .labels(ImmutableMap.of(CLUSTER_LABEL_AOU, "true", CLUSTER_LABEL_CREATED_BY, userEmail))
        .defaultClientId(config.server.oauthClientId)
        // Note: Filenames must be kept in sync with files in cluster-resources directory.
        .jupyterUserScriptUri(assetsBaseUrl + "/initialize_notebook_cluster.sh")
        .jupyterStartUserScriptUri(assetsBaseUrl + "/start_notebook_cluster.sh")
        .userJupyterExtensionConfig(
            new UserJupyterExtensionConfig()
                .nbExtensions(nbExtensions)
                .serverExtensions(ImmutableMap.of("jupyterlab", "jupyterlab"))
                .combinedExtensions(ImmutableMap.<String, String>of())
                .labExtensions(ImmutableMap.<String, String>of()))
        // Matches Terra UI's scopes, see RW-3531 for rationale.
        .addScopesItem("https://www.googleapis.com/auth/cloud-platform")
        .addScopesItem("https://www.googleapis.com/auth/userinfo.email")
        .addScopesItem("https://www.googleapis.com/auth/userinfo.profile")
        .enableWelder(true)
        .machineConfig(
            new MachineConfig()
                .masterDiskSize(
                    Optional.ofNullable(clusterOverride.masterDiskSize)
                        .orElse(config.firecloud.clusterDefaultDiskSizeGb))
                .masterMachineType(
                    Optional.ofNullable(clusterOverride.machineType)
                        .orElse(config.firecloud.clusterDefaultMachineType)))
        .toolDockerImage(workbenchConfigProvider.get().firecloud.jupyterDockerImage)
        .welderDockerImage(workbenchConfigProvider.get().firecloud.welderDockerImage)
        .customClusterEnvironmentVariables(customClusterEnvironmentVariables);
  }

  @Override
  public Cluster createCluster(
      String googleProject, String clusterName, String workspaceFirecloudName) {
    ClusterApi clusterApi = clusterApiProvider.get();

    DbUser user = userProvider.get();
    DbWorkspace workspace = workspaceService.getRequired(googleProject, workspaceFirecloudName);
    Map<String, String> customClusterEnvironmentVariables = new HashMap<>();
    // i.e. is NEW or MIGRATED
    if (!workspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
      customClusterEnvironmentVariables.put(
          WORKSPACE_CDR,
          workspace.getCdrVersion().getBigqueryProject()
              + "."
              + workspace.getCdrVersion().getBigqueryDataset());
    }

    return retryHandler.run(
        (context) ->
            clusterApi.createClusterV2(
                googleProject,
                clusterName,
                createFirecloudClusterRequest(
                    user.getUsername(),
                    user.getClusterConfigDefault(),
                    customClusterEnvironmentVariables)));
  }

  @Override
  public List<ListClusterResponse> listClustersByProject(String googleProject) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run(
        (context) -> clusterApi.listClustersByProject(googleProject, null, false));
  }

  @Override
  public List<ListClusterResponse> listClustersByProjectAsAdmin(String googleProject) {
    ClusterApi clusterApi = serviceClusterApiProvider.get();
    return retryHandler.run(
        (context) -> clusterApi.listClustersByProject(googleProject, null, false));
  }

  @Override
  public void deleteCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    retryHandler.run(
        (context) -> {
          clusterApi.deleteCluster(googleProject, clusterName);
          return null;
        });
  }

  @Override
  public Cluster getCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run((context) -> clusterApi.getCluster(googleProject, clusterName));
  }

  @Override
  public void deleteClusterAsAdmin(String googleProject, String clusterName) {
    ClusterApi clusterApi = serviceClusterApiProvider.get();
    retryHandler.run(
        (context) -> {
          clusterApi.deleteCluster(googleProject, clusterName);
          return null;
        });
  }

  @Override
  public void localize(String googleProject, String clusterName, Map<String, String> fileList) {
    Localize welderReq =
        new Localize()
            .entries(
                fileList.entrySet().stream()
                    .map(
                        e ->
                            new LocalizationEntry()
                                .sourceUri(e.getValue())
                                .localDestinationPath(e.getKey()))
                    .collect(Collectors.toList()));
    NotebooksApi notebooksApi = notebooksApiProvider.get();
    retryHandler.run(
        (context) -> {
          notebooksApi.welderLocalize(googleProject, clusterName, welderReq);
          return null;
        });
  }

  @Override
  public StorageLink createStorageLink(
      String googleProject, String clusterName, StorageLink storageLink) {
    NotebooksApi notebooksApi = notebooksApiProvider.get();
    return retryHandler.run(
        (context) -> notebooksApi.welderCreateStorageLink(googleProject, clusterName, storageLink));
  }

  @Override
  public boolean getNotebooksStatus() {
    try {
      new StatusApi().getSystemStatus();
    } catch (ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      log.log(Level.WARNING, "notebooks status check request failed", e);
      return false;
    }
    return true;
  }
}
