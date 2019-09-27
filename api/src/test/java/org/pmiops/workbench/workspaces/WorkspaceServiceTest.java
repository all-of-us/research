package org.pmiops.workbench.workspaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceACL;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceServiceTest {
  public static String workspaceNamespace = "namespace";

  @TestConfiguration
  @Import({WorkspaceMapper.class})
  static class Configuration {}

  @Mock private CohortCloningService cohortCloningService;
  @Mock private ConceptSetService conceptSetService;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;
  @Mock private Provider<User> userProvider;
  @Autowired private UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired private WorkspaceMapper workspaceMapper;
  @Mock private FireCloudService fireCloudService;
  @Mock private Clock clock;

  private WorkspaceService workspaceService;

  private List<WorkspaceResponse> workspaceResponses = new ArrayList<>();
  private List<org.pmiops.workbench.db.model.Workspace> workspaces = new ArrayList<>();
  private AtomicLong workspaceIdIncrementer = new AtomicLong(1);
  private Instant NOW = Instant.now();
  private long USER_ID = 1L;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workspaceService =
        new WorkspaceServiceImpl(
            clock,
            cohortCloningService,
            conceptSetService,
            fireCloudService,
            userDao,
            userProvider,
            userRecentWorkspaceDao,
            workspaceDao,
            workspaceMapper);

    workspaceResponses.clear();
    workspaces.clear();
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "reader",
        WorkspaceAccessLevel.READER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "writer",
        WorkspaceAccessLevel.WRITER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "owner",
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "extra",
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "another_extra",
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);

    doReturn(workspaceResponses).when(fireCloudService).getWorkspaces();
  }

  private WorkspaceResponse mockFirecloudWorkspaceResponse(
      String workspaceId, String workspaceName, WorkspaceAccessLevel accessLevel) {
    Workspace workspace = mock(Workspace.class);
    doReturn(workspaceNamespace).when(workspace).getNamespace();
    doReturn(workspaceName).when(workspace).getName();
    doReturn(workspaceId).when(workspace).getWorkspaceId();
    WorkspaceResponse workspaceResponse = mock(WorkspaceResponse.class);
    doReturn(workspace).when(workspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(workspaceResponse).getAccessLevel();
    return workspaceResponse;
  }

  private org.pmiops.workbench.db.model.Workspace buildDbWorkspace(
      long id, String name, WorkspaceActiveStatus activeStatus) {
    org.pmiops.workbench.db.model.Workspace workspace =
        new org.pmiops.workbench.db.model.Workspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    workspace.setLastModifiedTime(nowTimestamp);
    workspace.setCreationTime(nowTimestamp);
    workspace.setName(name);
    workspace.setWorkspaceId(id);
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    workspace.setFirecloudName(name);
    workspace.setFirecloudUuid(Long.toString(id));
    return workspace;
  }

  private void addMockedWorkspace(
      long workspaceId,
      String workspaceName,
      WorkspaceAccessLevel accessLevel,
      WorkspaceActiveStatus activeStatus) {

    WorkspaceACL workspaceAccessLevelResponse = spy(WorkspaceACL.class);
    HashMap<String, WorkspaceAccessEntry> acl = spy(HashMap.class);
    WorkspaceAccessEntry accessLevelEntry =
        new WorkspaceAccessEntry().accessLevel(accessLevel.toString());
    doReturn(acl).when(workspaceAccessLevelResponse).getAcl();
    doReturn(accessLevelEntry).when(acl).get(anyString());
    workspaceAccessLevelResponse.setAcl(acl);
    WorkspaceResponse workspaceResponse =
        mockFirecloudWorkspaceResponse(Long.toString(workspaceId), workspaceName, accessLevel);
    doReturn(workspaceAccessLevelResponse)
        .when(fireCloudService)
        .getWorkspaceAcl(workspaceNamespace, workspaceName);
    workspaceResponses.add(workspaceResponse);

    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                workspaceId, workspaceResponse.getWorkspace().getName(), activeStatus));

    workspaces.add(dbWorkspace);
  }

  @Test
  public void getWorkspaces() {
    assertThat(workspaceService.getWorkspaces()).hasSize(5);
  }

  @Test
  public void getWorkspaces_skipPending() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "inactive",
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void getWorkspaces_skipDeleted() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "deleted",
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.DELETED);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void activeStatus() {
    EnumSet.allOf(WorkspaceActiveStatus.class)
        .forEach(
            status ->
                assertThat(
                        buildDbWorkspace(workspaceIdIncrementer.getAndIncrement(), "1", status)
                            .getWorkspaceActiveStatusEnum())
                    .isEqualTo(status));
  }

  @Test
  public void updateRecentWorkspaces() {
    workspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          workspaceService.updateRecentWorkspaces(
              workspace.getWorkspaceId(),
              USER_ID,
              Timestamp.from(NOW.minusSeconds(workspaces.size() - workspace.getWorkspaceId())));
        });
    List<UserRecentWorkspace> recentWorkspaces =
        workspaceService.getRecentWorkspacesByUser(USER_ID);
    assertThat(recentWorkspaces.size()).isEqualTo(WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT);
    assertThat(
            recentWorkspaces.stream()
                .map(UserRecentWorkspace::getWorkspaceId)
                .collect(Collectors.toList()))
        .containsAll(
            workspaces
                .subList(
                    workspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                    workspaces.size())
                .stream()
                .map(org.pmiops.workbench.db.model.Workspace::getWorkspaceId)
                .collect(Collectors.toList()));
  }
}
