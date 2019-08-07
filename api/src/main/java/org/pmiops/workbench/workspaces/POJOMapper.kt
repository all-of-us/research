package org.pmiops.workbench.workspaces

import org.mapstruct.Mapper

@Mapper(componentModel = "spring") interface POJOMapper {
    fun sourceToDestination(source: POJOSource): POJODest
    fun destinationToSource(dest: POJODest): POJOSource
}