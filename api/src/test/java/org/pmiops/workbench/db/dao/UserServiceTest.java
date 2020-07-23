package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetailsV1;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserServiceTest {

  private static final String USERNAME = "abc@fake-research-aou.org";
  private static final int MOODLE_ID = 1001;

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final long TIMESTAMP_SECS = START_INSTANT.getEpochSecond();
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);
  private static final int CLOCK_INCREMENT_MILLIS = 1000;
  private static DbUser providedDbUser;
  private static WorkbenchConfig providedWorkbenchConfig;
  private DbInstitution dbInstitution;

  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private ComplianceService mockComplianceService;
  @MockBean private DirectoryService mockDirectoryService;
  @MockBean private UserServiceAuditor mockUserServiceAuditAdapter;
  @MockBean private UserTermsOfServiceDao mockUserTermsOfServiceDao;

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired private InstitutionDao institutionDao;

  @Import(UserServiceTestConfiguration.class)
  @TestConfiguration
  static class Configuration {
    @Bean
    Clock clock() {
      return PROVIDED_CLOCK;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    Random getRandom() {
      return new Random();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return providedDbUser;
    }
  }

  @Before
  public void setUp() {
    DbUser user = new DbUser();
    user.setUsername(USERNAME);
    userDao.save(user);
    providedDbUser = user;

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();

    // Since we're injecting the same static instance of this FakeClock,
    // increments and other mutations will carry across tests if we don't reset it here.
    // I tried easier ways of ensuring this, like creating new instances for every test run,
    // but it was injecting null clocks giving NPEs long after construction, and so far this
    // is the only working approach I've seen.
    PROVIDED_CLOCK.setInstant(START_INSTANT);

    dbInstitution = buildInstitution();
    final DbVerifiedInstitutionalAffiliation dbAffiliation = buildAffiliation(dbInstitution);
  }

  public DbVerifiedInstitutionalAffiliation buildAffiliation(DbInstitution dbInstitution) {
    final DbVerifiedInstitutionalAffiliation dbAffiliation = new DbVerifiedInstitutionalAffiliation();
    dbAffiliation.setVerifiedInstitutionalAffiliationId(101L);
    dbAffiliation.setInstitution(dbInstitution);
    dbAffiliation.setUser(providedDbUser);
    dbAffiliation.setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    return verifiedInstitutionalAffiliationDao.save(dbAffiliation);
  }

  public DbInstitution buildInstitution() {
    final DbInstitution dbInstitution = new DbInstitution();
    dbInstitution.setDisplayName("Disney World");
    dbInstitution.setDuaTypeEnum(DuaType.MASTER);
    dbInstitution.setOrganizationTypeEnum(OrganizationType.OTHER);
    dbInstitution.setOrganizationTypeOtherText("Happiest place on earth.");
    dbInstitution.setShortName("disney");
    return institutionDao.save(dbInstitution);
  }

  @Test
  public void testSyncComplianceTrainingStatusV1() throws Exception {
    providedWorkbenchConfig.featureFlags.enableMoodleV2Api = false;

    BadgeDetailsV1 badge = new BadgeDetailsV1();
    badge.setName("All of us badge");
    long expiry = PROVIDED_CLOCK.instant().toEpochMilli() + 100000;
    badge.setDateexpire(Long.toString(expiry));

    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(MOODLE_ID);
    when(mockComplianceService.getUserBadgeV1(MOODLE_ID))
        .thenReturn(Collections.singletonList(badge));

    userService.syncComplianceTrainingStatusV1();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(new Timestamp(START_INSTANT.toEpochMilli()));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochMilli(expiry)));

    // Completion timestamp should not change when the method is called again.
    tick();
    Timestamp completionTime = user.getComplianceTrainingCompletionTime();
    userService.syncComplianceTrainingStatusV1();
    assertThat(user.getComplianceTrainingCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testSyncComplianceTrainingStatus() throws Exception {
    providedWorkbenchConfig.featureFlags.enableMoodleV2Api = true;

    BadgeDetailsV2 retBadge = new BadgeDetailsV2();
    long expiry = PROVIDED_CLOCK.instant().getEpochSecond() + 100;
    retBadge.setDateexpire(expiry);
    retBadge.setValid(true);

    Map<String, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    userBadgesByName.put(mockComplianceService.getResearchEthicsTrainingField(), retBadge);

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochSecond(TIMESTAMP_SECS)));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochSecond(expiry)));

    // Completion timestamp should not change when the method is called again.
    tick();
    Timestamp completionTime = user.getComplianceTrainingCompletionTime();
    userService.syncComplianceTrainingStatusV2();
    assertThat(user.getComplianceTrainingCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testUpdateComplianceTrainingStatus() throws Exception {
    providedWorkbenchConfig.featureFlags.enableMoodleV2Api = true;

    BadgeDetailsV2 retBadge = new BadgeDetailsV2();
    long expiry = PROVIDED_CLOCK.instant().getEpochSecond();
    retBadge.setDateexpire(expiry);
    retBadge.setValid(true);

    Map<String, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    userBadgesByName.put(mockComplianceService.getResearchEthicsTrainingField(), retBadge);

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochSecond(TIMESTAMP_SECS)));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochSecond(expiry)));

    // Deprecate the old training.
    long newExpiry = expiry - 1;
    retBadge.setDateexpire(newExpiry);
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    userService.syncComplianceTrainingStatusV2();
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();

    // The user does a new training.
    long newerExpiry = expiry + 1;
    retBadge.setDateexpire(newerExpiry);
    retBadge.setValid(true);

    // Completion and expiry timestamp should be updated.
    userService.syncComplianceTrainingStatusV2();
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochSecond(TIMESTAMP_SECS)));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochSecond(newerExpiry)));

    // A global expiration is set.
    long globalExpiry = expiry - 1000;
    retBadge.setGlobalexpiration(globalExpiry);
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the globalexpiry timestamp passing.
    userService.syncComplianceTrainingStatusV2();
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  private void tick() {
    PROVIDED_CLOCK.increment(CLOCK_INCREMENT_MILLIS);
  }

  @Test
  public void testSyncComplianceTrainingStatusNoMoodleIdV1() throws Exception {
    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(null);
    userService.syncComplianceTrainingStatusV1();

    verify(mockComplianceService, never()).getUserBadgeV1(anyInt());
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadgeV1() throws ApiException {
    // When Moodle returns an empty badge response, we should clear the completion bit.
    DbUser user = userDao.findUserByUsername(USERNAME);
    user.setComplianceTrainingCompletionTime(new Timestamp(12345));
    userDao.save(user);

    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(1);
    when(mockComplianceService.getUserBadgeV1(1)).thenReturn(null);
    userService.syncComplianceTrainingStatusV1();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadge() throws ApiException {
    // When Moodle returns an empty RET badge response, we should clear the completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    user.setComplianceTrainingCompletionTime(new Timestamp(12345));
    userDao.save(user);

    // An empty map should be returned when we have no badge information.
    Map<String, BadgeDetailsV2> userBadgesByName = new HashMap<>();

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatusV2();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test(expected = NotFoundException.class)
  public void testSyncComplianceTrainingStatusBadgeNotFoundV1() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(MOODLE_ID);
    when(mockComplianceService.getUserBadgeV1(MOODLE_ID))
        .thenThrow(
            new org.pmiops.workbench.moodle.ApiException(
                HttpStatus.NOT_FOUND.value(), "user not found"));
    userService.syncComplianceTrainingStatusV1();
  }

  @Test(expected = NotFoundException.class)
  public void testSyncComplianceTrainingStatusBadgeNotFound() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME))
        .thenThrow(
            new org.pmiops.workbench.moodle.ApiException(
                HttpStatus.NOT_FOUND.value(), "user not found"));
    userService.syncComplianceTrainingStatusV2();
  }

  @Test
  public void testSyncComplianceTraining_SkippedForServiceAccountV1() throws ApiException {
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    userService.syncComplianceTrainingStatusV1();
    assertThat(providedDbUser.getMoodleId()).isNull();
  }

  @Test
  public void testSyncComplianceTraining_SkippedForServiceAccount() throws ApiException {
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    userService.syncComplianceTrainingStatusV2();
    assertThat(providedDbUser.getMoodleId()).isNull();
  }

  @Test
  public void testSyncEraCommonsStatus() {
    FirecloudNihStatus nihStatus = new FirecloudNihStatus();
    nihStatus.setLinkedNihUsername("nih-user");
    // FireCloud stores the NIH status in seconds, not msecs.
    final long FC_LINK_EXPIRATION_SECONDS = START_INSTANT.toEpochMilli() / 1000;
    nihStatus.setLinkExpireTime(FC_LINK_EXPIRATION_SECONDS);

    when(mockFireCloudService.getNihStatus()).thenReturn(nihStatus);

    userService.syncEraCommonsStatus();

    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(Timestamp.from(START_INSTANT));
    assertThat(user.getEraCommonsLinkExpireTime()).isEqualTo(Timestamp.from(START_INSTANT));
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo("nih-user");

    // Completion timestamp should not change when the method is called again.
    tick();
    Timestamp completionTime = user.getEraCommonsCompletionTime();
    userService.syncEraCommonsStatus();
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testClearsEraCommonsStatus() {
    DbUser testUser = userDao.findUserByUsername(USERNAME);
    // Put the test user in a state where eRA commons is completed.
    testUser.setEraCommonsCompletionTime(Timestamp.from(Instant.ofEpochSecond(TIMESTAMP_SECS)));
    testUser.setEraCommonsLinkedNihUsername("nih-user");

    //noinspection UnusedAssignment
    testUser = userDao.save(testUser);

    userService.syncEraCommonsStatus();

    DbUser retrievedUser = userDao.findUserByUsername(USERNAME);
    assertThat(retrievedUser.getEraCommonsCompletionTime()).isNull();
  }

  @Test
  public void testSyncTwoFactorAuthStatus() {
    com.google.api.services.directory.model.User googleUser =
        new com.google.api.services.directory.model.User();
    googleUser.setPrimaryEmail(USERNAME);
    googleUser.setIsEnrolledIn2Sv(true);

    when(mockDirectoryService.getUser(USERNAME)).thenReturn(googleUser);
    userService.syncTwoFactorAuthStatus();
    // twoFactorAuthCompletionTime should now be set
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNotNull();

    // twoFactorAuthCompletionTime should not change when already set
    tick();
    Timestamp twoFactorAuthCompletionTime = user.getTwoFactorAuthCompletionTime();
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getTwoFactorAuthCompletionTime()).isEqualTo(twoFactorAuthCompletionTime);

    // unset 2FA in google and check that twoFactorAuthCompletionTime is set to null
    googleUser.setIsEnrolledIn2Sv(false);
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNull();
  }

  @Test
  public void testSetBypassTimes() {
    DbUser dbUser = userDao.findUserByUsername(USERNAME);

    // Make sure we're starting with a clean slate before doing the operations and assertions
    // below. This both a sanity check against future changes to the test user initialization
    // logic that could accidentally render one of the assertions moot as well as executable
    // documentation that these fields are expected to be null by default.
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNull();
    assertThat(dbUser.getComplianceTrainingBypassTime()).isNull();
    assertThat(dbUser.getBetaAccessBypassTime()).isNull();
    assertThat(dbUser.getEraCommonsBypassTime()).isNull();
    assertThat(dbUser.getTwoFactorAuthBypassTime()).isNull();

    final Timestamp duaBypassTime = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
    userService.setDataUseAgreementBypassTime(dbUser.getUserId(), null, duaBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
            Optional.empty(),
            nullableTimestampToOptionalInstant(duaBypassTime));
    assertThat(dbUser.getDataUseAgreementBypassTime()).isEqualTo(duaBypassTime);

    userService.setDataUseAgreementBypassTime(dbUser.getUserId(), duaBypassTime, null);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
            nullableTimestampToOptionalInstant(duaBypassTime),
            Optional.empty());
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNull();

    final Timestamp complianceTrainingBypassTime =
        Timestamp.from(Instant.parse("2001-01-01T00:00:00.00Z"));
    userService.setComplianceTrainingBypassTime(
        dbUser.getUserId(), null, complianceTrainingBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
            Optional.empty(),
            nullableTimestampToOptionalInstant(complianceTrainingBypassTime));
    assertThat(dbUser.getComplianceTrainingBypassTime()).isEqualTo(complianceTrainingBypassTime);

    final Timestamp betaAccessBypassTime = Timestamp.from(Instant.parse("2002-01-01T00:00:00.00Z"));
    userService.setBetaAccessBypassTime(dbUser.getUserId(), null, betaAccessBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.BETA_ACCESS_BYPASS_TIME,
            Optional.empty(),
            nullableTimestampToOptionalInstant(betaAccessBypassTime));
    assertThat(dbUser.getBetaAccessBypassTime()).isEqualTo(betaAccessBypassTime);

    final Timestamp eraCommonsBypassTime = Timestamp.from(Instant.parse("2003-01-01T00:00:00.00Z"));
    userService.setEraCommonsBypassTime(dbUser.getUserId(), null, eraCommonsBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME,
            Optional.empty(),
            nullableTimestampToOptionalInstant(eraCommonsBypassTime));
    assertThat(dbUser.getEraCommonsBypassTime()).isEqualTo(eraCommonsBypassTime);

    final Timestamp twoFactorBypassTime = Timestamp.from(Instant.parse("2004-01-01T00:00:00.00Z"));
    userService.setTwoFactorAuthBypassTime(dbUser.getUserId(), null, twoFactorBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
            Optional.empty(),
            nullableTimestampToOptionalInstant(twoFactorBypassTime));
    assertThat(dbUser.getTwoFactorAuthBypassTime()).isEqualTo(twoFactorBypassTime);
  }

  private Optional<Instant> nullableTimestampToOptionalInstant(
      @Nullable Timestamp complianceTrainingBypassTime) {
    return Optional.ofNullable(complianceTrainingBypassTime).map(Timestamp::toInstant);
  }

  @Test
  public void testSubmitTermsOfService() {
    userService.submitTermsOfService(userDao.findUserByUsername(USERNAME), /* tosVersion */ 1);

    verify(mockUserTermsOfServiceDao).save(any(DbUserTermsOfService.class));
    verify(mockUserServiceAuditAdapter).fireAcknowledgeTermsOfService(any(DbUser.class), eq(1));
  }

  @Test
  public void testAffiliationField() {
    final DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getVerifiedInstitutionalAffiliation().getInstitution())
        .isEqualTo(dbInstitution);
  }
}
