package org.pmiops.workbench.cohorts;

import com.google.cloud.bigquery.BigQuery;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.DemoQueryBuilder;
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.CdrConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.CdrQuery;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DataTableSpecification;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.CdrJpaConfig;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({LiquibaseAutoConfiguration.class, FieldSetQueryBuilder.class, AnnotationQueryBuilder.class,
    TestBigQueryCdrSchemaConfig.class, CohortQueryBuilder.class,
    CdrBigQuerySchemaConfigService.class, DomainLookupService.class,
    DemoQueryBuilder.class, QueryBuilderFactory.class, BigQueryService.class,
    CohortMaterializationService.class, ConceptService.class, TestJpaConfig.class, CdrJpaConfig.class})
@MockBean({BigQuery.class})
public class CohortMaterializationServiceTest {

  private static final String DATA_SET_ID = "data_set_id";
  private static final String PROJECT_ID = "project_id";
  
  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  CohortMaterializationService cohortMaterializationService;

  @TestConfiguration
  static class Configuration {

    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.cdr = new CdrConfig();
      workbenchConfig.cdr.debugQueries = false;
      return workbenchConfig;
    }
  }

  private CohortReview cohortReview;


  @Before
  public void setUp() {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset(DATA_SET_ID);
    cdrVersion.setBigqueryProject(PROJECT_ID);
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);


    Workspace workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    workspaceDao.save(workspace);

    Cohort cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setName("males");
    cohort.setType("AOU");
    Gson gson = new Gson();
    cohort.setCriteria(gson.toJson(SearchRequests.males()));
    cohortDao.save(cohort);

    Cohort cohort2 = new Cohort();
    cohort2.setWorkspaceId(workspace.getWorkspaceId());
    cohort2.setName("all genders");
    cohort2.setType("AOU");
    cohort2.setCriteria(gson.toJson(SearchRequests.allGenders()));
    cohortDao.save(cohort2);

    cohortReview = new CohortReview();
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setCohortId(cohort2.getCohortId());
    cohortReview.setMatchedParticipantCount(3);
    cohortReview.setReviewedCount(2);
    cohortReview.setReviewSize(3);
    cohortReviewDao.save(cohortReview);

    participantCohortStatusDao.save(makeStatus(cohortReview.getCohortReviewId(), 1L, CohortStatus.INCLUDED));
    participantCohortStatusDao.save(makeStatus(cohortReview.getCohortReviewId(), 2L, CohortStatus.EXCLUDED));
  }

  @Test
  public void testGetCdrQueryNoTableQuery() {
    CdrQuery cdrQuery = cohortMaterializationService.getCdrQuery(
        SearchRequests.allGenders(), new DataTableSpecification(), cohortReview, null);
    assertThat(cdrQuery.getBigqueryDataset()).isEqualTo(DATA_SET_ID);
    assertThat(cdrQuery.getBigqueryProject()).isEqualTo(PROJECT_ID);
    assertThat(cdrQuery.getSql()).isEqualTo(
        "select person.person_id person_id\n"
        + "from `project_id.data_set_id.person` person\n"
        + "where\n"
        + "person.person_id in (select person_id\n"
        + "from `project_id.data_set_id.person` p\n"
        + "where\n"
        + "p.gender_concept_id in unnest(@p0)\n"
        + ")\n"
        + "and person.person_id not in unnest(@person_id_blacklist)\n\n"
        + "order by person.person_id\n");
    Map<String, Map<String, Object>> params = getParameters(cdrQuery);
    Map<String, Object> genderParam = params.get("p0");
    Map<String, Object> personIdBlacklistParam = params.get("person_id_blacklist");
    assertParameterArray(genderParam, 8507, 8532, 2);
    assertParameterArray(personIdBlacklistParam, 2L);
  }

  private Map<String, Map<String, Object>> getParameters(CdrQuery cdrQuery) {
    Map<String, Object> configuration = (Map<String, Object>) cdrQuery.getConfiguration();
    Object[] queryParameters = (Object[])
        ((Map<String, Object>) configuration.get("query")).get("queryParameters");
    Map<String, Map<String, Object>> result = new HashMap<>();
    for (Object obj : queryParameters) {
      Map<String, Object> param = (Map<String, Object>) obj;
      result.put((String) param.get("name"), param);
    }
    return result;
  }

  private void assertParameterArray(Map<String, Object> param, long... values) {
    Map<String, Object> parameterTypeMap = (Map<String, Object>) param.get("parameterType");
    assertThat(parameterTypeMap.get("type")).isEqualTo("ARRAY");
    assertThat(((Map<String, Object>) parameterTypeMap.get("arrayType"))
          .get("type")).isEqualTo("INT64");

    Object[] paramValues = (Object[])
        ((Map<String, Object>) param.get("parameterValue")).get("arrayValues");
    assertThat(paramValues.length).isEqualTo(values.length);
    for (int i = 0; i < values.length; i++) {
      assertThat(((Map<String, Object>) paramValues[i]).get("value"))
          .isEqualTo(String.valueOf(values[i]));
    }
  }

  @Test
  public void testMaterializeAnnotationQueryNoPagination() {
    FieldSet fieldSet = new FieldSet().annotationQuery(new AnnotationQuery());
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(cohortReview, SearchRequests.allGenders(),
            null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED");
    assertResults(response, p1Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeAnnotationQueryWithPagination() {
    FieldSet fieldSet = new FieldSet().annotationQuery(new AnnotationQuery());
    MaterializeCohortRequest request = makeRequest(fieldSet, 1)
        .statusFilter(ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.EXCLUDED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(cohortReview, SearchRequests.allGenders(), null, 0, request);
    ImmutableMap<String, Object> p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED");
    assertResults(response, p1Map);
    assertThat(response.getNextPageToken()).isNotNull();

    request.setPageToken(response.getNextPageToken());
    MaterializeCohortResponse response2 =
        cohortMaterializationService.materializeCohort(cohortReview, SearchRequests.allGenders(), null, 0, request);
    ImmutableMap<String, Object> p2Map = ImmutableMap.of("person_id", 2L, "review_status", "EXCLUDED");
    assertResults(response2, p2Map);
    assertThat(response2.getNextPageToken()).isNull();
  }

  private MaterializeCohortRequest makeRequest(int pageSize) {
    return new MaterializeCohortRequest().pageSize(pageSize);
  }

  private MaterializeCohortRequest makeRequest(FieldSet fieldSet, int pageSize) {
    return makeRequest(pageSize).fieldSet(fieldSet);
  }

  private ParticipantCohortStatus makeStatus(long cohortReviewId, long participantId, CohortStatus status) {
    ParticipantCohortStatusKey key = new ParticipantCohortStatusKey()
        .cohortReviewId(cohortReviewId)
        .participantId(participantId);
    ParticipantCohortStatus result = new ParticipantCohortStatus()
        .statusEnum(status)
        .participantKey(key);
    return result;
  }

  private void assertResults(MaterializeCohortResponse actualResponse,
      ImmutableMap<String, Object>... expectedResults) {
    if (actualResponse.getResults().size() != expectedResults.length) {
      fail("Expected " + expectedResults.length + ", got " + actualResponse.getResults().size()
          + "; actual results: " + actualResponse.getResults());
    }
    for (int i = 0; i < actualResponse.getResults().size(); i++) {
      MapDifference<String, Object> difference =
          Maps.difference((Map<String, Object>) actualResponse.getResults().get(i),
              expectedResults[i]);
      if (!difference.areEqual()) {
        fail("Result " + i + " had difference: " + difference.entriesDiffering()
            + "; unexpected entries: " + difference.entriesOnlyOnLeft()
            + "; missing entries: " + difference.entriesOnlyOnRight());
      }
    }
  }


}
