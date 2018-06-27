package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

@RestController
public class BugReportController implements BugReportApiDelegate {
  private static final Logger log = Logger.getLogger(BugReportController.class.getName());
  private static final List<String> notebookLogFiles =
      ImmutableList.of("delocalization.log", "jupyter.log", "localization.log");

  private final Provider<JiraService> jiraServiceProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<JupyterApi> jupyterApiProvider;
  private Provider<User> userProvider;
  private Provider<CloudStorageService> cloudStorageServiceProvider;

  @Autowired
  BugReportController(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<User> userProvider,
      Provider<JupyterApi> jupyterApiProvider,
      Provider<CloudStorageService> cloudStorageService,
      Provider<JiraService> jiraService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.jupyterApiProvider = jupyterApiProvider;
    this.jiraServiceProvider = jiraService;
    this.cloudStorageServiceProvider = cloudStorageService;
  }

  @VisibleForTesting
  void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }


  @Override
  public ResponseEntity<BugReport> sendBugReport(BugReport bugReport) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    User user = userProvider.get();
    JupyterApi jupyterApi = jupyterApiProvider.get();
    Properties props = new Properties();
    CloudStorageService cloudStorageService = cloudStorageServiceProvider.get();
    JiraService jiraService = jiraServiceProvider.get();
    try {
      String issueKey = jiraService.createIssue(bugReport);
      // If requested, try to pull logs from the notebook cluster using the researcher's creds. Some
      // or all of these log files might be missing, or the cluster may not even exist, so ignore
      // failures here.
      if (Optional.ofNullable(bugReport.getIncludeNotebookLogs()).orElse(false) &&
          BillingProjectStatus.READY.equals(user.getFreeTierBillingProjectStatus())) {
        for (String fileName : BugReportController.notebookLogFiles) {
          try {
            String logContent = jupyterApi.getRootContents(
                user.getFreeTierBillingProjectName(), NotebooksService.DEFAULT_CLUSTER_NAME,
                fileName, "file", "text", /* content */ 1).getContent();
            if (logContent == null) {
              log.info(
                  String.format("Jupyter returned null content for '%s', continuing", fileName));
              continue;
            }
            jiraService.uploadAttachment(issueKey, fileName, logContent);
          } catch (ApiException e) {
            log.info(String.format("failed to retrieve notebook log '%s', continuing", fileName));
          }
        }
     }
    } catch (org.pmiops.workbench.jira.ApiException e) {
      log.severe("Error while connecting to JIRA server" + e.getMessage());
      throw new ServerErrorException("Error while connecting to JIRA server ");
    }
   return ResponseEntity.ok(bugReport);
  }

}
