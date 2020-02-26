package org.pmiops.workbench.elasticsearch;

import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/** Utility for conversion of Criteria tree search request into Elasticsearch filters. */
public final class ElasticCriteriaFilters {

  public static QueryBuilder criteriaWhereCodeEqual(String domain, String term) {
    return QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("domain_id", domain))
        .must(QueryBuilders.termQuery("code", term));
  }

  public static QueryBuilder criteriaWhereCodePrefix(
      String domain, Boolean isStandard, String term) {
    return QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("domain_id", domain))
        .must(QueryBuilders.termQuery("is_standard", BooleanUtils.toInteger(isStandard)))
        .must(QueryBuilders.prefixQuery("code", term));
  }

  public static QueryBuilder criteriaWhereSynonymsMatch(
      String domain, Boolean isStandard, String term) {
    return QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("domain_id", domain))
        .must(QueryBuilders.termQuery("is_standard", BooleanUtils.toInteger(isStandard)))
        .must(QueryBuilders.matchQuery("synonyms", term).operator(Operator.AND).fuzziness("1"));
  }

  public static QueryBuilder criteriaWhereSynonymsMatchPhrasePrefix(
      String domain, List<String> types, Boolean isStandard, String term) {
    return QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("domain_id", domain))
        .must(QueryBuilders.termQuery("is_standard", BooleanUtils.toInteger(isStandard)))
        .must(QueryBuilders.termsQuery("type", types))
        .must(QueryBuilders.matchPhrasePrefixQuery("synonyms", term));
  }
}
