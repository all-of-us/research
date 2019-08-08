package org.pmiops.workbench.workspaces;

import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMapper {

  private POJOJavaMapper mapper;

  @Autowired
  public WorkspaceMapper(POJOJavaMapper pojoJavaMapper) {
    this.mapper = pojoJavaMapper;
  }

  public WorkspaceAccessLevel toApiWorkspaceAccessLevel(String firecloudAccessLevel) {
    if (firecloudAccessLevel.equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(firecloudAccessLevel);
    }
  }

  public Workspace toApiWorkspace(
      org.pmiops.workbench.db.model.Workspace workspace,
      org.pmiops.workbench.firecloud.model.Workspace fcWorkspace) {
    ResearchPurpose researchPurpose = mapper.workspaceToResearchPurpose(workspace);

    Workspace result =
        new Workspace()
            .etag(Etags.fromVersion(workspace.getVersion()))
            .lastModifiedTime(workspace.getLastModifiedTime().getTime())
            .creationTime(workspace.getCreationTime().getTime())
            .dataAccessLevel(workspace.getDataAccessLevelEnum())
            .name(workspace.getName())
            .id(fcWorkspace.getName())
            .namespace(fcWorkspace.getNamespace())
            .researchPurpose(researchPurpose)
            .published(workspace.getPublished())
            .googleBucketName(fcWorkspace.getBucketName());

    if (fcWorkspace.getCreatedBy() != null) {
      result.setCreator(fcWorkspace.getCreatedBy());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    return result;
  }

  public org.pmiops.workbench.db.model.Workspace toDbWorkspace(Workspace workspace) {
    org.pmiops.workbench.db.model.Workspace result = new org.pmiops.workbench.db.model.Workspace();

    if (workspace.getDataAccessLevel() != null) {
      result.setDataAccessLevelEnum(workspace.getDataAccessLevel());
    }

    result.setName(workspace.getName());

    if (workspace.getResearchPurpose() != null) {
      mapper.mergeResearchPurposeIntoWorkspace(result, workspace.getResearchPurpose());
    }

    return result;
  }

  public UserRole toApiUserRole(
      org.pmiops.workbench.db.model.User user, WorkspaceAccessEntry aclEntry) {
    UserRole result = new UserRole();
    result.setEmail(user.getEmail());
    result.setGivenName(user.getGivenName());
    result.setFamilyName(user.getFamilyName());
    result.setRole(WorkspaceAccessLevel.fromValue(aclEntry.getAccessLevel()));
    return result;
  }

}
