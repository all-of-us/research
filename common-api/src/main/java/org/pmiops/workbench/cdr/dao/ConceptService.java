package org.pmiops.workbench.cdr.dao;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

  public static enum SearchType {
    CONCEPT_SEARCH,
    SURVEY_COUNTS,
    DOMAIN_COUNTS;
  }

  public static class ConceptIds {

    private final List<Long> standardConceptIds;
    private final List<Long> sourceConceptIds;

    public ConceptIds(List<Long> standardConceptIds, List<Long> sourceConceptIds) {
      this.standardConceptIds = standardConceptIds;
      this.sourceConceptIds = sourceConceptIds;
    }

    public List<Long> getStandardConceptIds() {
      return standardConceptIds;
    }

    public List<Long> getSourceConceptIds() {
      return sourceConceptIds;
    }
  }

  public enum StandardConceptFilter {
    ALL_CONCEPTS,
    STANDARD_CONCEPTS,
    NON_STANDARD_CONCEPTS,
    STANDARD_OR_CODE_ID_MATCH
  }

  @PersistenceContext(unitName = "cdr")
  private EntityManager entityManager;

  @Autowired private ConceptDao conceptDao;
  public static final String STANDARD_CONCEPT_CODE = "S";
  public static final String CLASSIFICATION_CONCEPT_CODE = "C";
  public static final String EMPTY_CONCEPT_CODE = "";
  public static final List<String> STANDARD_CONCEPT_CODES =
      ImmutableList.of(STANDARD_CONCEPT_CODE, CLASSIFICATION_CONCEPT_CODE);
  public static final List<String> ALL_CONCEPT_CODES =
      ImmutableList.of(STANDARD_CONCEPT_CODE, CLASSIFICATION_CONCEPT_CODE, EMPTY_CONCEPT_CODE);

  public ConceptService() {}

  // Used for tests
  public ConceptService(EntityManager entityManager, ConceptDao conceptDao) {
    this.entityManager = entityManager;
    this.conceptDao = conceptDao;
  }

  public static String modifyMultipleMatchKeyword(String query, SearchType searchType) {
    // This function modifies the keyword to match all the words if multiple words are present(by
    // adding + before each word to indicate match that matching each word is essential)
    if (query == null || query.trim().isEmpty()) {
      return null;
    }
    String[] keywords = query.split("[,+\\s+]");
    List<String> temp = new ArrayList<>();
    for (String key : keywords) {
      String tempKey;
      // This is to exact match concept codes like 100.0, 507.01. Without this mysql was matching
      // 100*, 507*.
      if (key.contains(".")) {
        tempKey = "\"" + key + "\"";
      } else {
        tempKey = key;
      }
      if (!tempKey.isEmpty()) {
        String toAdd = new String("+" + tempKey);
        if (tempKey.contains("-") && !temp.contains(tempKey)) {
          temp.add(tempKey);
        } else if (tempKey.contains("*") && tempKey.length() > 1) {
          temp.add(toAdd);
        } else {
          if (key.length() < 3) {
            temp.add(key);
          } else {
            // Only in the case of calling this method from getDomainSearchResults to fetch survey
            // counts add wildcard* search.
            // The survey view angular code fetches all the results of each survey module and then
            // checks if the search text is present in the concept name / stratum4 of achilles
            // results using regex test.
            // Without this the number of results for search smoke would be 6 while also the actual
            // results would be 12 as smoking, smoked (smoke*) are considered.
            // Changing this would address the search count discrepancy for survey results. If *
            // wildcard is added to all search types, source vocabulary code match on 507 fetches
            // all the results matching 507*. (which is not desired to show the source / standard
            // code mapping)
            // So added different search type for each purpose
            if (searchType == SearchType.SURVEY_COUNTS) {
              temp.add(new String("+" + key + "*"));
            } else {
              temp.add(toAdd);
            }
          }
        }
      }
    }

    StringBuilder query2 = new StringBuilder();
    for (String key : temp) {
      query2.append(key);
    }

    return query2.toString();
  }

  public Slice<DbConcept> searchConcepts(
      String query, String standardConceptFilter, List<String> domainIds, int limit, int page) {
    final String keyword = modifyMultipleMatchKeyword(query, SearchType.CONCEPT_SEARCH);
    Pageable pageable = new PageRequest(page, limit, new Sort(Direction.DESC, "countValue"));
    List<String> conceptTypes =
        StandardConceptFilter.STANDARD_CONCEPTS.toString().equals(standardConceptFilter)
            ? STANDARD_CONCEPT_CODES
            : ALL_CONCEPT_CODES;
    return StringUtils.isBlank(keyword)
        ? conceptDao.findConcepts(conceptTypes, domainIds, pageable)
        : conceptDao.findConcepts(keyword, conceptTypes, domainIds, pageable);
  }

  public ConceptIds classifyConceptIds(Set<Long> conceptIds) {
    ImmutableList.Builder<Long> standardConceptIds = ImmutableList.builder();
    ImmutableList.Builder<Long> sourceConceptIds = ImmutableList.builder();

    Iterable<DbConcept> concepts = conceptDao.findAll(conceptIds);
    for (DbConcept concept : concepts) {
      if (ConceptService.STANDARD_CONCEPT_CODE.equals(concept.getStandardConcept())
          || ConceptService.CLASSIFICATION_CONCEPT_CODE.equals(concept.getStandardConcept())) {
        standardConceptIds.add(concept.getConceptId());
      } else {
        // We may need to handle classification / concept hierarchy here eventually...
        sourceConceptIds.add(concept.getConceptId());
      }
    }
    return new ConceptIds(standardConceptIds.build(), sourceConceptIds.build());
  }
}
