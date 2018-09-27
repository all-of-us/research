package org.pmiops.workbench.cohortreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageRequest;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

@RunWith(MockitoJUnitRunner.class)
public class CohortReviewServiceImplTest {

    @Mock
    private CohortReviewDao cohortReviewDao;

    @Mock
    private CohortDao cohortDao;

    @Mock
    private ParticipantCohortStatusDao participantCohortStatusDao;

    @Mock
    private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

    @Mock
    private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private CohortReviewServiceImpl cohortReviewService;

    @Test
    public void findCohortNotFound() throws Exception {
        long cohortId = 1;

        when(cohortDao.findById(cohortId)).thenReturn(Optional.<Cohort>empty());

        try {
            cohortReviewService.findCohort(cohortId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao).findById(cohortId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohort() throws Exception {
        long cohortId = 1;

        Cohort cohort = new Cohort();
        when(cohortDao.findById(cohortId)).thenReturn(Optional.of(cohort));

        assertEquals(cohort, cohortReviewService.findCohort(cohortId));

        verify(cohortDao).findById(cohortId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void validateMatchingWorkspaceNotFound() throws Exception {
        String workspaceNamespace = "test-workspace";
        String workspaceName = "test";
        long workspaceId = 1;
        long badWorkspaceId = 99;

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(badWorkspaceId);
        WorkspaceAccessLevel owner = WorkspaceAccessLevel.OWNER;

        when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceName, WorkspaceAccessLevel.READER)).thenReturn(workspace);

        try {
            cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(workspaceNamespace, workspaceName, workspaceId,
                WorkspaceAccessLevel.READER);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No workspace matching workspaceNamespace: "
                    + workspaceNamespace + ", workspaceId: " + workspaceName, e.getMessage());
        }
    }

    @Test
    public void validateMatchingWorkspace() throws Exception {
        String workspaceNamespace = "test-workspace";
        String workspaceName = "test";
        long workspaceId = 1;

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        WorkspaceAccessLevel owner = WorkspaceAccessLevel.OWNER;

        when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceName, WorkspaceAccessLevel.READER)).thenReturn(workspace);

        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(workspaceNamespace, workspaceName, workspaceId,
            WorkspaceAccessLevel.READER);
    }

    @Test
    public void findCohortReviewCohortIdAndCdrVersionIdNotFound() throws Exception {
        long cohortReviewId = 1;
        long cdrVersionId = 1;

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId)).thenReturn(null);

        try {
            cohortReviewService.findCohortReview(cohortReviewId, cdrVersionId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: Cohort Review does not exist for cohortId: "
                    + cohortReviewId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewDao).findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortReviewCohortIdAndCdrVersionId() throws Exception {
        long cohortReviewId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId)).thenReturn(cohortReview);

        CohortReview actualCohortReview = cohortReviewService.findCohortReview(cohortReviewId, cdrVersionId);

        assertEquals(cohortReview, actualCohortReview);

        verify(cohortReviewDao).findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveCohortReview() throws Exception {
        CohortReview cohortReview = new CohortReview();

        when(cohortReviewDao.save(cohortReview)).thenReturn(cohortReview);

        assertEquals(cohortReview, cohortReviewService.saveCohortReview(cohortReview));

        verify(cohortReviewDao).save(cohortReview);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortStatus() throws Exception {
        ParticipantCohortStatus pcs = new ParticipantCohortStatus();

        when(participantCohortStatusDao.save(pcs)).thenReturn(pcs);

        ParticipantCohortStatus actualPcs = cohortReviewService.saveParticipantCohortStatus(pcs);

        assertEquals(pcs, actualPcs);

        verify(participantCohortStatusDao).save(pcs);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findParticipantCohortStatusNotFound() throws Exception {
        long cohortReviewId = 1;
        long participantId = 1;

        when(participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId)).thenReturn(null);

        try {
            cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: Participant Cohort Status does not exist for cohortReviewId: "
                    + cohortReviewId + ", participantId: " + participantId, e.getMessage());
        }

        verify(participantCohortStatusDao).findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findParticipantCohortStatus() throws Exception {
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortStatus pcs = new ParticipantCohortStatus();
        when(participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId)).thenReturn(pcs);

        ParticipantCohortStatus actualpcs = cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

        assertEquals(pcs, actualpcs);

        verify(participantCohortStatusDao).findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findParticipantCohortStatuses() throws Exception {
        long cohortReviewId = 1;
        PageRequest pageRequest = new PageRequest()
          .page(0)
          .pageSize(1)
          .sortColumn(ParticipantCohortStatusColumns.PARTICIPANTID.toString());

        when(participantCohortStatusDao.findAll(cohortReviewId, Collections.<Filter>emptyList(), pageRequest)).thenReturn(new ArrayList<>());

        cohortReviewService.findAll(cohortReviewId, Collections.<Filter>emptyList(), pageRequest);

        verify(participantCohortStatusDao).findAll(cohortReviewId, Collections.<Filter>emptyList(), pageRequest);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortAnnotationDefinitionNotFound() throws Exception {
        long cohortAnnotationDefinitionId = 1;

        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.<CohortAnnotationDefinition>empty());

        try {
            cohortReviewService.findCohortAnnotationDefinition(cohortAnnotationDefinitionId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No cohort annotation definition found for id: "
                    + cohortAnnotationDefinitionId, e.getMessage());
        }

        verify(cohortAnnotationDefinitionDao).findById(cohortAnnotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortAnnotationDefinition() throws Exception {
        long cohortAnnotationDefinitionId = 1;

        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId))
            .thenReturn(Optional.of(new CohortAnnotationDefinition()));

        cohortReviewService.findCohortAnnotationDefinition(cohortAnnotationDefinitionId);

        verify(cohortAnnotationDefinitionDao).findById(cohortAnnotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortAnnotationAllTypes() throws Exception {
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        //Boolean Type
        ParticipantCohortAnnotation expectedAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueBoolean(Boolean.TRUE);

        assertSaveParticipantCohortAnnotation(expectedAnnotation, AnnotationType.BOOLEAN);

        //Date Type
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        expectedAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueDateString("1999-02-01")
                .annotationValueDate(new Date(sdf.parse("1999-02-01").getTime()));

        assertSaveParticipantCohortAnnotation(expectedAnnotation, AnnotationType.DATE);

        //Enum Type
        expectedAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueEnum("enumValue");

        assertSaveParticipantCohortAnnotation(expectedAnnotation, AnnotationType.ENUM);

        //Integer Type
        expectedAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueInteger(1);

        assertSaveParticipantCohortAnnotation(expectedAnnotation, AnnotationType.INTEGER);

        //String Type
        expectedAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueString("String");

        assertSaveParticipantCohortAnnotation(expectedAnnotation, AnnotationType.STRING);
    }

    private void assertSaveParticipantCohortAnnotation(ParticipantCohortAnnotation participantCohortAnnotation, AnnotationType annotationType) {
        long cohortAnnotationDefinitionId = participantCohortAnnotation.getCohortAnnotationDefinitionId();
        long cohortReviewId = participantCohortAnnotation.getCohortReviewId();
        long participantId = participantCohortAnnotation.getParticipantId();

        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition(cohortAnnotationDefinitionId, annotationType);

        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.of(cohortAnnotationDefinition));

        when(participantCohortAnnotationDao.findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(cohortReviewId,
                cohortAnnotationDefinitionId, participantId)).thenReturn(null);
        when(participantCohortAnnotationDao.save(participantCohortAnnotation)).thenReturn(participantCohortAnnotation);

        cohortReviewService.saveParticipantCohortAnnotation(cohortReviewId, participantCohortAnnotation);

        verify(cohortAnnotationDefinitionDao, atLeastOnce()).findById(cohortAnnotationDefinitionId);

        verify(participantCohortAnnotationDao, atLeastOnce()).findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(cohortReviewId,
                cohortAnnotationDefinitionId, participantId);
        verify(participantCohortAnnotationDao, atLeastOnce()).save(participantCohortAnnotation);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortAnnotationNotFoundCohortAnnotationDefinition() throws Exception {
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortAnnotation participantCohortAnnotation = new ParticipantCohortAnnotation()
                .annotationValueBoolean(Boolean.TRUE)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId);

        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.<CohortAnnotationDefinition>empty());


        try {
            cohortReviewService.saveParticipantCohortAnnotation(cohortReviewId, participantCohortAnnotation);
            fail("Should have thrown NotFoundExcpetion!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No cohort annotation definition found for id: " + cohortAnnotationDefinitionId, e.getMessage());
        }

        verify(cohortAnnotationDefinitionDao).findById(cohortAnnotationDefinitionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortAnnotationBadRequestCohortAnnotationDefinitionExists() throws Exception {
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortAnnotation participantCohortAnnotation = new ParticipantCohortAnnotation()
                .annotationValueBoolean(Boolean.TRUE)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId);

        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.BOOLEAN);

        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.of(cohortAnnotationDefinition));

        when(participantCohortAnnotationDao.findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(cohortReviewId,
                cohortAnnotationDefinitionId, participantId)).thenReturn(participantCohortAnnotation);

        try {
            cohortReviewService.saveParticipantCohortAnnotation(cohortReviewId, participantCohortAnnotation);
            fail("Should have thrown BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort annotation definition exists for id: " + cohortAnnotationDefinitionId, e.getMessage());
        }

        verify(cohortAnnotationDefinitionDao).findById(cohortAnnotationDefinitionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortAnnotationBadRequest() throws Exception {
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortAnnotation participantCohortAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId);

        assertParticipantCohortAnnotationBadRequest(participantCohortAnnotation, createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.BOOLEAN));
        assertParticipantCohortAnnotationBadRequest(participantCohortAnnotation, createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.STRING));
        assertParticipantCohortAnnotationBadRequest(participantCohortAnnotation, createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.INTEGER));
        assertParticipantCohortAnnotationBadRequest(participantCohortAnnotation, createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.STRING));
        assertParticipantCohortAnnotationBadRequest(participantCohortAnnotation, createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.ENUM));
    }

    @Test
    public void updateParticipantCohortAnnotationModify() throws Exception {
        long annotationId = 1;
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortAnnotation participantCohortAnnotation = new ParticipantCohortAnnotation()
                .annotationValueBoolean(Boolean.TRUE)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId);

        ModifyParticipantCohortAnnotationRequest modifyRequest = new ModifyParticipantCohortAnnotationRequest()
                .annotationValueBoolean(Boolean.FALSE);

        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition(cohortAnnotationDefinitionId, AnnotationType.BOOLEAN);

        when(participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId)).thenReturn(participantCohortAnnotation);
        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.of(cohortAnnotationDefinition));


        cohortReviewService.updateParticipantCohortAnnotation(annotationId, cohortReviewId, participantId, modifyRequest);

        verify(participantCohortAnnotationDao).findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId);
        verify(cohortAnnotationDefinitionDao).findById(cohortAnnotationDefinitionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateParticipantCohortAnnotationModifyNotFoundCohortAnnotationDefinition() throws Exception {
        long annotationId = 1;
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortAnnotation participantCohortAnnotation = new ParticipantCohortAnnotation()
                .annotationValueBoolean(Boolean.TRUE)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId);

        ModifyParticipantCohortAnnotationRequest modifyRequest = new ModifyParticipantCohortAnnotationRequest()
                .annotationValueBoolean(Boolean.FALSE);

        when(participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId)).thenReturn(participantCohortAnnotation);
        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.<CohortAnnotationDefinition>empty());


        try {
            cohortReviewService.updateParticipantCohortAnnotation(annotationId, cohortReviewId, participantId, modifyRequest);
            fail("Should have thrown NotFoundException!");
        } catch(NotFoundException e) {
            assertEquals("Not Found: No cohort annotation definition found for id: " + cohortAnnotationDefinitionId, e.getMessage());
        }

        verify(participantCohortAnnotationDao).findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId);
        verify(cohortAnnotationDefinitionDao).findById(cohortAnnotationDefinitionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void updateParticipantCohortAnnotationModifyNotFoundParticipantCohortAnnotation() throws Exception {
        long annotationId = 1;
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ModifyParticipantCohortAnnotationRequest modifyRequest = new ModifyParticipantCohortAnnotationRequest()
                .annotationValueBoolean(Boolean.FALSE);

        when(participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId)).thenReturn(null);

        try {
            cohortReviewService.updateParticipantCohortAnnotation(annotationId, cohortReviewId, participantId, modifyRequest);
            fail("Should have thrown NotFoundException!");
        } catch(NotFoundException e) {
            assertEquals("Not Found: Participant Cohort Annotation does not exist for annotationId: " + annotationId + ", cohortReviewId: "
                    + cohortReviewId + ", participantId: " + cohortAnnotationDefinitionId, e.getMessage());
        }

        verify(participantCohortAnnotationDao).findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortAnnotationModifyBadRequest() throws Exception {
        long annotationId = 1;
        long cohortAnnotationDefinitionId = 1;
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortAnnotation participantCohortAnnotation = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId);

        assertParticipantCohortAnnotationModifyBadRequest(annotationId, participantCohortAnnotation, AnnotationType.BOOLEAN);
        assertParticipantCohortAnnotationModifyBadRequest(annotationId, participantCohortAnnotation, AnnotationType.INTEGER);
        assertParticipantCohortAnnotationModifyBadRequest(annotationId, participantCohortAnnotation, AnnotationType.STRING);
        assertParticipantCohortAnnotationModifyBadRequest(annotationId, participantCohortAnnotation, AnnotationType.ENUM);
        assertParticipantCohortAnnotationModifyBadRequest(annotationId, participantCohortAnnotation, AnnotationType.DATE);
    }

    @Test
    public void deleteParticipantCohortAnnotationNotFound() throws Exception {
        long annotationId = 1;
        long cohortReviewId = 2;
        long participantId = 3;

        when(participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId,
                cohortReviewId,
                participantId)).thenReturn(null);

        try {
            cohortReviewService.deleteParticipantCohortAnnotation(annotationId,
                    cohortReviewId,
                    participantId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No participant cohort annotation found for annotationId: "
                    + annotationId + ", cohortReviewId: " + cohortReviewId + ", participantId: "
                    + participantId, e.getMessage());
        }

        verify(participantCohortAnnotationDao).findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId,
                cohortReviewId,
                participantId);

        verifyNoMoreMockInteractions();
    }

    @Test
    public void deleteParticipantCohortAnnotation() throws Exception {
        long annotationId = 1;
        long cohortReviewId = 2;
        long participantId = 3;

        when(participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId,
                cohortReviewId,
                participantId)).thenReturn(new ParticipantCohortAnnotation());

        cohortReviewService.deleteParticipantCohortAnnotation(annotationId,
                    cohortReviewId,
                    participantId);

        verify(participantCohortAnnotationDao).findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId,
                cohortReviewId,
                participantId);

        verifyNoMoreMockInteractions();
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(
                cohortDao,
                cohortReviewDao,
                participantCohortStatusDao,
                workspaceService);
    }

    private CohortAnnotationDefinition createCohortAnnotationDefinition(long cohortAnnotationDefinitionId, AnnotationType annotationType) {
        return new CohortAnnotationDefinition()
                .annotationTypeEnum(annotationType)
                .columnName("name")
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortId(1)
                .enumValues(new TreeSet<CohortAnnotationEnumValue>(Arrays.asList(new CohortAnnotationEnumValue().name("enumValue"))));
    }

    private void assertParticipantCohortAnnotationBadRequest(ParticipantCohortAnnotation participantCohortAnnotation, CohortAnnotationDefinition cohortAnnotationDefinition) {
        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinition.getCohortAnnotationDefinitionId())).thenReturn(Optional.of(cohortAnnotationDefinition));

        try {
            cohortReviewService.saveParticipantCohortAnnotation(participantCohortAnnotation.getCohortReviewId(), participantCohortAnnotation);
            fail("Should have thrown BadRequestExcpetion!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Please provide a valid " + cohortAnnotationDefinition.getAnnotationTypeEnum().name()
                    + " value for annotation defintion id: " + cohortAnnotationDefinition.getCohortAnnotationDefinitionId(), e.getMessage());
        }

        verify(cohortAnnotationDefinitionDao, atLeastOnce()).findById(cohortAnnotationDefinition.getCohortAnnotationDefinitionId());
        verifyNoMoreMockInteractions();
    }

    private void assertParticipantCohortAnnotationModifyBadRequest(Long annotationId, ParticipantCohortAnnotation participantCohortAnnotation,
                                                                   AnnotationType annotationType) {
        Long cohortAnnotationDefinitionId = participantCohortAnnotation.getCohortAnnotationDefinitionId();
        Long cohortReviewId = participantCohortAnnotation.getCohortReviewId();
        Long participantId = participantCohortAnnotation.getParticipantId();

        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition(cohortAnnotationDefinitionId, annotationType);

        when(participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId,
                cohortReviewId, participantId)).thenReturn(participantCohortAnnotation);
        when(cohortAnnotationDefinitionDao.findById(cohortAnnotationDefinitionId)).thenReturn(Optional.of(cohortAnnotationDefinition));


        try {
            cohortReviewService.updateParticipantCohortAnnotation(annotationId, cohortReviewId, participantId, new ModifyParticipantCohortAnnotationRequest());
            fail("Should have thrown BadRequestExcpetion!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Please provide a valid " + cohortAnnotationDefinition.getAnnotationTypeEnum().name()
                    + " value for annotation defintion id: " + cohortAnnotationDefinition.getCohortAnnotationDefinitionId(), e.getMessage());
        }

        verify(participantCohortAnnotationDao, atLeastOnce()).findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId, cohortReviewId, participantId);
        verify(cohortAnnotationDefinitionDao, atLeastOnce()).findById(cohortAnnotationDefinitionId);

        verifyNoMoreMockInteractions();
    }

}
