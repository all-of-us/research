package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Surveys;

public class CommonStorageEnums {

  private static final BiMap<Domain, Short> CLIENT_TO_STORAGE_DOMAIN =
      ImmutableBiMap.<Domain, Short>builder()
          .put(Domain.CONDITION, (short) 0)
          .put(Domain.DEATH, (short) 1)
          .put(Domain.DEVICE, (short) 2)
          .put(Domain.DRUG, (short) 3)
          .put(Domain.MEASUREMENT, (short) 4)
          .put(Domain.OBSERVATION, (short) 5)
          .put(Domain.PROCEDURE, (short) 6)
          .put(Domain.VISIT, (short) 7)
          .build();

  // A mapping from our Domain enum to OMOP domain ID values.
  private static final BiMap<Domain, String> DOMAIN_ID_MAP =
      ImmutableBiMap.<Domain, String>builder()
          .put(Domain.CONDITION, "Condition")
          .put(Domain.DEATH, "Death")
          .put(Domain.DEVICE, "Device")
          .put(Domain.DRUG, "Drug")
          .put(Domain.MEASUREMENT, "Measurement")
          .put(Domain.OBSERVATION, "Observation")
          .put(Domain.PROCEDURE, "Procedure")
          .put(Domain.VISIT, "Visit")
          .build();

  public static Domain domainFromStorage(Short domain) {
    return CLIENT_TO_STORAGE_DOMAIN.inverse().get(domain);
  }

  public static Short domainToStorage(Domain domain) {
    return CLIENT_TO_STORAGE_DOMAIN.get(domain);
  }

  public static String domainToDomainId(Domain domain) {
    return DOMAIN_ID_MAP.get(domain);
  }

  public static Domain domainIdToDomain(String domainId) {
    return DOMAIN_ID_MAP.inverse().get(domainId);
  }

  private static final BiMap<DataAccessLevel, Short> CLIENT_TO_STORAGE_DATA_ACCESS_LEVEL =
      ImmutableBiMap.<DataAccessLevel, Short>builder()
          .put(DataAccessLevel.UNREGISTERED, (short) 0)
          .put(DataAccessLevel.REGISTERED, (short) 1)
          .put(DataAccessLevel.PROTECTED, (short) 2)
          .build();

  public static DataAccessLevel dataAccessLevelFromStorage(Short level) {
    return CLIENT_TO_STORAGE_DATA_ACCESS_LEVEL.inverse().get(level);
  }

  public static Short dataAccessLevelToStorage(DataAccessLevel level) {
    return CLIENT_TO_STORAGE_DATA_ACCESS_LEVEL.get(level);
  }

  private static final BiMap<Surveys, Short> CLIENT_TO_STORAGE_SURVEY =
      ImmutableBiMap.<Surveys, Short>builder()
          .put(Surveys.THE_BASICS, (short) 0)
          .put(Surveys.LIFESTYLE, (short) 1)
          .put(Surveys.OVERALL_HEALTH, (short) 2)
          .build();

  private static final BiMap<Surveys, String> SURVEY_ID_MAP =
      ImmutableBiMap.<Surveys, String>builder()
          .put(Surveys.THE_BASICS, "THE BASICS")
          .put(Surveys.LIFESTYLE, "LIFESTYLE")
          .put(Surveys.OVERALL_HEALTH, "OVERALL HEALTH")
          .build();

  public static Surveys surveysFromStorage(Short survey) {
    return CLIENT_TO_STORAGE_SURVEY.inverse().get(survey);
  }

  public static Short surveysToStorage(Surveys survey) {
    return CLIENT_TO_STORAGE_SURVEY.get(survey);
  }

  public static String surveyToSurveyId(Surveys survey) {
    return SURVEY_ID_MAP.get(survey);
  }

  public static Surveys surveyIdToSurvey(String survey) {
    return SURVEY_ID_MAP.inverse().get(survey);
  }
}
