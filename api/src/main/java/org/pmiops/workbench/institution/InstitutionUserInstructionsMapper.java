package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionUserInstructionsMapper {
  @Mapping(target = "institutionUserInstructionsId", ignore = true)
  @Mapping(target = "institution", ignore = true) // set by setFields()
  @Mapping(target = "userInstructions", ignore = true) // set by setFields()
  DbInstitutionUserInstructions modelToDb(
      InstitutionUserInstructions modelObject, @Context InstitutionService institutionService);

  @AfterMapping
  default void setFields(
      @MappingTarget DbInstitutionUserInstructions target,
      InstitutionUserInstructions modelObject,
      @Context InstitutionService institutionService) {

    final DbInstitution institution =
        institutionService.getDbInstitutionOrThrow(modelObject.getInstitutionShortName());
    final PolicyFactory removeAllTags = new HtmlPolicyBuilder().toFactory();
    final String sanitizedInstructions =
        removeAllTags.sanitize(modelObject.getInstructions()).trim();

    target.setInstitution(institution).setUserInstructions(sanitizedInstructions);
  }
}
