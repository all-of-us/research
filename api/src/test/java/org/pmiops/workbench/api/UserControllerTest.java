package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.billing.GoogleApisConfig.END_USER_CLOUD_BILLING;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.apache.commons.collections4.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.BillingConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FeatureFlagsConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.BillingAccount;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserResponse;
import org.pmiops.workbench.model.WorkbenchListBillingAccountsResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.PaginationToken;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserControllerTest {

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();
  private static long incrementedUserId = 1;
  private static final Cloudbilling testCloudbilling = TestMockFactory.createMockedCloudbilling();

  @Mock private static FreeTierBillingService testFreeTierBillingService;

  @TestConfiguration
  @Import({
    UserController.class,
    UserServiceTestConfiguration.class,
  })
  @MockBean({
    FireCloudService.class,
    ComplianceService.class,
    DirectoryService.class,
    AdminActionHistoryDao.class,
    UserServiceAuditor.class
  })
  static class Configuration {

    @Bean
    @Scope("prototype")
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return user;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean(END_USER_CLOUD_BILLING)
    Cloudbilling getCloudBilling() {
      return testCloudbilling;
    }

    @Bean
    FreeTierBillingService getFreeTierBillingService() {
      return testFreeTierBillingService;
    }
  }

  @Autowired UserController userController;
  @Autowired UserDao userDao;
  @Autowired FireCloudService fireCloudService;

  @Before
  public void setUp() {
    config.firecloud = new WorkbenchConfig.FireCloudConfig();
    config.featureFlags = new FeatureFlagsConfig();

    saveFamily();
  }

  @Test(expected = ForbiddenException.class)
  public void testUnregistered() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(false);
    userController.user("Robinson", null, null, null).getBody();
  }

  @Test
  public void testUserSearch() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    DbUser john = userDao.findUserByUsername("john@lis.org");

    UserResponse response = userController.user("John", null, null, null).getBody();
    assertThat(response.getUsers()).hasSize(1);
    assertThat(response.getUsers().get(0).getEmail()).isSameAs(john.getUsername());
    assertThat(response.getUsers().get(0).getUserName()).isSameAs(john.getUsername());
  }

  @Test
  public void testUserPartialStringSearch() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    List<DbUser> allUsers = Lists.newArrayList(userDao.findAll());

    UserResponse response = userController.user("obin", null, null, null).getBody();

    // We only want to include users that have active billing projects to avoid users not
    // initialized in FC.
    assertThat(response.getUsers()).hasSize(5);
  }

  @Test
  public void testUserEmptyResponse() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    UserResponse response = userController.user("", null, null, null).getBody();
    assertThat(response.getUsers()).hasSize(0);
  }

  @Test
  public void testUserNoUsersResponse() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    UserResponse response = userController.user("Smith", null, null, null).getBody();
    assertThat(response.getUsers()).hasSize(0);
  }

  @Test
  public void testInvalidPageTokenCharacters() {
    ResponseEntity<UserResponse> response =
        userController.user("Robinson", "Inv@l!dT0k3n#", null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getUsers()).hasSize(0);
  }

  @Test
  public void testInvalidPageToken() {
    ResponseEntity<UserResponse> response =
        userController.user("Robinson", "eyJvZmZzZXQBhcmFtZF9", null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getUsers()).hasSize(0);
  }

  @Test
  public void testNegativePageOffset() {
    ResponseEntity<UserResponse> response =
        userController.user("Robinson", PaginationToken.of(-1).toBase64(), null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getUsers()).hasSize(0);
  }

  @Test
  public void testUserPageSize() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    int size = 1;
    UserResponse robinsons_0 =
        userController.user("Robinson", PaginationToken.of(0).toBase64(), size, null).getBody();
    UserResponse robinsons_1 =
        userController.user("Robinson", PaginationToken.of(1).toBase64(), size, null).getBody();
    UserResponse robinsons_2 =
        userController.user("Robinson", PaginationToken.of(2).toBase64(), size, null).getBody();
    UserResponse robinsons_3 =
        userController.user("Robinson", PaginationToken.of(3).toBase64(), size, null).getBody();
    UserResponse robinsons_4 =
        userController.user("Robinson", PaginationToken.of(4).toBase64(), size, null).getBody();

    assertThat(robinsons_0.getUsers()).hasSize(size);
    assertThat(robinsons_0.getNextPageToken()).isEqualTo(PaginationToken.of(1).toBase64());
    assertThat(robinsons_1.getUsers()).hasSize(size);
    assertThat(robinsons_1.getNextPageToken()).isEqualTo(PaginationToken.of(2).toBase64());
    assertThat(robinsons_2.getUsers()).hasSize(size);
    assertThat(robinsons_2.getNextPageToken()).isEqualTo(PaginationToken.of(3).toBase64());
    assertThat(robinsons_3.getUsers()).hasSize(size);
    assertThat(robinsons_3.getNextPageToken()).isEqualTo(PaginationToken.of(4).toBase64());
    assertThat(robinsons_4.getUsers()).hasSize(size);
    assertThat(robinsons_4.getNextPageToken()).isEqualTo("");
  }

  @Test
  public void testUserPagedResponses() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    UserResponse robinsons_0_1 =
        userController.user("Robinson", PaginationToken.of(0).toBase64(), 2, null).getBody();
    UserResponse robinsons_2_3 =
        userController.user("Robinson", PaginationToken.of(1).toBase64(), 2, null).getBody();
    UserResponse robinsons_4 =
        userController.user("Robinson", PaginationToken.of(3).toBase64(), 1, null).getBody();

    // Assert the expected size for each page
    assertThat(robinsons_0_1.getUsers()).hasSize(2);
    assertThat(robinsons_2_3.getUsers()).hasSize(2);
    assertThat(robinsons_4.getUsers()).hasSize(1);

    // Assert uniqueness across pages
    assertThat(robinsons_0_1.getUsers()).containsNoneOf(robinsons_2_3, robinsons_4);
    assertThat(robinsons_2_3.getUsers()).containsNoneOf(robinsons_0_1, robinsons_4);
    assertThat(robinsons_4.getUsers()).containsNoneOf(robinsons_0_1, robinsons_2_3);
  }

  @Test
  public void testUserSort() {
    when(fireCloudService.isUserMemberOfGroup(any(), any())).thenReturn(true);
    UserResponse robinsonsAsc = userController.user("Robinson", null, null, "asc").getBody();
    UserResponse robinsonsDesc = userController.user("Robinson", null, null, "desc").getBody();

    // Assert we have the same elements in both responses
    assertThat(robinsonsAsc.getUsers()).containsAllIn(robinsonsDesc.getUsers());

    // Now reverse one and assert both in the same order
    List<User> descendingReversed = Lists.reverse(robinsonsDesc.getUsers());
    assertThat(robinsonsAsc.getUsers()).containsAllIn(descendingReversed).inOrder();

    // Test that JPA sorting is really what we expected it to be by re-sorting one into a new list
    List<User> newAscending = Lists.newArrayList(robinsonsAsc.getUsers());
    newAscending.sort(Comparator.comparing(User::getUserName));
    assertThat(robinsonsAsc.getUsers()).containsAllIn(newAscending).inOrder();
  }

  // Combinatorial tests for listBillingAccounts:
  // free tier available vs. expired
  // cloud accounts available vs. none
  // enableBillingUpgrade feature flag on/off

  final BillingAccount freeTierBillingAccount =
      new BillingAccount()
          .isFreeTier(true)
          .displayName("Use All of Us free credits")
          .name("billingAccounts/free-tier")
          .isOpen(true);

  final List<com.google.api.services.cloudbilling.model.BillingAccount> cloudbillingAccounts =
      Lists.newArrayList(
          new com.google.api.services.cloudbilling.model.BillingAccount()
              .setName("googlebucks")
              .setDisplayName("Paid using your credit card"),
          new com.google.api.services.cloudbilling.model.BillingAccount()
              .setName("a2")
              .setDisplayName("Account 2 - Open")
              .setOpen(true));

  final List<BillingAccount> cloudbillingAccountsInWorkbench =
      Lists.newArrayList(
          new BillingAccount()
              .name("googlebucks")
              .displayName("Paid using your credit card")
              .isFreeTier(false)
              .isOpen(false),
          new BillingAccount()
              .name("a2")
              .displayName("Account 2 - Open")
              .isFreeTier(false)
              .isOpen(true));

  // free tier available, cloud accounts exist, billing upgrade is true

  @Test
  public void listBillingAccounts_freeYES_cloudYES_upgradeYES() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = true;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(true);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(cloudbillingAccounts));

    final List<BillingAccount> expectedWorkbenchBillingAccounts =
        ListUtils.union(
            Lists.newArrayList(freeTierBillingAccount), cloudbillingAccountsInWorkbench);

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  // free tier available, cloud accounts exist, billing upgrade is false

  @Test
  public void listBillingAccounts_freeYES_cloudYES_upgradeNO() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = false;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(true);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(cloudbillingAccounts));

    final List<BillingAccount> expectedWorkbenchBillingAccounts =
        Lists.newArrayList(freeTierBillingAccount);

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  // free tier available, no cloud accounts, billing upgrade is true

  @Test
  public void listBillingAccounts_freeYES_cloudNO_upgradeYES() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = false;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(true);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(null));

    final List<BillingAccount> expectedWorkbenchBillingAccounts =
        Lists.newArrayList(freeTierBillingAccount);

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  // free tier available, no cloud accounts, billing upgrade is false

  @Test
  public void listBillingAccounts_freeYES_cloudNO_upgradeNO() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = false;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(true);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(null));

    final List<BillingAccount> expectedWorkbenchBillingAccounts =
        Lists.newArrayList(freeTierBillingAccount);

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  // free tier expired, cloud accounts exist, billing upgrade is true

  @Test
  public void listBillingAccounts_freeNO_cloudYES_upgradeYES() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = true;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(false);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(cloudbillingAccounts));

    final List<BillingAccount> expectedWorkbenchBillingAccounts = cloudbillingAccountsInWorkbench;

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  // free tier expired, cloud accounts exist, billing upgrade is false

  @Test
  public void listBillingAccounts_freeNO_cloudYES_upgradeNO() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = false;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(false);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(cloudbillingAccounts));

    // because billing upgrade is false, the user's only option is free credits.
    // we continue to display it, but we disable the dropdown control in the UI.
    // see RW-4857

    final List<BillingAccount> expectedWorkbenchBillingAccounts =
        Lists.newArrayList(freeTierBillingAccount);

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  // free tier expired, no cloud accounts, billing upgrade is true

  @Test
  public void listBillingAccounts_freeNO_cloudNO_upgradeYES() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = true;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(false);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(null));

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEmpty();
  }

  // free tier expired, no cloud accounts, billing upgrade is false

  @Test
  public void listBillingAccounts_freeNO_cloudNO_upgradeNO() throws IOException {
    config.billing = new BillingConfig();
    config.billing.accountId = "free-tier";
    config.featureFlags.enableBillingUpgrade = false;

    when(testFreeTierBillingService.userHasRemainingFreeTierCredits(any())).thenReturn(false);

    when(testCloudbilling.billingAccounts().list().execute())
        .thenReturn(new ListBillingAccountsResponse().setBillingAccounts(null));

    // because billing upgrade is false, the user's only option is free credits.
    // we continue to display it, but we disable the dropdown control in the UI.
    // see RW-4857

    final List<BillingAccount> expectedWorkbenchBillingAccounts =
        Lists.newArrayList(freeTierBillingAccount);

    final WorkbenchListBillingAccountsResponse response =
        userController.listBillingAccounts().getBody();
    assertThat(response.getBillingAccounts()).isEqualTo(expectedWorkbenchBillingAccounts);
  }

  /*
   * Testing helpers
   */

  private void saveFamily() {
    saveUser("jill@lis.org", "Jill", "Robinson", false);
    saveUser("john@lis.org", "John", "Robinson", true);
    saveUser("judy@lis.org", "Judy", "Robinson", true);
    saveUser("maureen@lis.org", "Mauren", "Robinson", true);
    saveUser("penny@lis.org", "Penny", "Robinson", true);
    saveUser("will@lis.org", "Will", "Robinson", true);
    saveUserNotInFirecloud("bob@lis.org", "Bob", "Robinson", true);
  }

  @SuppressWarnings("SameParameterValue")
  private void saveUser(String email, String givenName, String familyName, boolean registered) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    user.setGivenName(givenName);
    user.setFamilyName(familyName);
    user.setFirstSignInTime(new Timestamp(CLOCK.instant().toEpochMilli()));
    if (registered) {
      user.setDataAccessLevel(DbStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));
    } else {
      user.setDataAccessLevel(
          DbStorageEnums.dataAccessLevelToStorage(DataAccessLevel.UNREGISTERED));
    }
    incrementedUserId++;
    userDao.save(user);
  }

  private void saveUserNotInFirecloud(
      String email, String givenName, String familyName, boolean registered) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    user.setGivenName(givenName);
    user.setFamilyName(familyName);
    if (registered) {
      user.setDataAccessLevel(DbStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));
    } else {
      user.setDataAccessLevel(
          DbStorageEnums.dataAccessLevelToStorage(DataAccessLevel.UNREGISTERED));
    }
    incrementedUserId++;
    userDao.save(user);
  }
}
