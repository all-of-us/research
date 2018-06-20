package org.pmiops.workbench.publicapi;;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DbDomainDao;
import org.pmiops.workbench.cdr.dao.QuestionConceptDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDao;
import org.pmiops.workbench.cdr.dao.AchillesAnalysisDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.pmiops.workbench.cdr.model.*;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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
public class SampleTest {

    private static final Concept CLIENT_CONCEPT_1 = new Concept()
            .conceptId(123L)
            .conceptName("a concept")
            .standardConcept("S")
            .conceptCode("conceptA")
            .conceptClassId("classId")
            .vocabularyId("V1")
            .domainId("Condition")
            .count(123L)
            .prevalence(0.2F);

    private static final Concept CLIENT_CONCEPT_2 = new Concept()
            .conceptId(456L)
            .conceptName("b concept")
            .conceptCode("conceptB")
            .conceptClassId("classId2")
            .vocabularyId("V2")
            .domainId("Measurement")
            .count(456L)
            .prevalence(0.3F);

    private static final Concept CLIENT_CONCEPT_3 = new Concept()
            .conceptId(789L)
            .conceptName("multi word concept")
            .conceptCode("conceptC")
            .conceptClassId("classId3")
            .vocabularyId("V3")
            .domainId("Condition")
            .count(789L)
            .prevalence(0.4F);

    private static final Concept CLIENT_CONCEPT_4 = new Concept()
            .conceptId(1234L)
            .conceptName("sample test con to test the multi word search")
            .standardConcept("S")
            .conceptCode("conceptD")
            .conceptClassId("classId4")
            .vocabularyId("V4")
            .domainId("Observation")
            .count(1250L)
            .prevalence(0.5F);

    private static final Concept CLIENT_CONCEPT_5 = new Concept()
            .conceptId(7890L)
            .conceptName("conceptD test concept")
            .standardConcept("S")
            .conceptCode("conceptE")
            .conceptClassId("classId5")
            .vocabularyId("V5")
            .domainId("Condition")
            .count(7890L)
            .prevalence(0.9F);

    private static final Concept CLIENT_CONCEPT_6 = new Concept()
            .conceptId(7891L)
            .conceptName("conceptD test concept 2")
            .standardConcept(null)
            .conceptCode("conceptD")
            .conceptClassId("classId6")
            .vocabularyId("V6")
            .domainId("Condition")
            .count(7891L)
            .prevalence(0.1F);

    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
            makeConcept(CLIENT_CONCEPT_1);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
            makeConcept(CLIENT_CONCEPT_2);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_3 =
            makeConcept(CLIENT_CONCEPT_3);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_4 =
            makeConcept(CLIENT_CONCEPT_4);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_5 =
            makeConcept(CLIENT_CONCEPT_5);
    private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_6 =
            makeConcept(CLIENT_CONCEPT_6);

    @TestConfiguration
    @Import({
            ConceptService.class
    })
    @MockBean({
            ConceptService.class
    })
    static class Configuration {
    }

    @Autowired
    private ConceptDao conceptDao;
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    @Autowired
    private DbDomainDao dbDomainDao;
    @Autowired
    private ConceptService conceptService;

    @PersistenceContext
    private EntityManager entityManager;

    private DataBrowserController dataBrowserController;


    @Before
    public void setUp() {
        ConceptService conceptService = new ConceptService(entityManager);
        dataBrowserController = new DataBrowserController(conceptService, conceptDao);
    }


    @Test
    public void testSearch() throws Exception {
        assertResults(
                dataBrowserController.getConceptsSearch("conceptD","S","Condition"), CLIENT_CONCEPT_4);
    }

    private static org.pmiops.workbench.cdr.model.Concept makeConcept(Concept concept) {
        org.pmiops.workbench.cdr.model.Concept result = new org.pmiops.workbench.cdr.model.Concept();
        result.setConceptId(concept.getConceptId());
        result.setConceptName(concept.getConceptName());
        result.setStandardConcept(concept.getStandardConcept() == null ? null :
                (concept.getStandardConcept().equals("S") ? "S" : "C"));
        result.setConceptCode(concept.getConceptCode());
        result.setConceptClassId(concept.getConceptClassId());
        result.setVocabularyId(concept.getVocabularyId());
        result.setDomainId(concept.getDomainId());
        result.setCountValue(concept.getCountValue());
        result.setPrevalence(concept.getPrevalence());
        return result;
    }

    private void saveConcepts() {
        conceptDao.save(CONCEPT_1);
        conceptDao.save(CONCEPT_2);
        conceptDao.save(CONCEPT_3);
        conceptDao.save(CONCEPT_4);
        conceptDao.save(CONCEPT_5);
        conceptDao.save(CONCEPT_6);
    }

    private void assertResults(ResponseEntity<ConceptListResponse> response,
                               Concept... expectedConcepts) {
        assertThat(response.getBody().getItems().equals(Arrays.asList(expectedConcepts)));
    }
}
