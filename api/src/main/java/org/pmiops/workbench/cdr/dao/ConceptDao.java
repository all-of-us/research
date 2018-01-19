package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.persistence.QueryHint;
import java.util.List;

@CacheConfig(cacheNames = "concept")
public interface ConceptDao extends CrudRepository<Concept, Long> {

    @Query(value = "select c.* " +
            "from cdr.concept c " +
            "where (match(c.concept_name) against(:conceptName in boolean mode) or " +
            "match(c.concept_code) against(:conceptName in boolean mode)) and " +
            "c.domain_id in ('Condition','Observation','Procedure', 'Measurement', 'Drug') " +
            "order by c.count_value desc limit 10;",
            nativeQuery = true)
    List<Concept> findConceptLikeName(@Param("conceptName") String conceptName);

    @Query(value = "select c.* from cdr.concept c " +
            "where c.domain_id in ('Condition','Observation','Procedure', 'Measurement', 'Drug') " +
            "order by c.count_value desc limit 10;",
    nativeQuery = true)
    List<Concept> findConceptsOrderedByCount();

    @Query(value = "select c.* from cdr.concept c " +
            "join cdr.concept_relationship rel on " +
            "rel.concept_id_1 = c.concept_id and rel.concept_id_2 = :conceptId and " +
            "rel.relationship_id = 'maps to' where c.concept_id != :conceptId order " +
            "by c.count_value desc",
            nativeQuery = true)
    List<Concept> findConceptsMapsToChildren(@Param("conceptId") long conceptId);

    @Query(value = "select c.* from cdr.concept c " +
            "join cdr.concept_relationship rel on rel.concept_id_2 = c.concept_id " +
            "and rel.concept_id_1 = :conceptId and rel.relationship_id = 'maps to' " +
            "where c.concept_id != :conceptId order by c.count_value desc",
            nativeQuery = true)
    List<Concept> findConceptsMapsToParents(@Param("conceptId") long conceptId);

    List<Concept> findByConceptName(String conceptName);

    @QueryHints(value = {
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "CacheRegion")
    })
    List<Concept> findByVocabularyIdIn(List<String> ids);
}
