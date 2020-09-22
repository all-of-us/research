package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.springframework.test.util.AssertionErrors.fail;

import java.time.Clock;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.ReportingMapper;
import org.pmiops.workbench.reporting.ReportingMapperImpl;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkspaceDaoTest {
  private static final String WORKSPACE_1_NAME = "Foo";
  private static final String WORKSPACE_NAMESPACE = "aou-1";

  @Autowired WorkspaceDao workspaceDao;
  @Autowired ReportingMapper reportingMapper;

  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired UserDao userDao;

  @TestConfiguration
  @Import({CommonMappers.class, ReportingMapperImpl.class, ReportingTestConfig.class})
  @MockBean({Clock.class})
  public static class conifg {}

  @Test
  public void testWorkspaceVersionLocking() {
    DbWorkspace ws = new DbWorkspace();
    ws.setVersion(1);
    ws = workspaceDao.save(ws);

    // Version incremented to 2.
    ws.setName("foo");
    ws = workspaceDao.save(ws);

    try {
      ws.setName("bar");
      ws.setVersion(1);
      workspaceDao.save(ws);
      fail("expected optimistic lock exception on stale version update");
    } catch (ObjectOptimisticLockingFailureException e) {
      // expected
    }
  }

  private DbWorkspace createWorkspace() {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setVersion(1);
    workspace.setName(WORKSPACE_1_NAME);
    workspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace = workspaceDao.save(workspace);
    return workspace;
  }

  @Test
  public void testGetReportingWorkspaces() {
    final DbCdrVersion cdrVersion = getDbCdrVersion();

    final DbUser creator = getDbUser();

    final DbWorkspace dbWorkspace =
        workspaceDao.save(ReportingTestUtils.createDbWorkspace(creator, cdrVersion));
    final List<ProjectedReportingWorkspace> workspaces = workspaceDao.getReportingWorkspaces();

    assertThat(workspaces).hasSize(1);
    ReportingTestUtils.assertPrjWorkspaceFields(
        workspaces.get(0),
        dbWorkspace.getWorkspaceId(),
        cdrVersion.getCdrVersionId(),
        creator.getUserId());

    // check that the projection built by the DAO satisfies the mapper. Very frequently, projection
    // issues show up in the mapper first (though the assertions above *should* be sufficient.
    final ReportingWorkspace dtoWorkspace = reportingMapper.toDto(workspaces.get(0));
    ReportingTestUtils.assertDtoWorkspaceFields(
        dtoWorkspace,
        dbWorkspace.getWorkspaceId(),
        cdrVersion.getCdrVersionId(),
        creator.getUserId());
  }

  public DbUser getDbUser() {
    DbUser user = new DbUser();
    user.setGivenName("Jay");
    user = userDao.save(user);
    return user;
  }

  public DbCdrVersion getDbCdrVersion() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrDbName("foo");
    cdrVersion = cdrVersionDao.save(cdrVersion);
    return cdrVersion;
  }
}
