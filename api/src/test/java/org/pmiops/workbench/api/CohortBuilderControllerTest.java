package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.CriteriaMapperImpl;
import org.pmiops.workbench.cohortbuilder.mappers.CriteriaMapper;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortBuilderControllerTest {

  private CohortBuilderController controller;

  @Mock private BigQueryService bigQueryService;

  @Mock private CloudStorageService cloudStorageService;

  @Mock private CohortQueryBuilder cohortQueryBuilder;

  @Mock private CdrVersionService cdrVersionService;

  @Mock private CohortBuilderService cohortBuilderService;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CriteriaMapper criteriaMapper;

  @Mock private Provider<WorkbenchConfig> configProvider;

  @TestConfiguration
  @Import({CriteriaMapperImpl.class})
  static class Configuration {}

  @Before
  public void setUp() {
    ElasticSearchService elasticSearchService =
        new ElasticSearchService(cbCriteriaDao, cloudStorageService, configProvider);

    controller =
        new CohortBuilderController(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaDao,
            cbCriteriaAttributeDao,
            cdrVersionService,
            elasticSearchService,
            configProvider,
            cohortBuilderService,
            criteriaMapper);
  }

  @Test
  public void getCriteriaBy() {
    DbCriteria icd9CriteriaParent =
        DbCriteria.builder()
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addCount("0")
            .addHierarchy(true)
            .addStandard(false)
            .addParentId(0L)
            .build();
    cbCriteriaDao.save(icd9CriteriaParent);
    DbCriteria icd9Criteria =
        DbCriteria.builder()
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addCount("0")
            .addHierarchy(true)
            .addStandard(false)
            .addParentId(icd9CriteriaParent.getId())
            .build();
    cbCriteriaDao.save(icd9Criteria);

    assertEquals(
        createResponseCriteria(icd9CriteriaParent),
        controller
            .getCriteriaBy(
                1L, DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L)
            .getBody()
            .getItems()
            .get(0));
    assertEquals(
        createResponseCriteria(icd9Criteria),
        controller
            .getCriteriaBy(
                1L,
                DomainType.CONDITION.toString(),
                CriteriaType.ICD9CM.toString(),
                false,
                icd9CriteriaParent.getId())
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaByExceptions() {
    try {
      controller.getCriteriaBy(1L, null, null, false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. null is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaBy(1L, "blah", null, false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. blah is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaBy(1L, DomainType.CONDITION.toString(), "blah", false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void getCriteriaByDemo() {
    DbCriteria demoCriteria =
        DbCriteria.builder()
            .addDomainId(DomainType.PERSON.toString())
            .addType(CriteriaType.AGE.toString())
            .addCount("0")
            .addParentId(0L)
            .build();
    cbCriteriaDao.save(demoCriteria);

    assertEquals(
        createResponseCriteria(demoCriteria),
        controller
            .getCriteriaBy(
                1L, DomainType.PERSON.toString(), CriteriaType.AGE.toString(), false, null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteMatchesSynonyms() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(DomainType.MEASUREMENT.toString())
            .addType(CriteriaType.LOINC.toString())
            .addCount("0")
            .addHierarchy(true)
            .addStandard(true)
            .addSynonyms("LP12*[MEASUREMENT_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getCriteriaAutoComplete(
                1L,
                DomainType.MEASUREMENT.toString(),
                "LP12",
                CriteriaType.LOINC.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteMatchesCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(DomainType.MEASUREMENT.toString())
            .addType(CriteriaType.LOINC.toString())
            .addCount("0")
            .addHierarchy(true)
            .addStandard(true)
            .addCode("LP123")
            .addSynonyms("+[MEASUREMENT_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getCriteriaAutoComplete(
                1L,
                DomainType.MEASUREMENT.toString(),
                "LP12",
                CriteriaType.LOINC.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteSnomed() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.SNOMED.toString())
            .addCount("0")
            .addHierarchy(true)
            .addStandard(true)
            .addSynonyms("LP12*[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getCriteriaAutoComplete(
                1L,
                DomainType.CONDITION.toString(),
                "LP12",
                CriteriaType.SNOMED.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void getCriteriaAutoCompleteExceptions() {
    try {
      controller.getCriteriaAutoComplete(1L, null, "blah", null, null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. null is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaAutoComplete(1L, "blah", "blah", "blah", null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. blah is not valid.", bre.getMessage());
    }

    try {
      controller.getCriteriaAutoComplete(
          1L, DomainType.CONDITION.toString(), "blah", "blah", null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesSourceCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("001")
            .addCount("10")
            .addConceptId("123")
            .addDomainId(DomainType.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.ICD9CM.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(false)
            .addSynonyms("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "001", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermLikeSourceCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("00")
            .addCount("10")
            .addConceptId("123")
            .addDomainId(DomainType.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.ICD9CM.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(false)
            .addSynonyms("+[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    List<Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "00", null)
            .getBody()
            .getItems();

    assertEquals(1, results.size());
    assertEquals(createResponseCriteria(criteria), results.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesStandardCodeBrand() {
    DbCriteria criteria1 =
        DbCriteria.builder()
            .addCode("672535")
            .addCount("-1")
            .addConceptId("19001487")
            .addDomainId(DomainType.DRUG.toString())
            .addGroup(Boolean.FALSE)
            .addSelectable(Boolean.TRUE)
            .addName("4-Way")
            .addParentId(0)
            .addType(CriteriaType.BRAND.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addSynonyms("[DRUG_rank1]")
            .build();
    cbCriteriaDao.save(criteria1);

    List<Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "672535", null)
            .getBody()
            .getItems();
    assertEquals(1, results.size());
    assertEquals(createResponseCriteria(criteria1), results.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesStandardCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("LP12")
            .addCount("10")
            .addConceptId("123")
            .addDomainId(DomainType.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.LOINC.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addSynonyms("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesSynonyms() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("001")
            .addCount("10")
            .addConceptId("123")
            .addDomainId(DomainType.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.LOINC.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addSynonyms("LP12*[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesSynonyms() {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("001")
            .addCount("10")
            .addConceptId("123")
            .addDomainId(DomainType.DRUG.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.ATC.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addSynonyms("LP12*[DRUG_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void getStandardCriteriaByDomainAndConceptId() {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(DomainType.CONDITION.toString())
            .addType(CriteriaType.ICD10CM.toString())
            .addStandard(true)
            .addCount("1")
            .addConceptId("1")
            .addSynonyms("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);
    assertEquals(
        createResponseCriteria(criteria),
        controller
            .getStandardCriteriaByDomainAndConceptId(1L, DomainType.CONDITION.toString(), 12345L)
            .getBody()
            .getItems()
            .get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void getDrugBrandOrIngredientByName() {
    DbCriteria drugATCCriteria =
        DbCriteria.builder()
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.ATC.toString())
            .addParentId(0L)
            .addCode("LP12345")
            .addName("drugName")
            .addConceptId("12345")
            .addGroup(true)
            .addSelectable(true)
            .addCount("12")
            .build();
    cbCriteriaDao.save(drugATCCriteria);
    DbCriteria drugBrandCriteria =
        DbCriteria.builder()
            .addDomainId(DomainType.DRUG.toString())
            .addType(CriteriaType.BRAND.toString())
            .addParentId(0L)
            .addCode("LP6789")
            .addName("brandName")
            .addConceptId("1235")
            .addGroup(true)
            .addSelectable(true)
            .addCount("33")
            .build();
    cbCriteriaDao.save(drugBrandCriteria);

    assertEquals(
        createResponseCriteria(drugATCCriteria),
        controller.getDrugBrandOrIngredientByValue(1L, "drugN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrandCriteria),
        controller.getDrugBrandOrIngredientByValue(1L, "brandN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrandCriteria),
        controller.getDrugBrandOrIngredientByValue(1L, "LP6789", null).getBody().getItems().get(0));
  }

  @Test
  public void getCriteriaAttributeByConceptId() {
    DbCriteriaAttribute criteriaAttributeMin =
        cbCriteriaAttributeDao.save(
            new DbCriteriaAttribute()
                .conceptId(1L)
                .conceptName("MIN")
                .estCount("10")
                .type("NUM")
                .valueAsConceptId(0L));
    DbCriteriaAttribute criteriaAttributeMax =
        cbCriteriaAttributeDao.save(
            new DbCriteriaAttribute()
                .conceptId(1L)
                .conceptName("MAX")
                .estCount("100")
                .type("NUM")
                .valueAsConceptId(0L));

    List<CriteriaAttribute> attrs =
        controller
            .getCriteriaAttributeByConceptId(1L, criteriaAttributeMin.getConceptId())
            .getBody()
            .getItems();
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin)));
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax)));
  }

  @Test
  public void isApproximate() {
    SearchParameter inSearchParameter = new SearchParameter();
    SearchParameter exSearchParameter = new SearchParameter();
    SearchGroupItem inSearchGroupItem =
        new SearchGroupItem().addSearchParametersItem(inSearchParameter);
    SearchGroupItem exSearchGroupItem =
        new SearchGroupItem().addSearchParametersItem(exSearchParameter);
    SearchGroup inSearchGroup = new SearchGroup().addItemsItem(inSearchGroupItem);
    SearchGroup exSearchGroup = new SearchGroup().addItemsItem(exSearchGroupItem);
    SearchRequest searchRequest =
        new SearchRequest().addIncludesItem(inSearchGroup).addExcludesItem(exSearchGroup);
    // Temporal includes
    inSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP includes
    inSearchGroup.temporal(false);
    inSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased includes
    inSearchParameter.type(CriteriaType.DECEASED.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP includes
    inSearchGroup.temporal(true);
    inSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // No temporal/BP/Decease
    inSearchGroup.temporal(false);
    inSearchParameter.type(CriteriaType.ETHNICITY.toString()).subtype(null);
    assertFalse(controller.isApproximate(searchRequest));
    // Temporal excludes
    exSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP excludes
    exSearchGroup.temporal(false);
    exSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased excludes
    exSearchParameter.type(CriteriaType.DECEASED.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP excludes
    exSearchGroup.temporal(true);
    exSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
  }

  private Criteria createResponseCriteria(DbCriteria cbCriteria) {
    return new Criteria()
        .code(cbCriteria.getCode())
        .conceptId(cbCriteria.getConceptId() == null ? null : new Long(cbCriteria.getConceptId()))
        .count(new Long(cbCriteria.getCount()))
        .domainId(cbCriteria.getDomainId())
        .group(cbCriteria.getGroup())
        .hasAttributes(cbCriteria.getAttribute())
        .id(cbCriteria.getId())
        .name(cbCriteria.getName())
        .parentId(cbCriteria.getParentId())
        .selectable(cbCriteria.getSelectable())
        .subtype(cbCriteria.getSubtype())
        .type(cbCriteria.getType())
        .path(cbCriteria.getPath())
        .hasAncestorData(cbCriteria.getAncestorData())
        .hasHierarchy(cbCriteria.getHierarchy())
        .isStandard(cbCriteria.getStandard())
        .value(cbCriteria.getValue());
  }

  private CriteriaAttribute createResponseCriteriaAttribute(DbCriteriaAttribute criteriaAttribute) {
    return new CriteriaAttribute()
        .id(criteriaAttribute.getId())
        .valueAsConceptId(criteriaAttribute.getValueAsConceptId())
        .conceptName(criteriaAttribute.getConceptName())
        .type(criteriaAttribute.getType())
        .estCount(criteriaAttribute.getEstCount());
  }
}
