package org.pmiops.workbench.workspaces;

import org.pmiops.workbench.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMapper {

  private POJOJavaMapper mapper;

  @Autowired
  public WorkspaceMapper(POJOJavaMapper pojoJavaMapper) {
    this.mapper = pojoJavaMapper;
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

}
