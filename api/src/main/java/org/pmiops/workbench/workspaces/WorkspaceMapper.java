package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

  UserRole userToUserRole(User user);

  @Mapping(target = "researchPurpose", source="dbWorkspace")
  @Mapping(target = "etag", source="dbWorkspace.version", qualifiedByName = "etag")
  @Mapping(target = "dataAccessLevel", source="dbWorkspace.dataAccessLevelEnum")
  @Mapping(target = "name", source="dbWorkspace.name")
  @Mapping(target = "id", source="fcWorkspace.name")
  @Mapping(target = "googleBucketName", source="fcWorkspace.bucketName")
  @Mapping(target = "creator", source="fcWorkspace.createdBy")
  @Mapping(target = "cdrVersionId", source="dbWorkspace.cdrVersion")
  org.pmiops.workbench.model.Workspace toApiWorkspace(Workspace dbWorkspace,
      org.pmiops.workbench.firecloud.model.Workspace fcWorkspace);

  @Mapping(target = "approved", ignore = true)
  @Mapping(target = "specificPopulationsEnum", source="populationDetails")
  void mergeResearchPurposeIntoWorkspace(@MappingTarget Workspace workspace, ResearchPurpose researchPurpose);

  @Named("etag")
  default String etag(int version) {
    return Etags.fromVersion(version);
  }

  default String cdrVersionId(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }

  default WorkspaceAccessLevel fromFcAccessLevel(String firecloudAccessLevel) {
    if (firecloudAccessLevel.equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(firecloudAccessLevel);
    }
  }

  default Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }

    return null;
  }

  default Timestamp timestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  @Mapping(target="populationDetails", ignore = true)
  ResearchPurpose workspaceToResearchPurpose(Workspace workspace);

  @AfterMapping
  default void afterWorkspaceIntoResearchPurpose(@MappingTarget ResearchPurpose researchPurpose, Workspace workspace) {
    if (workspace.getPopulation()) {
      researchPurpose.setPopulationDetails(new ArrayList<>(workspace.getSpecificPopulationsEnum()));
    }
  }

  @AfterMapping
  default void afterResearchPurposeIntoWorkspace(@MappingTarget Workspace workspace, ResearchPurpose researchPurpose) {
    if (researchPurpose.getPopulation()) {
      workspace.setSpecificPopulationsEnum(new HashSet<>(researchPurpose.getPopulationDetails()));
    }
  }
}