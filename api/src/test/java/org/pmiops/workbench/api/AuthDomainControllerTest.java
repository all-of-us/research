package org.pmiops.workbench.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Provider;
import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class AuthDomainControllerTest {

  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";

  @Mock
  private AdminActionHistoryDao adminActionHistoryDao;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private Provider<User> userProvider;
  @Mock
  private ComplianceService complianceService;
  @Autowired
  private UserDao userDao;
  
  private AuthDomainController authDomainController;

  @Before
  public void setUp() {
    User adminUser = new User();
    adminUser.setUserId(0L);
    doNothing().when(fireCloudService).addUserToBillingProject(any(), any());
    doNothing().when(fireCloudService).removeUserFromBillingProject(any(), any());
    when(fireCloudService.createGroup(any())).thenReturn(new ManagedGroupWithMembers());
    when(userProvider.get()).thenReturn(adminUser);
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new WorkbenchConfig.FireCloudConfig();
    config.firecloud.registeredDomainName = "";
    config.access = new WorkbenchConfig.AccessConfig();
    config.access.enableDataUseAgreement = true;
    FakeClock clock = new FakeClock(Instant.now());
    UserService userService = new UserService(userProvider, userDao, adminActionHistoryDao, clock,
        new FakeLongRandom(12345), fireCloudService, Providers.of(config),
        complianceService);
    this.authDomainController = new AuthDomainController(fireCloudService, userService, userDao);
  }

  @Test
  public void testCreateAuthDomain() {
    ResponseEntity<EmptyResponse> response = this.authDomainController.createAuthDomain("");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testDisableUser() {
    createUser(false);
    UpdateUserDisabledRequest request = new UpdateUserDisabledRequest().
        email(PRIMARY_EMAIL).
        disabled(true);
    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    User updatedUser = userDao.findUserByEmail(PRIMARY_EMAIL);
    assertThat(updatedUser.getDisabled());
  }

  @Test
  public void testEnableUser() {
    createUser(true);
    UpdateUserDisabledRequest request = new UpdateUserDisabledRequest().
        email(PRIMARY_EMAIL).
        disabled(false);
    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    User updatedUser = userDao.findUserByEmail(PRIMARY_EMAIL);
    assertThat(!updatedUser.getDisabled());
  }

  private void createUser(boolean disabled) {
    User user = new User();
    user.setGivenName(GIVEN_NAME);
    user.setFamilyName(FAMILY_NAME);
    user.setEmail(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setOrganization(ORGANIZATION);
    user.setCurrentPosition(CURRENT_POSITION);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(disabled);
    userDao.save(user);
  }

}
