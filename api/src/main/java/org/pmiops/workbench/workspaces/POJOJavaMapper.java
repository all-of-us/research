package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;

@Mapper(componentModel = "spring")
public interface POJOJavaMapper {
  POJODest sourceToDestination(POJOSource source);
  POJOSource destinationToSource(POJODest destination);

  @Mapping(source = "key.name", target = "name")
  POJODest multiSource(POJOSource source, POJOSourceKey key);

  POJOSuperDest transform(POJOSuperSource source);

  ResearchPurpose workspaceToResearchPurpose(Workspace workspace);

  default Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }

    return null;
  }

  @AfterMapping
  default void afterResearchPurpose(ResearchPurpose researchPurpose, @MappingTarget Workspace workspace) {
    if (researchPurpose.getPopulation()) {
      researchPurpose.setPopulationDetails(new ArrayList<>(workspace.getSpecificPopulationsEnum()));
    }
  }
}
