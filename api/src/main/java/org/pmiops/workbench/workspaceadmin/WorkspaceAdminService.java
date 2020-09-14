package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import javax.annotation.Nullable;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;

public interface WorkspaceAdminService {
  Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace);

  AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId);

  AdminWorkspaceCloudStorageCounts getAdminWorkspaceCloudStorageCounts(
      String workspaceNamespace, String workspaceName);

  CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace);

  WorkspaceAdminView getWorkspaceAdminView(String workspaceNamespace);

  WorkspaceAuditLogQueryResponse getAuditLogEntries(
      String workspaceNamespace,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillisNullable);

  String getReadOnlyNotebook(
      String workspaceNamespace,
      String workspaceName,
      AccessReason accessReason,
      String notebookName);
}
