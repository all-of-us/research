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
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.User.ClusterConfig;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.pmiops.workbench.notebooks.api.StatusApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterRequest;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.notebooks.model.MachineConfig;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.notebooks.model.UserJupyterExtensionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoNotebooksClientImpl implements LeonardoNotebooksClient {
  private static final String CLUSTER_LABEL_AOU = "all-of-us";
  private static final String CLUSTER_LABEL_CREATED_BY = "created-by";

  private static final Logger log = Logger.getLogger(LeonardoNotebooksClientImpl.class.getName());

  private final Provider<ClusterApi> clusterApiProvider;
  private final Provider<NotebooksApi> notebooksApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<User> userProvider;
  private final NotebooksRetryHandler retryHandler;

  @Autowired
  public LeonardoNotebooksClientImpl(
      @Qualifier(NotebooksConfig.USER_CLUSTER_API) Provider<ClusterApi> clusterApiProvider,
      Provider<NotebooksApi> notebooksApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<User> userProvider,
      NotebooksRetryHandler retryHandler) {
    this.clusterApiProvider = clusterApiProvider;
    this.notebooksApiProvider = notebooksApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.retryHandler = retryHandler;
  }

  private ClusterRequest createFirecloudClusterRequest(
      String userEmail, @Nullable ClusterConfig clusterOverride) {
    if (clusterOverride == null) {
      clusterOverride = new ClusterConfig();
    }

    WorkbenchConfig config = workbenchConfigProvider.get();
    String gcsPrefix = "gs://" + config.googleCloudStorageService.clusterResourcesBucketName;

    Map<String, String> nbExtensions = new HashMap<>();
    nbExtensions.put("aou-snippets-menu", gcsPrefix + "/aou-snippets-menu.js");
    if (!config.featureFlags.enableLeoWelder) {
      // Enabling Welder automatically installs a Leo-maintained playground extension.
      nbExtensions.put("aou-playground-extension", gcsPrefix + "/playground-extension.js");
    }

    return new ClusterRequest()
        .labels(ImmutableMap.of(CLUSTER_LABEL_AOU, "true", CLUSTER_LABEL_CREATED_BY, userEmail))
        .defaultClientId(config.server.oauthClientId)
        // Note: Filenames must be kept in sync with files in cluster-resources directory.
        .jupyterUserScriptUri(gcsPrefix + "/setup_notebook_cluster.sh")
        .userJupyterExtensionConfig(
            new UserJupyterExtensionConfig()
                .nbExtensions(nbExtensions)
                .serverExtensions(ImmutableMap.of("jupyterlab", "jupyterlab"))
                .combinedExtensions(ImmutableMap.<String, String>of())
                .labExtensions(ImmutableMap.<String, String>of()))
        .enableWelder(config.featureFlags.enableLeoWelder)
        .machineConfig(
            new MachineConfig()
                .masterDiskSize(
                    Optional.ofNullable(clusterOverride.masterDiskSize).orElse(20 /* GB */))
                .masterMachineType(
                    Optional.ofNullable(clusterOverride.machineType).orElse("n1-standard-2")));
  }

  @Override
  public Cluster createCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    User user = userProvider.get();
    return retryHandler.run(
        (context) ->
            clusterApi.createClusterV2(
                googleProject,
                clusterName,
                createFirecloudClusterRequest(user.getEmail(), user.getClusterConfigDefault())));
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
  public List<Cluster> listClusters(String labels, boolean includeDeleted) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run((context) -> clusterApi.listClusters(labels, includeDeleted));
  }

  @Override
  public Cluster getCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run((context) -> clusterApi.getCluster(googleProject, clusterName));
  }

  @Override
  public void localize(String googleProject, String clusterName, Map<String, String> fileList) {
    boolean enableLeoWelder = workbenchConfigProvider.get().featureFlags.enableLeoWelder;
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
          if (enableLeoWelder) {
            notebooksApi.welderLocalize(googleProject, clusterName, welderReq);
          } else {
            notebooksApi.proxyLocalize(googleProject, clusterName, fileList, /* async */ false);
          }
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
