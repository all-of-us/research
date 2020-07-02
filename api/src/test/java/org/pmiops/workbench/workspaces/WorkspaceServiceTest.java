package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cohortreview.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.profile.ProfileMapper;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceServiceTest {

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    ConceptSetMapperImpl.class,
    CommonMappers.class,
    DataSetMapperImpl.class,
    WorkspaceMapperImpl.class,
    WorkspaceServiceImpl.class
  })
  @MockBean({
    ConceptSetService.class,
    CohortService.class,
    CohortCloningService.class,
    DataSetService.class,
    FirecloudMapper.class,
    FireCloudService.class,
    ProfileMapper.class,
    UserDao.class,
    FreeTierBillingService.class,
    UserMapper.class
  })
  static class Configuration {
    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.billing = new WorkbenchConfig.BillingConfig();
      workbenchConfig.billing.accountId = "free-tier-account";
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.enableBillingLockout = true;
      return workbenchConfig;
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }
  }

  @MockBean private Clock mockClock;
  @MockBean private FireCloudService mockFireCloudService;

  @Autowired private Provider<DbUser> userProvider;
  @Autowired private UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;

  private static DbUser currentUser;

  private final List<FirecloudWorkspaceResponse> firecloudWorkspaceResponses = new ArrayList<>();
  private final List<DbWorkspace> dbWorkspaces = new ArrayList<>();
  private static final Instant NOW = Instant.parse("1985-11-05T22:04:00.00Z");
  private static final long USER_ID = 1L;
  private static final String DEFAULT_USERNAME = "mock@mock.com";
  private static final String DEFAULT_WORKSPACE_NAMESPACE = "namespace";

  private final AtomicLong workspaceIdIncrementer = new AtomicLong(1);

  @Before
  public void setUp() {
    doReturn(NOW).when(mockClock).instant();

    firecloudWorkspaceResponses.clear();
    dbWorkspaces.clear();
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.READER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "writer",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.WRITER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "owner",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "another_extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);

    doReturn(firecloudWorkspaceResponses).when(mockFireCloudService).getWorkspaces(any());

    currentUser = new DbUser();
    currentUser.setUsername(DEFAULT_USERNAME);
    currentUser.setUserId(USER_ID);
    currentUser.setDisabled(false);
    currentUser.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
  }

  private FirecloudWorkspaceResponse mockFirecloudWorkspaceResponse(
      String workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel) {
    FirecloudWorkspace mockWorkspace = mock(FirecloudWorkspace.class);
    doReturn(workspaceNamespace).when(mockWorkspace).getNamespace();
    doReturn(workspaceName).when(mockWorkspace).getName();
    doReturn(workspaceId).when(mockWorkspace).getWorkspaceId();

    FirecloudWorkspaceResponse mockWorkspaceResponse = mock(FirecloudWorkspaceResponse.class);
    doReturn(mockWorkspace).when(mockWorkspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(mockWorkspaceResponse).getAccessLevel();
    return mockWorkspaceResponse;
  }

  private DbWorkspace buildDbWorkspace(
      long dbId, String name, String namespace, WorkspaceActiveStatus activeStatus) {
    DbWorkspace workspace = new DbWorkspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    workspace.setLastModifiedTime(nowTimestamp);
    workspace.setCreationTime(nowTimestamp);
    workspace.setName(name);
    workspace.setWorkspaceId(dbId);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    workspace.setFirecloudName(name);
    workspace.setFirecloudUuid(Long.toString(dbId));
    workspace.setNeedsReviewPrompt(false);
    return workspace;
  }

  private DbWorkspace addMockedWorkspace(
      long workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel,
      WorkspaceActiveStatus activeStatus) {

    FirecloudWorkspaceResponse mockWorkspaceResponse =
        mockFirecloudWorkspaceResponse(
            Long.toString(workspaceId), workspaceName, workspaceNamespace, accessLevel);
    firecloudWorkspaceResponses.add(mockWorkspaceResponse);
    doReturn(mockWorkspaceResponse)
        .when(mockFireCloudService)
        .getWorkspace(workspaceNamespace, workspaceName);

    DbWorkspace dbWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                workspaceId,
                mockWorkspaceResponse.getWorkspace().getName(),
                workspaceNamespace,
                activeStatus));

    dbWorkspaces.add(dbWorkspace);
    return dbWorkspace;
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
        DEFAULT_WORKSPACE_NAMESPACE,
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
        DEFAULT_WORKSPACE_NAMESPACE,
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
                        buildDbWorkspace(
                                workspaceIdIncrementer.getAndIncrement(),
                                "1",
                                DEFAULT_WORKSPACE_NAMESPACE,
                                status)
                            .getWorkspaceActiveStatusEnum())
                    .isEqualTo(status));
  }

  @Test
  public void updateRecentWorkspaces() {
    dbWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          workspaceService.updateRecentWorkspaces(
              workspace,
              USER_ID,
              Timestamp.from(NOW.minusSeconds(dbWorkspaces.size() - workspace.getWorkspaceId())));
        });
    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT);

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        dbWorkspaces
            .subList(
                dbWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                dbWorkspaces.size())
            .stream()
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAllIn(expectedIds);
  }

  @Test
  public void updateRecentWorkspaces_multipleUsers() {
    long OTHER_USER_ID = 2L;
    workspaceService.updateRecentWorkspaces(
        dbWorkspaces.get(0), OTHER_USER_ID, Timestamp.from(NOW));
    dbWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          workspaceService.updateRecentWorkspaces(
              workspace,
              USER_ID,
              Timestamp.from(NOW.minusSeconds(dbWorkspaces.size() - workspace.getWorkspaceId())));
        });
    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();

    assertThat(recentWorkspaces.size()).isEqualTo(4);
    recentWorkspaces.forEach(
        userRecentWorkspace ->
            assertThat(userRecentWorkspace.getId())
                .isNotEqualTo(userRecentWorkspace.getWorkspaceId()));

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        dbWorkspaces
            .subList(
                dbWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                dbWorkspaces.size())
            .stream()
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAllIn(expectedIds);

    currentUser.setUsername(DEFAULT_USERNAME);
    currentUser.setUserId(OTHER_USER_ID);

    final List<DbUserRecentWorkspace> otherRecentWorkspaces =
        workspaceService.getRecentWorkspaces();
    assertThat(otherRecentWorkspaces.size()).isEqualTo(1);
    assertThat(otherRecentWorkspaces.get(0).getWorkspaceId())
        .isEqualTo(dbWorkspaces.get(0).getWorkspaceId());
  }

  @Test
  public void updateRecentWorkspaces_flipFlop() {
    workspaceService.updateRecentWorkspaces(
        dbWorkspaces.get(0), USER_ID, Timestamp.from(NOW.minusSeconds(4)));
    workspaceService.updateRecentWorkspaces(
        dbWorkspaces.get(1), USER_ID, Timestamp.from(NOW.minusSeconds(3)));
    workspaceService.updateRecentWorkspaces(
        dbWorkspaces.get(0), USER_ID, Timestamp.from(NOW.minusSeconds(2)));
    workspaceService.updateRecentWorkspaces(
        dbWorkspaces.get(1), USER_ID, Timestamp.from(NOW.minusSeconds(1)));
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(0), USER_ID, Timestamp.from(NOW));

    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(2);
    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAllOf(1L, 2L);
  }

  @Test
  public void enforceFirecloudAclsInRecentWorkspaces() {
    long ownedId = workspaceIdIncrementer.getAndIncrement();
    DbWorkspace ownedWorkspace =
        addMockedWorkspace(
            ownedId,
            "owned",
            "owned_namespace",
            WorkspaceAccessLevel.OWNER,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(ownedWorkspace, USER_ID, Timestamp.from(NOW));

    DbWorkspace sharedWorkspace =
        addMockedWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "shared",
            "shared_namespace",
            WorkspaceAccessLevel.NO_ACCESS,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(sharedWorkspace, USER_ID, Timestamp.from(NOW));

    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(1);
    assertThat(recentWorkspaces.get(0).getWorkspaceId()).isEqualTo(ownedId);
  }
}
