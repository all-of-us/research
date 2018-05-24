package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptsControllerTest {

  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
      makeConcept(1L, "a concept", "S",
          "conceptA", "classId", "V1",
          "D1", 123L, 0.2F);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
      makeConcept(2L, "b concept", null,
          "conceptB", "classId2", "V2",
          "D2", 456L, 0.3F);

  private static final Concept CLIENT_CONCEPT_1 = makeClientConcept(CONCEPT_1);
  private static final Concept CLIENT_CONCEPT_2 = makeClientConcept(CONCEPT_2);

  @Autowired
  private ConceptDao conceptDao;
  @PersistenceContext
  private EntityManager entityManager;

  private ConceptsController conceptsController;

  @Before
  public void setUp() {
    // Injecting ConceptsController and ConceptService doesn't work well without using
    // SpringBootTest, which causes problems with CdrDbConfig. Just construct the service and
    // controller directly.
    ConceptService conceptService = new ConceptService(entityManager);
    conceptsController = new ConceptsController(conceptService);
  }

  @Test
  public void testSearchNoConcepts() throws Exception {
    assertResults(
        conceptsController.searchConcepts("a", null, null,
          null, null));
  }

  @Test
  public void testConceptsSearchNameOneMatch() throws Exception {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    assertResults(
        conceptsController.searchConcepts("a", null, null,
            null, null), CLIENT_CONCEPT_1);
  }

  private static org.pmiops.workbench.cdr.model.Concept makeConcept(
      long conceptId, String conceptName, String standardConcept, String conceptCode,
      String conceptClassId, String vocabularyId, String domainId, long countValue,
      float prevalence) {
    org.pmiops.workbench.cdr.model.Concept result = new org.pmiops.workbench.cdr.model.Concept();
    result.setConceptId(conceptId);
    result.setConceptName(conceptName);
    result.setStandardConcept(standardConcept);
    result.setConceptCode(conceptCode);
    result.setConceptClassId(conceptClassId);
    result.setVocabularyId(vocabularyId);
    result.setDomainId(domainId);
    result.setCountValue(countValue);
    result.setPrevalence(prevalence);
    return result;
  }

  private static Concept makeClientConcept(org.pmiops.workbench.cdr.model.Concept dbConcept) {
    Concept concept = new Concept();
    concept.setConceptId(dbConcept.getConceptId());
    concept.setConceptName(dbConcept.getConceptName());
    concept.setStandardConcept(ConceptService.STANDARD_CONCEPT_CODE.equals(
        dbConcept.getStandardConcept()));
    concept.setConceptCode(dbConcept.getConceptCode());
    concept.setConceptClassId(dbConcept.getConceptClassId());
    concept.setVocabularyId(dbConcept.getVocabularyId());
    concept.setDomainId(dbConcept.getDomainId());
    concept.setCountValue(dbConcept.getCountValue());
    concept.setPrevalence(dbConcept.getPrevalence());
    return concept;
  }


  private void assertResults(ResponseEntity<ConceptListResponse> response,
      Concept... expectedConcepts) {
    assertThat(response.getBody().getItems().equals(Arrays.asList(expectedConcepts)));
  }
}
