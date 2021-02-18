package org.pmiops.workbench.db.jdbc;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test the unique ReportingNativeQueryService, which bypasses Spring in favor of low-level JDBC
 * queries. This means we need real DAOs.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingQueryServiceTest {

  public static final int BATCH_SIZE = 2;
  @Autowired private ReportingQueryService reportingQueryService;

  // It's necessary to bring in several Dao classes, since we aim to populate join tables
  // that have neither entities of their own nor stand-alone DAOs.
  @Autowired private CdrVersionDao cCdrVersionDao;
  @Autowired private CohortDao cohortDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("REPORTING_USER_TEST_FIXTURE")
  ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  @Import({ReportingQueryServiceImpl.class, ReportingUserFixture.class, ReportingTestConfig.class})
  @TestConfiguration
  public static class config {}

  @Before
  public void setup() {}

  @Test
  public void testGetReportingDatasetCohorts() {
    final DbUser user1 = createDbUser();
    final DbCdrVersion cdrVersion1 = createCdrVersion();
    final DbWorkspace workspace1 = createDbWorkspace(user1, cdrVersion1);
    final DbCohort cohort1 = createCohort(user1, workspace1);
    final DbDataset dataset1 = createDataset(workspace1, cohort1);
    entityManager.flush();

    final List<ReportingDatasetCohort> datasetCohorts = reportingQueryService.getDatasetCohorts();
    assertThat(datasetCohorts).hasSize(1);
    assertThat(datasetCohorts.get(0).getCohortId()).isEqualTo(cohort1.getCohortId());
    assertThat(datasetCohorts.get(0).getDatasetId()).isEqualTo(dataset1.getDataSetId());
  }

  @NotNull
  @Transactional
  public DbDataset createDataset(DbWorkspace workspace1, DbCohort cohort1) {
    DbDataset dataset1 = ReportingTestUtils.createDbDataset(workspace1.getWorkspaceId());
    dataset1.setCohortIds(ImmutableList.of(cohort1.getCohortId()));
    dataset1 = dataSetDao.save(dataset1);
    assertThat(dataSetDao.count()).isEqualTo(1);
    assertThat(dataset1.getCohortIds()).containsExactly(cohort1.getCohortId());
    cohortDao.save(cohort1);
    return dataset1;
  }

  @Transactional
  public DbCohort createCohort(DbUser user1, DbWorkspace workspace1) {
    final DbCohort cohort1 = cohortDao.save(ReportingTestUtils.createDbCohort(user1, workspace1));
    assertThat(cohortDao.count()).isEqualTo(1);
    assertThat(reportingQueryService.getDatasetCohorts()).isEmpty();
    return cohort1;
  }

  @Transactional
  public DbWorkspace createDbWorkspace(DbUser user1, DbCdrVersion cdrVersion1) {
    final long initialWorkspaceCount = workspaceDao.count();
    final DbWorkspace workspace1 =
        workspaceDao.save(
            ReportingTestUtils.createDbWorkspace(user1, cdrVersion1)); // save cdr version too
    assertThat(workspaceDao.count()).isEqualTo(initialWorkspaceCount + 1);
    return workspace1;
  }

  @Transactional
  public DbCdrVersion createCdrVersion() {
    DbCdrVersion cdrVersion1 = new DbCdrVersion();
    cdrVersion1.setName("foo");
    cdrVersion1 = cCdrVersionDao.save(cdrVersion1);
    assertThat(cCdrVersionDao.count()).isEqualTo(1);
    return cdrVersion1;
  }

  @Transactional
  public DbUser createDbUser() {
    int currentSize = userDao.findUsers().size();
    final DbUser user1 = userDao.save(userFixture.createEntity());
    assertThat(userDao.count()).isEqualTo(currentSize + 1);

    DbUser user2 = userDao.findAll().iterator().next();
    System.out.println("~~~~~~USER");
    System.out.println(user2.getDemographicSurvey().getUser());
    System.out.println(user2.getDemographicSurvey().getEducationEnum());
    System.out.println(user2.getDemographicSurvey().getEducation());
    System.out.println(user2.getDemographicSurvey().getDisability());
    System.out.println(user2.getDemographicSurvey().getEthnicityEnum());
    return user1;
  }

  @Test
  public void testWorkspaceIterator_oneEntry() {
    final DbUser user = createDbUser();
    final DbCdrVersion cdrVersion = createCdrVersion();
    final DbWorkspace workspace = createDbWorkspace(user, cdrVersion);

    final Iterator<List<ReportingWorkspace>> iterator =
        reportingQueryService.getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    List<ReportingWorkspace> firstBatch = iterator.next();
    assertThat(firstBatch).hasSize(1);
    assertThat(firstBatch.get(0).getName()).isEqualTo(workspace.getName());
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testWorkspaceIterator_noEntries() {
    final Iterator<List<ReportingWorkspace>> iterator =
        reportingQueryService.getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testWorkspaceIIterator_twoAndAHalfBatches() {
    createWorkspaces(5);

    final Iterator<List<ReportingWorkspace>> iterator =
        reportingQueryService.getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    final List<ReportingWorkspace> batch1 = iterator.next();
    assertThat(batch1).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingWorkspace> batch2 = iterator.next();
    assertThat(batch2).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingWorkspace> batch3 = iterator.next();
    assertThat(batch3).hasSize(1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testIteratorStream() {
    final int numWorkspaces = 5;
    createWorkspaces(numWorkspaces);

    final int totalRows = reportingQueryService.getWorkspacesStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(numWorkspaces);

    final long totalBatches = reportingQueryService.getWorkspacesStream().count();
    assertThat(totalBatches).isEqualTo((long) Math.ceil(1.0 * numWorkspaces / BATCH_SIZE));

    // verify that we get all of them and they're distinct in terms of their PKs
    final Set<Long> ids =
        reportingQueryService
            .getWorkspacesStream()
            .flatMap(List::stream)
            .map(ReportingWorkspace::getWorkspaceId)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(ids).hasSize(numWorkspaces);
  }

  @Test
  public void testEmptyStream() {
    workspaceDao.deleteAll();
    final int totalRows = reportingQueryService.getWorkspacesStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(0);

    final long totalBatches = reportingQueryService.getWorkspacesStream().count();
    assertThat(totalBatches).isEqualTo(0);
  }

  @Test
  public void testWorkspaceCount() {
    createWorkspaces(5);
    assertThat(reportingQueryService.getWorkspacesCount()).isEqualTo(5);
  }

  @Test
  public void testUserIterator_twoAndAHalfBatches() {
    createUsers(5);

    final Iterator<List<ReportingUser>> iterator = reportingQueryService.getUserBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    final List<ReportingUser> batch1 = iterator.next();
    assertThat(batch1).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingUser> batch2 = iterator.next();
    assertThat(batch2).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingUser> batch3 = iterator.next();
    assertThat(batch3).hasSize(1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testQueryUser() {
    createUsers(1);

    final List<List<ReportingUser>> stream =
        reportingQueryService.getUserStream().collect(Collectors.toList());
    assertThat(stream.size()).isEqualTo(1);
    ReportingUser reportingUser = stream.stream().findFirst().get().get(0);
    System.out.println(reportingUser);
  }

  @Test
  public void testUserStream_twoAndAHalfBatches() {
    createUsers(5);

    final List<List<ReportingUser>> stream =
        reportingQueryService.getUserStream().collect(Collectors.toList());
    assertThat(stream.size()).isEqualTo(3);
  }

  @Test
  public void testUserCount() {
    createUsers(3);
    assertThat(reportingQueryService.getUserCount()).isEqualTo(3);
  }

  private void createWorkspaces(int count) {
    final DbUser user = createDbUser();
    final DbCdrVersion cdrVersion = createCdrVersion();
    for (int i = 0; i < count; ++i) {
      createDbWorkspace(user, cdrVersion);
    }
    entityManager.flush();
  }

  private void createUsers(int count) {
    for (int i = 0; i < count; ++i) {
      createDbUser();
    }
    entityManager.flush();
  }
}
