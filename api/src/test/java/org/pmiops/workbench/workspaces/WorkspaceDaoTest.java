package org.pmiops.workbench.workspaces;

import static org.springframework.test.util.AssertionErrors.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
  @Autowired WorkspaceDao workspaceDao;

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
}
