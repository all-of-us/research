package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspacesController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan({"org.pmiops.workbench.db.model"})
public class BulkCloneWorkspacesTool {

  private ProfileApi profileApi;
  private NihApi nihApi;
  private StaticNotebooksApi staticNotebooksApi;
  private WorkspacesApi workspacesApi;

  private User providedUser;

  @Bean
  @Scope("prototype")
  ProfileApi profileApi() {
    return profileApi;
  }

  @Bean
  @Scope("prototype")
  NihApi nihApi() {
    return nihApi;
  }

  @Bean
  @Scope("prototype")
  StaticNotebooksApi staticNotebooksApi() {
    return staticNotebooksApi;
  }

  @Bean
  @Primary
  @Qualifier("workspacesApi")
  @Scope("prototype")
  WorkspacesApi workspacesApi() {
    return workspacesApi;
  }

  @Bean
  @Primary
  @Qualifier("workspaceAclsApi")
  WorkspacesApi workspaceAclsApi(@Qualifier("saApiClient") ApiClient apiClient) {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Primary
  BillingApi billingApi(@Qualifier("saApiClient") ApiClient apiClient) {
    BillingApi api = new BillingApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Primary
  GroupsApi groupsApi(@Qualifier("saApiClient") ApiClient apiClient) {
    GroupsApi api = new GroupsApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Scope("prototype")
  User user() { return providedUser; }

  @Bean
  @Qualifier("saApiClient")
  ApiClient saApiClient(Provider<WorkbenchConfig> configProvider) throws IOException {
    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    return apiClient;
  }

  private static final String[] BILLING_SCOPES =
      new String[] {
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/cloud-billing"
      };

  private void initializeApis() {
    profileApi = new ProfileApi();
    nihApi = new NihApi();
    staticNotebooksApi = new StaticNotebooksApi();
    workspacesApi = new WorkspacesApi();
  }

  private void impersonateUser(ApiClient apiClient) {
    profileApi.setApiClient(apiClient);
    nihApi.setApiClient(apiClient);
    staticNotebooksApi.setApiClient(apiClient);
    workspacesApi.setApiClient(apiClient);
  }

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao,
      WorkspacesController workspacesController,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      UserDao userDao,
      BillingProjectBufferService billingProjectBufferService,
      @Qualifier("saApiClient") ApiClient saApiClient,
      @Qualifier("workspaceAclsApi") WorkspacesApi saWorkspaceApi) {
    return (args) -> {
      padding();
      initializeApis();
      System.out.println("Apis initialized");

      int numToProcess = Integer.parseInt(args[0]);

      boolean dryRun = false;
      if (args.length > 1 && args[1].equals("true")) {
        dryRun = true;
      }

      List<WorkspaceResponse> processed = new ArrayList<>();
      List<Pair<WorkspaceResponse, String>> failedWorkspaces = new ArrayList<>();

      for (WorkspaceResponse workspaceResponse : saWorkspaceApi.listWorkspaces()) {
        Workspace dbWorkspace = workspaceDao.findByFirecloudUuid(workspaceResponse.getWorkspace().getWorkspaceId());

        if (dbWorkspace == null) {
          // System.out.println("Found workspace in FC but not recorded in AoU " + shorthand(workspaceResponse.getWorkspace()));
          continue;
        }

        if (dbWorkspace.getWorkspaceActiveStatusEnum().equals(WorkspaceActiveStatus.DELETED) ||
            !dbWorkspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
          continue;
        }

        if (workspaceResponse.getAccessLevel().equals("NO ACCESS")) {
          System.out
              .println("Found NO ACCESS workspace " + shorthand(workspaceResponse.getWorkspace()));
          failedWorkspaces.add(Pair.of(workspaceResponse, "NO ACCESS"));
          continue;
        }

        while (billingProjectBufferService.availableProportion() < .25) {
          System.out.println("Less than 25% of the buffer is available (" + billingProjectBufferService.availableProportion()*100 + "%)... Sleeping for 30 seconds");
          Thread.sleep(30000);
        }

        try {
          System.out.println("About to clone " + shorthand(dbWorkspace));

          workspacesApi.setApiClient(saApiClient);
          org.pmiops.workbench.model.WorkspaceResponse apiWorkspace = workspaceService.getWorkspace(
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName());

          org.pmiops.workbench.model.Workspace toWorkspace = new org.pmiops.workbench.model.Workspace();
          toWorkspace.setNamespace(dbWorkspace.getWorkspaceNamespace());
          toWorkspace.setName(dbWorkspace.getName());
          toWorkspace.setResearchPurpose(apiWorkspace.getWorkspace().getResearchPurpose());
          toWorkspace.setCdrVersionId(apiWorkspace.getWorkspace().getCdrVersionId());

          CloneWorkspaceRequest request = new CloneWorkspaceRequest();
          request.setWorkspace(toWorkspace);
          request.setIncludeUserRoles(true);

          providedUser = userDao.findUserByEmail(apiWorkspace.getWorkspace().getCreator());
          impersonateUser(fireCloudService.getApiClientWithImpersonation(apiWorkspace.getWorkspace().getCreator()));

          if (!dryRun) {
            System.out.println("Sending clone request");
            System.out.println(request);

            CloneWorkspaceResponse cloneResponse = workspacesController
                .cloneWorkspace(dbWorkspace.getWorkspaceNamespace(),
                    dbWorkspace.getFirecloudName(), request).getBody();
            System.out.println("Successful Clone into " + shorthand(cloneResponse.getWorkspace()));

            dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.MIGRATED);
            workspaceDao.save(dbWorkspace);
          }

          processed.add(workspaceResponse);
        } catch (Exception e) {
          System.out.println("Failed on " + shorthand(dbWorkspace));
          System.out.println(workspaceResponse);
          e.printStackTrace();
          failedWorkspaces.add(Pair.of(workspaceResponse, e.getMessage()));
        }

        if (processed.size() + failedWorkspaces.size() == numToProcess) {
          break;
        }
      }

      padding();
      System.out.println("Processed Workspaces : " + processed.size());
      for (WorkspaceResponse workspaceResponse : processed) {
        System.out.println(shorthand(workspaceResponse.getWorkspace()));
      }
      System.out.println("Failed Workspaces : " + failedWorkspaces.size());
      for (Pair<WorkspaceResponse, String> failedWorkspace : failedWorkspaces) {
        System.out.println(shorthand(failedWorkspace.first.getWorkspace()));
        System.out.println(failedWorkspace.second);
      }
      padding();
    };
  }

  private String shorthand(Workspace workspace) {
    return "(" + workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName() + ")";
  }

  private String shorthand(org.pmiops.workbench.model.Workspace workspace) {
    return "(" + workspace.getNamespace() + " : " + workspace.getId() + ")";
  }

  private String shorthand(org.pmiops.workbench.firecloud.model.Workspace workspace) {
    return "(" + workspace.getNamespace() + " : " + workspace.getName() + ")";
  }

  private void padding() {
    for (int i = 0; i < 15; i++) {
      System.out.println("***");
    }
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(BulkCloneWorkspacesTool.class).web(false).run(args);
  }
}
