package org.pmiops.workbench.audit

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Payload.Type
import com.google.common.collect.ImmutableList
import org.hibernate.validator.internal.xml.PayloadType
import java.util.Arrays
import java.util.Collections
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class ActionAuditServiceTest {
    @Mock
    private val mockLogging: Logging? = null
    @Mock
    private val mockConfigProvider: Provider<WorkbenchConfig>? = null
    @Captor
    private val logEntryListCaptor: ArgumentCaptor<List<LogEntry>>? = null

    private var event1: ActionAuditEvent? = null
    private var event2: ActionAuditEvent? = null
    private var actionAuditService: ActionAuditService? = null

    @Before
    fun setUp() {
        val actionAuditConfig = ActionAuditConfig()
        actionAuditConfig.logName = "log_path_1"
        val serverConfig = ServerConfig()
        serverConfig.projectId = "gcp-project-id"

        val workbenchConfig = WorkbenchConfig()
        workbenchConfig.actionAudit = actionAuditConfig
        workbenchConfig.server = serverConfig
        doReturn(workbenchConfig).`when`<Provider<WorkbenchConfig>>(mockConfigProvider).get()

        actionAuditService = ActionAuditServiceImpl(mockConfigProvider!!, mockLogging!!)
        val actionId = ActionAuditEvent.newActionId()

        // ordinarily events sharing an action would have more things in common than this,
        // but the schema doesn't require it
        event1 = ActionAuditEvent(
                agentEmailMaybe = "a@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 1L,
                agentType = AgentType.USER,
                agentId = AGENT_ID_1,
                actionId = actionId,
                actionType = ActionType.EDIT,
                targetPropertyMaybe = "foot",
                previousValueMaybe = "bare",
                newValueMaybe = "shod",
                timestamp = System.currentTimeMillis()
        )
//        event2 = ActionAuditEventImpl.Builder()
//                .setAgentEmail("f@b.co")
//                .setTargetType(TargetType.DATASET)
//                .setTargetId(2L)
//                .setAgentType(AgentType.USER)
//                .setAgentId(AGENT_ID_2)
//                .setActionId(actionId)
//                .setActionType(ActionType.EDIT)
//                .setTargetProperty("height")
//                .setPreviousValue("yay high")
//                .setNewValue("about that tall")
//                .setTimestamp(System.currentTimeMillis())
//                .build()
        event2 = ActionAuditEvent(
                agentEmailMaybe = "f@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 2L,
                agentType = AgentType.USER,
                agentId = AGENT_ID_2,
                actionId = actionId,
                actionType = ActionType.EDIT,
                targetPropertyMaybe = "height",
                previousValueMaybe = "yay high",
                newValueMaybe = "about that tall",
                timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun testSendsSingleEvent() {
        actionAuditService!!.send(event1!!)
        verify<Logging>(mockLogging).write(logEntryListCaptor!!.capture())

        val entryList: List<LogEntry> = logEntryListCaptor.value
        assertThat(entryList.size).isEqualTo(1)

        val entry: LogEntry = entryList[0]
        val payload: Payload = entry.<JsonPayload>getPayload<>()
//        assertThat<Type>(entry.getPayload<Payload>().getType()).isEqualTo(Type.JSON)
        assertThat(entry.getPayload()?.type).isEqualTo(Type.JSON)
        val jsonPayload = entry.getPayload<JsonPayload>()
        assertThat(jsonPayload.dataAsMap.size).isEqualTo(11)

        val payloadMap = jsonPayload.dataAsMap
        assertThat(payloadMap[AuditColumn.NEW_VALUE.name]).isEqualTo("shod")
        // Logging passes numeric json fields as doubles when building a JsonPayload
        assertThat(payloadMap[AuditColumn.AGENT_ID.name]).isEqualTo(AGENT_ID_1.toDouble())
    }

    @Test
    fun testSendsExpectedColumnNames() {
        actionAuditService!!.send(event1!!)
        verify<Logging>(mockLogging).write(logEntryListCaptor!!.capture())
        val entryList = logEntryListCaptor.value
        assertThat(entryList.size).isEqualTo(1)
        val entry = entryList[0]
        assertThat<Type>(entry.getPayload<Payload>().getType()).isEqualTo(Type.JSON)
        val jsonPayload = entry.getPayload<JsonPayload>()

        for (key in jsonPayload.dataAsMap.keys) {
            assertThat(Arrays.stream(AuditColumn.values()).anyMatch { col -> col.toString() == key })
                    .isTrue()
        }
    }

    @Test
    fun testSendsMultipleEventsAsSingleAction() {
        actionAuditService!!.send(ImmutableList.of(event1!!, event2!!))
        verify<Logging>(mockLogging).write(logEntryListCaptor!!.capture())
        val entryList = logEntryListCaptor.value
        assertThat(entryList.size).isEqualTo(2)

        val payloads = entryList.stream()
                .map<Any>(Function<LogEntry, Any> { it.getPayload() })
                .filter { p -> (p as Payload<*>).type == Type.JSON }
                .map { p -> p as JsonPayload }
                .collect<ImmutableList<JsonPayload>, Any>(ImmutableList.toImmutableList())

        assertThat(
                payloads.stream()
                        .map<Map<String, Any>>(Function<JsonPayload, Map<String, Any>> { it.getDataAsMap() })
                        .map<Any> { entry -> entry[AuditColumn.ACTION_ID.name] }
                        .distinct()
                        .count())
                .isEqualTo(1)
    }

    @Test
    fun testNullPayloadDoesNotThrow() {
        actionAuditService!!.send((null as Collection<ActionAuditEvent>?)!!)
    }

    @Test
    fun testSendWithEmptyCollectionDoesNotCallCloudLoggingApi() {
        actionAuditService!!.send(emptySet())
        verify<Logging>(mockLogging, never()).write(anyList())
    }

    companion object {

        private val AGENT_ID_1 = 101L
        private val AGENT_ID_2 = 102L
    }
}
