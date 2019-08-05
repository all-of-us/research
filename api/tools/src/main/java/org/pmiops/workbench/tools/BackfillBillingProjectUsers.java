package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceACL;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Backfill script for granting the billing project user role to all AoU owners (corresponding to
 * Firecloud OWNER access). For http://broad.io/1ppw, collaborators need to be either writers or
 * billing project users in order to launch clusters within shared billing projects, see RW-3009 and
 * RW-3188 for details.
 */
@Configuration
public class BackfillBillingProjectUsers {
  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();
  private static Option billingProjectPrefixOpt =
      Option.builder()
          .longOpt("billing-project-prefix")
          .desc("Billing project prefix to filter by, other workspaces are ignored")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();
  private static Options options =
      new Options().addOption(fcBaseUrlOpt).addOption(billingProjectPrefixOpt).addOption(dryRunOpt);

  private static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
      };

  private static final Logger log = Logger.getLogger(BackfillBillingProjectUsers.class.getName());

  private static ApiClient newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(FC_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    return apiClient;
  }

  private static WorkspacesApi newWorkspacesApi(String apiUrl) throws IOException {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  private static BillingApi newBillingApi(String apiUrl) throws IOException {
    BillingApi api = new BillingApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    log.info(prefix + msg);
  }

  /**
   * Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
   * instead. Run this through a typed Gson conversion process to coerce it into the desired type.
   */
  private static Map<String, WorkspaceAccessEntry> extractAclResponse(WorkspaceACL aclResp) {
    Type accessEntryType = new TypeToken<Map<String, WorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }

  private static void backfill(
      WorkspacesApi workspacesApi,
      BillingApi billingApi,
      String billingProjectPrefix,
      boolean dryRun)
      throws ApiException {
    int userUpgrades = 0;
    for (WorkspaceResponse resp : workspacesApi.listWorkspaces()) {
      Workspace w = resp.getWorkspace();
      if (!w.getNamespace().startsWith(billingProjectPrefix)) {
        continue;
      }

      String id = w.getNamespace() + "/" + w.getName();
      if (!"PROJECT_OWNER".equals(resp.getAccessLevel())) {
        log.warning(
            String.format(
                "service account has '%s' access to workspace '%s'; skipping",
                resp.getAccessLevel(), id));
        continue;
      }

      List<WorkspaceACLUpdate> updates = new ArrayList<>();
      Map<String, WorkspaceAccessEntry> acl =
          extractAclResponse(workspacesApi.getWorkspaceAcl(w.getNamespace(), w.getName()));
      for (String user : acl.keySet()) {
        WorkspaceAccessEntry entry = acl.get(user);
        if (!"OWNER".equals(entry.getAccessLevel())) {
          // Only owners should be granted billing project user.
          continue;
        }
        dryLog(
            dryRun,
            String.format(
                "granting billing project user on '%s' to '%s' (%s)",
                w.getNamespace(), user, entry.getAccessLevel()));
        if (!dryRun) {
          billingApi.addUserToBillingProject(w.getNamespace(), "user", user);
        }
        userUpgrades++;
      }
    }

    dryLog(dryRun, String.format("added %d users as billing project users", userUpgrades));
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      backfill(
          newWorkspacesApi(opts.getOptionValue(fcBaseUrlOpt.getLongOpt())),
          newBillingApi(opts.getOptionValue(fcBaseUrlOpt.getLongOpt())),
          opts.getOptionValue(billingProjectPrefixOpt.getLongOpt()),
          opts.hasOption(dryRunOpt.getLongOpt()));
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(BackfillBillingProjectUsers.class).web(false).run(args);
  }
}
