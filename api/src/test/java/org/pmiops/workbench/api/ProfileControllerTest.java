package org.pmiops.workbench.api;


import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;
import javax.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.BillingProjectMembership.CreationStatusEnum;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.IdVerificationStatus;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.IdVerificationListResponse;
import org.pmiops.workbench.model.IdVerificationReviewRequest;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class ProfileControllerTest {

  private static final Instant NOW = Instant.now();
  private static final Timestamp TIMESTAMP = new Timestamp(NOW.toEpochMilli());
  private static final String USERNAME = "bob";
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String INVITATION_KEY = "secretpassword";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String BILLING_PROJECT_PREFIX = "all-of-us-free-";

  @Mock
  private Provider<User> userProvider;
  @Mock
  private Provider<UserAuthentication> userAuthenticationProvider;
  @Autowired
  private UserDao userDao;
  @Autowired
  private AdminActionHistoryDao adminActionHistoryDao;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private NotebooksService notebooksService;
  @Mock
  private DirectoryService directoryService;
  @Mock
  private CloudStorageService cloudStorageService;
  @Mock
  private Provider<WorkbenchConfig> configProvider;
  @Mock
  private MailService mailService;

  private ProfileController profileController;
  private ProfileController cloudProfileController;
  private CreateAccountRequest createAccountRequest;
  private InvitationVerificationRequest invitationVerificationRequest;
  private com.google.api.services.admin.directory.model.User googleUser;
  private FakeClock clock;
  private User user;

  @Before
  public void setUp() throws MessagingException {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.firecloud.billingProjectPrefix = BILLING_PROJECT_PREFIX;
    config.firecloud.billingRetryCount = 2;
    config.admin = new WorkbenchConfig.AdminConfig();
    config.admin.adminIdVerification = "adminIdVerify@dummyMockEmail.com";

    WorkbenchEnvironment environment = new WorkbenchEnvironment(true, "appId");
    WorkbenchEnvironment cloudEnvironment = new WorkbenchEnvironment(false, "appId");
    createAccountRequest = new CreateAccountRequest();
    invitationVerificationRequest = new InvitationVerificationRequest();
    Profile profile = new Profile();
    profile.setContactEmail(CONTACT_EMAIL);
    profile.setFamilyName(FAMILY_NAME);
    profile.setGivenName(GIVEN_NAME);
    profile.setUsername(USERNAME);
    createAccountRequest.setProfile(profile);
    createAccountRequest.setInvitationKey(INVITATION_KEY);
    invitationVerificationRequest.setInvitationKey(INVITATION_KEY);
    googleUser = new com.google.api.services.admin.directory.model.User();
    googleUser.setPrimaryEmail(PRIMARY_EMAIL);
    googleUser.setChangePasswordAtNextLogin(true);
    googleUser.setPassword("testPassword");

    clock = new FakeClock(NOW);

    doNothing().when(mailService).sendIdVerificationRequestEmail(Mockito.any());
    UserService userService = new UserService(userProvider, userDao, adminActionHistoryDao, clock, fireCloudService, configProvider);
    ProfileService profileService = new ProfileService(fireCloudService, userDao);
    this.profileController = new ProfileController(profileService, userProvider, userAuthenticationProvider,
        userDao, clock, userService, fireCloudService, directoryService,
        cloudStorageService, notebooksService, Providers.of(config), environment,
        Providers.of(mailService));
    this.cloudProfileController = new ProfileController(profileService, userProvider, userAuthenticationProvider,
        userDao, clock, userService, fireCloudService, directoryService,
        cloudStorageService, notebooksService, Providers.of(config),
        cloudEnvironment, Providers.of(mailService));
    when(directoryService.getUser(PRIMARY_EMAIL)).thenReturn(googleUser);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invitationKeyMismatch() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testInvitationKeyVerification_invitationKeyMismatch() throws Exception {
    profileController.invitationKeyVerification(invitationVerificationRequest);
  }

  @Test
  public void testCreateAccount_success() throws Exception {
    createUser();
    User user = userDao.findUserByEmail(PRIMARY_EMAIL);
    assertThat(user).isNotNull();
    assertThat(user.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testSubmitDemographicSurvey_success() throws Exception {
    createUser();
    Profile profile = profileController.submitDemographicsSurvey().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getIdVerificationStatus()).isEqualTo(IdVerificationStatus.UNVERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getTermsOfServiceCompletionTime()).isNull();
    assertThat(profile.getEthicsTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSubmitTermsOfService_success() throws Exception {
    createUser();
    Profile profile = profileController.submitTermsOfService().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getIdVerificationStatus()).isEqualTo(IdVerificationStatus.UNVERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isNull();
    assertThat(profile.getTermsOfServiceCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getEthicsTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSubmitEthicsTraining_success() throws Exception {
    createUser();
    Profile profile = profileController.completeEthicsTraining().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getIdVerificationStatus()).isEqualTo(IdVerificationStatus.UNVERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isNull();
    assertThat(profile.getTermsOfServiceCompletionTime()).isNull();
    assertThat(profile.getEthicsTrainingCompletionTime()).isEqualTo(NOW.toEpochMilli());
  }

  @Test
  public void testSubmitEverything_success() throws Exception {
    createUser();
    WorkbenchConfig testConfig = new WorkbenchConfig();
    testConfig.firecloud = new FireCloudConfig();
    testConfig.firecloud.registeredDomainName = "";

    when(configProvider.get()).thenReturn(testConfig);
    Profile profile = profileController.completeEthicsTraining().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    IdVerificationReviewRequest reviewStatus = new IdVerificationReviewRequest();
    reviewStatus.setNewStatus(IdVerificationStatus.VERIFIED);
    profileController.reviewIdVerification(profile.getUserId(), reviewStatus);
    profile = profileController.submitDemographicsSurvey().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    profile = profileController.submitTermsOfService().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.REGISTERED);
    verify(fireCloudService).addUserToGroup("bob@researchallofus.org", "");

    assertThat(profile.getIdVerificationStatus()).isEqualTo(IdVerificationStatus.VERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getTermsOfServiceCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getEthicsTrainingCompletionTime()).isEqualTo(NOW.toEpochMilli());
  }


  @Test(expected = ServerErrorException.class)
  public void testCreateAccount_directoryServiceFail() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);

    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, CONTACT_EMAIL))
        .thenThrow(new ServerErrorException());
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testGetIdVerificationsForReview() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    IdVerificationListResponse response = profileController.getIdVerificationsForReview().getBody();
    assertThat(response.getProfileList().size()).isEqualTo(1);

    IdVerificationReviewRequest request =
        new IdVerificationReviewRequest().newStatus(IdVerificationStatus.VERIFIED);
    profileController.reviewIdVerification(user.getUserId(), request);
    response = profileController.getIdVerificationsForReview().getBody();
    assertThat(response.getProfileList()).isEmpty();
  }

  @Test
  public void testMe_success() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, true, null);
    assertThat(profile.getFreeTierBillingProjectName()).isNotEmpty();
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(anyString());
    verify(fireCloudService).addUserToBillingProject(
        PRIMARY_EMAIL, profile.getFreeTierBillingProjectName());
  }

  @Test
  public void testMe_secondCallInitializesProject() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    String projectName = profile.getFreeTierBillingProjectName();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Ready".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.READY);
    membership.setProjectName(projectName);
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.READY);

    verify(fireCloudService).grantGoogleRoleToUser(
        projectName, FireCloudService.BIGQUERY_JOB_USER_GOOGLE_ROLE, PRIMARY_EMAIL);
  }

  @Test
  public void testMe_retriesBillingProjectErrors() throws Exception {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.firecloud.billingRetryCount = 2;
    when(configProvider.get()).thenReturn(config);
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Error".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.ERROR);
    membership.setProjectName(profile.getFreeTierBillingProjectName());
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_errorsAfterFourProjectFailures() throws Exception {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.firecloud.billingRetryCount = 2;
    when(configProvider.get()).thenReturn(config);
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Error".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.ERROR);
    membership.setProjectName(profile.getFreeTierBillingProjectName());
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    for (int i = 0; i <= configProvider.get().firecloud.billingRetryCount; i++) {
      profile = profileController.getMe().getBody();
    }
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.ERROR);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_secondCallStillPendingProject() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Creating".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.CREATING);
    membership.setProjectName(profile.getFreeTierBillingProjectName());
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_secondCallProjectNotReturned() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.READY);
    membership.setProjectName("unrelated-project");
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_successDevProjectConflict() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();

    String projectName = profile.getFreeTierBillingProjectName();
    doThrow(new ConflictException())
        .when(fireCloudService).createAllOfUsBillingProject(projectName);

    // When a conflict occurs in dev, log the exception but continue.
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, true, null);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);
  }

  @Test
  public void testMe_userBeforeSuccessCloudProjectConflict() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    ConflictException conflict = new ConflictException();
    doThrow(conflict)
        .doThrow(conflict)
        .doNothing()
        .when(fireCloudService).createAllOfUsBillingProject(anyString());

    Profile profile = cloudProfileController.getMe().getBody();

    // When a conflict occurs in dev, log the exception but continue.
    String projectName = BILLING_PROJECT_PREFIX + user.getUserId();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, true, null);
    assertThat(profile.getFreeTierBillingProjectName()).isEqualTo(projectName + "-2");
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).addUserToBillingProject(
        PRIMARY_EMAIL, profile.getFreeTierBillingProjectName());
  }

  @Test
  public void testMe_userBeforeSuccessCloudProjectTooManyConflicts() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    doThrow(new ConflictException())
        .when(fireCloudService).createAllOfUsBillingProject(anyString());

    try {
      cloudProfileController.getMe();
      fail("ServerErrorException expected");
    } catch (ServerErrorException e) {
      // expected
    }

    // When too many conflicts occur, the user doesn't have their project name set or first
    // sign in time.
    String projectName = BILLING_PROJECT_PREFIX + user.getUserId();
    assertThat(user.getFreeTierBillingProjectName()).isNull();
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-1");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-2");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-3");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-4");
  }

  @Test
  public void testMe_userBeforeNotLoggedInSuccess() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    Profile profile = profileController.getMe().getBody();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, true, null);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    verify(fireCloudService).createAllOfUsBillingProject(profile.getFreeTierBillingProjectName());
    verify(fireCloudService).addUserToBillingProject(
        PRIMARY_EMAIL, profile.getFreeTierBillingProjectName());

    // An additional call to getMe() should have no effect.
    clock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, true, null);
  }

  @Test
  public void testGetBillingProjects_empty() throws Exception {
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(
        ImmutableList.<org.pmiops.workbench.firecloud.model.BillingProjectMembership>of());
    assertThat(profileController.getBillingProjects().getBody()).isEmpty();
  }

  @Test
  public void testGetBillingProjects_notEmpty() throws Exception {
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setProjectName("a");
    membership.setRole("c");
    membership.setCreationStatus(CreationStatusEnum.CREATING);
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(
        ImmutableList.of(membership));
    List<BillingProjectMembership> memberships =
        profileController.getBillingProjects().getBody();
    assertThat(memberships.size()).isEqualTo(1);
    BillingProjectMembership result = memberships.get(0);
    assertThat(result.getProjectName()).isEqualTo("a");
    assertThat(result.getRole()).isEqualTo("c");
    assertThat(result.getStatus()).isEqualTo(BillingProjectStatus.PENDING);
  }

  @Test
  public void testMe_institutionalAffiliationsAlphabetical() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
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

  @Test
  public void testMe_institutionalAffiliationsNotAlphabetical() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
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

  @Test
  public void testMe_removeSingleInstitutionalAffiliation() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
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
    affiliations = new ArrayList<InstitutionalAffiliation>();
    affiliations.add(first);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(1);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
  }

  @Test
  public void testMe_removeAllInstitutionalAffiliations() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
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

  @Test
  public void updateContactEmail_forbidden() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    user.setFirstSignInTime(new Timestamp(new Date().getTime()));
    UpdateContactEmailRequest request = new UpdateContactEmailRequest();
    String originalEmail = user.getContactEmail();
    request.setContactEmail("newcontactEmail@whatever.com");
    request.setUsername(user.getEmail());

    ResponseEntity response = profileController.updateContactEmail(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(user.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_badRequest() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    user.setFirstSignInTime(null);
    UpdateContactEmailRequest request = new UpdateContactEmailRequest();
    String originalEmail = user.getContactEmail();
    request.setContactEmail("bad email address *(SD&(*D&F&*(DS ");
    request.setUsername(user.getEmail());

    ResponseEntity response = profileController.updateContactEmail(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(user.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_OK() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    user.setFirstSignInTime(null);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    UpdateContactEmailRequest request = new UpdateContactEmailRequest();
    request.setContactEmail("newContactEmail@whatever.com");
    request.setUsername(user.getEmail());

    ResponseEntity response = profileController.updateContactEmail(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(user.getContactEmail()).isEqualTo("newContactEmail@whatever.com");
  }

  @Test
  public void resendWelcomeEmail_messagingException() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    user.setFirstSignInTime(null);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doThrow(new MessagingException("exception")).when(mailService).sendWelcomeEmail(any(), any(), any());
    ResendWelcomeEmailRequest request = new ResendWelcomeEmailRequest();
    request.setUsername(user.getEmail());

    ResponseEntity response = profileController.resendWelcomeEmail(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    //called twice, once during account creation, once on resend
    verify(mailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(directoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void resendWelcomeEmail_OK() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doNothing().when(mailService).sendWelcomeEmail(any(), any(), any());
    ResendWelcomeEmailRequest request = new ResendWelcomeEmailRequest();
    request.setUsername(user.getEmail());

    ResponseEntity response = profileController.resendWelcomeEmail(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    //called twice, once during account creation, once on resend
    verify(mailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(directoryService, times(1)).resetUserPassword(anyString());
  }

  private Profile createUser() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, CONTACT_EMAIL))
        .thenReturn(googleUser);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(false);
    Profile result = profileController.createAccount(createAccountRequest).getBody();
    user = userDao.findUserByEmail(PRIMARY_EMAIL);
    user.setEmailVerificationStatus(EmailVerificationStatus.SUBSCRIBED);
    userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    when(userAuthenticationProvider.get()).thenReturn(
        new UserAuthentication(user, null, null, UserType.RESEARCHER));
    return result;
  }

  private void assertProfile(Profile profile, String primaryEmail, String contactEmail,
      String familyName, String givenName, DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime, boolean enabledInFirecloud, Boolean contactEmailFailure) {
    assertThat(profile).isNotNull();
    assertThat(profile.getContactEmail()).isEqualTo(contactEmail);
    assertThat(profile.getFamilyName()).isEqualTo(familyName);
    assertThat(profile.getGivenName()).isEqualTo(givenName);
    assertThat(profile.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(profile.getEnabledInFireCloud()).isEqualTo(enabledInFirecloud);
    assertThat(profile.getContactEmailFailure()).isEqualTo(contactEmailFailure);
    assertUser(primaryEmail, contactEmail, familyName, givenName, dataAccessLevel, firstSignInTime);
  }

  private void assertUser(String primaryEmail, String contactEmail,
      String familyName, String givenName, DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime) {
    User user = userDao.findUserByEmail(primaryEmail);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(contactEmail);
    assertThat(user.getFamilyName()).isEqualTo(familyName);
    assertThat(user.getGivenName()).isEqualTo(givenName);
    assertThat(user.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(user.getFirstSignInTime()).isEqualTo(firstSignInTime);
    assertThat(user.getDataAccessLevel()).isEqualTo(dataAccessLevel);
  }

}
