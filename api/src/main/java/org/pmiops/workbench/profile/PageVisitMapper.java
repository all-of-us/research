package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface PageVisitMapper {
  @Mapping(target = "page", source = "pageId")
  PageVisit dbPageVisitToPageVisit(DbPageVisit dbPageVisit);

  @Mapping(target = "pageId", source = "page")
  @Mapping(target = "pageVisitId", ignore = true)
  @Mapping(target = "user", ignore = true) // set by ProfileController.updatePageVisits
  DbPageVisit pageVisitToDbPageVisit(PageVisit pageVisit);
}
