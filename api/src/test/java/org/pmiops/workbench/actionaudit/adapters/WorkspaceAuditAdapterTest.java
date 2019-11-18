package org.pmiops.workbench.actionaudit.adapters;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceAuditAdapterTest {

  private static final long WORKSPACE_1_DB_ID = 101L;
  private static final long Y2K_EPOCH_MILLIS =
      Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli();
  private static final long REMOVED_USER_ID = 301L;
  private static final long ADDED_USER_ID = 401L;
  private static final String ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d";

  private WorkspaceAuditAdapter workspaceAuditAdapter;
  private Workspace workspace1;
  private DbUser user1;
  private DbWorkspace dbWorkspace1;
  private DbWorkspace dbWorkspace2;

  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private Clock mockClock;
  @Mock private ActionAuditService mockActionAuditService;
  @Mock private Provider<String> mockActionIdProvider;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventCollectionCaptor;
  @Captor private ArgumentCaptor<ActionAuditEvent> eventCaptor;

  @TestConfiguration
  @MockBean(value = {ActionAuditService.class})
  static class Configuration {}

  @Before
  public void setUp() {
    user1 = new DbUser();
    user1.setUserId(101L);
    user1.setEmail("fflinstone@slate.com");
    user1.setGivenName("Fred");
    user1.setFamilyName("Flintstone");
    doReturn(user1).when(mockUserProvider).get();
    workspaceAuditAdapter =
        new WorkspaceAuditAdapterImpl(
            mockUserProvider, mockActionAuditService, mockClock, mockActionIdProvider);

    final ResearchPurpose researchPurpose1 = new ResearchPurpose();
    researchPurpose1.setIntendedStudy("stubbed toes");
    researchPurpose1.setAdditionalNotes("I really like the cloud.");
    final long now = System.currentTimeMillis();

    workspace1 = new Workspace();
    workspace1.setName("DbWorkspace 1");
    workspace1.setId("fc-id-1");
    workspace1.setNamespace("aou-rw-local1-c4be869a");
    workspace1.setCreator("user@fake-research-aou.org");
    workspace1.setCdrVersionId("1");
    workspace1.setResearchPurpose(researchPurpose1);
    workspace1.setCreationTime(now);
    workspace1.setLastModifiedTime(now);
    workspace1.setEtag("etag_1");
    workspace1.setDataAccessLevel(DataAccessLevel.REGISTERED);
    workspace1.setPublished(false);

    dbWorkspace1 = WorkspaceConversionUtils.toDbWorkspace(workspace1);
    dbWorkspace1.setWorkspaceId(WORKSPACE_1_DB_ID);
    dbWorkspace1.setLastAccessedTime(new Timestamp(now));
    dbWorkspace1.setLastModifiedTime(new Timestamp(now));
    dbWorkspace1.setCreationTime(new Timestamp(now));

    dbWorkspace2 = new DbWorkspace();
    dbWorkspace2.setWorkspaceId(201L);
    dbWorkspace2.setPublished(false);
    dbWorkspace2.setLastModifiedTime(new Timestamp(now));
    dbWorkspace2.setCreationTime(new Timestamp(now));
    dbWorkspace2.setCreator(user1);

    doReturn(Y2K_EPOCH_MILLIS).when(mockClock).millis();
    doReturn(ACTION_ID).when(mockActionIdProvider).get();
  }

  @Test
  public void testFiresCreateWorkspaceEvents() {
    workspaceAuditAdapter.fireCreateAction(workspace1, WORKSPACE_1_DB_ID);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent.size()).isEqualTo(6);
    Optional<ActionAuditEvent> firstEvent = eventsSent.stream().findFirst();
    assertThat(firstEvent.isPresent()).isTrue();
    assertThat(firstEvent.get().getActionType()).isEqualTo(ActionType.CREATE);
    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getActionType)
                .collect(Collectors.toSet())
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testFiresDeleteWorkspaceEvent() {
    workspaceAuditAdapter.fireDeleteAction(dbWorkspace1);
    verify(mockActionAuditService).send(eventCaptor.capture());
    final ActionAuditEvent eventSent = eventCaptor.getValue();
    assertThat(eventSent.getActionType()).isEqualTo(ActionType.DELETE);
    assertThat(eventSent.getTimestamp()).isEqualTo(Y2K_EPOCH_MILLIS);
  }

  @Test
  public void testFiresDuplicateEvent() {
    workspaceAuditAdapter.fireDuplicateAction(dbWorkspace1, dbWorkspace2);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    final Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(2);

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::getActionId).distinct().count())
        .isEqualTo(1);

    assertThat(
            eventsSent.stream()
                .map(ActionAuditEvent::getTargetType)
                .allMatch(t -> t.equals(TargetType.WORKSPACE)))
        .isTrue();

    ImmutableSet<ActionType> expectedActionTypes =
        ImmutableSet.of(ActionType.DUPLICATE_FROM, ActionType.DUPLICATE_TO);
    ImmutableSet<ActionType> actualActionTypes =
        eventsSent.stream()
            .map(ActionAuditEvent::getActionType)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(actualActionTypes).containsExactlyElementsIn(expectedActionTypes);
  }

  @Test
  public void testFiresCollaborateAction() {
    final ImmutableMap<Long, String> aclsByUserId =
        ImmutableMap.of(
            user1.getUserId(),
            WorkspaceAccessLevel.OWNER.toString(),
            REMOVED_USER_ID,
            WorkspaceAccessLevel.NO_ACCESS.toString(),
            ADDED_USER_ID,
            WorkspaceAccessLevel.READER.toString());
    workspaceAuditAdapter.fireCollaborateAction(dbWorkspace1.getWorkspaceId(), aclsByUserId);
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    Collection<ActionAuditEvent> eventsSent = eventCollectionCaptor.getValue();
    assertThat(eventsSent).hasSize(4);

    Map<String, Long> countByTargetType =
        eventsSent.stream()
            .collect(
                Collectors.groupingBy(e -> e.getTargetType().toString(), Collectors.counting()));

    assertThat(countByTargetType.get(TargetType.WORKSPACE.toString())).isEqualTo(1);
    assertThat(countByTargetType.get(TargetType.USER.toString())).isEqualTo(3);

    Optional<String> targetPropertyMaybe =
        eventsSent.stream()
            .filter(e -> e.getTargetType() == TargetType.USER)
            .findFirst()
            .flatMap(e -> Optional.ofNullable(e.getTargetPropertyMaybe()));

    assertThat(targetPropertyMaybe.isPresent()).isTrue();
    assertThat(targetPropertyMaybe.get()).isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());

    // need same actionId for all events
    assertThat(eventsSent.stream().map(ActionAuditEvent::getActionId).distinct().count())
        .isEqualTo(1);

    Optional<ActionAuditEvent> readerEventMaybe =
        eventsSent.stream()
            .filter(
                e ->
                    e.getTargetType() == TargetType.USER
                        && e.getTargetIdMaybe() != null
                        && e.getTargetIdMaybe().equals(ADDED_USER_ID))
            .findFirst();
    assertThat(readerEventMaybe.isPresent()).isTrue();
    assertThat(readerEventMaybe.get().getTargetPropertyMaybe()).isNotNull();
    assertThat(readerEventMaybe.get().getTargetPropertyMaybe())
        .isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString());
    assertThat(readerEventMaybe.get().getNewValueMaybe())
        .isEqualTo(WorkspaceAccessLevel.READER.toString());
    assertThat(readerEventMaybe.get().getPreviousValueMaybe()).isNull();
  }

  @Test
  public void testCollaborateWithEmptyMapDoesNothing() {
    workspaceAuditAdapter.fireCollaborateAction(WORKSPACE_1_DB_ID, Collections.emptyMap());
    verifyZeroInteractions(mockActionAuditService);
  }

  @Test
  public void testDoesNotThrowWhenMissingRequiredFields() {
    workspace1.setResearchPurpose(null); // programming error
    workspaceAuditAdapter.fireCreateAction(workspace1, WORKSPACE_1_DB_ID);
  }

  @Test
  public void testDoesNotThrowWhenUserProviderFails() {
    doReturn(null).when(mockUserProvider).get();
    workspaceAuditAdapter.fireDeleteAction(dbWorkspace1);
  }

  @Test
  public void testFireEditAction_sendsNoEventsForSameWorkspace() {
    workspaceAuditAdapter.fireEditAction(workspace1, workspace1, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());
    assertThat(eventCollectionCaptor.getValue()).isEmpty();
  }

  @Test
  public void testFireEditAction_sendsChangedProperties() {
    final ResearchPurpose editedResearchPurpose = new ResearchPurpose();
    editedResearchPurpose.setIntendedStudy("stubbed toes");
    editedResearchPurpose.setAdditionalNotes("I really like the cloud.");
    editedResearchPurpose.setAnticipatedFindings("I want to find my keys.");
    editedResearchPurpose.setControlSet(true);

    Workspace editedWorkspace = new Workspace();
    editedWorkspace.setName("New name");
    editedWorkspace.setId("fc-id-1");
    editedWorkspace.setNamespace("aou-rw-local1-c4be869a");
    editedWorkspace.setCreator("user@fake-research-aou.org");
    editedWorkspace.setCdrVersionId("1");
    editedWorkspace.setResearchPurpose(editedResearchPurpose);
    editedWorkspace.setCreationTime(Y2K_EPOCH_MILLIS);
    editedWorkspace.setLastModifiedTime(Y2K_EPOCH_MILLIS);
    editedWorkspace.setEtag("etag_1");
    editedWorkspace.setDataAccessLevel(DataAccessLevel.REGISTERED);
    editedWorkspace.setPublished(false);

    workspaceAuditAdapter.fireEditAction(
        workspace1, editedWorkspace, dbWorkspace1.getWorkspaceId());
    verify(mockActionAuditService).send(eventCollectionCaptor.capture());

    assertThat(eventCollectionCaptor.getValue()).hasSize(3);
  }
}
