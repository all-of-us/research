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
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.model.LeonardoUserJupyterExtensionConfig;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoNotebooksClientImpl implements LeonardoNotebooksClient {

  private static final String RUNTIME_LABEL_AOU = "all-of-us";
  private static final String RUNTIME_LABEL_AOU_CONFIG = "all-of-us-config";
  private static final String RUNTIME_LABEL_CREATED_BY = "created-by";
  private static final String WORKSPACE_CDR = "WORKSPACE_CDR";
  public static final Map<RuntimeConfigurationType, String>
      RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP =
          ImmutableMap.of(
              RuntimeConfigurationType.USEROVERRIDE, "user-override",
              RuntimeConfigurationType.DEFAULTGCE, "default-gce",
              RuntimeConfigurationType.DEFAULTDATAPROC, "default-dataproc");

  private static final Logger log = Logger.getLogger(LeonardoNotebooksClientImpl.class.getName());

  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<RuntimesApi> serviceRuntimesApiProvider;
  private final Provider<ProxyApi> proxyApiProvider;
  private final Provider<ServiceInfoApi> serviceInfoApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final NotebooksRetryHandler notebooksRetryHandler;
  private final LeonardoRetryHandler leonardoRetryHandler;
  private final WorkspaceService workspaceService;

  @Autowired
  public LeonardoNotebooksClientImpl(
      @Qualifier(NotebooksConfig.USER_RUNTIMES_API) Provider<RuntimesApi> runtimesApiProvider,
      @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API)
          Provider<RuntimesApi> serviceRuntimesApiProvider,
      Provider<ProxyApi> proxyApiProvider,
      Provider<ServiceInfoApi> serviceInfoApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      NotebooksRetryHandler notebooksRetryHandler,
      LeonardoRetryHandler leonardoRetryHandler,
      WorkspaceService workspaceService) {
    this.runtimesApiProvider = runtimesApiProvider;
    this.serviceRuntimesApiProvider = serviceRuntimesApiProvider;
    this.proxyApiProvider = proxyApiProvider;
    this.serviceInfoApiProvider = serviceInfoApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.notebooksRetryHandler = notebooksRetryHandler;
    this.leonardoRetryHandler = leonardoRetryHandler;
    this.workspaceService = workspaceService;
  }

  private LeonardoCreateRuntimeRequest buildCreateRuntimeRequest(
      String userEmail,
      @Nullable ClusterConfig clusterOverride,
      Runtime runtime,
      Map<String, String> customEnvironmentVariables) {
    // TODO(RW-5406): Remove cluster override.
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

    Map<String, String> runtimeLabels = new HashMap<>();
    runtimeLabels.put(RUNTIME_LABEL_AOU, "true");
    runtimeLabels.put(RUNTIME_LABEL_CREATED_BY, userEmail);

    if (runtime.getConfigurationType() != null) {
      runtimeLabels.put(
          RUNTIME_LABEL_AOU_CONFIG,
          RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP.get(runtime.getConfigurationType()));
    }

    return new LeonardoCreateRuntimeRequest()
        .labels(runtimeLabels)
        .defaultClientId(config.server.oauthClientId)
        // Note: Filenames must be kept in sync with files in api/src/main/webapp/static.
        .jupyterUserScriptUri(assetsBaseUrl + "/initialize_notebook_runtime.sh")
        .jupyterStartUserScriptUri(assetsBaseUrl + "/start_notebook_runtime.sh")
        .userJupyterExtensionConfig(
            new LeonardoUserJupyterExtensionConfig().nbExtensions(nbExtensions))
        // Matches Terra UI's scopes, see RW-3531 for rationale.
        .addScopesItem("https://www.googleapis.com/auth/cloud-platform")
        .addScopesItem("https://www.googleapis.com/auth/userinfo.email")
        .addScopesItem("https://www.googleapis.com/auth/userinfo.profile")
        .runtimeConfig(
            new LeonardoMachineConfig()
                .cloudService(CloudServiceEnum.DATAPROC)
                .masterDiskSize(
                    Optional.ofNullable(clusterOverride.masterDiskSize)
                        .orElse(config.firecloud.notebookRuntimeDefaultDiskSizeGb))
                .masterMachineType(
                    Optional.ofNullable(clusterOverride.machineType)
                        .orElse(config.firecloud.notebookRuntimeDefaultMachineType)))
        .toolDockerImage(workbenchConfigProvider.get().firecloud.jupyterDockerImage)
        .welderDockerImage(workbenchConfigProvider.get().firecloud.welderDockerImage)
        .customEnvironmentVariables(customEnvironmentVariables);
  }

  @Override
  public void createRuntime(Runtime runtime, String workspaceFirecloudName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();

    DbUser user = userProvider.get();
    DbWorkspace workspace =
        workspaceService.getRequired(runtime.getGoogleProject(), workspaceFirecloudName);
    Map<String, String> customEnvironmentVariables = new HashMap<>();
    // i.e. is NEW or MIGRATED
    if (!workspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
      customEnvironmentVariables.put(
          WORKSPACE_CDR,
          workspace.getCdrVersion().getBigqueryProject()
              + "."
              + workspace.getCdrVersion().getBigqueryDataset());
    }

    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.createRuntime(
              runtime.getGoogleProject(),
              runtime.getRuntimeName(),
              buildCreateRuntimeRequest(
                  user.getUsername(),
                  user.getClusterConfigDefault(),
                  runtime,
                  customEnvironmentVariables));
          return null;
        });
  }

  @Override
  public List<LeonardoListRuntimeResponse> listRuntimesByProjectAsService(String googleProject) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    return leonardoRetryHandler.run(
        (context) -> runtimesApi.listRuntimesByProject(googleProject, null, false));
  }

  @Override
  public void deleteRuntime(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, /* deleteDisk */ false);
          return null;
        });
  }

  @Override
  public LeonardoGetRuntimeResponse getRuntime(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    try {
      return leonardoRetryHandler.runAndThrowChecked(
          (context) -> runtimesApi.getRuntime(googleProject, runtimeName));
    } catch (ApiException e) {
      throw ExceptionUtils.convertLeonardoException(e);
    }
  }

  @Override
  public void deleteRuntimeAsService(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, /* deleteDisk */ false);
          return null;
        });
  }

  @Override
  public void localize(String googleProject, String runtimeName, Map<String, String> fileList) {
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
    ProxyApi proxyApi = proxyApiProvider.get();
    notebooksRetryHandler.run(
        (context) -> {
          proxyApi.welderLocalize(googleProject, runtimeName, welderReq);
          return null;
        });
  }

  @Override
  public StorageLink createStorageLink(
      String googleProject, String runtime, StorageLink storageLink) {
    ProxyApi proxyApi = proxyApiProvider.get();
    return notebooksRetryHandler.run(
        (context) -> proxyApi.welderCreateStorageLink(googleProject, runtime, storageLink));
  }

  @Override
  public boolean getLeonardoStatus() {
    try {
      serviceInfoApiProvider.get().getSystemStatus();
    } catch (org.pmiops.workbench.leonardo.ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      log.log(Level.WARNING, "notebooks status check request failed", e);
      return false;
    }
    return true;
  }
}
