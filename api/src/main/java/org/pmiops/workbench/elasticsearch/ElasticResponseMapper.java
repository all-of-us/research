package org.pmiops.workbench.elasticsearch;

import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.search.SearchHit;
import org.pmiops.workbench.model.Criteria;

public class ElasticResponseMapper {

  public static Criteria mapCriteriaResponse(SearchHit hit) {
    Map<String, Object> source = hit.getSourceAsMap();
    return new Criteria()
        .id(Long.valueOf(hit.getId()))
        .parentId(longValueOf(source, "parent_id"))
        .domainId(stringValueOf(source, "domain_id"))
        .isStandard(booleanValueOf(source, "is_standard"))
        .type(stringValueOf(source, "type"))
        .subtype(stringValueOf(source, "subtype"))
        .conceptId(longValueOf(source, "concept_id"))
        .code(stringValueOf(source, "code"))
        .name(stringValueOf(source, "name"))
        .value(stringValueOf(source, "value"))
        .count(longValueOf(source, "est_count"))
        .group(booleanValueOf(source, "is_group"))
        .selectable(booleanValueOf(source, "is_selectable"))
        .hasAttributes(booleanValueOf(source, "has_attribute"))
        .hasHierarchy(booleanValueOf(source, "has_hierarchy"))
        .hasAncestorData(booleanValueOf(source, "has_ancestor_data"))
        .path(stringValueOf(source, "path"));
  }

  private static String stringValueOf(Map<String, Object> source, String name) {
    return (String) source.get(name);
  }

  private static Long longValueOf(Map<String, Object> source, String name) {
    return Long.valueOf(stringValueOf(source, name));
  }

  private static Boolean booleanValueOf(Map<String, Object> source, String name) {
    return BooleanUtils.toBooleanObject(Integer.valueOf(stringValueOf(source, name)));
  }
}
