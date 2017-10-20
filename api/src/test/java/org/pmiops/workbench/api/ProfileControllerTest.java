package org.pmiops.workbench.api;


import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.mysql.fabric.Server;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.*;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.RegistrationRequest;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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
  private static final String PASSWORD = "12345";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String BILLING_PROJECT_PREFIX = "all-of-us-free-";

  @Mock
  private Provider<User> userProvider;
  @Autowired
  private UserDao userDao;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private DirectoryService directoryService;
  @Mock
  private CloudStorageService cloudStorageService;

  private ProfileController profileController;
  private ProfileController cloudProfileController;
  private CreateAccountRequest createAccountRequest;
  private com.google.api.services.admin.directory.model.User googleUser;
  private FakeClock clock;

  @Before
  public void setUp() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.firecloud.billingProjectPrefix = BILLING_PROJECT_PREFIX;

    WorkbenchEnvironment environment = new WorkbenchEnvironment(true, "appId");
    WorkbenchEnvironment cloudEnvironment = new WorkbenchEnvironment(false, "appId");
    createAccountRequest = new CreateAccountRequest();
    createAccountRequest.setContactEmail(CONTACT_EMAIL);
    createAccountRequest.setFamilyName(FAMILY_NAME);
    createAccountRequest.setGivenName(GIVEN_NAME);
    createAccountRequest.setInvitationKey(INVITATION_KEY);
    createAccountRequest.setPassword(PASSWORD);
    createAccountRequest.setUsername(USERNAME);

    googleUser = new com.google.api.services.admin.directory.model.User();
    googleUser.setPrimaryEmail(PRIMARY_EMAIL);

    clock = new FakeClock(NOW);

    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail(PRIMARY_EMAIL);
    userInfo.setFamilyName(FAMILY_NAME);
    userInfo.setGivenName(GIVEN_NAME);

    ProfileService profileService = new ProfileService(fireCloudService, userProvider, userDao);
    this.profileController = new ProfileController(profileService, userProvider,
        Providers.of(userInfo), userDao, clock, fireCloudService, directoryService,
        cloudStorageService, Providers.of(config), environment);
    this.cloudProfileController = new ProfileController(profileService, userProvider,
        Providers.of(userInfo), userDao, clock, fireCloudService, directoryService,
        cloudStorageService, Providers.of(config), cloudEnvironment);
  }



  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invitationKeyMismatch() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testCreateAccount_success() throws Exception {
    Profile profile = createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(false);
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, null, null, false);
  }

  @Test(expected = ServerErrorException.class)
  public void testCreateAccount_directoryServiceFail() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);

    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, PASSWORD))
        .thenThrow(new IOException());
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testMe_noUserBeforeSuccess() throws Exception {
    when(userProvider.get()).thenReturn(null);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();

    String projectName = BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();
    assertProfile(profile, PRIMARY_EMAIL, null, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
    verify(fireCloudService).registerUser(null, GIVEN_NAME, FAMILY_NAME);

    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);
  }

  @Test
  public void testMe_noUserBeforeSuccessDevProjectConflict() throws Exception {
    when(userProvider.get()).thenReturn(null);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();

    String projectName = BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName);

    // When a conflict occurs in dev, log the exception but continue.
    assertProfile(profile, PRIMARY_EMAIL, null, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
    verify(fireCloudService).registerUser(null, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);
  }

  @Test
  public void testMe_userBeforeSuccessCloudProjectConflict() throws Exception {
    createUser();
    User user = userDao.findUserByEmail(PRIMARY_EMAIL);
    when(userProvider.get()).thenReturn(user);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    String projectName = BILLING_PROJECT_PREFIX + user.getUserId();
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName);
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-1");

    Profile profile = cloudProfileController.getMe().getBody();

    // When a conflict occurs in dev, log the exception but continue.
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName + "-2",
        true);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-1");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-2");
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName + "-2");
  }

  @Test
  public void testMe_userBeforeSuccessCloudProjectTooManyConflicts() throws Exception {
    createUser();
    User user = userDao.findUserByEmail(PRIMARY_EMAIL);
    when(userProvider.get()).thenReturn(user);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    String projectName = BILLING_PROJECT_PREFIX + user.getUserId();
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName);
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-1");
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-2");
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-3");
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-4");

    try {
      cloudProfileController.getMe();
      fail("ServerErrorException expected");
    } catch (ServerErrorException e) {
      // expected
    }

    // When too many conflicts occur, the user doesn't have their project name set or first
    // sign in time.
    assertUser(PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, null, null);
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
    when(userProvider.get()).thenReturn(userDao.findUserByEmail(PRIMARY_EMAIL));
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    Profile profile = profileController.getMe().getBody();
    String projectName = BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);


    // An additional call to getMe() should have no effect.
    clock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
  }

  @Test
  public void testRegister_noUserBeforeSuccess() throws Exception {
    when(userProvider.get()).thenReturn(null);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.register(new RegistrationRequest()).getBody();
    String projectName = BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();
    assertProfile(profile, PRIMARY_EMAIL, null, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.REGISTERED, TIMESTAMP, projectName, true);
    verify(fireCloudService).registerUser(null, GIVEN_NAME, FAMILY_NAME);

    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);
  }


  private Profile createUser() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, PASSWORD))
        .thenReturn(googleUser);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(false);
    return profileController.createAccount(createAccountRequest).getBody();
  }


  private void assertProfile(Profile profile, String primaryEmail, String contactEmail,
      String familyName, String givenName, DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime, String freeTierBillingProject, boolean enabledInFirecloud) {
    assertThat(profile).isNotNull();
    assertThat(profile.getContactEmail()).isEqualTo(contactEmail);
    assertThat(profile.getFamilyName()).isEqualTo(familyName);
    assertThat(profile.getGivenName()).isEqualTo(givenName);
    assertThat(profile.getDataAccessLevel()).isEqualTo(
        Profile.DataAccessLevelEnum.fromValue(dataAccessLevel.toString().toLowerCase()));
    assertThat(profile.getFreeTierBillingProjectName()).isEqualTo(freeTierBillingProject);
    assertThat(profile.getEnabledInFireCloud()).isEqualTo(enabledInFirecloud);
    assertUser(primaryEmail, contactEmail, familyName, givenName, dataAccessLevel, firstSignInTime,
        freeTierBillingProject);
  }

  private void assertUser(String primaryEmail, String contactEmail,
      String familyName, String givenName, DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime, String freeTierBillingProject) {
    User user = userDao.findUserByEmail(primaryEmail);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(contactEmail);
    assertThat(user.getFamilyName()).isEqualTo(familyName);
    assertThat(user.getGivenName()).isEqualTo(givenName);
    assertThat(user.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(user.getFirstSignInTime()).isEqualTo(firstSignInTime);
    assertThat(user.getFreeTierBillingProjectName()).isEqualTo(freeTierBillingProject);
    assertThat(user.getDataAccessLevel()).isEqualTo(dataAccessLevel);
  }

}


