package org.pmiops.workbench.db.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({LiquibaseAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class UserServiceTest {

  private final String EMAIL_ADDRESS = "abc@fake-research-aou.org";

  private Long incrementedUserId = 1L;

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final long TIMESTAMP_MSECS = 100;
  private static final FakeClock CLOCK = new FakeClock(Instant.ofEpochMilli(TIMESTAMP_MSECS));

  @Autowired
  private UserDao userDao;
  @Mock
  private AdminActionHistoryDao adminActionHistoryDao;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private ComplianceService complianceService;
  @Mock
  private DirectoryService directoryService;

  private UserService userService;

  private User testUser;
  private com.google.api.services.admin.directory.model.User googleUser;


  @Before
  public void setUp() {
    Provider<WorkbenchConfig> configProvider = Providers.of(WorkbenchConfig.createEmptyConfig());
    testUser = insertUser(EMAIL_ADDRESS);

    userService = new UserService(Providers.of(testUser), userDao, adminActionHistoryDao, CLOCK,
        new Random(), fireCloudService, configProvider, complianceService, directoryService);

    googleUser = new com.google.api.services.admin.directory.model.User();
    googleUser.setPrimaryEmail(EMAIL_ADDRESS);
    googleUser.setChangePasswordAtNextLogin(true);
    googleUser.setPassword("testPassword");
    googleUser.setIsEnrolledIn2Sv(true);
  }

  private User insertUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setUserId(incrementedUserId);
    incrementedUserId++;
    userDao.save(user);
    return user;
  }

  @Test
  public void testSyncComplianceTrainingStatus() throws Exception {
    BadgeDetails badge = new BadgeDetails();
    badge.setName("All of us badge");
    badge.setDateexpire("12345");

    when(complianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(1);
    when(complianceService.getUserBadge(1)).thenReturn(Arrays.asList(badge));

    userService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion and expiration time.
    User user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getComplianceTrainingCompletionTime()).isEqualTo(
        new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getComplianceTrainingExpirationTime()).isEqualTo(
        new Timestamp(12345));
  }

  @Test
  public void testSyncComplianceTrainingStatusNoMoodleId() throws Exception {
    when(complianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(null);
    userService.syncComplianceTrainingStatus();

    verify(complianceService, never()).getUserBadge(anyInt());
    User user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadge() throws Exception {
    // When Moodle returns an empty badge response, we should clear the completion bit.
    User user = userDao.findUserByEmail(EMAIL_ADDRESS);
    user.setComplianceTrainingCompletionTime(new Timestamp(12345));
    userDao.save(user);

    when(complianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(1);
    when(complianceService.getUserBadge(1)).thenReturn(null);
    userService.syncComplianceTrainingStatus();
    user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test(expected = NotFoundException.class)
  public void testSyncComplianceTrainingStatusBadgeNotFound() throws Exception {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(complianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(1);
    when(complianceService.getUserBadge(1))
        .thenThrow(new org.pmiops.workbench.moodle.ApiException
            (HttpStatus.NOT_FOUND.value(), "user not found"));
    userService.syncComplianceTrainingStatus();
  }

  @Test
  public void testSyncEraCommonsStatus() throws Exception {
    NihStatus nihStatus = new NihStatus();
    nihStatus.setLinkedNihUsername("nih-user");
    nihStatus.setLinkExpireTime(TIMESTAMP_MSECS + 1);

    when(fireCloudService.getNihStatus()).thenReturn(nihStatus);

    userService.syncEraCommonsStatus();

    User user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(
        new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getEraCommonsLinkExpireTime()).isEqualTo(
        new Timestamp(TIMESTAMP_MSECS + 1));
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo("nih-user");
  }

  @Test
  public void testClearsEraCommonsStatus() throws Exception {
    // Put the test user in a state where eRA commons is completed.
    testUser.setEraCommonsCompletionTime(new Timestamp(TIMESTAMP_MSECS));
    testUser.setEraCommonsLinkedNihUsername("nih-user");
    userDao.save(testUser);

    // API returns a null value.
    when(fireCloudService.getNihStatus()).thenReturn(null);

    userService.syncEraCommonsStatus();

    User user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getEraCommonsCompletionTime()).isNull();
  }

  @Test
  public void testSyncTwoFactorAuthStatus() throws Exception {
    when(directoryService.getUser(EMAIL_ADDRESS)).thenReturn(googleUser);
    userService.syncTwoFactorAuthStatus();
    // 2FA completion time should now be set
    user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNotNull();
  }

}
