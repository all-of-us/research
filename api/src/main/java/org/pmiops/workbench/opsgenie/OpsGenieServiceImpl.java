package org.pmiops.workbench.opsgenie;

import com.google.common.collect.ImmutableList;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.model.EgressEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpsGenieServiceImpl implements OpsGenieService {
  private static final Logger logger = Logger.getLogger(OpsGenieServiceImpl.class.getName());

  private Provider<AlertApi> alertApiProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceDao workspaceDao;
  private final UserDao userDao;

  @Autowired
  public OpsGenieServiceImpl(
      Provider<AlertApi> alertApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceDao workspaceDao,
      UserDao userDao) {
    this.alertApiProvider = alertApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;
  }

  private CreateAlertRequest egressEventToOpsGenieAlert(
      EgressEvent egressEvent, WorkbenchConfig workbenchConfig) {
    CreateAlertRequest request = new CreateAlertRequest();
    request.setMessage(String.format("High-egress event (%s)", egressEvent.getProjectName()));
    request.setDescription(getAlertDescription(egressEvent));

    // Add a note with some more specific details about the alerting criteria and threshold. Notes
    // are appended to an existing Opsgenie ticket if this request is de-duplicated against an
    // existing ticket, so they're a helpful way to summarize temporal updates to the status of
    // an incident.
    request.setNote(
        String.format(
            "Time window: %d secs, threshold: %.2f Mib, observed: %.2f Mib",
            egressEvent.getTimeWindowDuration(),
            egressEvent.getEgressMibThreshold(),
            egressEvent.getEgressMib()));
    request.setTags(ImmutableList.of("high-egress-event"));

    // Set the alias, which is Opsgenie's string key for alert de-duplication. See
    // https://docs.opsgenie.com/docs/alert-deduplication
    request.setAlias(egressEvent.getProjectName() + " | " + egressEvent.getVmName());
    return request;
  }

  private String getAlertDescription(EgressEvent egressEvent) {
    //    final DbWorkspace dbWorkspace =
    // workspaceDao.findAllByWorkspaceNamespace(egressEvent.getVmName());
    return String.format("Workspace project: %s\n", egressEvent.getProjectName())
        + String.format("Workspace Name (Jupyter VM name): %s\n", egressEvent.getVmName())
        + String.format(
            "Egress detected: %.2f Mib in %d secs\n\n",
            egressEvent.getEgressMib(), egressEvent.getTimeWindowDuration())
        + String.format(
            "Workspace Admin Console (Prod Admin User): %s/admin/workspaces/%s/\n",
            workbenchConfigProvider.get().server.uiBaseUrl, egressEvent.getProjectName())
        + "Playbook Entry: https://broad.io/aou-high-egress-event";
  }

  @Override
  public void createEgressEventAlert(EgressEvent egressEvent) {
    try {
      SuccessResponse response =
          this.alertApiProvider
              .get()
              .createAlert(egressEventToOpsGenieAlert(egressEvent, workbenchConfigProvider.get()));
      logger.info(
          String.format(
              "Successfully created or updated Opsgenie alert for high-egress event on project %s (Opsgenie request ID %s)",
              egressEvent.getProjectName(), response.getRequestId()));
    } catch (ApiException e) {
      logger.severe(
          String.format(
              "Error creating Opsgenie alert for egress event on project %s: %s",
              egressEvent.getProjectName(), e.getMessage()));
      e.printStackTrace();
    }
  }
}
