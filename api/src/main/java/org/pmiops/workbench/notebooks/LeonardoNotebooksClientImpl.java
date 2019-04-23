package org.pmiops.workbench.notebooks;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.pmiops.workbench.notebooks.model.MachineConfig;
import org.pmiops.workbench.notebooks.model.UserJupyterExtensionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoNotebooksClientImpl implements LeonoardoNotebooksClient {
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
      Provider<WorkbenchConfig> workbenchConfigProvider, Provider<User> userProvider,
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
    return new ClusterRequest()
        .labels(ImmutableMap.of(
            CLUSTER_LABEL_AOU, "true",
            CLUSTER_LABEL_CREATED_BY, userEmail))
        .defaultClientId(config.server.oauthClientId)
        .jupyterUserScriptUri(config.firecloud.jupyterUserScriptUri)
        .userJupyterExtensionConfig(new UserJupyterExtensionConfig()
            .nbExtensions(ImmutableMap.of(
                "playground-extension", config.firecloud.jupyterPlaygroundExtensionUri))
            .serverExtensions(ImmutableMap.of("jupyterlab", "jupyterlab"))
            .combinedExtensions(ImmutableMap.<String, String>of())
            .labExtensions(ImmutableMap.<String, String>of()))
        .machineConfig(new MachineConfig()
            .masterDiskSize(Optional.ofNullable(clusterOverride.masterDiskSize).orElse(20 /* GB */))
            .masterMachineType(Optional.ofNullable(clusterOverride.machineType).orElse("n1-standard-2")));
  }

  @Override
  public Cluster createCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    User user = userProvider.get();
    return retryHandler.run((context) ->
        clusterApi.createClusterV2(googleProject, clusterName,
            createFirecloudClusterRequest(user.getEmail(), user.getClusterConfigDefault())));
  }

  @Override
  public void deleteCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    retryHandler.run((context) -> {
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
    NotebooksApi notebooksApi = notebooksApiProvider.get();
    retryHandler.run((context) -> {
      notebooksApi.proxyLocalize(googleProject, clusterName, fileList, /* async */ false);
      return null;
    });
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
