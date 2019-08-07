package org.pmiops.workbench.workspaces;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface POJOJavaMapper {
  POJODest sourceToDestination(POJOSource source);
  POJOSource destinationToSource(POJODest destination);

  @Mapping(source = "key.name", target = "name")
  POJODest multiSource(POJOSource source, POJOSourceKey key);

  POJOSuperDest transform(POJOSuperSource source);
}
