package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.AcademicRole;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.EducationalRole;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.Gender;
import org.pmiops.workbench.model.IndustryRole;
import org.pmiops.workbench.model.NonAcademicAffiliation;
import org.pmiops.workbench.model.Race;

public class DemographicSurveyEnum {
  private static final BiMap<Race, Short> CLIENT_TO_STORAGE_RACE =
      ImmutableBiMap.<Race, Short>builder()
          .put(Race.AA, (short) 1)
          .put(Race.AIAN, (short) 2)
          .put(Race.ASIAN, (short) 3)
          .put(Race.NHOPI, (short) 4)
          .put(Race.WHITE, (short) 5)
          .put(Race.PREFER_NO_ANSWER, (short) 6)
          .put(Race.NONE, (short) 7)
          .build();

  private static final BiMap<Ethnicity, Short> CLIENT_TO_STORAGE_ETHNICITY =
      ImmutableBiMap.<Ethnicity, Short>builder()
          .put(Ethnicity.HISPANIC, (short) 1)
          .put(Ethnicity.NOT_HISPANIC, (short) 2)
          .put(Ethnicity.PREFER_NO_ANSWER, (short) 3)
          .build();

  private static final BiMap<Gender, Short> CLIENT_TO_STORAGE_GENDER =
      ImmutableBiMap.<Gender, Short>builder()
          .put(Gender.MALE, (short) 1)
          .put(Gender.FEMALE, (short) 2)
          .put(Gender.NON_BINARY, (short) 3)
          .put(Gender.TRANSGENDER, (short) 4)
          .put(Gender.INTERSEX, (short) 5)
          .put(Gender.NONE, (short) 6)
          .put(Gender.PREFER_NO_ANSWER, (short) 7)
          .build();

  private static final BiMap<Education, Short> CLIENT_TO_STORAGE_EDUCATION =
      ImmutableBiMap.<Education, Short>builder()
          .put(Education.NO_EDUCATION, (short) 1)
          .put(Education.GRADES_1_12, (short) 2)
          .put(Education.COLLEGE_GRADUATE, (short) 3)
          .put(Education.UNDERGRADUATE, (short) 4)
          .put(Education.MASTER, (short) 5)
          .put(Education.DOCTORATE, (short) 6)
          .build();

  private static final BiMap<Disability, Short> CLIENT_TO_STORAGE_DISABILITY =
      ImmutableBiMap.<Disability, Short>builder()
          .put(Disability.TRUE, (short) 1)
          .put(Disability.FALSE, (short) 2)
          .build();

  private static final BiMap<IndustryRole, Short> CLIENT_TO_STORAGE_INDUSTRY_ROLE =
      ImmutableBiMap.<IndustryRole, Short>builder()
          .put(IndustryRole.EARLY, (short) 1)
          .put(IndustryRole.FREE_TEXT, (short) 2)
          .put(IndustryRole.PI, (short) 3)
          .put(IndustryRole.POST_DOCTORAL, (short) 4)
          .put(IndustryRole.PRE_DOCTORAL, (short) 5)
          .build();

  private static final BiMap<NonAcademicAffiliation, Short>
      CLIENT_TO_STORAGE_NON_ACADEMIC_AFFILIATION =
          ImmutableBiMap.<NonAcademicAffiliation, Short>builder()
              .put(NonAcademicAffiliation.COMMUNITY_SCIENTIST, (short) 1)
              .put(NonAcademicAffiliation.EDUCATIONAL_INSTITUTION, (short) 2)
              .put(NonAcademicAffiliation.INDUSTRY, (short) 3)
              .put(NonAcademicAffiliation.FREE_TEXT, (short) 4)
              .build();

  private static final BiMap<EducationalRole, Short> CLIENT_TO_STORAGE_EDUCATIONAL_ROLE =
      ImmutableBiMap.<EducationalRole, Short>builder()
          .put(EducationalRole.TEACHER, (short) 1)
          .put(EducationalRole.STUDENT, (short) 2)
          .put(EducationalRole.ADMIN, (short) 3)
          .put(EducationalRole.FREE_TEXT, (short) 4)
          .build();

  private static final BiMap<AcademicRole, Short> CLIENT_TO_STORAGE_ROLE =
      ImmutableBiMap.<AcademicRole, Short>builder()
          .put(AcademicRole.UNDERGRADUATE, (short) 1)
          .put(AcademicRole.TRAINEE, (short) 2)
          .put(AcademicRole.FELLOW, (short) 3)
          .put(AcademicRole.EARLY_CAREER, (short) 4)
          .put(AcademicRole.NON_TENURE, (short) 5)
          .put(AcademicRole.MID_CAREER, (short) 6)
          .put(AcademicRole.LATE_CAREER, (short) 7)
          .put(AcademicRole.PROJECT_PERSONNEL, (short) 8)
          .build();

  public static Race raceFromStorage(Short race) {
    return CLIENT_TO_STORAGE_RACE.inverse().get(race);
  }

  public static Short raceToStorage(Race race) {
    return CLIENT_TO_STORAGE_RACE.get(race);
  }

  public static Ethnicity ethnicityFromStorage(Short ethnicity) {
    return CLIENT_TO_STORAGE_ETHNICITY.inverse().get(ethnicity);
  }

  public static Short ethnicityToStorage(Ethnicity ethnicity) {
    return CLIENT_TO_STORAGE_ETHNICITY.get(ethnicity);
  }

  public static Short genderToStorage(Gender gender) {
    return CLIENT_TO_STORAGE_GENDER.get(gender);
  }

  public static Gender genderFromStorage(Short gender) {
    return CLIENT_TO_STORAGE_GENDER.inverse().get(gender);
  }

  public static Short educationToStorage(Education education) {
    return CLIENT_TO_STORAGE_EDUCATION.get(education);
  }

  public static Education educationFromStorage(Short education) {
    return CLIENT_TO_STORAGE_EDUCATION.inverse().get(education);
  }

  public static Short disabilityToStorage(Disability disability) {
    return CLIENT_TO_STORAGE_DISABILITY.get(disability);
  }

  public static Short disabilityToStorage(Boolean disability) {
    return disability ? (short) 1 : (short) 2;
  }

  public static Disability disabilityFromStorage(Short disability) {
    return CLIENT_TO_STORAGE_DISABILITY.inverse().get(disability);
  }

  public static Short roleToStorage(AcademicRole role) {
    return CLIENT_TO_STORAGE_ROLE.get(role);
  }

  public static AcademicRole roleFromStorage(Short role) {
    return CLIENT_TO_STORAGE_ROLE.inverse().get(role);
  }

  public static Short industryRoleToStorage(IndustryRole role) {
    return CLIENT_TO_STORAGE_INDUSTRY_ROLE.get(role);
  }

  public static IndustryRole industryRoleFromStorage(Short role) {
    return CLIENT_TO_STORAGE_INDUSTRY_ROLE.inverse().get(role);
  }

  public static Short nonAcademicAffiliationToStorage(NonAcademicAffiliation role) {
    return CLIENT_TO_STORAGE_NON_ACADEMIC_AFFILIATION.get(role);
  }

  public static NonAcademicAffiliation nonAcademicAffiliationFromStorage(Short role) {
    return CLIENT_TO_STORAGE_NON_ACADEMIC_AFFILIATION.inverse().get(role);
  }

  public static Short educationRoleToStorage(EducationalRole role) {
    return CLIENT_TO_STORAGE_EDUCATIONAL_ROLE.get(role);
  }

  public static EducationalRole educationRoleFromStorage(Short role) {
    return CLIENT_TO_STORAGE_EDUCATIONAL_ROLE.inverse().get(role);
  }
}
