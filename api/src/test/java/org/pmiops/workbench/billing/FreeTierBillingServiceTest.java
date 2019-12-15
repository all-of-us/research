package org.pmiops.workbench.billing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class FreeTierBillingServiceTest {

  private static final double DEFAULT_PERCENTAGE_TOLERANCE = 0.001;

  @Autowired BigQueryService bigQueryService;

  @Autowired FreeTierBillingService freeTierBillingService;

  @Autowired UserDao userDao;

  @Autowired NotificationService notificationService;

  @Autowired WorkspaceDao workspaceDao;

  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  private static WorkbenchConfig workbenchConfig;

  // the relevant portion of the BigQuery response for the single-project tests:
  //
  //  {
  //    "id": "aou-test-f1-26",
  //    "cost": "372.1294129999994"
  //  }

  private static final String SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-f1-26";
  private static final double SINGLE_WORKSPACE_TEST_COST = 372.1294129999994;

  @TestConfiguration
  @Import({FreeTierBillingService.class})
  @MockBean({BigQueryService.class, NotificationService.class})
  static class Configuration {

    @Bean
    @Scope("prototype")
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() throws Exception {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();

    // returns the contents of resources/bigquery/get_billing_project_costs.json
    InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("bigquery/get_billing_project_costs.ser");
    TableResult tableResult = (TableResult) (new ObjectInputStream(inputStream)).readObject();

    doReturn(tableResult).when(bigQueryService).executeQuery(any());
  }

  @Test
  public void checkFreeTierBillingUsage_singleProjectExceedsLimit() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user), any());

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE);
  }

  @Test
  public void checkFreeTierBillingUsage_disabledUserNotIgnored() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    user.setDisabled(true);
    userDao.save(user);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user), any());

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE);
  }

  @Test
  public void checkFreeTierBillingUsage_deletedWorkspaceNotIgnored() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(workspace);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user), any());

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE);
  }

  @Test
  public void checkFreeTierBillingUsage_noAlert() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 500.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE);
  }

  @Test
  public void checkFreeTierBillingUsage_workspaceMissingCreator() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 500.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(null, "rumney");

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE);
  }

  @Test
  public void checkFreeTierBillingUsage_override() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 10.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user), any());

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE);

    user.setFreeTierCreditsLimitDollarsOverride(1000.0);
    userDao.save(user);

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE);
  }

  // the relevant portion of the BigQuery response for this test:
  //
  //  {
  //    "id": "aou-test-f1-47",
  //    "cost": "397.5191679999987"
  //  },
  //  {
  //    "id": "aou-test-f1-26",
  //    "cost": "372.1294129999994"
  //  }

  @Test
  public void checkFreeTierBillingUsage_combinedProjectsExceedsLimit() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 500.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace ws1 = createWorkspace(user, "aou-test-f1-26");
    DbWorkspace ws2 = createWorkspace(user, "aou-test-f1-47");
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user), any());

    Map<Long, Double> expectedCosts = new HashMap<>();
    expectedCosts.put(ws1.getWorkspaceId(), 372.1294129999994);
    expectedCosts.put(ws2.getWorkspaceId(), 397.5191679999987);

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    for (DbWorkspace ws : Arrays.asList(ws1, ws2)) {
      // retrieve from DB again to reflect update after cron
      DbWorkspace dbWorkspace = workspaceDao.findOne(ws.getWorkspaceId());
      assertThat(dbWorkspace.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
      assertThat(dbWorkspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

      DbWorkspaceFreeTierUsage usage = workspaceFreeTierUsageDao.findOneByWorkspace(ws);
      assertThat(usage.getUser()).isEqualTo(user);
      assertThat(usage.getWorkspace()).isEqualTo(ws);

      assertWithinBillingTolerance(usage.getCost(), expectedCosts.get(ws.getWorkspaceId()));
    }
  }

  @Test
  public void checkFreeTierBillingUsage_twoUsers() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user1 = createUser("test@test.com");
    DbWorkspace ws1 = createWorkspace(user1, "aou-test-f1-26");
    DbUser user2 = createUser("more@test.com");
    DbWorkspace ws2 = createWorkspace(user2, "aou-test-f1-47");
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user1), any());
    verify(notificationService).alertUser(eq(user2), any());

    Map<Long, Double> expectedCosts = new HashMap<>();
    expectedCosts.put(ws1.getWorkspaceId(), 372.1294129999994);
    expectedCosts.put(ws2.getWorkspaceId(), 397.5191679999987);

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    for (DbWorkspace ws : Arrays.asList(ws1, ws2)) {
      // retrieve from DB again to reflect update after cron
      DbWorkspace dbWorkspace = workspaceDao.findOne(ws.getWorkspaceId());
      assertThat(dbWorkspace.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
      assertThat(dbWorkspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

      DbWorkspaceFreeTierUsage usage = workspaceFreeTierUsageDao.findOneByWorkspace(ws);
      assertThat(usage.getUser()).isEqualTo(ws.getCreator());
      assertThat(usage.getWorkspace()).isEqualTo(ws);
      assertWithinBillingTolerance(usage.getCost(), expectedCosts.get(ws.getWorkspaceId()));
    }
  }

  @Test
  public void checkFreeTierBillingUsage_ignoreOLDMigrationStatus() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.OLD);
    freeTierBillingService.checkFreeTierBillingUsage();

    verifyZeroInteractions(notificationService);
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(0);
  }

  @Test
  public void checkFreeTierBillingUsage_ignoreMIGRATEDMigrationStatus() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.MIGRATED);
    freeTierBillingService.checkFreeTierBillingUsage();

    verifyZeroInteractions(notificationService);
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(0);
  }

  @Test
  public void checkFreeTierBillingUsage_dbUpdate() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUser(eq(user), any());
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE);

    Timestamp t0 = workspaceFreeTierUsageDao.findAll().iterator().next().getLastUpdateTime();

    // time elapses, and this project incurs additional cost

    Map<String, Double> newCosts = new HashMap<>();
    newCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, SINGLE_WORKSPACE_TEST_COST * 2);
    doReturn(mockBQTableResult(newCosts)).when(bigQueryService).executeQuery(any());

    // we alert again, and the cost field is updated in the DB

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService, times(2)).alertUser(eq(user), any());

    // retrieve from DB again to reflect update after cron
    DbWorkspace dbWorkspace = workspaceDao.findOne(workspace.getWorkspaceId());
    assertThat(dbWorkspace.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
    assertThat(dbWorkspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(1);
    DbWorkspaceFreeTierUsage dbEntry = workspaceFreeTierUsageDao.findAll().iterator().next();
    assertThat(dbEntry.getUser()).isEqualTo(user);
    assertThat(dbEntry.getWorkspace()).isEqualTo(workspace);
    assertWithinBillingTolerance(dbEntry.getCost(), SINGLE_WORKSPACE_TEST_COST * 2.0);
    Timestamp t1 = dbEntry.getLastUpdateTime();
    assertThat(t1.after(t0)).isTrue();
  }

  @Test
  public void getUserFreeTierDollarLimit_default() {
    DbUser user = createUser("test@test.com");
    final double initialFreeCreditsDollarLimit = 1.0;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = initialFreeCreditsDollarLimit;
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user), initialFreeCreditsDollarLimit);

    final double fractionalFreeCreditsDollarLimit = 123.456;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = fractionalFreeCreditsDollarLimit;
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user), fractionalFreeCreditsDollarLimit);
  }

  @Test
  public void getUserFreeTierDollarLimit_override() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 123.456;

    DbUser user = createUser("test@test.com");

    final double freeTierCreditsDollarLimitOverride = 100.0;
    user.setFreeTierCreditsLimitDollarsOverride(freeTierCreditsDollarLimitOverride);
    user = userDao.save(user);
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user),
        freeTierCreditsDollarLimitOverride);

    final double doubleFreeTierCreditsDollarLimitOverrideNew = 200.0;
    user.setFreeTierCreditsLimitDollarsOverride(doubleFreeTierCreditsDollarLimitOverrideNew);
    user = userDao.save(user);

    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user),
        doubleFreeTierCreditsDollarLimitOverrideNew);
  }

  @Test
  public void getUserFreeTierDaysLimit_default() {
    DbUser user = createUser("test@test.com");
    final short initialFreeCreditsDaysLimit = 1;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = initialFreeCreditsDaysLimit;
    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(initialFreeCreditsDaysLimit);

    final short freeCreditsDaysLimitNew = 100;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = freeCreditsDaysLimitNew;
    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(freeCreditsDaysLimitNew);
  }

  @Test
  public void getUserFreeTierDaysLimit_override() {
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 123;

    DbUser user = createUser("test@test.com");

    final short freeTierCreditsDaysLimitOverride = 100;
    user.setFreeTierCreditsLimitDaysOverride(freeTierCreditsDaysLimitOverride);
    user = userDao.save(user);
    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(freeTierCreditsDaysLimitOverride);

    final short freeTierCreditsDaysLimitNew = 200;
    user.setFreeTierCreditsLimitDaysOverride(freeTierCreditsDaysLimitNew);
    user = userDao.save(user);

    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(freeTierCreditsDaysLimitNew);
  }

  @Test
  public void getUserCachedFreeTierUsage() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    DbUser user = createUser("test@test.com");
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    // we have not yet had a chance to cache this usage
    assertThat(freeTierBillingService.getUserCachedFreeTierUsage(user)).isNull();

    freeTierBillingService.checkFreeTierBillingUsage();

    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user), SINGLE_WORKSPACE_TEST_COST);

    createWorkspace(user, "another project");
    Map<String, Double> newCosts = new HashMap<>();
    newCosts.put("another project", 100.0);
    newCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 1000.0);
    doReturn(mockBQTableResult(newCosts)).when(bigQueryService).executeQuery(any());

    // we have not yet cached the new workspace costs
    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user), SINGLE_WORKSPACE_TEST_COST);

    freeTierBillingService.checkFreeTierBillingUsage();
    final double expectedTotalCachedFreeTierUsage = 1100.0;
    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user), expectedTotalCachedFreeTierUsage);

    final double user2Costs = 999.0;
    DbUser user2 = createUser("another user");
    createWorkspace(user2, "project 3");
    newCosts.put("project 3", user2Costs);
    doReturn(mockBQTableResult(newCosts)).when(bigQueryService).executeQuery(any());

    freeTierBillingService.checkFreeTierBillingUsage();

    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user), expectedTotalCachedFreeTierUsage);
    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user2), user2Costs);
  }

  private TableResult mockBQTableResult(Map<String, Double> costMap) {
    Field idField = Field.of("id", LegacySQLTypeName.STRING);
    Field costField = Field.of("cost", LegacySQLTypeName.FLOAT);
    Schema s = Schema.of(idField, costField);

    List<FieldValueList> tableRows =
        costMap.entrySet().stream()
            .map(
                e -> {
                  FieldValue id = FieldValue.of(Attribute.PRIMITIVE, e.getKey());
                  FieldValue cost = FieldValue.of(Attribute.PRIMITIVE, e.getValue().toString());
                  return FieldValueList.of(Arrays.asList(id, cost));
                })
            .collect(Collectors.toList());

    return new TableResult(s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));
  }

  private void assertSingleWorkspaceTestDbState(
      DbUser user, DbWorkspace workspaceForQuerying, BillingStatus billingStatus) {

    final DbWorkspace workspace = workspaceDao.findOne(workspaceForQuerying.getWorkspaceId());
    assertThat(workspace.getBillingStatus()).isEqualTo(billingStatus);
    assertThat(workspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(1);
    final DbWorkspaceFreeTierUsage dbEntry = workspaceFreeTierUsageDao.findAll().iterator().next();
    assertThat(dbEntry.getUser()).isEqualTo(user);
    assertThat(dbEntry.getWorkspace()).isEqualTo(workspace);
    assertWithinBillingTolerance(dbEntry.getCost(), SINGLE_WORKSPACE_TEST_COST);
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUserName(email);
    return userDao.save(user);
  }

  // we only alert/record for BillingMigrationStatus.NEW workspaces
  private DbWorkspace createWorkspace(DbUser creator, String namespace) {
    return createWorkspace(creator, namespace, BillingMigrationStatus.NEW);
  }

  private DbWorkspace createWorkspace(
      DbUser creator, String namespace, BillingMigrationStatus billingMigrationStatus) {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setCreator(creator);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setBillingMigrationStatusEnum(billingMigrationStatus);
    return workspaceDao.save(workspace);
  }

  private void assertWithinBillingTolerance(double actualValue, double expectedValue) {
    final double tolerance = DEFAULT_PERCENTAGE_TOLERANCE * expectedValue * 0.01;
    assertThat(actualValue).isWithin(tolerance).of(expectedValue);
  }
}
