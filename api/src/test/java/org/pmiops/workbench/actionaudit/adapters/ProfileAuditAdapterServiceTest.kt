package org.pmiops.workbench.actionaudit.adapters

import com.google.common.truth.Truth.assertThat

import java.time.Clock
import java.time.Instant
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.*
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal

@RunWith(SpringRunner::class)
class ProfileAuditAdapterServiceTest {

    @Mock
    private val mockUserProvider: Provider<DbUser>? = null
    @Mock
    private val mockActionAuditService: ActionAuditService? = null
    @Mock
    private val mockClock: Clock? = null
    @Mock
    private val mockActionIdProvider: Provider<String>? = null

    @Captor
    private var eventListCaptor: ArgumentCaptor<List<ActionAuditEvent>>? = null

    @Captor
    private var eventCaptor: ArgumentCaptor<ActionAuditEvent>? = null

    private var profileAuditAdapterService: ProfileAuditAdapterService? = null
    private var user: DbUser? = null

    @Before
    fun setUp() {
        reset(mockActionAuditService)
        user = DbUser()
        user?.userId = 1001
        user?.email = USER_EMAIL
        profileAuditAdapterService = ProfileAuditAdapterServiceImpl(
                userProvider = mockUserProvider!!,
                actionAuditService = mockActionAuditService!!,
                clock = mockClock!!,
                actionIdProvider = mockActionIdProvider!!)
        doReturn(Y2K_EPOCH_MILLIS).`when`(mockClock).millis()
        doReturn(user).`when`(mockUserProvider).get()
        doReturn(ACTION_ID).`when`(mockActionIdProvider).get()
    }

    @Test
    fun testCreateUserProfile() {
        val createdProfile = buildProfile()

        profileAuditAdapterService!!.fireCreateAction(createdProfile)
        verify(mockActionAuditService)?.send(anyList())
//        verify(mockActionAuditService)?.send(eventListCaptor?.capture()!!)
//        val sentEvents: List<ActionAuditEvent> = eventListCaptor?.value?.toList().orEmpty()
//
//        assertThat(sentEvents).hasSize(10)
    }

    private fun buildProfile(): Profile {
        val caltechAffiliation = InstitutionalAffiliation()
        caltechAffiliation.institution = "Caltech"
        caltechAffiliation.role = "T.A."
        caltechAffiliation.nonAcademicAffiliation = NonAcademicAffiliation.COMMUNITY_SCIENTIST
        caltechAffiliation.other = "They are all fine houses."

        val mitAffiliation = InstitutionalAffiliation()
        mitAffiliation.institution = "MIT"
        mitAffiliation.role = "Professor"
        mitAffiliation.nonAcademicAffiliation = NonAcademicAffiliation.EDUCATIONAL_INSTITUTION

        val demographicSurvey = DemographicSurvey()
        demographicSurvey.disability = false
        demographicSurvey.ethnicity = Ethnicity.NOT_HISPANIC
        demographicSurvey.gender = listOf(Gender.PREFER_NO_ANSWER)
        demographicSurvey.yearOfBirth = BigDecimal.valueOf(1999)
        demographicSurvey.race = listOf(Race.PREFER_NO_ANSWER)
        demographicSurvey.education = Education.MASTER

        val createdProfile = Profile()
        createdProfile.username = "slim_shady"
        createdProfile.contactEmail = USER_EMAIL
        createdProfile.dataAccessLevel = DataAccessLevel.REGISTERED
        createdProfile.givenName = "Robert"
        createdProfile.familyName = "Paulson"
        createdProfile.phoneNumber = "867-5309"
        createdProfile.currentPosition = "Grad Student"
        createdProfile.organization = "Classified"
        createdProfile.disabled = false
        createdProfile.aboutYou = "Nobody in particular"
        createdProfile.areaOfResearch = "Aliens"
        createdProfile.institutionalAffiliations = listOf(caltechAffiliation, mitAffiliation)
        createdProfile.demographicSurvey = demographicSurvey
        return createdProfile
    }

    @Test
    fun testDeleteUserProfile() {
        profileAuditAdapterService!!.fireDeleteAction(USER_ID, USER_EMAIL)
//        verify<ActionAuditService>(mockActionAuditService).send(eventCaptor!!.capture())
//        val eventSent = eventCaptor!!.value
//
//        assertThat(eventSent.targetType).isEqualTo(TargetType.PROFILE)
//        assertThat(eventSent.agentType).isEqualTo(AgentType.USER)
//        assertThat(eventSent.agentId).isEqualTo(USER_ID)
//        assertThat(eventSent.targetIdMaybe).isEqualTo(USER_ID)
//        assertThat(eventSent.actionType).isEqualTo(ActionType.DELETE)
//        assertThat(eventSent.timestamp).isEqualTo(Y2K_EPOCH_MILLIS)
//        assertThat(eventSent.targetPropertyMaybe).isNull()
//        assertThat(eventSent.newValueMaybe).isNull()
//        assertThat(eventSent.previousValueMaybe).isNull()
    }

    companion object {

        private const val USER_ID = 101L
        private const val USER_EMAIL = "a@b.com"
        private val Y2K_EPOCH_MILLIS = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli()
        private const val ACTION_ID = "58cbae08-447f-499f-95b9-7bdedc955f4d"

    }
}
