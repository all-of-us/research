package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cdr.model.StandardProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CBCriteriaDao extends CrudRepository<CBCriteria, Long> {

  @Query(value = "select * from cb_criteria where domain_id=:domain and type=:type and is_standard=:standard and parent_id=:parentId and has_hierarchy = 1 order by id asc", nativeQuery = true)
  List<CBCriteria> findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(@Param("domain") String domain,
                                                                        @Param("type") String type,
                                                                        @Param("standard") Boolean isStandard,
                                                                        @Param("parentId") Long parentId);

  @Query(value = "select c from CBCriteria c where domainId=:domain and type=:type order by id asc")
  List<CBCriteria> findCriteriaByDomainAndTypeOrderByIdAsc(@Param("domain") String domain,
                                                           @Param("type") String type);

  @Query(value = "select c.standard as standard from CBCriteria c where domainId=:domain and code=:term order by standard desc")
  List<StandardProjection> findStandardProjectionByCode(@Param("domain") String domain,
                                                        @Param("term") String term);

  @Query(value = "select c from CBCriteria c where domainId=:domain and standard=:standard and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndCode(@Param("domain") String domain,
                                               @Param("standard") Boolean isStandard,
                                               @Param("term") String term,
                                               Pageable page);

  @Query(value = "select c from CBCriteria c where domainId=:domain and standard=:standard and match(synonyms, :term) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndSynonyms(@Param("domain") String domain,
                                                   @Param("standard") Boolean isStandard,
                                                   @Param("term") String term,
                                                   Pageable page);

  @Query(value = "select c from CBCriteria c where domainId=:domain and type=:type and standard=:standard and hierarchy=1 and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeAndStandardAndCode(@Param("domain") String domain,
                                                                 @Param("type") String type,
                                                                 @Param("standard") Boolean standard,
                                                                 @Param("term") String term,
                                                                 Pageable page);

  @Query(value = "select c from CBCriteria c where domainId = :domain and type = :type and standard = :standard and hierarchy=1 and match(synonyms, :term) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeAndStandardAndSynonyms(@Param("domain") String domain,
                                                                     @Param("type") String type,
                                                                     @Param("standard") Boolean standard,
                                                                     @Param("term") String term,
                                                                     Pageable page);

  @Query(value = "select * from cb_criteria c where domain_id = 'DRUG' and type in ('ATC', 'BRAND', 'RXNORM') and c.is_selectable = 1 and " +
    "(upper(c.name) like upper(concat('%',:term,'%')) or upper(c.code) like upper(concat('%',:term,'%'))) order by c.name asc limit :limit", nativeQuery = true)
  List<CBCriteria> findDrugBrandOrIngredientByValue(@Param("term") String term,
                                                    @Param("limit") Long limit);

  @Query(value = "select * from cb_criteria c inner join ( select cr.concept_id_2 from cb_criteria_relationship cr join concept c1 on (cr.concept_id_2 = c1.concept_id " +
    "and cr.concept_id_1 in (:conceptIds) and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2 and c.domain_id = 'DRUG' and c.type = 'RXNORM'", nativeQuery = true)
  List<CBCriteria> findDrugIngredientByConceptId(@Param("conceptIds") List<Long> conceptIds);
}
