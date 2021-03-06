package org.pmiops.workbench.rdr;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;

public class RdrExportEnums {

  private static final BiMap<Race, org.pmiops.workbench.rdr.model.Race> CLIENT_TO_RDR_RACE =
      ImmutableBiMap.<Race, org.pmiops.workbench.rdr.model.Race>builder()
          .put(Race.AA, org.pmiops.workbench.rdr.model.Race.AA)
          .put(Race.AIAN, org.pmiops.workbench.rdr.model.Race.AIAN)
          .put(Race.ASIAN, org.pmiops.workbench.rdr.model.Race.ASIAN)
          .put(Race.NHOPI, org.pmiops.workbench.rdr.model.Race.NHOPI)
          .put(Race.WHITE, org.pmiops.workbench.rdr.model.Race.WHITE)
          .put(Race.PREFER_NO_ANSWER, org.pmiops.workbench.rdr.model.Race.PREFER_NOT_TO_ANSWER)
          .put(Race.NONE, org.pmiops.workbench.rdr.model.Race.NONE)
          .build();

  private static final BiMap<Ethnicity, org.pmiops.workbench.rdr.model.Ethnicity>
      CLIENT_TO_RDR_ETHNICITY =
          ImmutableBiMap.<Ethnicity, org.pmiops.workbench.rdr.model.Ethnicity>builder()
              .put(Ethnicity.HISPANIC, org.pmiops.workbench.rdr.model.Ethnicity.HISPANIC)
              .put(Ethnicity.NOT_HISPANIC, org.pmiops.workbench.rdr.model.Ethnicity.NOT_HISPANIC)
              .put(
                  Ethnicity.PREFER_NO_ANSWER,
                  org.pmiops.workbench.rdr.model.Ethnicity.PREFER_NOT_TO_ANSWER)
              .build();

  private static final BiMap<GenderIdentity, org.pmiops.workbench.rdr.model.Gender>
      CLIENT_TO_RDR_GENDER =
          ImmutableBiMap.<GenderIdentity, org.pmiops.workbench.rdr.model.Gender>builder()
              .put(GenderIdentity.MAN, org.pmiops.workbench.rdr.model.Gender.MAN)
              .put(GenderIdentity.WOMAN, org.pmiops.workbench.rdr.model.Gender.WOMAN)
              .put(GenderIdentity.NON_BINARY, org.pmiops.workbench.rdr.model.Gender.NON_BINARY)
              .put(GenderIdentity.TRANSGENDER, org.pmiops.workbench.rdr.model.Gender.TRANSGENDER)
              .put(
                  GenderIdentity.NONE_DESCRIBE_ME,
                  org.pmiops.workbench.rdr.model.Gender.NONE_DESCRIBE_ME)
              .put(
                  GenderIdentity.PREFER_NO_ANSWER,
                  org.pmiops.workbench.rdr.model.Gender.PREFER_NOT_TO_ANSWER)
              .build();

  private static final BiMap<Education, org.pmiops.workbench.rdr.model.Education>
      CLIENT_TO_RDR_EDUCATION =
          ImmutableBiMap.<Education, org.pmiops.workbench.rdr.model.Education>builder()
              .put(Education.NO_EDUCATION, org.pmiops.workbench.rdr.model.Education.NO_EDUCATION)
              .put(Education.GRADES_1_12, org.pmiops.workbench.rdr.model.Education.GRADES_1_12)
              .put(
                  Education.COLLEGE_GRADUATE,
                  org.pmiops.workbench.rdr.model.Education.COLLEGE_GRADUATE)
              .put(Education.UNDERGRADUATE, org.pmiops.workbench.rdr.model.Education.UNDERGRADUATE)
              .put(Education.MASTER, org.pmiops.workbench.rdr.model.Education.MASTER)
              .put(Education.DOCTORATE, org.pmiops.workbench.rdr.model.Education.DOCTORATE)
              .build();

  private static final BiMap<SexAtBirth, org.pmiops.workbench.rdr.model.SexAtBirth>
      CLIENT_TO_RDR_SEX_AT_BIRTH =
          ImmutableBiMap.<SexAtBirth, org.pmiops.workbench.rdr.model.SexAtBirth>builder()
              .put(SexAtBirth.MALE, org.pmiops.workbench.rdr.model.SexAtBirth.MALE)
              .put(SexAtBirth.FEMALE, org.pmiops.workbench.rdr.model.SexAtBirth.FEMALE)
              .put(SexAtBirth.INTERSEX, org.pmiops.workbench.rdr.model.SexAtBirth.INTERSEX)
              .put(
                  SexAtBirth.NONE_OF_THESE_DESCRIBE_ME,
                  org.pmiops.workbench.rdr.model.SexAtBirth.NONE_OF_THESE_DESCRIBE_ME)
              .put(
                  SexAtBirth.PREFER_NO_ANSWER,
                  org.pmiops.workbench.rdr.model.SexAtBirth.PREFER_NOT_TO_ANSWER)
              .build();

  private static final BiMap<Disability, org.pmiops.workbench.rdr.model.Disability>
      CLIENT_TO_RDR_DISABILITY =
          ImmutableBiMap.<Disability, org.pmiops.workbench.rdr.model.Disability>builder()
              .put(Disability.TRUE, org.pmiops.workbench.rdr.model.Disability.YES)
              .put(Disability.FALSE, org.pmiops.workbench.rdr.model.Disability.NO)
              .build();

  private static final BiMap<SpecificPopulationEnum, RdrWorkspaceDemographic.RaceEthnicityEnum>
      CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY =
          ImmutableBiMap
              .<SpecificPopulationEnum, RdrWorkspaceDemographic.RaceEthnicityEnum>builder()
              .put(SpecificPopulationEnum.RACE_AA, RdrWorkspaceDemographic.RaceEthnicityEnum.AA)
              .put(SpecificPopulationEnum.RACE_AIAN, RdrWorkspaceDemographic.RaceEthnicityEnum.AIAN)
              .put(
                  SpecificPopulationEnum.RACE_ASIAN,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.ASIAN)
              .put(SpecificPopulationEnum.RACE_NHPI, RdrWorkspaceDemographic.RaceEthnicityEnum.NHPI)
              .put(SpecificPopulationEnum.RACE_MENA, RdrWorkspaceDemographic.RaceEthnicityEnum.MENA)
              .put(
                  SpecificPopulationEnum.RACE_HISPANIC,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.HISPANIC)
              .put(
                  SpecificPopulationEnum.RACE_MORE_THAN_ONE,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.MULTI)
              .build();

  private static final BiMap<SpecificPopulationEnum, RdrWorkspaceDemographic.AgeEnum>
      CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE =
          ImmutableBiMap.<SpecificPopulationEnum, RdrWorkspaceDemographic.AgeEnum>builder()
              .put(SpecificPopulationEnum.AGE_CHILDREN, RdrWorkspaceDemographic.AgeEnum.AGE_0_11)
              .put(
                  SpecificPopulationEnum.AGE_ADOLESCENTS, RdrWorkspaceDemographic.AgeEnum.AGE_12_17)
              .put(SpecificPopulationEnum.AGE_OLDER, RdrWorkspaceDemographic.AgeEnum.AGE_65_74)
              .put(
                  SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75,
                  RdrWorkspaceDemographic.AgeEnum.AGE_75_AND_MORE)
              .build();

  private static final BiMap<Degree, org.pmiops.workbench.rdr.model.Degree> CLIENT_TO_RDR_DEGREE =
      ImmutableBiMap.<Degree, org.pmiops.workbench.rdr.model.Degree>builder()
          .put(Degree.PHD, org.pmiops.workbench.rdr.model.Degree.PHD)
          .put(Degree.MD, org.pmiops.workbench.rdr.model.Degree.MD)
          .put(Degree.JD, org.pmiops.workbench.rdr.model.Degree.JD)
          .put(Degree.EDD, org.pmiops.workbench.rdr.model.Degree.EDD)
          .put(Degree.MSN, org.pmiops.workbench.rdr.model.Degree.MSN)
          .put(Degree.MS, org.pmiops.workbench.rdr.model.Degree.MS)
          .put(Degree.MA, org.pmiops.workbench.rdr.model.Degree.MA)
          .put(Degree.MBA, org.pmiops.workbench.rdr.model.Degree.MBA)
          .put(Degree.ME, org.pmiops.workbench.rdr.model.Degree.ME)
          .put(Degree.MSW, org.pmiops.workbench.rdr.model.Degree.MSW)
          .put(Degree.MPH, org.pmiops.workbench.rdr.model.Degree.MPH)
          .put(Degree.BA, org.pmiops.workbench.rdr.model.Degree.BA)
          .put(Degree.BS, org.pmiops.workbench.rdr.model.Degree.BS)
          .put(Degree.BSN, org.pmiops.workbench.rdr.model.Degree.BSN)
          .put(Degree.NONE, org.pmiops.workbench.rdr.model.Degree.UNSET)
          .build();

  public static org.pmiops.workbench.rdr.model.Race raceToRdrRace(Race race) {
    if (race == null) return null;
    return CLIENT_TO_RDR_RACE.get(race);
  }

  public static org.pmiops.workbench.rdr.model.Ethnicity ethnicityToRdrEthnicity(
      Ethnicity ethnicity) {
    if (ethnicity == null) return null;
    return CLIENT_TO_RDR_ETHNICITY.get(ethnicity);
  }

  public static org.pmiops.workbench.rdr.model.Gender genderToRdrGender(
      GenderIdentity genderIdentity) {
    if (genderIdentity == null) return null;
    return CLIENT_TO_RDR_GENDER.get(genderIdentity);
  }

  public static org.pmiops.workbench.rdr.model.Education educationToRdrEducation(
      Education education) {
    if (education == null) return null;
    return CLIENT_TO_RDR_EDUCATION.get(education);
  }

  public static org.pmiops.workbench.rdr.model.SexAtBirth sexAtBirthToRdrSexAtBirth(
      SexAtBirth sexAtBirth) {
    if (sexAtBirth == null) return null;
    return CLIENT_TO_RDR_SEX_AT_BIRTH.get(sexAtBirth);
  }

  public static org.pmiops.workbench.rdr.model.Disability disabilityToRdrDisability(
      Disability disability) {
    if (disability == null) return org.pmiops.workbench.rdr.model.Disability.PREFER_NOT_TO_ANSWER;
    return CLIENT_TO_RDR_DISABILITY.get(disability);
  }

  public static org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.RaceEthnicityEnum
      specificPopulationToRaceEthnicity(SpecificPopulationEnum specificPopulationEnum) {
    if (CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY.containsKey(specificPopulationEnum)) {
      return CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY.get(specificPopulationEnum);
    } else {
      return null;
    }
  }

  public static org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic.AgeEnum
      specificPopulationToAge(SpecificPopulationEnum specificPopulationEnum) {
    if (CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE.containsKey(specificPopulationEnum)) {
      return CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE.get(specificPopulationEnum);
    } else {
      return null;
    }
  }

  public static org.pmiops.workbench.rdr.model.Degree degreeToRdrDegree(Degree degree) {
    if (degree == null) return null;
    return CLIENT_TO_RDR_DEGREE.get(degree);
  }
}
