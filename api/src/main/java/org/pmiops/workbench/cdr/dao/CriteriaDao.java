package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.ConceptCriteria;
import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

    List<Criteria> findCriteriaByTypeAndParentIdOrderByIdAsc(@Param("type") String type, @Param("parentId") Long parentId);

    @Query(value = "select * from criteria c " +
            "where c.type = :type " +
            "and c.subtype = :subtype " +
            "and c.is_group = 0 and c.is_selectable = 1 " +
            "order by c.name asc", nativeQuery = true)
    List<Criteria> findCriteriaByTypeAndSubtypeOrderByNameAsc(@Param("type") String type,
                                                              @Param("subtype") String subtype);

    @Query(value = "select distinct c.domain_id as domainId from criteria c " +
            "where c.parent_id in (" +
            "select id from criteria " +
            "where type = :type " +
            "and code like :code% " +
            "and is_selectable = 1 " +
            "and is_group = 1) " +
            "and c.is_group = 0 and c.is_selectable = 1", nativeQuery = true)
    List<String> findCriteriaByTypeAndCode(@Param("type") String type,
                                           @Param("code") String code);

    @Query(value = "select distinct c.domain_id as domainId from criteria c " +
            "where c.parent_id in (" +
            "select id from criteria " +
            "where type = :type " +
            "and subtype = :subtype " +
            "and code like :code% " +
            "and is_selectable = 1 " +
            "and is_group = 1) " +
            "and c.is_group = 0 and c.is_selectable = 1", nativeQuery = true)
    List<String> findCriteriaByTypeAndSubtypeAndCode(@Param("type") String type,
                                                     @Param("subtype") String subtype,
                                                     @Param("code") String code);

    @Query(value = "select * from criteria c " +
            "where c.type = :type " +
            "and (match(c.name) against(:value in boolean mode) or match(c.code) against(:value in boolean mode)) " +
            "and c.is_selectable = 1 " +
            "order by c.code asc", nativeQuery = true)
    List<Criteria> findCriteriaByTypeAndNameOrCode(@Param("type") String type,
                                                   @Param("value") String value);

    @Query(value =
      "select concept_id as conceptId, " +
      "       concept_name as conceptName, " +
      "       1 as isGroup " +
      "from concept_ancestor a " +
      "join concept b on a.ancestor_concept_id = b.concept_id " +
      "where descendant_concept_id in " +
      "        (select concept_id " +
      "          from concept " +
      "         where standard_concept in ('S','C') " +
      "           and domain_id = :domainId " +
      "           and match(concept_name) against(:value in boolean mode)) " +
       "order by max_levels_of_separation desc limit 1", nativeQuery = true)
    List<ConceptCriteria> findConceptCriteriaParent(@Param("domainId") String domainId,
                                                    @Param("value") String value);

    @Query(value =
      "select b.concept_id, " +
      "       b.concept_name, " +
      "       case " +
      "           when c.concept_id_1 is null then 0 " +
      "           else 1 " +
      "       end as is_group " +
      "from concept_relationship a " +
      "left join concept b on a.concept_id_2 = b.concept_id " +
      "left join " +
      "  (select a.concept_id_1, " +
      "          count(*) " +
      "     from concept_relationship a " +
      "     join concept b on a.concept_id_2 = b.concept_id " +
      "     join concept_relationship c on c.concept_id_2 = a.concept_id_1" +
      "     where a.relationship_id = 'Subsumes' " +
      "     and c.concept_id_1 = :conceptId " +
      "     group by a.concept_id_1) c on a.concept_id_2 = c.concept_id_1 " +
      "where a.concept_id_1 = :conceptId " +
      "  and relationship_id = 'Subsumes' " +
      "  and concept_id_2 IN " +
      "    (select ancestor_concept_id " +
      "       from concept_ancestor a " +
      "       left join concept b on a.ancestor_concept_id = b.concept_id " +
      "       where descendant_concept_id in " +
      "           (select concept_id " +
      "              from concept " +
      "             where standard_concept IN ('S','C') " +
      "               and domain_Id = :domainId " +
      "               and concept_name regexp :value" +
      "               and concept_id is not null) )", nativeQuery = true)
    List<ConceptCriteria> findConceptCriteriaChildren(@Param("conceptId") Long conceptId,
                                                      @Param("domainId") String domainId,
                                                      @Param("value") String value);

}
