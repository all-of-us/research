package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.mail.MessagingException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.pmiops.workbench.actionaudit.ActionAuditQueryServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.captcha.ApiException;
import org.pmiops.workbench.captcha.CaptchaVerificationService;
import org.pmiops.workbench.compliance.ComplianceServiceImpl;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserDataUseAgreementDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserDataUseAgreement;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudJWTWrapper;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionMapperImpl;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.institution.deprecated.InstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.profile.AddressMapperImpl;
import org.pmiops.workbench.profile.DemographicSurveyMapperImpl;
import org.pmiops.workbench.profile.PageVisitMapperImpl;
import org.pmiops.workbench.profile.ProfileMapperImpl;
import org.pmiops.workbench.profile.ProfileService;
import org.pmiops.workbench.shibboleth.ShibbolethService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.mappers.AuditLogEntryMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProfileControllerTest extends BaseControllerTest {

  private static final FakeClock fakeClock = new FakeClock(Instant.parse("1995-06-05T00:00:00Z"));
  private static final long NONCE_LONG = 12345;
  private static final String CAPTCHA_TOKEN = "captchaToken";
  private static final String CITY = "Exampletown";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String COUNTRY = "Example";
  private static final String CURRENT_POSITION = "Tester";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String GIVEN_NAME = "Bob";
  private static final String GSUITE_DOMAIN = "researchallofus.org";
  private static final String INVITATION_KEY = "secretpassword";
  private static final String NONCE = Long.toString(NONCE_LONG);
  private static final String ORGANIZATION = "Test";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String RESEARCH_PURPOSE = "To test things";
  private static final String STATE = "EX";
  private static final String STREET_ADDRESS = "1 Example Lane";
  private static final String USER_PREFIX = "bob";
  private static final String WRONG_CAPTCHA_TOKEN = "WrongCaptchaToken";
  private static final String ZIP_CODE = "12345";
  private static final Timestamp TIMESTAMP = new Timestamp(fakeClock.millis());
  private static final double TIME_TOLERANCE_MILLIS = 100.0;

  @MockBean private CaptchaVerificationService mockCaptchaVerificationService;
  @MockBean private CloudStorageService mockCloudStorageService;
  @MockBean private DirectoryService mockDirectoryService;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private MailService mockMailService;
  @MockBean private ProfileAuditor mockProfileAuditor;
  @MockBean private ShibbolethService shibbolethService;
  @MockBean private UserServiceAuditor mockUserServiceAuditor;

  @Autowired private InstitutionService institutionService;
  @Autowired private ProfileController profileController;
  @Autowired private ProfileService profileService;
  @Autowired private UserDao userDao;
  @Autowired private UserDataUseAgreementDao userDataUseAgreementDao;
  @Autowired private UserService userService;
  @Autowired private UserTermsOfServiceDao userTermsOfServiceDao;

  private CreateAccountRequest createAccountRequest;
  private InvitationVerificationRequest invitationVerificationRequest;
  private com.google.api.services.directory.model.User googleUser;
  private static DbUser dbUser;

  private int DUA_VERSION;

  @Rule public final ExpectedException exception = ExpectedException.none();

  @TestConfiguration
  @Import({
    ActionAuditQueryServiceImpl.class,
    AddressMapperImpl.class,
    AuditLogEntryMapperImpl.class,
    CaptchaVerificationService.class,
    CommonConfig.class,
    CommonMappers.class,
    ComplianceServiceImpl.class,
    DemographicSurveyMapperImpl.class,
    FreeTierBillingService.class,
    InstitutionMapperImpl.class,
    InstitutionServiceImpl.class,
    InstitutionalAffiliationMapperImpl.class,
    PageVisitMapperImpl.class,
    ProfileController.class,
    ProfileMapperImpl.class,
    ProfileService.class,
    UserServiceImpl.class,
    UserServiceTestConfiguration.class,
    VerifiedInstitutionalAffiliationMapperImpl.class,
  })
  @MockBean({BigQueryService.class})
  static class Configuration {
    @Bean
    @Primary
    Clock clock() {
      return fakeClock;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser dbUser() {
      return dbUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    UserAuthentication userAuthentication() {
      return new UserAuthentication(dbUser, null, null, UserType.RESEARCHER);
    }

    @Bean
    @Primary
    Random getRandom() {
      return new FakeLongRandom(NONCE_LONG);
    }
  }

  @Before
  @Override
  public void setUp() throws IOException {
    super.setUp();

    config.googleDirectoryService.gSuiteDomain = GSUITE_DOMAIN;

    Profile profile = new Profile();
    profile.setContactEmail(CONTACT_EMAIL);
    profile.setFamilyName(FAMILY_NAME);
    profile.setGivenName(GIVEN_NAME);
    profile.setUsername(USER_PREFIX);
    profile.setCurrentPosition(CURRENT_POSITION);
    profile.setOrganization(ORGANIZATION);
    profile.setAreaOfResearch(RESEARCH_PURPOSE);
    profile.setAddress(
        new Address()
            .streetAddress1(STREET_ADDRESS)
            .city(CITY)
            .state(STATE)
            .country(COUNTRY)
            .zipCode(ZIP_CODE));

    // TODO: this needs to be set in createAccountAndDbUserWithAffiliation() instead of here.  Why?
    // profile.setEmailVerificationStatus(EmailVerificationStatus.SUBSCRIBED);

    createAccountRequest = new CreateAccountRequest();
    createAccountRequest.setProfile(profile);
    createAccountRequest.setInvitationKey(INVITATION_KEY);
    createAccountRequest.setCaptchaVerificationToken(CAPTCHA_TOKEN);

    invitationVerificationRequest = new InvitationVerificationRequest();
    invitationVerificationRequest.setInvitationKey(INVITATION_KEY);

    googleUser = new com.google.api.services.directory.model.User();
    googleUser.setPrimaryEmail(PRIMARY_EMAIL);
    googleUser.setChangePasswordAtNextLogin(true);
    googleUser.setPassword("testPassword");
    googleUser.setIsEnrolledIn2Sv(true);

    DUA_VERSION = userService.getCurrentDuccVersion();

    when(mockDirectoryService.getUser(PRIMARY_EMAIL)).thenReturn(googleUser);
    when(mockCloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(mockDirectoryService.createUser(
            GIVEN_NAME, FAMILY_NAME, USER_PREFIX + "@" + GSUITE_DOMAIN, CONTACT_EMAIL))
        .thenReturn(googleUser);
    when(mockCloudStorageService.getCaptchaServerKey()).thenReturn("Server_Key");

    try {
      doNothing().when(mockMailService).sendBetaAccessRequestEmail(Mockito.any());
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    try {
      when(mockCaptchaVerificationService.verifyCaptcha(CAPTCHA_TOKEN)).thenReturn(true);
      when(mockCaptchaVerificationService.verifyCaptcha(WRONG_CAPTCHA_TOKEN)).thenReturn(false);
    } catch (ApiException e) {
      e.printStackTrace();
    }
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invitationKeyMismatch() {
    createAccountAndDbUserWithAffiliation();

    config.access.requireInvitationKey = true;
    when(mockCloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invalidCaptchaToken() {
    createAccountAndDbUserWithAffiliation();
    createAccountRequest.setCaptchaVerificationToken(WRONG_CAPTCHA_TOKEN);
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testCreateAccount_noRequireInvitationKey() {
    createAccountAndDbUserWithAffiliation();

    // When invitation key verification is turned off, even a bad invitation key should
    // allow a user to be created.
    config.access.requireInvitationKey = false;
    when(mockCloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testInvitationKeyVerification_invitationKeyMismatch() {
    invitationVerificationRequest.setInvitationKey("wrong key");
    profileController.invitationKeyVerification(invitationVerificationRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_MismatchEmailAddress() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailAddresses(Collections.singletonList(CONTACT_EMAIL))
            .emailDomains(Collections.singletonList("example.com"))
            .duaTypeEnum(DuaType.RESTRICTED);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail("bob@broad.com");
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_MismatchEmailDomain() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailAddresses(Collections.singletonList(CONTACT_EMAIL))
            .emailDomains(Collections.singletonList("example.com"))
            .duaTypeEnum(DuaType.MASTER);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail("bob@broad.com");
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_MismatchEmailDomainNullDUA() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailAddresses(Collections.singletonList(CONTACT_EMAIL))
            .emailDomains(Collections.singletonList("example.com"));
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail("bob@broadInstitute.com");
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_Success_RESTRICTEDDUA() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(Collections.singletonList(CONTACT_EMAIL))
            .emailDomains(Collections.singletonList("example.com"))
            .duaTypeEnum(DuaType.RESTRICTED)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail(CONTACT_EMAIL);
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_Success_MasterDUA() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(Collections.singletonList("institution@example.com"))
            .emailDomains(Collections.singletonList("example.com"))
            .duaTypeEnum(DuaType.MASTER)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail("bob@example.com");
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_Success_NULLDUA() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailDomains(Collections.singletonList("example.com"))
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail("bob@example.com");
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_success() {
    createAccountAndDbUserWithAffiliation();
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
    final DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(dbUser).isNotNull();
    assertThat(dbUser.getDataAccessLevelEnum()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testCreateAccount_withTosVersion() {
    createAccountRequest.setTermsOfServiceVersion(1);
    createAccountAndDbUserWithAffiliation();

    final DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    final List<DbUserTermsOfService> tosRows = Lists.newArrayList(userTermsOfServiceDao.findAll());
    assertThat(tosRows.size()).isEqualTo(1);
    assertThat(tosRows.get(0).getTosVersion()).isEqualTo(1);
    assertThat(tosRows.get(0).getUserId()).isEqualTo(dbUser.getUserId());
    assertThat(tosRows.get(0).getAgreementTime()).isNotNull();
    Profile profile = profileService.getProfile(dbUser);
    assertThat(profile.getLatestTermsOfServiceVersion()).isEqualTo(1);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_withBadTosVersion() {
    createAccountRequest.setTermsOfServiceVersion(999);
    createAccountAndDbUserWithAffiliation();
  }

  @Test
  public void testCreateAccount_invalidUser() {
    when(mockCloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    CreateAccountRequest accountRequest = new CreateAccountRequest();
    accountRequest.setInvitationKey(INVITATION_KEY);
    accountRequest.setCaptchaVerificationToken(CAPTCHA_TOKEN);
    createAccountRequest.getProfile().setUsername("12");
    accountRequest.setProfile(createAccountRequest.getProfile());
    exception.expect(BadRequestException.class);
    exception.expectMessage(
        "Username should be at least 3 characters and not more than 64 characters");
    createAccountAndDbUserWithAffiliation();
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
  }

  @Test
  public void testSubmitDataUseAgreement_success() {
    createAccountAndDbUserWithAffiliation();
    String duaInitials = "NIH";
    assertThat(profileController.submitDataUseAgreement(DUA_VERSION, duaInitials).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    List<DbUserDataUseAgreement> dbUserDataUseAgreementList =
        userDataUseAgreementDao.findByUserIdOrderByCompletionTimeDesc(dbUser.getUserId());
    assertThat(dbUserDataUseAgreementList.size()).isEqualTo(1);
    DbUserDataUseAgreement dbUserDataUseAgreement = dbUserDataUseAgreementList.get(0);
    assertThat(dbUserDataUseAgreement.getUserFamilyName()).isEqualTo(dbUser.getFamilyName());
    assertThat(dbUserDataUseAgreement.getUserGivenName()).isEqualTo(dbUser.getGivenName());
    assertThat(dbUserDataUseAgreement.getUserInitials()).isEqualTo(duaInitials);
    assertThat(dbUserDataUseAgreement.getDataUseAgreementSignedVersion()).isEqualTo(DUA_VERSION);
  }

  @Test(expected = BadRequestException.class)
  public void testSubmitDataUseAgreement_wrongVersion() {
    createAccountAndDbUserWithAffiliation();
    String duaInitials = "NIH";
    profileController.submitDataUseAgreement(DUA_VERSION - 1, duaInitials);
  }

  @Test
  public void test_outdatedDataUseAgreement() {
    // force version number to 2 instead of 3
    config.featureFlags.enableV3DataUserCodeOfConduct = false;
    final int previousDuaVersion = DUA_VERSION - 1;

    final long userId = createAccountAndDbUserWithAffiliation().getUserId();

    // bypass the other access requirements
    final DbUser dbUser = userDao.findUserByUserId(userId);
    dbUser.setBetaAccessBypassTime(TIMESTAMP);
    dbUser.setComplianceTrainingBypassTime(TIMESTAMP);
    dbUser.setEraCommonsBypassTime(TIMESTAMP);
    dbUser.setTwoFactorAuthBypassTime(TIMESTAMP);
    userDao.save(dbUser);

    // sign the older version

    String duaInitials = "NIH";
    assertThat(
            profileController
                .submitDataUseAgreement(previousDuaVersion, duaInitials)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.REGISTERED);

    // update and enforce the required version

    config.featureFlags.enableV3DataUserCodeOfConduct = true;

    // a bit of a hack here: use this to sync the registration status
    // see also https://precisionmedicineinitiative.atlassian.net/browse/RW-2352
    profileController.syncTwoFactorAuthStatus();

    profile = profileController.getMe().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testMe_success() {
    createAccountAndDbUserWithAffiliation();

    Profile profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        false);
    verify(mockFireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(mockProfileAuditor).fireLoginAction(dbUser);
  }

  @Test
  public void testMe_userBeforeNotLoggedInSuccess() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        false);
    verify(mockFireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    // An additional call to getMe() should have no effect.
    fakeClock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        false);
  }

  @Deprecated // to be removed in RW-4362
  @Test
  public void testMe_institutionalAffiliationsAlphabetical() {
    createAccountAndDbUserWithAffiliation();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("test");
    first.setInstitution("Institution");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("zeta");
    second.setInstitution("Zeta");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);

    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(2);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
    assertThat(result.getInstitutionalAffiliations().get(1)).isEqualTo(second);
  }

  @Deprecated // to be removed in RW-4362
  @Test
  public void testMe_institutionalAffiliationsNotAlphabetical() {
    createAccountAndDbUserWithAffiliation();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("zeta");
    first.setInstitution("Zeta");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("test");
    second.setInstitution("Institution");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);

    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(2);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
    assertThat(result.getInstitutionalAffiliations().get(1)).isEqualTo(second);
  }

  @Deprecated // to be removed in RW-4362
  @Test
  public void testMe_removeSingleInstitutionalAffiliation() {
    createAccountAndDbUserWithAffiliation();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("test");
    first.setInstitution("Institution");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("zeta");
    second.setInstitution("Zeta");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    affiliations = new ArrayList<>();
    affiliations.add(first);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(1);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
  }

  @Deprecated // to be removed in RW-4362
  @Test
  public void testMe_removeAllInstitutionalAffiliations() {
    createAccountAndDbUserWithAffiliation();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("test");
    first.setInstitution("Institution");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("zeta");
    second.setInstitution("Zeta");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    affiliations.clear();
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(0);
  }

  @Test(expected = BadRequestException.class)
  public void testMe_verifiedInstitutionalAffiliation_missing() {
    final VerifiedInstitutionalAffiliation missing = null;
    createAccountAndDbUserWithAffiliation(missing);
  }

  @Test(expected = NotFoundException.class)
  public void testMe_verifiedInstitutionalAffiliation_invalidInstitution() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(Collections.singletonList(CONTACT_EMAIL))
            .duaTypeEnum(DuaType.RESTRICTED)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    // "Broad" is the only institution
    final String invalidInst = "Not the Broad";

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(invalidInst)
            .institutionalRoleEnum(InstitutionalRole.STUDENT);

    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test(expected = BadRequestException.class)
  public void testMe_verifiedInstitutionalAffiliation_invalidEmail() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailAddresses(Collections.emptyList())
            .duaTypeEnum(DuaType.RESTRICTED);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(broad.getShortName())
            .institutionalRoleEnum(InstitutionalRole.ADMIN);
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test(expected = BadRequestException.class)
  public void create_verifiedInstitutionalAffiliation_invalidDomain() {
    ArrayList<String> emailDomains = new ArrayList<>();
    emailDomains.add("@broadinstitute.org");
    emailDomains.add("@broad.org");

    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
            .emailDomains(emailDomains)
            .duaTypeEnum(DuaType.MASTER);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(broad.getShortName())
            .institutionalRoleEnum(InstitutionalRole.ADMIN);

    // CONTACT_EMAIL has the domain @example.com
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test(expected = NotFoundException.class)
  public void updateVerifiedInstitutionalAffiliation_noSuchInstitution() {
    // ProfileController.updateVerifiedInstitutionalAffiliation() is gated on ACCESS_CONTROL_ADMIN
    // Authority which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation original = createVerifiedInstitutionalAffiliation();
    createAccountAndDbUserWithAffiliation(original, grantAdminAuthority);

    final VerifiedInstitutionalAffiliation newAffil =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("NotTheBroad")
            .institutionDisplayName("The Narrow Institute?")
            .institutionalRoleEnum(InstitutionalRole.PRE_DOCTORAL);

    profileController.updateVerifiedInstitutionalAffiliation(dbUser.getUserId(), newAffil);
  }

  @Test
  public void updateVerifiedInstitutionalAffiliation_update() {
    // ProfileController.updateVerifiedInstitutionalAffiliation() is gated on ACCESS_CONTROL_ADMIN
    // Authority which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createVerifiedInstitutionalAffiliation();
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation, grantAdminAuthority);

    // original is PROJECT_PERSONNEL
    verifiedInstitutionalAffiliation.setInstitutionalRoleEnum(InstitutionalRole.ADMIN);
    profileController.updateVerifiedInstitutionalAffiliation(
        dbUser.getUserId(), verifiedInstitutionalAffiliation);

    Profile updatedProfile = profileService.getProfile(dbUser);
    assertThat(updatedProfile.getVerifiedInstitutionalAffiliation())
        .isEqualTo(verifiedInstitutionalAffiliation);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_removeVerifiedInstitutionalAffiliationForbidden() {
    final VerifiedInstitutionalAffiliation original = createVerifiedInstitutionalAffiliation();
    createAccountAndDbUserWithAffiliation(original);

    final Profile profile = profileController.getMe().getBody();
    profile.setVerifiedInstitutionalAffiliation(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateVerifiedInstitutionalAffiliation_removeForbidden() {
    // ProfileController.updateVerifiedInstitutionalAffiliation() is gated on ACCESS_CONTROL_ADMIN
    // Authority which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation original = createVerifiedInstitutionalAffiliation();
    createAccountAndDbUserWithAffiliation(original, grantAdminAuthority);

    profileController.updateVerifiedInstitutionalAffiliation(dbUser.getUserId(), null);
  }

  @Test
  public void updateContactEmail_forbidden() {
    createAccountAndDbUserWithAffiliation();
    dbUser.setFirstSignInTime(TIMESTAMP);
    String originalEmail = dbUser.getContactEmail();

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("newContactEmail@whatever.com")
                .username(dbUser.getUsername())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(dbUser.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_badRequest() {
    createAccountAndDbUserWithAffiliation();
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    dbUser.setFirstSignInTime(null);
    String originalEmail = dbUser.getContactEmail();

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("bad email address *(SD&(*D&F&*(DS ")
                .username(dbUser.getUsername())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(dbUser.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_OK() {
    createAccountAndDbUserWithAffiliation();
    dbUser.setFirstSignInTime(null);
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("newContactEmail@whatever.com")
                .username(dbUser.getUsername())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(dbUser.getContactEmail()).isEqualTo("newContactEmail@whatever.com");
  }

  @Test
  public void updateName_alsoUpdatesDua() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setGivenName("OldGivenName");
    profile.setFamilyName("OldFamilyName");
    profileController.updateProfile(profile);
    profileController.submitDataUseAgreement(DUA_VERSION, "O.O.");
    profile.setGivenName("NewGivenName");
    profile.setFamilyName("NewFamilyName");
    profileController.updateProfile(profile);
    List<DbUserDataUseAgreement> duas =
        userDataUseAgreementDao.findByUserIdOrderByCompletionTimeDesc(profile.getUserId());
    assertThat(duas.get(0).isUserNameOutOfDate()).isTrue();
  }

  @Test(expected = BadRequestException.class)
  public void updateGivenName_badRequest() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    String newName =
        "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson";
    profile.setGivenName(newName);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullAddress() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setAddress(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullCountry() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.getAddress().country(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullState() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.getAddress().state(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullZipCode() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.getAddress().zipCode(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_emptyReasonForResearch() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setAreaOfResearch("");
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_UpdateUserName() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setUsername("newUserName@fakeDomain.com");
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_UpdateContactEmail() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setContactEmail("newContact@fakeDomain.com");
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateFamilyName_badRequest() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    String newName =
        "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson";
    profile.setFamilyName(newName);
    profileController.updateProfile(profile);
  }

  @Test
  public void resendWelcomeEmail_messagingException() throws MessagingException {
    createAccountAndDbUserWithAffiliation();
    dbUser.setFirstSignInTime(null);
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doThrow(new MessagingException("exception"))
        .when(mockMailService)
        .sendWelcomeEmail(any(), any(), any());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(dbUser.getUsername()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    // called twice, once during account creation, once on resend
    verify(mockMailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(mockDirectoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void resendWelcomeEmail_OK() throws MessagingException {
    createAccountAndDbUserWithAffiliation();
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doNothing().when(mockMailService).sendWelcomeEmail(any(), any(), any());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(dbUser.getUsername()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    // called twice, once during account creation, once on resend
    verify(mockMailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(mockDirectoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void sendUserInstructions_none() throws MessagingException {
    // default Institution in this test class has no instructions
    createAccountAndDbUserWithAffiliation();
    verify(mockMailService).sendWelcomeEmail(any(), any(), any());

    // don't send the user instructions email if there are no instructions
    verifyNoMoreInteractions(mockMailService);
  }

  @Test
  public void sendUserInstructions_sanitized() throws MessagingException {
    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createVerifiedInstitutionalAffiliation();

    final String rawInstructions =
        "<html><script>window.alert('hacked');</script></html>"
            + "Wash your hands for 20 seconds"
            + "<STYLE type=\"text/css\">BODY{background:url(\"javascript:alert('XSS')\")} "
            + "div {color: 'red'}</STYLE>\n"
            + "<img src=\"https://eviltrackingpixel.com\" />\n";

    final String sanitizedInstructions = "Wash your hands for 20 seconds";

    final InstitutionUserInstructions instructions =
        new InstitutionUserInstructions()
            .institutionShortName(verifiedInstitutionalAffiliation.getInstitutionShortName())
            .instructions(rawInstructions);
    institutionService.setInstitutionUserInstructions(instructions);

    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
    verify(mockMailService).sendWelcomeEmail(any(), any(), any());
    verify(mockMailService).sendInstitutionUserInstructions(CONTACT_EMAIL, sanitizedInstructions);
  }

  @Test
  public void sendUserInstructions_deleted() throws MessagingException {
    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createVerifiedInstitutionalAffiliation();

    final InstitutionUserInstructions instructions =
        new InstitutionUserInstructions()
            .institutionShortName(verifiedInstitutionalAffiliation.getInstitutionShortName())
            .instructions("whatever");
    institutionService.setInstitutionUserInstructions(instructions);

    institutionService.deleteInstitutionUserInstructions(
        verifiedInstitutionalAffiliation.getInstitutionShortName());

    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
    verify(mockMailService).sendWelcomeEmail(any(), any(), any());

    // don't send the user instructions email if the instructions have been deleted
    verifyNoMoreInteractions(mockMailService);
  }

  @Test
  public void testUpdateNihToken() {
    config.featureFlags.useNewShibbolethService = false;

    NihToken nihToken = new NihToken().jwt("test");
    FirecloudJWTWrapper firecloudJwt = new FirecloudJWTWrapper().jwt("test");
    createAccountAndDbUserWithAffiliation();
    profileController.updateNihToken(nihToken);
    verify(mockFireCloudService).postNihCallback(eq(firecloudJwt));
  }

  @Test(expected = ServerErrorException.class)
  public void testUpdateNihToken_serverError() {
    config.featureFlags.useNewShibbolethService = false;

    doThrow(new ServerErrorException()).when(mockFireCloudService).postNihCallback(any());
    profileController.updateNihToken(new NihToken().jwt("test"));
  }

  @Test
  public void testUpdateNihToken_newShibbolethService() {
    config.featureFlags.useNewShibbolethService = true;

    NihToken nihToken = new NihToken().jwt("test");
    String jwt = "test";
    createAccountAndDbUserWithAffiliation();
    profileController.updateNihToken(nihToken);
    verify(shibbolethService).updateShibbolethToken(eq(jwt));
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateNihToken_badRequest_1() {
    profileController.updateNihToken(null);
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateNihToken_badRequest_noJwt() {
    profileController.updateNihToken(new NihToken());
  }

  @Test
  public void testSyncEraCommons() {
    FirecloudNihStatus nihStatus = new FirecloudNihStatus();
    String linkedUsername = "linked";
    nihStatus.setLinkedNihUsername(linkedUsername);
    nihStatus.setLinkExpireTime(TIMESTAMP.getTime());
    when(mockFireCloudService.getNihStatus()).thenReturn(nihStatus);

    createAccountAndDbUserWithAffiliation();

    profileController.syncEraCommonsStatus();
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsLinkedNihUsername())
        .isEqualTo(linkedUsername);
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsLinkExpireTime()).isNotNull();
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsCompletionTime()).isNotNull();
  }

  @Test
  public void testDeleteProfile() {
    createAccountAndDbUserWithAffiliation();

    profileController.deleteProfile();
    verify(mockProfileAuditor).fireDeleteAction(dbUser.getUserId(), dbUser.getUsername());
  }

  @Test
  public void testBypassAccessModule() {
    Profile profile = createAccountAndDbUserWithAffiliation();
    profileController.bypassAccessRequirement(
        profile.getUserId(),
        new AccessBypassRequest().isBypassed(true).moduleName(AccessModule.DATA_USE_AGREEMENT));

    DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNotNull();
  }

  @Test
  public void testUpdateProfile_updateDemographicSurvey() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();

    DemographicSurvey demographicSurvey = profile.getDemographicSurvey();
    demographicSurvey.addRaceItem(Race.AA);
    demographicSurvey.setEthnicity(Ethnicity.HISPANIC);
    demographicSurvey.setIdentifiesAsLgbtq(true);
    demographicSurvey.setLgbtqIdentity("very");
    demographicSurvey.addGenderIdentityListItem(GenderIdentity.NONE_DESCRIBE_ME);
    demographicSurvey.addSexAtBirthItem(SexAtBirth.FEMALE);
    demographicSurvey.setYearOfBirth(new BigDecimal(2000));
    demographicSurvey.setEducation(Education.NO_EDUCATION);
    demographicSurvey.setDisability(false);

    profile.setDemographicSurvey(demographicSurvey);

    profileController.updateProfile(profile);

    Profile updatedProfile = profileController.getMe().getBody();
    assertProfile(
        updatedProfile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        false);
  }

  @Test(expected = NotFoundException.class)
  public void test_updateAccountProperties_null_user() {
    profileService.updateAccountProperties(new AccountPropertyUpdate());
  }

  @Test(expected = NotFoundException.class)
  public void test_updateAccountProperties_user_not_found() {
    final AccountPropertyUpdate request = new AccountPropertyUpdate().username("not found");
    profileService.updateAccountProperties(request);
  }

  @Test
  public void test_updateAccountProperties_no_change() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    // valid user but no fields updated
    final AccountPropertyUpdate request = new AccountPropertyUpdate().username(PRIMARY_EMAIL);
    final Profile retrieved = profileService.updateAccountProperties(request);

    // RW-5257 Demo Survey completion time is incorrectly updated
    retrieved.setDemographicSurveyCompletionTime(null);
    assertThat(retrieved).isEqualTo(original);
  }

  @Test
  public void test_updateAccountProperties_contactEmail() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    // pre-affiliate with an Institution which will validate the user's existing
    // CONTACT_EMAIL and also a new one
    final String newContactEmail = "eric.lander@broadinstitute.org";

    final Institution broadPlus =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(ImmutableList.of(CONTACT_EMAIL, newContactEmail))
            .duaTypeEnum(DuaType.RESTRICTED)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broadPlus);

    final VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(broadPlus.getShortName())
            .institutionDisplayName(broadPlus.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);

    final Profile original =
        createAccountAndDbUserWithAffiliation(affiliation, grantAdminAuthority);
    assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).contactEmail(newContactEmail);

    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getContactEmail()).isEqualTo(newContactEmail);

    verify(mockProfileAuditor).fireUpdateAction(original, retrieved);
  }

  @Test(expected = BadRequestException.class)
  public void test_updateAccountProperties_contactEmail_user() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = false;

    // pre-affiliate with an Institution which will validate the user's existing
    // CONTACT_EMAIL and also a new one
    final String newContactEmail = "eric.lander@broadinstitute.org";

    final Institution broadPlus =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(ImmutableList.of(CONTACT_EMAIL, newContactEmail))
            .duaTypeEnum(DuaType.RESTRICTED)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broadPlus);

    final VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(broadPlus.getShortName())
            .institutionDisplayName(broadPlus.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);

    final Profile original =
        createAccountAndDbUserWithAffiliation(affiliation, grantAdminAuthority);
    assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).contactEmail(newContactEmail);

    profileService.updateAccountProperties(request);
  }

  @Test(expected = BadRequestException.class)
  public void test_updateAccountProperties_contactEmail_no_match() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    // the existing Institution for this user only matches the single CONTACT_EMAIL
    createAccountAndDbUserWithAffiliation(grantAdminAuthority);

    final String newContactEmail = "eric.lander@broadinstitute.org";
    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).contactEmail(newContactEmail);
    profileService.updateAccountProperties(request);
  }

  @Test
  public void test_updateAccountProperties_newAffiliation() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation expectedOriginalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);
    final Profile original = createAccountAndDbUserWithAffiliation(grantAdminAuthority);

    assertThat(original.getVerifiedInstitutionalAffiliation())
        .isEqualTo(expectedOriginalAffiliation);

    // define a new affiliation which will match the user's existing CONTACT_EMAIL

    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .emailAddresses(ImmutableList.of(CONTACT_EMAIL))
            .duaTypeEnum(DuaType.RESTRICTED)
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);

    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).affiliation(newAffiliation);
    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getVerifiedInstitutionalAffiliation()).isEqualTo(newAffiliation);

    verify(mockProfileAuditor).fireUpdateAction(original, retrieved);
  }

  @Test(expected = BadRequestException.class)
  public void test_updateAccountProperties_newAffiliation_no_match() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    createAccountAndDbUserWithAffiliation(grantAdminAuthority);

    // define a new affiliation which will not match the user's CONTACT_EMAIL

    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .duaTypeEnum(DuaType.MASTER)
            .emailDomains(ImmutableList.of("mgh.org", "massgeneral.hospital"))
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);

    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).affiliation(newAffiliation);
    profileService.updateAccountProperties(request);
  }

  @Test
  public void test_updateAccountProperties_contactEmail_newAffiliation_self_match() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation expectedOriginalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);

    final Profile original = createAccountAndDbUserWithAffiliation(grantAdminAuthority);
    assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);
    assertThat(original.getVerifiedInstitutionalAffiliation())
        .isEqualTo(expectedOriginalAffiliation);

    // update both the contact email and the affiliation, and validate against each other

    final String newContactEmail = "doctor@mgh.org";

    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .duaTypeEnum(DuaType.MASTER)
            .emailDomains(ImmutableList.of("mgh.org", "massgeneral.hospital"))
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);

    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(PRIMARY_EMAIL)
            .contactEmail(newContactEmail)
            .affiliation(newAffiliation);
    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getContactEmail()).isEqualTo(newContactEmail);
    assertThat(retrieved.getVerifiedInstitutionalAffiliation()).isEqualTo(newAffiliation);
    verify(mockProfileAuditor).fireUpdateAction(original, retrieved);
  }

  @Test(expected = BadRequestException.class)
  public void test_updateAccountProperties_contactEmail_newAffiliation_no_match() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation expectedOriginalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);

    final Profile original = createAccountAndDbUserWithAffiliation(grantAdminAuthority);
    assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);
    assertThat(original.getVerifiedInstitutionalAffiliation())
        .isEqualTo(expectedOriginalAffiliation);

    // update both the contact email and the affiliation, and fail to validate against each other

    final String newContactEmail = "notadoctor@hotmail.com";

    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .duaTypeEnum(DuaType.MASTER)
            .emailDomains(ImmutableList.of("mgh.org", "massgeneral.hospital"))
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);

    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(PRIMARY_EMAIL)
            .contactEmail(newContactEmail)
            .affiliation(newAffiliation);
    profileService.updateAccountProperties(request);
  }

  @Test
  public void test_updateAccountProperties_no_bypass_requests() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(PRIMARY_EMAIL)
            .accessBypassRequests(Collections.emptyList());
    final Profile retrieved = profileService.updateAccountProperties(request);

    // RW-5257 Demo Survey completion time is incorrectly updated
    retrieved.setDemographicSurveyCompletionTime(null);
    assertThat(retrieved).isEqualTo(original);
  }

  @Test
  public void test_updateAccountProperties_bypass_requests() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    // user has no bypasses at test start
    assertThat(original.getDataUseAgreementBypassTime()).isNull();
    assertThat(original.getComplianceTrainingBypassTime()).isNull();
    assertThat(original.getBetaAccessBypassTime()).isNull();
    assertThat(original.getEraCommonsBypassTime()).isNull();
    assertThat(original.getTwoFactorAuthBypassTime()).isNull();

    final List<AccessBypassRequest> bypasses1 =
        ImmutableList.of(
            new AccessBypassRequest().moduleName(AccessModule.DATA_USE_AGREEMENT).isBypassed(true),
            new AccessBypassRequest().moduleName(AccessModule.COMPLIANCE_TRAINING).isBypassed(true),
            // would un-bypass if a bypass had existed
            new AccessBypassRequest().moduleName(AccessModule.BETA_ACCESS).isBypassed(false));

    final AccountPropertyUpdate request1 =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).accessBypassRequests(bypasses1);
    final Profile retrieved1 = profileService.updateAccountProperties(request1);

    // these two are now bypassed
    assertThat(retrieved1.getDataUseAgreementBypassTime()).isNotNull();
    assertThat(retrieved1.getComplianceTrainingBypassTime()).isNotNull();
    // remains unbypassed because the flag was set to false
    assertThat(retrieved1.getBetaAccessBypassTime()).isNull();
    // unchanged: unbypassed
    assertThat(retrieved1.getEraCommonsBypassTime()).isNull();
    assertThat(retrieved1.getTwoFactorAuthBypassTime()).isNull();

    final List<AccessBypassRequest> bypasses2 =
        ImmutableList.of(
            // un-bypass the previously bypassed
            new AccessBypassRequest().moduleName(AccessModule.DATA_USE_AGREEMENT).isBypassed(false),
            new AccessBypassRequest()
                .moduleName(AccessModule.COMPLIANCE_TRAINING)
                .isBypassed(false),
            // bypass
            new AccessBypassRequest().moduleName(AccessModule.ERA_COMMONS).isBypassed(true),
            new AccessBypassRequest().moduleName(AccessModule.TWO_FACTOR_AUTH).isBypassed(true));

    final AccountPropertyUpdate request2 = request1.accessBypassRequests(bypasses2);
    final Profile retrieved2 = profileService.updateAccountProperties(request2);

    // these two are now unbypassed
    assertThat(retrieved2.getDataUseAgreementBypassTime()).isNull();
    assertThat(retrieved2.getComplianceTrainingBypassTime()).isNull();
    // remains unbypassed
    assertThat(retrieved2.getBetaAccessBypassTime()).isNull();
    // the two are now bypassed
    assertThat(retrieved2.getEraCommonsBypassTime()).isNotNull();
    assertThat(retrieved2.getTwoFactorAuthBypassTime()).isNotNull();

    verify(mockProfileAuditor).fireUpdateAction(original, retrieved1);
    verify(mockProfileAuditor).fireUpdateAction(retrieved1, retrieved2);

    // DUA and COMPLIANCE x2, one for each request

    verify(mockUserServiceAuditor, times(2))
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME),
            any(),
            any());
    verify(mockUserServiceAuditor, times(2))
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME),
            any(),
            any());

    // BETA once in request 1

    verify(mockUserServiceAuditor)
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.BETA_ACCESS_BYPASS_TIME),
            any(),
            any());

    // ERA and 2FA once in request 2

    verify(mockUserServiceAuditor)
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME),
            any(),
            any());
    verify(mockUserServiceAuditor)
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME),
            any(),
            any());
  }

  @Test
  public void test_updateAccountProperties_free_tier_quota() {
    createAccountAndDbUserWithAffiliation();

    final Double originalQuota = dbUser.getFreeTierCreditsLimitDollarsOverride();
    final Double newQuota = 123.4;

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(PRIMARY_EMAIL).freeCreditsLimit(newQuota);

    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getFreeTierDollarQuota()).isWithin(0.01).of(newQuota);

    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(dbUser.getUserId(), originalQuota, newQuota);
  }

  @Test
  public void test_updateAccountProperties_free_tier_quota_no_change() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(PRIMARY_EMAIL)
            .freeCreditsLimit(original.getFreeTierDollarQuota());
    profileService.updateAccountProperties(request);

    verify(mockUserServiceAuditor, never())
        .fireSetFreeTierDollarLimitOverride(anyLong(), anyDouble(), anyDouble());
  }

  // don't set an override if the value to set is equal to the system default
  // and observe that the user's limit tracks with the default

  @Test
  public void test_updateAccountProperties_free_tier_quota_no_override() {
    config.billing.defaultFreeCreditsDollarLimit = 123.45;

    final Profile original = createAccountAndDbUserWithAffiliation();
    assertThat(original.getFreeTierDollarQuota()).isWithin(0.01).of(123.45);

    // update the default - the user's profile also updates

    config.billing.defaultFreeCreditsDollarLimit = 234.56;
    assertThat(profileService.getProfile(dbUser).getFreeTierDollarQuota())
        .isWithin(0.01)
        .of(234.56);

    // setting a Free Credits Limit equal to the default will not override

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(PRIMARY_EMAIL)
            .freeCreditsLimit(config.billing.defaultFreeCreditsDollarLimit);
    profileService.updateAccountProperties(request);
    verify(mockUserServiceAuditor, never())
        .fireSetFreeTierDollarLimitOverride(anyLong(), anyDouble(), anyDouble());

    // the user's profile continues to track default changes

    config.billing.defaultFreeCreditsDollarLimit = 345.67;
    assertThat(profileService.getProfile(dbUser).getFreeTierDollarQuota())
        .isWithin(0.01)
        .of(345.67);
  }

  private Profile createAccountAndDbUserWithAffiliation(
      VerifiedInstitutionalAffiliation verifiedAffiliation, boolean grantAdminAuthority) {

    createAccountRequest.getProfile().setVerifiedInstitutionalAffiliation(verifiedAffiliation);

    Profile result = profileController.createAccount(createAccountRequest).getBody();

    // initialize the global test dbUser
    dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);

    // TODO: why is this necessary instead of initializing in setUp() ?
    dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);

    if (grantAdminAuthority) {
      dbUser.setAuthoritiesEnum(Collections.singleton(Authority.ACCESS_CONTROL_ADMIN));
    }

    dbUser = userDao.save(dbUser);

    // match dbUser updates

    result.setEmailVerificationStatus(dbUser.getEmailVerificationStatusEnum());
    result.setAuthorities(Lists.newArrayList(dbUser.getAuthoritiesEnum()));

    return result;
  }

  private Profile createAccountAndDbUserWithAffiliation(
      VerifiedInstitutionalAffiliation verifiedAffiliation) {
    boolean grantAdminAuthority = false;
    return createAccountAndDbUserWithAffiliation(verifiedAffiliation, grantAdminAuthority);
  }

  private Profile createAccountAndDbUserWithAffiliation() {
    return createAccountAndDbUserWithAffiliation(createVerifiedInstitutionalAffiliation());
  }

  private Profile createAccountAndDbUserWithAffiliation(boolean grantAdminAuthority) {
    return createAccountAndDbUserWithAffiliation(
        createVerifiedInstitutionalAffiliation(), grantAdminAuthority);
  }

  private void assertProfile(
      Profile profile,
      String primaryEmail,
      String contactEmail,
      String familyName,
      String givenName,
      DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime,
      Boolean contactEmailFailure) {
    assertThat(profile).isNotNull();
    assertThat(profile.getContactEmail()).isEqualTo(contactEmail);
    assertThat(profile.getFamilyName()).isEqualTo(familyName);
    assertThat(profile.getGivenName()).isEqualTo(givenName);
    assertThat(profile.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(profile.getContactEmailFailure()).isEqualTo(contactEmailFailure);
    assertUser(primaryEmail, contactEmail, familyName, givenName, dataAccessLevel, firstSignInTime);
  }

  private void assertUser(
      String primaryEmail,
      String contactEmail,
      String familyName,
      String givenName,
      DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime) {
    DbUser user = userDao.findUserByUsername(primaryEmail);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(contactEmail);
    assertThat(user.getFamilyName()).isEqualTo(familyName);
    assertThat(user.getGivenName()).isEqualTo(givenName);
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(dataAccessLevel);
    assertThat((double) user.getFirstSignInTime().getTime())
        .isWithin(TIME_TOLERANCE_MILLIS)
        .of(firstSignInTime.getTime());
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(dataAccessLevel);
  }

  private VerifiedInstitutionalAffiliation createVerifiedInstitutionalAffiliation() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(Collections.singletonList(CONTACT_EMAIL))
            .duaTypeEnum(DuaType.RESTRICTED)
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    return new VerifiedInstitutionalAffiliation()
        .institutionShortName(broad.getShortName())
        .institutionDisplayName(broad.getDisplayName())
        .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);
  }
}
