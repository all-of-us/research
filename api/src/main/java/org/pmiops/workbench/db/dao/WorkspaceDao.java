package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;


/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 */
public interface WorkspaceDao extends CrudRepository<Workspace, Long>, WorkspaceDaoCustom {
  List<Workspace> findByWorkspaceNamespace(String workspaceNamespace);
  Workspace findByWorkspaceNamespaceAndFirecloudName(String workspaceNamespace,
      String firecloudName);
  List<Workspace> findByCreatorOrderByNameAsc(User creator);
}
