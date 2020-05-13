package org.pmiops.workbench.rdr;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface RdrMapper {
  @Mapping(target = "modifiedTime", source = "lastModifiedTime")
  @Mapping(target = "intendToStudy", source = "intendedStudy")
  @Mapping(target = "diseaseFocusedResearchName", source = "diseaseOfFocus")
  @Mapping(target = "ethicalLegalSocialImplications", source = "ethics")
  @Mapping(target = "scientificApproaches", source = "scientificApproach")
  @Mapping(target = "findingsFromStudy", source = "anticipatedFindings")
  @Mapping(target = "status", source = "workspaceActiveStatusEnum")
  @Mapping(target = "workspaceUsers", ignore = true)
  @Mapping(target = "excludeFromPublicDirectory", ignore = true)
  @Mapping(target = "focusOnUnderrepresentedPopulations", source = "specificPopulationsEnum")
  @Mapping(target = "workspaceDemographic", source = "specificPopulationsEnum")
  RdrWorkspace toRdrModel(DbWorkspace employeeDbEntity);

  ZoneOffset offset = OffsetDateTime.now().getOffset();

  default OffsetDateTime toModelOffsetTime(Timestamp dbTime) {
    return dbTime.toLocalDateTime().atOffset(offset);
  }

  default RdrWorkspace.StatusEnum toModelStatus(WorkspaceActiveStatus workspaceActiveStatus) {
    return workspaceActiveStatus == WorkspaceActiveStatus.ACTIVE
        ? RdrWorkspace.StatusEnum.ACTIVE
        : RdrWorkspace.StatusEnum.INACTIVE;
  }

  default boolean toModelFocusOnUnderrepresentedPopulation(
      Set<SpecificPopulationEnum> dbSpecificPopulationSet) {
    return dbSpecificPopulationSet != null && dbSpecificPopulationSet.size() > 0;
  }

  default RdrWorkspaceDemographic toModelWorkspaceDemographic(
      Set<SpecificPopulationEnum> dbPopulationEnumSet) {
    RdrWorkspaceDemographic rdrDemographic = new RdrWorkspaceDemographic();

    rdrDemographic.setAccessToCare(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.ACCESS_TO_CARE)
            ? RdrWorkspaceDemographic.AccessToCareEnum.NOT_EASILY_ACCESS_CARE
            : RdrWorkspaceDemographic.AccessToCareEnum.UNSET);

    rdrDemographic.setDisabilityStatus(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.DISABILITY_STATUS)
            ? RdrWorkspaceDemographic.DisabilityStatusEnum.DISABILITY
            : RdrWorkspaceDemographic.DisabilityStatusEnum.UNSET);

    rdrDemographic.setEducationLevel(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.EDUCATION_LEVEL)
            ? RdrWorkspaceDemographic.EducationLevelEnum.LESS_THAN_HIGH_SCHOOL
            : RdrWorkspaceDemographic.EducationLevelEnum.UNSET);

    rdrDemographic.setIncomeLevel(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.INCOME_LEVEL)
            ? RdrWorkspaceDemographic.IncomeLevelEnum.BELOW_FEDERAL_POVERTY_LEVEL_200_PERCENT
            : RdrWorkspaceDemographic.IncomeLevelEnum.UNSET);

    rdrDemographic.setGeography(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.GEOGRAPHY)
            ? RdrWorkspaceDemographic.GeographyEnum.RURAL
            : RdrWorkspaceDemographic.GeographyEnum.UNSET);

    rdrDemographic.setSexualOrientation(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.SEXUAL_ORIENTATION)
            ? RdrWorkspaceDemographic.SexualOrientationEnum.OTHER_THAN_STRAIGHT
            : RdrWorkspaceDemographic.SexualOrientationEnum.UNSET);

    rdrDemographic.setGenderIdentity(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.GENDER_IDENTITY)
            ? RdrWorkspaceDemographic.GenderIdentityEnum.OTHER_THAN_MAN_WOMAN
            : RdrWorkspaceDemographic.GenderIdentityEnum.UNSET);

    rdrDemographic.setSexAtBirth(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.SEX)
            ? RdrWorkspaceDemographic.SexAtBirthEnum.INTERSEX
            : RdrWorkspaceDemographic.SexAtBirthEnum.UNSET);

    rdrDemographic.setRaceEthnicity(
        dbPopulationEnumSet.stream()
            .map(RdrExportEnums::specificPopulationToRaceEthnicity)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

    if (rdrDemographic.getRaceEthnicity().isEmpty()) {
      rdrDemographic.setRaceEthnicity(
          Arrays.asList(RdrWorkspaceDemographic.RaceEthnicityEnum.UNSET));
    }

    rdrDemographic.setAge(
        dbPopulationEnumSet.stream()
            .map(RdrExportEnums::specificPopulationToAge)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

    if (rdrDemographic.getAge().isEmpty()) {
      rdrDemographic.setAge(Arrays.asList(RdrWorkspaceDemographic.AgeEnum.UNSET));
    }

    return rdrDemographic;
  }
}
