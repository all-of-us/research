package org.pmiops.workbench.elasticsearch;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ElasticFiltersTest {

  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static CBCriteria icd9Criteria() {
    return new CBCriteria()
        .domainId(DomainType.CONDITION.toString())
        .type(CriteriaType.ICD9CM.toString())
        .attribute(Boolean.FALSE);
  }

  private static CBCriteria drugCriteria() {
    return new CBCriteria()
        .domainId(DomainType.DRUG.toString())
        .type(CriteriaType.ATC.toString())
        .attribute(Boolean.FALSE);
  }

  private static CBCriteria basicsCriteria() {
    return new CBCriteria()
        .domainId(DomainType.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BASICS.toString())
        .attribute(Boolean.FALSE);
  }

  private SearchParameter leafParam2;

  @Before
  public void setUp() {
    // Generate a simple test criteria tree
    // 1
    // | - 2
    // | - 3 - 4
    cbCriteriaDao.save(
        icd9Criteria()
            .id(1)
            .code("001")
            .conceptId("771")
            .group(true)
            .selectable(false)
            .parentId(0)
            .path(""));
    cbCriteriaDao.save(
        icd9Criteria()
            .id(2)
            .code("001.002")
            .conceptId("772")
            .group(false)
            .selectable(true)
            .parentId(1)
            .path("1"));
    cbCriteriaDao.save(
        icd9Criteria()
            .id(3)
            .code("001.003")
            .conceptId("773")
            .group(true)
            .selectable(true)
            .parentId(1)
            .path("1"));
    cbCriteriaDao.save(
        icd9Criteria()
            .id(4)
            .code("001.003.004")
            .conceptId("774")
            .group(false)
            .selectable(true)
            .parentId(3)
            .path("1.3"));

    // Singleton SNOMED code.
    cbCriteriaDao.save(
        new CBCriteria()
            .id(5)
            .code("005")
            .conceptId("775")
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .attribute(Boolean.FALSE)
            .group(false)
            .selectable(true)
            .parentId(0)
            .path(""));

    // Four node PPI tree, survey, question, 2 answers.
    cbCriteriaDao.save(
        basicsCriteria()
            .id(6)
            .code("006")
            .group(true)
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .standard(false)
            .selectable(true)
            .parentId(0)
            .path("")
            .synonyms("+[SURVEY_rank1]"));
    cbCriteriaDao.save(
        basicsCriteria()
            .id(7)
            .code("007")
            .conceptId("777")
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .standard(false)
            .group(true)
            .selectable(true)
            .parentId(6)
            .path("6.7")
            .synonyms("+[SURVEY_rank1]"));
    cbCriteriaDao.save(
        basicsCriteria()
            .id(8)
            .code("008")
            // Concept ID matches the question.
            .conceptId("7771")
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .standard(false)
            .group(false)
            .selectable(true)
            .parentId(7)
            .path("6.7")
            .synonyms("+[SURVEY_rank1]"));

    // drug tree
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 21600009)");
    cbCriteriaDao.save(
        drugCriteria()
            .id(10)
            .code("A")
            .conceptId("21600001")
            .group(true)
            .selectable(false)
            .parentId(0)
            .path("")
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString()));
    cbCriteriaDao.save(
        drugCriteria()
            .id(11)
            .code("A01")
            .conceptId("21600002")
            .group(true)
            .selectable(true)
            .parentId(10)
            .path("10")
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString()));
    cbCriteriaDao.save(
        drugCriteria()
            .id(12)
            .code("A01A")
            .conceptId("21600003")
            .group(true)
            .selectable(true)
            .parentId(11)
            .path("10.11")
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString()));
    cbCriteriaDao.save(
        drugCriteria()
            .id(13)
            .code("A01AA")
            .conceptId("21600004")
            .group(true)
            .selectable(true)
            .parentId(12)
            .path("10.11.12")
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString()));
    cbCriteriaDao.save(
        drugCriteria()
            .id(14)
            .code("9873")
            .conceptId("19069022")
            .group(false)
            .selectable(true)
            .parentId(13)
            .path("10.11.12.13")
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString()));

    leafParam2 =
        new SearchParameter()
            .conceptId(772L)
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .ancestorData(false)
            .standard(false)
            .group(false);
  }

  @After
  public void tearDown() {
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  private static final QueryBuilder singleNestedQuery(QueryBuilder... inners) {
    return singleNestedQueryOccurrences(1, inners);
  }

  private static final QueryBuilder singleNestedQueryOccurrences(int n, QueryBuilder... inners) {
    BoolQueryBuilder b = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      b.filter(in);
    }
    return QueryBuilders.boolQuery()
        .filter(
            QueryBuilders.boolQuery()
                .should(
                    QueryBuilders.functionScoreQuery(
                            QueryBuilders.nestedQuery(
                                "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
                        .setMinScore(n)));
  }

  private static final QueryBuilder nonNestedQuery(QueryBuilder... inners) {
    BoolQueryBuilder innerBuilder = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      BoolQueryBuilder b = QueryBuilders.boolQuery().filter(in);
      innerBuilder.should(b);
    }
    return QueryBuilders.boolQuery().filter(innerBuilder);
  }

  private static final QueryBuilder nonNestedMustNotQuery(QueryBuilder... inners) {
    BoolQueryBuilder innerBuilder = QueryBuilders.boolQuery();
    for (QueryBuilder in : inners) {
      BoolQueryBuilder b = QueryBuilders.boolQuery().filter(in);
      innerBuilder.should(b);
    }
    return QueryBuilders.boolQuery().filter(innerBuilder).mustNot(innerBuilder);
  }

  @Test
  public void testLeafQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(leafParam2))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))));
  }

  @Test
  public void testParentConceptQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .conceptId(21600002L)
                                        .domain(DomainType.DRUG.toString())
                                        .type(CriteriaType.ATC.toString())
                                        .ancestorData(true)
                                        .standard(true)
                                        .group(true)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of("21600009"))));
  }

  @Test
  public void testParentCodeQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .value("001")
                                        .type(TreeType.ICD9.toString())
                                        .subtype(TreeSubType.CM.toString())
                                        .group(true)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery(
                    "events.source_concept_id", ImmutableList.of("772", "774"))));
  }

  @Test
  public void testPPISurveyQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .type(TreeType.PPI.toString())
                                        .subtype(TreeSubType.BASICS.toString())
                                        .group(true)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("777"))));
  }

  @Test
  public void testPPIQuestionQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .type(TreeType.PPI.toString())
                                        .subtype(TreeSubType.BASICS.toString())
                                        .conceptId(777L)
                                        .group(true)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("777"))));
  }

  @Test
  public void testPPIAnswerQuery() {
    Attribute attr =
        new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).addOperandsItem("1");
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(
                                    new SearchParameter()
                                        .domain(DomainType.SURVEY.toString())
                                        .type(CriteriaType.PPI.toString())
                                        .subtype(CriteriaSubType.BASICS.toString())
                                        .conceptId(777L)
                                        .group(true)
                                        .ancestorData(false)
                                        .standard(false)
                                        .addAttributesItem(attr)))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("7771")),
                QueryBuilders.rangeQuery("events.value_as_number").gte(1.0F).lte(1.0F)));
  }

  @Test
  public void testAgeAtEventModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.AGE_AT_EVENT)
                                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                                        .addOperandsItem("18")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                QueryBuilders.rangeQuery("events.age_at_start").gte(18)));
  }

  @Test
  public void testDateModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.EVENT_DATE)
                                        .operator(Operator.BETWEEN)
                                        .addOperandsItem("12/25/1988")
                                        .addOperandsItem("12/27/1988")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                QueryBuilders.rangeQuery("events.start_date").gte("12/25/1988").lte("12/27/1988")));
  }

  @Test
  public void testVisitsModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.ENCOUNTERS)
                                        .operator(Operator.IN)
                                        .addOperandsItem("123")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                QueryBuilders.termsQuery("events.visit_concept_id", ImmutableList.of("123"))));
  }

  @Test
  public void testNumOfOccurrencesModifierQuery() {
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .addSearchParametersItem(leafParam2)
                                .addModifiersItem(
                                    new Modifier()
                                        .name(ModifierType.NUM_OF_OCCURRENCES)
                                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                                        .addOperandsItem("13")))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQueryOccurrences(
                13, QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))));
  }

  @Test
  public void testHeightAnyQuery() {
    String conceptId = "903133";
    SearchParameter heightAnyParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .type(TreeType.PM.toString())
            .subtype(TreeSubType.HEIGHT.toString())
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem().addSearchParametersItem(heightAnyParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testHeightEqualQuery() {
    Attribute attr =
        new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Object left = Float.parseFloat(attr.getOperands().get(0));
    Object right = Float.parseFloat(attr.getOperands().get(0));
    String conceptId = "903133";
    SearchParameter heightParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .type(TreeType.PM.toString())
            .subtype(TreeSubType.HEIGHT.toString())
            .group(false)
            .addAttributesItem(attr);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(heightParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.rangeQuery("events.value_as_number").gte(left).lte(right)));
  }

  @Test
  public void testWeightBetweenQuery() {
    Attribute attr =
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.BETWEEN)
            .operands(Arrays.asList("1", "2"));
    Object left = Float.parseFloat(attr.getOperands().get(0));
    Object right = Float.parseFloat(attr.getOperands().get(1));
    String conceptId = "903121";
    SearchParameter weightParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .type(TreeType.PM.toString())
            .subtype(TreeSubType.HEIGHT.toString())
            .group(false)
            .addAttributesItem(attr);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(weightParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.rangeQuery("events.value_as_number").gte(left).lte(right)));
  }

  @Test
  public void testGenderQuery() {
    String conceptId = "8507";
    SearchParameter genderParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .type(TreeType.DEMO.toString())
            .subtype(TreeSubType.GEN.toString())
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testGenderExcludeQuery() {
    String conceptId = "8507";
    SearchParameter genderParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addExcludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testGenderIncludeAndExcludeQuery() {
    String conceptId = "8507";
    SearchParameter genderParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam)))
                .addExcludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(genderParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedMustNotQuery(
                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testRaceQuery() {
    String conceptId = "8515";
    SearchParameter raceParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.RACE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(raceParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("race_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testEthnicityQuery() {
    String conceptId = "38003563";
    SearchParameter ethParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.ETHNICITY.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(ethParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.termsQuery("ethnicity_concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testPregnancyQuery() {
    String conceptId = "903120";
    String operand = "12345";
    Attribute attr =
        new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList(operand));
    SearchParameter pregParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .group(false)
            .ancestorData(false)
            .standard(false)
            .attributes(Arrays.asList(attr));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(pregParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.termsQuery("events.value_as_concept_id", ImmutableList.of(operand))));
  }

  @Test
  public void testMeasurementCategoricalQuery() {
    String conceptId = "3015813";
    String operand1 = "12345";
    String operand2 = "12346";
    Attribute attr =
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(Arrays.asList(operand1, operand2));
    SearchParameter measParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .attributes(Arrays.asList(attr));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(measParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of(conceptId)),
                QueryBuilders.termsQuery(
                    "events.value_as_concept_id", ImmutableList.of(operand1, operand2))));
  }

  @Test
  public void testVisitQuery() {
    String conceptId = "9202";
    SearchParameter visitParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .ancestorData(false)
            .standard(true)
            .group(false);
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(visitParam))));
    assertThat(resp)
        .isEqualTo(
            singleNestedQuery(
                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of(conceptId))));
  }

  @Test
  public void testAgeQuery() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Object left = now.minusYears(34).minusYears(1).toLocalDate();
    Object right = now.minusYears(20).toLocalDate();
    SearchParameter ethParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .addAttributesItem(
                new Attribute()
                    .name(AttrName.AGE)
                    .operator(Operator.BETWEEN)
                    .operands(Arrays.asList("20", "34")));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(new SearchGroupItem().addSearchParametersItem(ethParam))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.rangeQuery("birth_datetime")
                    .gt(left)
                    .lte(right)
                    .format("yyyy-MM-dd")));
  }

  @Test
  public void testAgeAndEthnicityQuery() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Object left = now.minusYears(34).minusYears(1).toLocalDate();
    Object right = now.minusYears(20).toLocalDate();
    SearchParameter ageParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .addAttributesItem(
                new Attribute()
                    .name(AttrName.AGE)
                    .operator(Operator.BETWEEN)
                    .operands(Arrays.asList("20", "34")));
    String conceptId = "38003563";
    SearchParameter ethParam =
        new SearchParameter()
            .conceptId(Long.parseLong(conceptId))
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.ETHNICITY.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(Long.parseLong(conceptId));
    QueryBuilder resp =
        ElasticFilters.fromCohortSearch(
            cbCriteriaDao,
            new SearchRequest()
                .addIncludesItem(
                    new SearchGroup()
                        .addItemsItem(
                            new SearchGroupItem()
                                .searchParameters(Arrays.asList(ageParam, ethParam)))));
    assertThat(resp)
        .isEqualTo(
            nonNestedQuery(
                QueryBuilders.rangeQuery("birth_datetime").gt(left).lte(right).format("yyyy-MM-dd"),
                QueryBuilders.termsQuery("ethnicity_concept_id", ImmutableList.of(conceptId))));
  }
}
