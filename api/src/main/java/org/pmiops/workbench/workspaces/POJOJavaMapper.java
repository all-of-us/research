package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.UserRole;

@Mapper(componentModel = "spring")
public interface POJOJavaMapper {

  UserRole userToUserRole(User user);

  ResearchPurpose workspaceToResearchPurpose(Workspace workspace);

  @Mapping(target = "approved", ignore = true)
  void mergeResearchPurposeIntoWorkspace(@MappingTarget Workspace workspace, ResearchPurpose researchPurpose);

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
