package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.adapters.UserServiceAuditAdapter;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({LiquibaseAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserServiceTest {

  private static final String USER_EMAIL = "abc@fake-research-aou.org";
  private final String EMAIL_ADDRESS = "abc@fake-research-aou.org";

  private Long incrementedUserId = 1L;

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final long TIMESTAMP_MSECS = 100;
  private static final FakeClock CLOCK = new FakeClock();
  private static DbUser dbUser;

  @Autowired private AdminActionHistoryDao mockAdminActionHistoryDao;
  @Autowired private FireCloudService mockFireCloudService;
  @Autowired private ComplianceService mockComplianceService;
  @Autowired private DirectoryService mockDirectoryService;
  @Autowired private UserServiceAuditAdapter mockUserServiceAuditAdapter;

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Autowired private UserDataUseAgreementDao userDataUseAgreementDao;

  private DbUser testUser;

  @TestConfiguration
  @Import({
      UserService.class
  })
  @MockBean({
      AdminActionHistoryDao.class,
      FireCloudService.class,
      ComplianceService.class,
      DirectoryService.class,
      UserServiceAuditAdapter.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    WorkbenchConfig getWorkbenchConfig() {
      return WorkbenchConfig.createEmptyConfig();
    }

    @Bean
    Random getRandom() {
      return new Random();
    }

    @Bean
    DbUser getDbUser() {
      return dbUser;
    }

  }

  @Before
  public void setUp() {
    CLOCK.setInstant(Instant.ofEpochMilli(TIMESTAMP_MSECS));
//    Provider<WorkbenchConfig> configProvider = Providers.of(WorkbenchConfig.createEmptyConfig());
    testUser = insertUser();
    dbUser = insertUser();
//    userService =
//        new UserService(
//            Providers.of(testUser),
//            userDao,
//            mockAdminActionHistoryDao,
//            userDataUseAgreementDao,
//            CLOCK,
//            new Random(),
//            mockFireCloudService,
//            configProvider,
//            mockComplianceService,
//            mockDirectoryService,
//            mockUserServiceAuditAdapter);
  }

  private DbUser insertUser() {
    DbUser user = new DbUser();
    user.setEmail(USER_EMAIL);
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

    when(mockComplianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(1);
    when(mockComplianceService.getUserBadge(1)).thenReturn(Arrays.asList(badge));

    userService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getComplianceTrainingExpirationTime()).isEqualTo(new Timestamp(12345));

    // Completion timestamp should not change when the method is called again.
    CLOCK.increment(1000);
    Timestamp completionTime = user.getComplianceTrainingCompletionTime();
    userService.syncComplianceTrainingStatus();
    assertThat(user.getComplianceTrainingCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testSyncComplianceTrainingStatusNoMoodleId() throws Exception {
    when(mockComplianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(null);
    userService.syncComplianceTrainingStatus();

    verify(mockComplianceService, never()).getUserBadge(anyInt());
    DbUser user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadge() throws Exception {
    // When Moodle returns an empty badge response, we should clear the completion bit.
    DbUser user = userDao.findUserByEmail(EMAIL_ADDRESS);
    user.setComplianceTrainingCompletionTime(new Timestamp(12345));
    userDao.save(user);

    when(mockComplianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(1);
    when(mockComplianceService.getUserBadge(1)).thenReturn(null);
    userService.syncComplianceTrainingStatus();
    user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test(expected = NotFoundException.class)
  public void testSyncComplianceTrainingStatusBadgeNotFound() throws Exception {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockComplianceService.getMoodleId(EMAIL_ADDRESS)).thenReturn(1);
    when(mockComplianceService.getUserBadge(1))
        .thenThrow(
            new org.pmiops.workbench.moodle.ApiException(
                HttpStatus.NOT_FOUND.value(), "user not found"));
    userService.syncComplianceTrainingStatus();
  }

  @Test
  public void testSyncEraCommonsStatus() {
    NihStatus nihStatus = new NihStatus();
    nihStatus.setLinkedNihUsername("nih-user");
    // FireCloud stores the NIH status in seconds, not msecs.
    nihStatus.setLinkExpireTime(TIMESTAMP_MSECS / 1000);

    when(mockFireCloudService.getNihStatus()).thenReturn(nihStatus);

    userService.syncEraCommonsStatus();

    DbUser user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getEraCommonsLinkExpireTime()).isEqualTo(new Timestamp(TIMESTAMP_MSECS / 1000));
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo("nih-user");

    // Completion timestamp should not change when the method is called again.
    CLOCK.increment(1000);
    Timestamp completionTime = user.getEraCommonsCompletionTime();
    userService.syncEraCommonsStatus();
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testClearsEraCommonsStatus() throws Exception {
    // Put the test user in a state where eRA commons is completed.
    testUser.setEraCommonsCompletionTime(new Timestamp(TIMESTAMP_MSECS));
    testUser.setEraCommonsLinkedNihUsername("nih-user");
    userDao.save(testUser);

    // API returns a null value.
    when(mockFireCloudService.getNihStatus()).thenReturn(null);

    userService.syncEraCommonsStatus();

    DbUser user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getEraCommonsCompletionTime()).isNull();
  }

  @Test
  public void testSyncTwoFactorAuthStatus() throws Exception {
    com.google.api.services.directory.model.User googleUser =
        new com.google.api.services.directory.model.User();
    googleUser.setPrimaryEmail(EMAIL_ADDRESS);
    googleUser.setIsEnrolledIn2Sv(true);

    when(mockDirectoryService.getUser(EMAIL_ADDRESS)).thenReturn(googleUser);
    userService.syncTwoFactorAuthStatus();
    // twoFactorAuthCompletionTime should now be set
    DbUser user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNotNull();

    // twoFactorAuthCompletionTime should not change when already set
    CLOCK.increment(1000);
    Timestamp twoFactorAuthCompletionTime = user.getTwoFactorAuthCompletionTime();
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getTwoFactorAuthCompletionTime()).isEqualTo(twoFactorAuthCompletionTime);

    // unset 2FA in google and check that twoFactorAuthCompletionTime is set to null
    googleUser.setIsEnrolledIn2Sv(false);
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNull();
  }

  @Test
  public void testSetBypassTimes() {
    DbUser dbUser = userDao.findUserByEmail(EMAIL_ADDRESS);
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNull();
    assertThat(dbUser.getComplianceTrainingBypassTime()).isNull();
    assertThat(dbUser.getBetaAccessBypassTime()).isNull();
    assertThat(dbUser.getEraCommonsBypassTime()).isNull();
    assertThat(dbUser.getTwoFactorAuthBypassTime()).isNull();

    final Timestamp duaBypassTime = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
    userService.setDataUseAgreementBypassTime(dbUser.getUserId(), duaBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
            nullableTimestampToOptionalInstant(duaBypassTime));
    assertThat(dbUser.getDataUseAgreementBypassTime()).isEqualTo(duaBypassTime);

    userService.setDataUseAgreementBypassTime(dbUser.getUserId(), null);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
            Optional.empty());
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNull();

    final Timestamp complianceTrainingBypassTime =
        Timestamp.from(Instant.parse("2001-01-01T00:00:00.00Z"));
    userService.setComplianceTrainingBypassTime(dbUser.getUserId(), complianceTrainingBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
            nullableTimestampToOptionalInstant(complianceTrainingBypassTime));
    assertThat(dbUser.getComplianceTrainingBypassTime()).isEqualTo(complianceTrainingBypassTime);

    final Timestamp betaAccessBypassTime = Timestamp.from(Instant.parse("2002-01-01T00:00:00.00Z"));
    userService.setBetaAccessBypassTime(dbUser.getUserId(), betaAccessBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.BETA_ACCESS_BYPASS_TIME,
            nullableTimestampToOptionalInstant(betaAccessBypassTime));
    assertThat(dbUser.getBetaAccessBypassTime()).isEqualTo(betaAccessBypassTime);

    final Timestamp eraCommonsBypassTime = Timestamp.from(Instant.parse("2003-01-01T00:00:00.00Z"));
    userService.setEraCommonsBypassTime(dbUser.getUserId(), eraCommonsBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME,
            nullableTimestampToOptionalInstant(eraCommonsBypassTime));
    assertThat(dbUser.getEraCommonsBypassTime()).isEqualTo(eraCommonsBypassTime);

    final Timestamp twoFactorBypassTime = Timestamp.from(Instant.parse("2004-01-01T00:00:00.00Z"));
    userService.setTwoFactorAuthBypassTime(dbUser.getUserId(), twoFactorBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
            nullableTimestampToOptionalInstant(twoFactorBypassTime));
    assertThat(dbUser.getTwoFactorAuthBypassTime()).isEqualTo(twoFactorBypassTime);
  }

  private Optional<Instant> nullableTimestampToOptionalInstant(
      Timestamp complianceTrainingBypassTime) {
    return Optional.ofNullable(complianceTrainingBypassTime).map(Timestamp::toInstant);
  }
}
