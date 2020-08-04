package org.pmiops.workbench.cdr.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaLookup;
import org.pmiops.workbench.cdr.model.DbMenuOption;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Some of our trees are polyhierarchical(Snomed and drug). Since a node may exist multiple times in
 * this scenario with the same concept_id we only want to return it once from a user search. The
 * query to rank/min this to a single row was expensive. Instead, when building the cb_criteria
 * table we add domain_rank1 to the synonyms column for the first node that matches in the tree for
 * a specific concept_id. This allows us to use the full text index and makes the query much faster.
 */
public interface CBCriteriaDao extends CrudRepository<DbCriteria, Long> {

  /**
   * This query returns the parents in addition to the criteria ancestors for each leave in order to
   * allow the client to determine the relationship between the returned Criteria. Since we use
   * the @Id annotation in the entity, JPA will treat those rows with the given id as a unique
   * element. For example if you have 3 rows where this @Id annotated field is the same, JPA will
   * use the first result elements and just duplicate them.
   *
   * <p>select statement returns:
   *
   * <p>id: 111, concept_id : 111
   *
   * <p>id: 111, concept_id : 222
   *
   * <p>id: 111, concept_id : 333
   *
   * <p>The result will be 3 rows retrieved by JPA:
   *
   * <p>id: 111 concept_id: 111
   *
   * <p>id: 111 concept_id: 111
   *
   * <p>id: 111 concept_id: 111
   *
   * <p>So to keep this from happening we use the descendant_id from cb_criteria_ancestor as the
   * unique id. Please do not use this method if you need proper cb_criteria ids, we don't in the
   * current use case.
   */
  @Query(
      value =
          "select distinct c.concept_id as conceptId, c2.id as parentId from ( "
              + "select distinct ca.descendant_id as concept_id,path "
              + "from cb_criteria_ancestor ca "
              + "   join (select a.* "
              + "           from cb_criteria a "
              + "           join (select concat('%.', id, '%') as path "
              + "                   from cb_criteria "
              + "                  where domain_id = :domain "
              + "                    and type = :type "
              + "                    and is_group = :group "
              + "                    and is_selectable = 1 "
              + "                    and concept_id in (:parentConceptIds)) b "
              + "             on (a.path like b.path) "
              + "          where domain_id = :domain "
              + "            and is_group = 0  "
              + "            and is_selectable = 1 "
              + "            and type = 'RXNORM') c "
              + "    on (ca.ancestor_id = c.concept_id) "
              + "union "
              + "select a.concept_id,a.path "
              + "  from cb_criteria a "
              + "  where a.domain_id = :domain "
              + "   and a.type = :type "
              + "   and a.concept_id in (:parentConceptIds)) c "
              + "join (select id from cb_criteria where concept_id in (:parentConceptIds)) c2 on c.path like concat('%', c2.id, '%') ",
      nativeQuery = true)
  List<DbCriteriaLookup> findCriteriaAncestors(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("group") Boolean group,
      @Param("parentConceptIds") Set<String> parentConceptIds);

  /** This query returns all parents matching the parentConceptIds. */
  @Query(
      value =
          "select c from DbCriteria c where conceptId in (:parentConceptIds) and domainId = :domain and type = :type and standard = :standard and "
              + "match(synonyms, concat('+[', :domain, '_rank1]')) > 0")
  List<DbCriteria> findCriteriaParentsByDomainAndTypeAndParentConceptIds(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("parentConceptIds") Set<String> parentConceptIds);

  @Query(value = "select c from DbCriteria c where match(path, :path) > 0 order by id asc")
  List<DbCriteria> findCriteriaLeavesAndParentsByPath(@Param("path") String path);

  @Query(
      value =
          "select distinct c.concept_id as conceptId, c2.id as parentId "
              + "from cb_criteria c "
              + "join (select id from cb_criteria where id in (:conceptIds)) c2 on c.path like concat('%', c2.id, '%') "
              + "where match(c.path) against(:path)",
      nativeQuery = true)
  List<DbCriteriaLookup> findCriteriaLeavesAndParentsByPath(
      @Param("conceptIds") List<Long> conceptIds, @Param("path") String path);

  @Query(
      value =
          "select distinct c.concept_id as conceptId, c2.id as parentId "
              + "from cb_criteria c join (select id from cb_criteria where id in (:parentIds)) c2 on c.path like concat('%', c2.id, '%') "
              + "where c.domain_id = :domain "
              + "and c.type = :type "
              + "and c.parent_id in (:parentIds) "
              + "or c.id in (:parentIds)",
      nativeQuery = true)
  List<DbCriteriaLookup> findCriteriaLeavesAndParentsByDomainAndTypeAndParentIds(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("parentIds") List<Long> parentIds);

  @Query(
      value =
          "select cr from DbCriteria cr where domain_id = ?1 and type = ?2 and subtype = ?3 and is_group = 0 and is_selectable = 1")
  List<DbCriteria> findCriteriaLeavesByDomainAndTypeAndSubtype(
      String domain, String type, String subtype);

  @Query(
      value = "select concept_id_2 from cb_criteria_relationship where concept_id_1 = :conceptId",
      nativeQuery = true)
  List<Integer> findConceptId2ByConceptId1(@Param("conceptId") Long conceptId);

  @Query(
      value =
          "select cr from DbCriteria cr where cr.conceptId in (:conceptIds) and cr.standard = :standard and cr.domainId = :domain and match(cr.synonyms, concat('+[', :domain, '_rank1]')) > 0)")
  List<DbCriteria> findStandardCriteriaByDomainAndConceptId(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("conceptIds") List<String> conceptIds);

  @Query(
      value =
          "select * from cb_criteria where domain_id=:domain and type=:type and is_standard=:standard and parent_id=:parentId order by id asc",
      nativeQuery = true)
  List<DbCriteria> findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("parentId") Long parentId);

  @Query(value = "select c from DbCriteria c where domainId=:domain and type=:type order by id asc")
  List<DbCriteria> findCriteriaByDomainAndTypeOrderByIdAsc(
      @Param("domain") String domain, @Param("type") String type);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and code=:term order by standard desc")
  List<DbCriteria> findExactMatchByCode(@Param("domain") String domain, @Param("term") String term);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and type=:type and standard=:standard and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndCode(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and standard=:standard and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndCode(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and standard=:standard and match(synonyms, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc, c.name asc")
  List<DbCriteria> findCriteriaByDomainAndSynonyms(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and type=:type and standard=:standard and hierarchy=1 and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndCode(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean standard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId = :domain and type = :type and standard = :standard and hierarchy=1 and match(synonyms, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndSynonyms(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean standard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select * from cb_criteria c "
              + "where c.domain_id = 'DRUG' "
              + "and c.type in ('ATC', 'BRAND', 'RXNORM') "
              + "and c.is_selectable = 1 "
              + "and (upper(c.name) like upper(concat('%',:value,'%')) "
              + "or upper(c.code) like upper(concat('%',:value,'%'))) "
              + "order by c.name asc "
              + "limit :limit",
      nativeQuery = true)
  List<DbCriteria> findDrugBrandOrIngredientByValue(
      @Param("value") String value, @Param("limit") Integer limit);

  @Query(
      value =
          "select * from cb_criteria c "
              + "inner join ( "
              + "select cr.concept_id_2 from cb_criteria_relationship cr "
              + "join concept c1 on (cr.concept_id_2 = c1.concept_id "
              + "and cr.concept_id_1 = :conceptId "
              + "and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2 "
              + "and c.domain_id = 'DRUG' and c.type = 'RXNORM' and match(synonyms) against('+[drug_rank1]' in boolean mode) order by c.est_count desc",
      nativeQuery = true)
  List<DbCriteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);

  @Query(
      value =
          "select * from cb_criteria where domain_id = 'PERSON' and type in ('GENDER', 'RACE', 'ETHNICITY', 'SEX') and parent_id != 0 order by name asc",
      nativeQuery = true)
  List<DbCriteria> findGenderRaceEthnicitySexAtBirth();

  List<DbCriteria> findByDomainIdAndTypeAndParentIdNotIn(
      @Param("domainId") String domainId,
      @Param("type") String type,
      @Param("parentId") Long parentId,
      Sort sort);

  @Query(
      value =
          "select distinct domain_id as domain, type, is_standard as standard from cb_criteria order by domain, type, is_standard",
      nativeQuery = true)
  List<DbMenuOption> findMenuOptions();
}
