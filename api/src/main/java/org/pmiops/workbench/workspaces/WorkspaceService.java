package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public interface WorkspaceService {

  String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";

  WorkspaceDao getDao();
  Workspace findByWorkspaceId(long workspaceId);
  FireCloudService getFireCloudService();
  Workspace get(String ns, String firecloudName);
  List<Workspace> getWorkspaces(WorkspaceAccessLevel accessLevel);
  Workspace getByName(String ns, String name);
  Workspace getRequired(String ns, String firecloudName);
  Workspace getRequiredWithCohorts(String ns, String firecloudName);
  Workspace saveWithLastModified(Workspace workspace);
  List<Workspace> findForReview();
  void setResearchPurposeApproved(String ns, String firecloudName, boolean approved);
  Workspace updateUserRoles(Workspace workspace, Set<WorkspaceUserRole> userRoleSet);
  Workspace saveAndCloneCohortsAndConceptSets(Workspace from, Workspace to);
  WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId);
  WorkspaceAccessLevel enforceWorkspaceAccessLevel(String workspaceNamespace,
      String workspaceId, WorkspaceAccessLevel requiredAccess);
  Workspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(String workspaceNamespace,
      String workspaceId, WorkspaceAccessLevel workspaceAccessLevel);

}
