package org.pmiops.workbench.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CohortReviewControllerTest {

    @Mock
    ParticipantCohortStatusDao participantCohortStatusDao;

    @Mock
    CohortReviewDao cohortReviewDao;

    @Mock
    CohortDao cohortDao;

    @Mock
    BigQueryService bigQueryService;

    @Mock
    ParticipantCounter participantCounter;

    @Mock
    WorkspaceDao workspaceDao;

    @InjectMocks
    CohortReviewController reviewController;

    @Test
    public void createCohortReview_ReviewAlreadyCreated() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        cohortReview.setReviewSize(1);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review already created for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void createCohortReview_MoreThanTenThousand() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(20000));
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review size must be between 0 and 10000", e.getMessage());
        }

        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void createCohortReview_BadCohortIdOrCdrVersionId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(null);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review does not exist for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void createCohortReview_NoCohortDefinitionFound() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        Cohort cohort = new Cohort();
        Workspace workspace = new Workspace();

        workspace.setWorkspaceId(0);
        cohort.setWorkspaceId(0);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceDao.findByWorkspaceNamespaceAndFirecloudName(namespace, name)).thenReturn(workspace);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort definition matching cohortId: "
                         + cohortId
                         + ", workspaceNamespace: " + namespace
                         + ", workspaceId: " + name,
                         e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceDao, times(1)).findByWorkspaceNamespaceAndFirecloudName(namespace, name);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter, workspaceDao);
    }

    @Test
    public void createCohortReview() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long cohortReviewId = 1;

        CohortReview cohortReview = new CohortReview();
        Cohort cohort = new Cohort();
        Workspace workspace = new Workspace();

        workspace.setWorkspaceId(0);
        cohort.setWorkspaceId(0);

        cohortReview.setCohortReviewId(cohortReviewId);
        cohortReview.setCohortId(cohortId);
        cohortReview.setCdrVersionId(cdrVersionId);
        cohortReview.setMatchedParticipantCount(1000);
        cohortReview.setReviewSize(0);
        cohortReview.setCreationTime(new Timestamp(System.currentTimeMillis()));

        CohortReview cohortReviewAfter = new CohortReview();
        cohortReviewAfter.setCohortReviewId(cohortReviewId);
        cohortReviewAfter.setCohortId(cohortId);
        cohortReviewAfter.setCdrVersionId(cdrVersionId);
        cohortReviewAfter.setMatchedParticipantCount(1000);
        cohortReviewAfter.setReviewSize(1);
        cohortReviewAfter.setCreationTime(new Timestamp(System.currentTimeMillis()));
        cohortReviewAfter.setReviewStatus(ReviewStatus.CREATED);

        String definition = "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":" +
                            "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":" +
                            "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";

        cohort.setCriteria(definition);

        SearchRequest request = new Gson().fromJson(definition, SearchRequest.class);
        QueryResult queryResult = mock(QueryResult.class);
        Iterable testIterable = new Iterable() {
            @Override
            public Iterator iterator() {
                List<FieldValue> list = new ArrayList<>();
                list.add(null);
                return list.iterator();
            }
        };
        Map<String, Integer> rm = new HashMap<>();
        rm.put("person_id", 0);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceDao.findByWorkspaceNamespaceAndFirecloudName(namespace, name)).thenReturn(workspace);
        when(participantCounter.buildParticipantIdQuery(request, 200)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(0L);
        when(cohortReviewDao.save(cohortReviewAfter)).thenReturn(cohortReviewAfter);

        reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceDao, times(1)).findByWorkspaceNamespaceAndFirecloudName(namespace, name);
        verify(participantCounter, times(1)).buildParticipantIdQuery(request, 200);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(queryResult, times(1)).iterateAll();
        verify(cohortReviewDao, times(1)).save(cohortReviewAfter);
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, participantCounter);
    }

    @Test
    public void getParticipants() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1L;
        long cdrVersionId = 1L;
        int page = 1;
        int limit = 22;
        String order = "desc";
        String column = "status";

        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null, null, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, null, null, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, limit, null, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null, order, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null, null, column);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, limit, order, "participantId");
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, limit, order, column);
    }

    private void assertFindByCohortIdAndCdrVersionId(String namespace,
                                                     String name,
                                                     long cohortId,
                                                     long cdrVersionId,
                                                     Integer page,
                                                     Integer limit,
                                                     String order,
                                                     String column) {
        Integer pageParam = page == null ? 0 : page;
        Integer limitParam = limit == null ? 25 : limit;
        Sort.Direction orderParam = (order == null || order.equals("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String columnParam = (column == null || column.equals("participantId")) ? "participantKey.participantId" : column;

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey().cohortReviewId(cohortId).participantId(1L);
        ParticipantCohortStatus dbParticipant = new ParticipantCohortStatus().participantKey(key).status(CohortStatus.INCLUDED);

        org.pmiops.workbench.model.ParticipantCohortStatus respParticipant =
                new org.pmiops.workbench.model.ParticipantCohortStatus()
                        .participantId(1L)
                        .status(CohortStatus.INCLUDED);
        org.pmiops.workbench.model.CohortReview respCohortReview =
                new org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(1L)
                        .cohortId(cohortId)
                        .cdrVersionId(cdrVersionId)
                        .matchedParticipantCount(1000L)
                        .reviewedCount(0L)
                        .reviewSize(200L)
                .participantCohortStatuses(Arrays.asList(respParticipant));

        List<ParticipantCohortStatus> participants = new ArrayList<ParticipantCohortStatus>();
        participants.add(dbParticipant);
        Page expectedPage = new PageImpl(participants);

        CohortReview cohortReviewAfter = new CohortReview();
        cohortReviewAfter.setCohortReviewId(1L);
        cohortReviewAfter.setCohortId(cohortId);
        cohortReviewAfter.setCdrVersionId(cdrVersionId);
        cohortReviewAfter.setMatchedParticipantCount(1000);
        cohortReviewAfter.setReviewSize(200);
        cohortReviewAfter.setCreationTime(new Timestamp(System.currentTimeMillis()));

        final Sort sort = (columnParam.equals(CohortReviewController.PARTICIPANT_ID))
                ? new Sort(orderParam, columnParam)
                : new Sort(orderParam, columnParam, CohortReviewController.PARTICIPANT_ID);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReviewAfter);
        when(participantCohortStatusDao.findByParticipantKey_CohortReviewId(
                cohortId,
                new PageRequest(pageParam, limitParam, sort)))
                .thenReturn(expectedPage);

        ResponseEntity<org.pmiops.workbench.model.CohortReview> response =
                reviewController.getParticipantCohortStatuses(namespace, name, cohortId, cdrVersionId, page, limit, order, column);

        org.pmiops.workbench.model.CohortReview actualCohortReview = response.getBody();
        respCohortReview.setCreationTime(actualCohortReview.getCreationTime());
        assertEquals(respCohortReview, response.getBody());

        verify(cohortReviewDao, atLeast(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(participantCohortStatusDao, times(1))
                .findByParticipantKey_CohortReviewId(
                        cohortId,
                        new PageRequest(pageParam, limitParam, sort));
        verifyNoMoreInteractions(participantCohortStatusDao);
    }

}
