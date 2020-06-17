package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;

@Mapper(componentModel = "spring")
public interface FirecloudMapper {

  ListClusterResponse toApiListClusterResponse(
      org.pmiops.workbench.notebooks.model.ListClusterResponse leonardoListClusterResponse);

  default ClusterStatus toApiClusterStatus(
      org.pmiops.workbench.notebooks.model.ClusterStatus leonardoClusterStatus) {
    return ClusterStatus.fromValue(leonardoClusterStatus.toString());
  }

  default WorkspaceAccessLevel fcAccessLevelToApiAccessLevel(FirecloudWorkspaceAccessEntry acl) {
    return WorkspaceAccessLevel.fromValue(acl.getAccessLevel());
  }

  default WorkspaceAccessLevel fcWorkspaceResponseToApiWorkspaceAccessLevel(
      FirecloudWorkspaceResponse fcResponse) {
    if (fcResponse.getAccessLevel().equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel());
    }
  }
}
