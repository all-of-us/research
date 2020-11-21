package org.pmiops.workbench.testconfig.fixtures;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import java.sql.Timestamp;
import java.time.Instant;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.ReportingUser;
import org.springframework.stereotype.Service;

/**
 * Test class helper methods for types associated with the user table.
 *
 * This was a static utility class, but it's handy to have all of the
 * reporting types implement a common interface.
 */
@Service
public class ReportingUserFixture implements ReportingTestFixture<DbUser, ProjectedReportingUser, ReportingUser> {
  // All constant values, mocking statements, and assertions in this file are generated. The values
  // are chosen so that errors with transposed columns can be caught.
  // Mapping Short values with valid enums can be tricky, and currently there are
  // a handful of places where we have to use use a Short in the projection interface but an Enum
  //  type in the model class. An example of such a manual fix is the following:
  // .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION.longValue())

  // This code was generated using reporting-wizard.rb at 2020-09-23T15:56:47-04:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  public static final String USER__ABOUT_YOU = "foo_0";
  public static final String USER__AREA_OF_RESEARCH = "foo_1";
  public static final Timestamp USER__COMPLIANCE_TRAINING_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-07T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_EXPIRATION_TIME =
      Timestamp.from(Instant.parse("2015-05-09T00:00:00.00Z"));
  public static final String USER__CONTACT_EMAIL = "foo_5";
  public static final Timestamp USER__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-11T00:00:00.00Z"));
  public static final String USER__CURRENT_POSITION = "foo_7";
  public static final Short USER__DATA_ACCESS_LEVEL = 1;
  public static final Timestamp USER__DATA_USE_AGREEMENT_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-14T00:00:00.00Z"));
  public static final Timestamp USER__DATA_USE_AGREEMENT_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-15T00:00:00.00Z"));
  public static final Integer USER__DATA_USE_AGREEMENT_SIGNED_VERSION = 11;
  public static final Timestamp USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-17T00:00:00.00Z"));
  public static final Boolean USER__DISABLED = false;
  public static final Timestamp USER__ERA_COMMONS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-19T00:00:00.00Z"));
  public static final Timestamp USER__ERA_COMMONS_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-20T00:00:00.00Z"));
  public static final String USER__FAMILY_NAME = "foo_16";
  public static final Timestamp USER__FIRST_REGISTRATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-22T00:00:00.00Z"));
  public static final Timestamp USER__FIRST_SIGN_IN_TIME =
      Timestamp.from(Instant.parse("2015-05-23T00:00:00.00Z"));
  public static final Short USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE = 19;
  public static final Double USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE = 20.500000;
  public static final String USER__GIVEN_NAME = "foo_21";
  public static final Timestamp USER__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-27T00:00:00.00Z"));
  public static final String USER__PROFESSIONAL_URL = "foo_23";
  public static final Timestamp USER__TWO_FACTOR_AUTH_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-29T00:00:00.00Z"));
  public static final Timestamp USER__TWO_FACTOR_AUTH_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-30T00:00:00.00Z"));
  public static final Long USER__USER_ID = 26L;
  public static final String USER__USERNAME = "foo_27";
  // Address fields - manually renamed
  public static final String USER__CITY = "foo_0";
  public static final String USER__COUNTRY = "foo_1";
  public static final String USER__STATE = "foo_2";
  public static final String USER__STREET_ADDRESS_1 = "foo_3";
  public static final String USER__STREET_ADDRESS_2 = "foo_4";
  public static final String USER__ZIP_CODE = "foo_5";
  public static final Long USER__INSTITUTION_ID = 0L;
  public static final InstitutionalRole USER__INSTITUTIONAL_ROLE_ENUM =
      InstitutionalRole.UNDERGRADUATE;
  public static final String USER__INSTITUTIONAL_ROLE_OTHER_TEXT = "foo_2";

  @Override
  public void assertDTOFieldsMatchConstants(ReportingUser user) {
    assertThat(user.getAboutYou()).isEqualTo(USER__ABOUT_YOU);
    assertThat(user.getAreaOfResearch()).isEqualTo(USER__AREA_OF_RESEARCH);
    assertTimeApprox(user.getComplianceTrainingBypassTime(), USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    assertTimeApprox(
        user.getComplianceTrainingCompletionTime(), USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    assertTimeApprox(
        user.getComplianceTrainingExpirationTime(), USER__COMPLIANCE_TRAINING_EXPIRATION_TIME);
    assertThat(user.getContactEmail()).isEqualTo(USER__CONTACT_EMAIL);
    assertTimeApprox(user.getCreationTime(), USER__CREATION_TIME);
    assertThat(user.getCurrentPosition()).isEqualTo(USER__CURRENT_POSITION);
    assertThat(user.getDataAccessLevel())
        .isEqualTo(
            DbStorageEnums.dataAccessLevelFromStorage(
                USER__DATA_ACCESS_LEVEL)); // manual adjustment
    assertTimeApprox(user.getDataUseAgreementBypassTime(), USER__DATA_USE_AGREEMENT_BYPASS_TIME);
    assertTimeApprox(
        user.getDataUseAgreementCompletionTime(), USER__DATA_USE_AGREEMENT_COMPLETION_TIME);
    assertThat(user.getDataUseAgreementSignedVersion())
        .isEqualTo(USER__DATA_USE_AGREEMENT_SIGNED_VERSION);
    assertTimeApprox(
        user.getDemographicSurveyCompletionTime(), USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    assertThat(user.getDisabled()).isEqualTo(USER__DISABLED);
    assertTimeApprox(user.getEraCommonsBypassTime(), USER__ERA_COMMONS_BYPASS_TIME);
    assertTimeApprox(user.getEraCommonsCompletionTime(), USER__ERA_COMMONS_COMPLETION_TIME);
    assertThat(user.getFamilyName()).isEqualTo(USER__FAMILY_NAME);
    assertTimeApprox(
        user.getFirstRegistrationCompletionTime(), USER__FIRST_REGISTRATION_COMPLETION_TIME);
    assertTimeApprox(user.getFirstSignInTime(), USER__FIRST_SIGN_IN_TIME);
    assertThat(user.getFreeTierCreditsLimitDaysOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    assertThat(user.getFreeTierCreditsLimitDollarsOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    assertThat(user.getGivenName()).isEqualTo(USER__GIVEN_NAME);
    assertTimeApprox(user.getLastModifiedTime(), USER__LAST_MODIFIED_TIME);
    assertThat(user.getProfessionalUrl()).isEqualTo(USER__PROFESSIONAL_URL);
    assertTimeApprox(user.getTwoFactorAuthBypassTime(), USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    assertTimeApprox(user.getTwoFactorAuthCompletionTime(), USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    assertThat(user.getUserId()).isEqualTo(USER__USER_ID);
    assertThat(user.getUsername()).isEqualTo(USER__USERNAME);
    assertThat(user.getInstitutionId()).isEqualTo(USER__INSTITUTION_ID);
    assertThat(user.getInstitutionalRoleEnum()).isEqualTo(USER__INSTITUTIONAL_ROLE_ENUM);
    assertThat(user.getInstitutionalRoleOtherText()).isEqualTo(USER__INSTITUTIONAL_ROLE_OTHER_TEXT);
  }

  @Override
  public void assertProjectionFieldsMatchConstants(ProjectedReportingUser user) {
    assertThat(user.getAboutYou()).isEqualTo(USER__ABOUT_YOU);
    assertThat(user.getAreaOfResearch()).isEqualTo(USER__AREA_OF_RESEARCH);
    assertTimeApprox(user.getComplianceTrainingBypassTime(), USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    assertTimeApprox(
        user.getComplianceTrainingCompletionTime(), USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    assertTimeApprox(
        user.getComplianceTrainingExpirationTime(), USER__COMPLIANCE_TRAINING_EXPIRATION_TIME);
    assertThat(user.getContactEmail()).isEqualTo(USER__CONTACT_EMAIL);
    assertTimeApprox(user.getCreationTime(), USER__CREATION_TIME);
    assertThat(user.getCurrentPosition()).isEqualTo(USER__CURRENT_POSITION);
    assertThat(user.getDataAccessLevel())
        .isEqualTo(
            DbStorageEnums.dataAccessLevelFromStorage(
                USER__DATA_ACCESS_LEVEL)); // manual adjustment
    assertTimeApprox(user.getDataUseAgreementBypassTime(), USER__DATA_USE_AGREEMENT_BYPASS_TIME);
    assertTimeApprox(
        user.getDataUseAgreementCompletionTime(), USER__DATA_USE_AGREEMENT_COMPLETION_TIME);
    assertThat(user.getDataUseAgreementSignedVersion())
        .isEqualTo(USER__DATA_USE_AGREEMENT_SIGNED_VERSION);
    assertTimeApprox(
        user.getDemographicSurveyCompletionTime(), USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    assertThat(user.getDisabled()).isEqualTo(USER__DISABLED);
    assertTimeApprox(user.getEraCommonsBypassTime(), USER__ERA_COMMONS_BYPASS_TIME);
    assertTimeApprox(user.getEraCommonsCompletionTime(), USER__ERA_COMMONS_COMPLETION_TIME);
    assertThat(user.getFamilyName()).isEqualTo(USER__FAMILY_NAME);
    assertTimeApprox(
        user.getFirstRegistrationCompletionTime(), USER__FIRST_REGISTRATION_COMPLETION_TIME);
    assertTimeApprox(user.getFirstSignInTime(), USER__FIRST_SIGN_IN_TIME);
    assertThat(user.getFreeTierCreditsLimitDaysOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    assertThat(user.getFreeTierCreditsLimitDollarsOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    assertThat(user.getGivenName()).isEqualTo(USER__GIVEN_NAME);
    assertTimeApprox(user.getLastModifiedTime(), USER__LAST_MODIFIED_TIME);
    assertThat(user.getProfessionalUrl()).isEqualTo(USER__PROFESSIONAL_URL);
    assertTimeApprox(user.getTwoFactorAuthBypassTime(), USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    assertTimeApprox(user.getTwoFactorAuthCompletionTime(), USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    assertThat(user.getUserId()).isEqualTo(USER__USER_ID);
    assertThat(user.getUsername()).isEqualTo(USER__USERNAME);
    assertThat(user.getInstitutionId()).isEqualTo(USER__INSTITUTION_ID);
    assertThat(user.getInstitutionalRoleEnum()).isEqualTo(USER__INSTITUTIONAL_ROLE_ENUM);
    assertThat(user.getInstitutionalRoleOtherText()).isEqualTo(USER__INSTITUTIONAL_ROLE_OTHER_TEXT);
  }

  @Override
  public ProjectedReportingUser mockProjection() {
    // This code was generated using reporting-wizard.rb at 2020-09-23T15:56:47-04:00.
    // Manual modification should be avoided if possible as this is a one-time generation
    // and does not run on every build and updates must be merged manually for now.
    final ProjectedReportingUser mockUser = mock(ProjectedReportingUser.class);
    doReturn(USER__ABOUT_YOU).when(mockUser).getAboutYou();
    doReturn(USER__AREA_OF_RESEARCH).when(mockUser).getAreaOfResearch();
    doReturn(USER__COMPLIANCE_TRAINING_BYPASS_TIME)
        .when(mockUser)
        .getComplianceTrainingBypassTime();
    doReturn(USER__COMPLIANCE_TRAINING_COMPLETION_TIME)
        .when(mockUser)
        .getComplianceTrainingCompletionTime();
    doReturn(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME)
        .when(mockUser)
        .getComplianceTrainingExpirationTime();
    doReturn(USER__CONTACT_EMAIL).when(mockUser).getContactEmail();
    doReturn(USER__CREATION_TIME).when(mockUser).getCreationTime();
    doReturn(USER__CURRENT_POSITION).when(mockUser).getCurrentPosition();
    doReturn(USER__DATA_ACCESS_LEVEL).when(mockUser).getDataAccessLevel();
    doReturn(USER__DATA_USE_AGREEMENT_BYPASS_TIME).when(mockUser).getDataUseAgreementBypassTime();
    doReturn(USER__DATA_USE_AGREEMENT_COMPLETION_TIME)
        .when(mockUser)
        .getDataUseAgreementCompletionTime();
    doReturn(USER__DATA_USE_AGREEMENT_SIGNED_VERSION)
        .when(mockUser)
        .getDataUseAgreementSignedVersion();
    doReturn(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME)
        .when(mockUser)
        .getDemographicSurveyCompletionTime();
    doReturn(USER__DISABLED).when(mockUser).getDisabled();
    doReturn(USER__ERA_COMMONS_BYPASS_TIME).when(mockUser).getEraCommonsBypassTime();
    doReturn(USER__ERA_COMMONS_COMPLETION_TIME).when(mockUser).getEraCommonsCompletionTime();
    doReturn(USER__FAMILY_NAME).when(mockUser).getFamilyName();
    doReturn(USER__FIRST_REGISTRATION_COMPLETION_TIME)
        .when(mockUser)
        .getFirstRegistrationCompletionTime();
    doReturn(USER__FIRST_SIGN_IN_TIME).when(mockUser).getFirstSignInTime();
    doReturn(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE)
        .when(mockUser)
        .getFreeTierCreditsLimitDaysOverride();
    doReturn(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .when(mockUser)
        .getFreeTierCreditsLimitDollarsOverride();
    doReturn(USER__GIVEN_NAME).when(mockUser).getGivenName();
    doReturn(USER__LAST_MODIFIED_TIME).when(mockUser).getLastModifiedTime();
    doReturn(USER__PROFESSIONAL_URL).when(mockUser).getProfessionalUrl();
    doReturn(USER__TWO_FACTOR_AUTH_BYPASS_TIME).when(mockUser).getTwoFactorAuthBypassTime();
    doReturn(USER__TWO_FACTOR_AUTH_COMPLETION_TIME).when(mockUser).getTwoFactorAuthCompletionTime();
    doReturn(USER__USER_ID).when(mockUser).getUserId();
    doReturn(USER__USERNAME).when(mockUser).getUsername();
    // address fields
    doReturn(USER__CITY).when(mockUser).getCity();
    doReturn(USER__COUNTRY).when(mockUser).getCountry();
    doReturn(USER__STATE).when(mockUser).getState();
    doReturn(USER__STREET_ADDRESS_1).when(mockUser).getStreetAddress1();
    doReturn(USER__STREET_ADDRESS_2).when(mockUser).getStreetAddress2();
    doReturn(USER__ZIP_CODE).when(mockUser).getZipCode();
    // affiliation fields
    doReturn(USER__INSTITUTION_ID).when(mockUser).getInstitutionId();
    doReturn(USER__INSTITUTIONAL_ROLE_ENUM).when(mockUser).getInstitutionalRoleEnum();
    doReturn(USER__INSTITUTIONAL_ROLE_OTHER_TEXT).when(mockUser).getInstitutionalRoleOtherText();
    return mockUser;
  }

  @Override
  public DbUser createEntity() {
    final DbUser user = new DbUser();
    user.setAboutYou(USER__ABOUT_YOU);
    user.setAreaOfResearch(USER__AREA_OF_RESEARCH);
    user.setComplianceTrainingBypassTime(USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    user.setComplianceTrainingCompletionTime(USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    user.setComplianceTrainingExpirationTime(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME);
    user.setContactEmail(USER__CONTACT_EMAIL);
    user.setCreationTime(USER__CREATION_TIME);
    user.setCurrentPosition(USER__CURRENT_POSITION);
    user.setDataAccessLevel(USER__DATA_ACCESS_LEVEL);
    user.setDataUseAgreementBypassTime(USER__DATA_USE_AGREEMENT_BYPASS_TIME);
    user.setDataUseAgreementCompletionTime(USER__DATA_USE_AGREEMENT_COMPLETION_TIME);
    user.setDataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION);
    user.setDemographicSurveyCompletionTime(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    user.setDisabled(USER__DISABLED);
    user.setEraCommonsBypassTime(USER__ERA_COMMONS_BYPASS_TIME);
    user.setEraCommonsCompletionTime(USER__ERA_COMMONS_COMPLETION_TIME);
    user.setFamilyName(USER__FAMILY_NAME);
    user.setFirstRegistrationCompletionTime(USER__FIRST_REGISTRATION_COMPLETION_TIME);
    user.setFirstSignInTime(USER__FIRST_SIGN_IN_TIME);
    user.setFreeTierCreditsLimitDaysOverride(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    user.setFreeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    user.setGivenName(USER__GIVEN_NAME);
    user.setLastModifiedTime(USER__LAST_MODIFIED_TIME);
    user.setProfessionalUrl(USER__PROFESSIONAL_URL);
    user.setTwoFactorAuthBypassTime(USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    user.setTwoFactorAuthCompletionTime(USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    //    user.setUserId(USER__USER_ID);
    user.setUsername(USER__USERNAME);
    return user;
  }

  @Override
  public ReportingUser createDto() {
    return new ReportingUser()
        .aboutYou(USER__ABOUT_YOU)
        .areaOfResearch(USER__AREA_OF_RESEARCH)
        .complianceTrainingBypassTime(offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_BYPASS_TIME))
        .complianceTrainingCompletionTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_COMPLETION_TIME))
        .complianceTrainingExpirationTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME))
        .contactEmail(USER__CONTACT_EMAIL)
        .creationTime(offsetDateTimeUtc(USER__CREATION_TIME))
        .currentPosition(USER__CURRENT_POSITION)
        .dataAccessLevel(DbStorageEnums.dataAccessLevelFromStorage(USER__DATA_ACCESS_LEVEL))
        .dataUseAgreementBypassTime(offsetDateTimeUtc(USER__DATA_USE_AGREEMENT_BYPASS_TIME))
        .dataUseAgreementCompletionTime(offsetDateTimeUtc(USER__DATA_USE_AGREEMENT_COMPLETION_TIME))
        .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION)
        .demographicSurveyCompletionTime(
            offsetDateTimeUtc(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME))
        .disabled(USER__DISABLED)
        .eraCommonsBypassTime(offsetDateTimeUtc(USER__ERA_COMMONS_BYPASS_TIME))
        .eraCommonsCompletionTime(offsetDateTimeUtc(USER__ERA_COMMONS_COMPLETION_TIME))
        .familyName(USER__FAMILY_NAME)
        .firstRegistrationCompletionTime(
            offsetDateTimeUtc(USER__FIRST_REGISTRATION_COMPLETION_TIME))
        .firstSignInTime(offsetDateTimeUtc(USER__FIRST_SIGN_IN_TIME))
        .freeTierCreditsLimitDaysOverride(
            USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE.intValue()) // manual adjustment
        .freeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .givenName(USER__GIVEN_NAME)
        .lastModifiedTime(offsetDateTimeUtc(USER__LAST_MODIFIED_TIME))
        .professionalUrl(USER__PROFESSIONAL_URL)
        .twoFactorAuthBypassTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_BYPASS_TIME))
        .twoFactorAuthCompletionTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_COMPLETION_TIME))
        .userId(USER__USER_ID)
        .username(USER__USERNAME)
        .city(USER__CITY)
        .country(USER__COUNTRY)
        .state(USER__STATE)
        .streetAddress1(USER__STREET_ADDRESS_1)
        .streetAddress2(USER__STREET_ADDRESS_2)
        .zipCode(USER__ZIP_CODE)
        .institutionId(USER__INSTITUTION_ID)
        .institutionalRoleEnum(USER__INSTITUTIONAL_ROLE_ENUM)
        .institutionalRoleOtherText(USER__INSTITUTIONAL_ROLE_OTHER_TEXT);
  }

  /**
   * DbAddress is a bit special, because it feeds into the user table
   * on the reporting side.
   * @return
   */
  public DbAddress createDbAddress() {
    final DbAddress address = new DbAddress();
    address.setCity(USER__CITY);
    address.setCountry(USER__COUNTRY);
    address.setState(USER__STATE);
    address.setStreetAddress1(USER__STREET_ADDRESS_1);
    address.setStreetAddress2(USER__STREET_ADDRESS_2);
    address.setZipCode(USER__ZIP_CODE);
    return address;
  }
}
