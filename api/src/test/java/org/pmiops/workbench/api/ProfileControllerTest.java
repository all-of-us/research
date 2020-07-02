package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.pmiops.workbench.actionaudit.ActionAuditQueryServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditorImpl;
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
import org.pmiops.workbench.model.Address;
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

  private static final double FREE_TIER_LIMIT = 300D;
  private static final double FREE_TIER_USAGE = 100D;
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
  @MockBean private FreeTierBillingService mockFreeTierBillingService;
  @MockBean private MailService mockMailService;
  @MockBean private ProfileAuditor mockProfileAuditor;
  @MockBean private ShibbolethService shibbolethService;
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
    CommonConfig.class,
    ComplianceServiceImpl.class,
    DemographicSurveyMapperImpl.class,
    InstitutionalAffiliationMapperImpl.class,
    PageVisitMapperImpl.class,
    ProfileService.class,
    ProfileController.class,
    ProfileMapperImpl.class,
    InstitutionServiceImpl.class,
    InstitutionMapperImpl.class,
    CommonMappers.class,
    VerifiedInstitutionalAffiliationMapperImpl.class,
    CaptchaVerificationService.class,
    UserServiceImpl.class,
    UserServiceTestConfiguration.class,
  })
  @MockBean({BigQueryService.class, UserServiceAuditorImpl.class})
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
    when(mockDirectoryService.getUser(PRIMARY_EMAIL)).thenReturn(googleUser);

    DUA_VERSION = userService.getCurrentDuccVersion();

    try {
      doNothing().when(mockMailService).sendBetaAccessRequestEmail(Mockito.any());
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    when(mockCloudStorageService.getCaptchaServerKey()).thenReturn("Server_Key");
    try {
      when(mockCaptchaVerificationService.verifyCaptcha(CAPTCHA_TOKEN)).thenReturn(true);
      when(mockCaptchaVerificationService.verifyCaptcha(WRONG_CAPTCHA_TOKEN)).thenReturn(false);
    } catch (ApiException e) {
      e.printStackTrace();
    }
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invitationKeyMismatch() {
    createUserWithVerifiedAffiliation();

    config.access.requireInvitationKey = true;
    when(mockCloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invalidCaptchaToken() {
    createUserWithVerifiedAffiliation();
    createAccountRequest.setCaptchaVerificationToken(WRONG_CAPTCHA_TOKEN);
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testCreateAccount_noRequireInvitationKey() {
    createUserWithVerifiedAffiliation();

    // When invitation key verification is turned off, even a bad invitation key should
    // allow a user to be created.
    config.access.requireInvitationKey = false;
    when(mockCloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testInvitationKeyVerification_invitationKeyMismatch() {
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
    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_success() {
    createUserWithVerifiedAffiliation();
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
    final DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(dbUser).isNotNull();
    assertThat(dbUser.getDataAccessLevelEnum()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testCreateAccount_withTosVersion() {
    createAccountRequest.setTermsOfServiceVersion(1);
    createUserWithVerifiedAffiliation();

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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
  }

  @Test
  public void testSubmitDataUseAgreement_success() {
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
    String duaInitials = "NIH";
    profileController.submitDataUseAgreement(DUA_VERSION - 1, duaInitials);
  }

  @Test
  public void test_outdatedDataUseAgreement() {
    // force version number to 2 instead of 3
    config.featureFlags.enableV3DataUserCodeOfConduct = false;
    final int previousDuaVersion = DUA_VERSION - 1;

    final long userId = createUserWithVerifiedAffiliation().getUserId();

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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();

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
    createUserWithVerifiedAffiliation();

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
    createUserWithVerifiedAffiliation();

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
    createUserWithVerifiedAffiliation();

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
    createAccount(missing);
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

    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
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
    createAccount(verifiedInstitutionalAffiliation);
  }

  @Test(expected = NotFoundException.class)
  public void updateVerifiedInstitutionalAffiliation_noSuchInstitution() {
    final VerifiedInstitutionalAffiliation original = createVerifiedInstitutionalAffiliation();
    createAccount(original);

    final VerifiedInstitutionalAffiliation newAffil =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("NotTheBroad")
            .institutionDisplayName("The Narrow Institute?")
            .institutionalRoleEnum(InstitutionalRole.PRE_DOCTORAL);

    profileController.updateVerifiedInstitutionalAffiliation(dbUser.getUserId(), newAffil);
  }

  @Test
  public void updateVerifiedInstitutionalAffiliation_update() {
    VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createVerifiedInstitutionalAffiliation();
    createAccount(verifiedInstitutionalAffiliation);

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
    createAccount(original);

    final Profile profile = profileController.getMe().getBody();
    profile.setVerifiedInstitutionalAffiliation(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateVerifiedInstitutionalAffiliation_removeForbidden() {
    final VerifiedInstitutionalAffiliation original = createVerifiedInstitutionalAffiliation();
    createAccount(original);

    profileController.updateVerifiedInstitutionalAffiliation(dbUser.getUserId(), null);
  }

  @Test
  public void updateContactEmail_forbidden() {
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    String newName =
        "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson";
    profile.setGivenName(newName);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullAddress() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setAddress(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullCountry() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.getAddress().country(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullState() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.getAddress().state(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_nullZipCode() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.getAddress().zipCode(null);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_emptyReasonForResearch() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setAreaOfResearch("");
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_UpdateUserName() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setUsername("newUserName@fakeDomain.com");
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateProfile_badRequest_UpdateContactEmail() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setContactEmail("newContact@fakeDomain.com");
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateFamilyName_badRequest() {
    createUserWithVerifiedAffiliation();
    Profile profile = profileController.getMe().getBody();
    String newName =
        "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson";
    profile.setFamilyName(newName);
    profileController.updateProfile(profile);
  }

  @Test
  public void resendWelcomeEmail_messagingException() throws MessagingException {
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
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
    createUserWithVerifiedAffiliation();
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

    createAccount(verifiedInstitutionalAffiliation);
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

    createAccount(verifiedInstitutionalAffiliation);
    verify(mockMailService).sendWelcomeEmail(any(), any(), any());

    // don't send the user instructions email if the instructions have been deleted
    verifyNoMoreInteractions(mockMailService);
  }

  @Test
  public void testUpdateNihToken() {
    config.featureFlags.useNewShibbolethService = false;

    NihToken nihToken = new NihToken().jwt("test");
    FirecloudJWTWrapper firecloudJwt = new FirecloudJWTWrapper().jwt("test");
    createUserWithVerifiedAffiliation();
    profileController.updateNihToken(nihToken);
    verify(mockFireCloudService).postNihCallback(ArgumentMatchers.eq(firecloudJwt));
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
    createUserWithVerifiedAffiliation();
    profileController.updateNihToken(nihToken);
    verify(shibbolethService).updateShibbolethToken(ArgumentMatchers.eq(jwt));
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

    createUserWithVerifiedAffiliation();

    profileController.syncEraCommonsStatus();
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsLinkedNihUsername())
        .isEqualTo(linkedUsername);
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsLinkExpireTime()).isNotNull();
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsCompletionTime()).isNotNull();
  }

  @Test
  public void testDeleteProfile() {
    createUserWithVerifiedAffiliation();

    profileController.deleteProfile();
    verify(mockProfileAuditor).fireDeleteAction(dbUser.getUserId(), dbUser.getUsername());
  }

  @Test
  public void testBypassAccessModule() {
    Profile profile = createUserWithVerifiedAffiliation();
    profileController.bypassAccessRequirement(
        profile.getUserId(),
        new AccessBypassRequest().isBypassed(true).moduleName(AccessModule.DATA_USE_AGREEMENT));

    DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNotNull();
  }

  @Test
  public void testFreeTierLimits() {
    createUserWithVerifiedAffiliation();
    DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);

    when(mockFreeTierBillingService.getCachedFreeTierUsage(dbUser)).thenReturn(FREE_TIER_USAGE);
    when(mockFreeTierBillingService.getUserFreeTierDollarLimit(dbUser)).thenReturn(FREE_TIER_LIMIT);

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
    assertThat(profile.getFreeTierUsage()).isEqualTo(FREE_TIER_USAGE);
    assertThat(profile.getFreeTierDollarQuota()).isEqualTo(FREE_TIER_LIMIT);
  }

  @Test
  public void testUpdateProfile_updateDemographicSurvey() {
    createUserWithVerifiedAffiliation();
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

  private Profile createAccount(VerifiedInstitutionalAffiliation verifiedAffiliation) {
    when(mockCloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(mockDirectoryService.createUser(
            GIVEN_NAME, FAMILY_NAME, USER_PREFIX + "@" + GSUITE_DOMAIN, CONTACT_EMAIL))
        .thenReturn(googleUser);

    createAccountRequest.getProfile().setVerifiedInstitutionalAffiliation(verifiedAffiliation);

    Profile result = profileController.createAccount(createAccountRequest).getBody();
    dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    userDao.save(dbUser);

    return result;
  }

  private Profile createUserWithVerifiedAffiliation() {
    return createAccount(createVerifiedInstitutionalAffiliation());
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
