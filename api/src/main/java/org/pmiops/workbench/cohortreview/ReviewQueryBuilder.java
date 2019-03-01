package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: delete when ui work is done.
 */
@Service
public class ReviewQueryBuilder {

  private static final String NAMED_PARTICIPANTID_PARAM = "participantId";
  private static final String NAMED_DOMAIN_PARAM = "domain";
  private static final String NAMED_LIMIT_PARAM = "limit";

  private static final String BASE_SQL_TEMPLATE =
    "select person_id as personId,\n" +
      "data_id as dataId,\n" +
      "start_datetime as startDate,\n" +
      "domain as domain,\n" +
      "standard_name as standardName,\n" +
      "standard_code as standardCode,\n" +
      "standard_vocabulary as standardVocabulary,\n" +
      "source_name as sourceName,\n" +
      "source_code as sourceCode,\n" +
      "source_vocabulary as sourceVocabulary,\n" +
      "age_at_event as ageAtEvent,\n" +
      "visit_type as visitType,\n" +
      "route as route,\n" +
      "dose as dose,\n" +
      "strength as strength,\n" +
      "unit as unit,\n" +
      "ref_range as refRange,\n" +
      "num_mentions as numMentions,\n" +
      "first_mention as firstMention,\n" +
      "last_mention as lastMention,\n" +
      "value_as_number as value\n" +
      "from `${projectId}.${dataSetId}.person_all_events`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  private static final String DOMAIN_SQL = "and domain = @" + NAMED_DOMAIN_PARAM + "\n";

  private static final String ORDER_BY = "order by %s %s, data_id\n" +
    "limit %d offset %d\n";

  private static final String SURVEY_SQL_TEMPLATE =
    "select person_id as personId,\n" +
      "data_id as dataId,\n" +
      "start_datetime as startDate,\n" +
      "survey as survey,\n" +
      "question as question,\n" +
      "answer as answer\n" +
      "from `${projectId}.${dataSetId}.person_survey`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  private static final String COUNT_TEMPLATE =
    "select count(*) as count\n" +
      "from `${projectId}.${dataSetId}.person_all_events`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  private static final String COUNT_SURVEY_TEMPLATE =
    "select count(*) as count\n" +
      "from `${projectId}.${dataSetId}.person_survey`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  private static final String CHART_DATA_TEMPLATE =
    "select distinct a.standard_name as standardName, a.standard_vocabulary as standardVocabulary, " +
      "DATE(a.start_datetime) as startDate, a.age_at_event as ageAtEvent, rnk as rank\n" +
      "from `${projectId}.${dataSetId}.person_all_events` a\n" +
      "left join (select standard_code, RANK() OVER(ORDER BY COUNT(*) DESC) as rnk\n" +
      "from `${projectId}.${dataSetId}.person_all_events`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "and domain = @" + NAMED_DOMAIN_PARAM + "\n" +
      "and standard_concept_id != 0 \n" +
      "group by standard_code\n" +
      "LIMIT @" + NAMED_LIMIT_PARAM + ") b on a.standard_code = b.standard_code\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "and domain = @" + NAMED_DOMAIN_PARAM + "\n" +
      "and rnk <= @" + NAMED_LIMIT_PARAM + "\n" +
      "order by rank, standardName, startDate\n";

  public QueryJobConfiguration buildQuery(Long participantId,
                                          DomainType domain,
                                          PageRequest pageRequest) {
    boolean isSurvey = DomainType.SURVEY.equals(domain);
    boolean isAllEvents = DomainType.ALL_EVENTS.equals(domain);
    String finalSql = isSurvey ? SURVEY_SQL_TEMPLATE : BASE_SQL_TEMPLATE;
    if (!isAllEvents && !isSurvey) {
      finalSql = finalSql + DOMAIN_SQL;
    }
    finalSql = finalSql + ORDER_BY;
    finalSql = String.format(finalSql,
      pageRequest.getSortColumn(),
      pageRequest.getSortOrder().toString(),
      pageRequest.getPageSize(),
      pageRequest.getPage() * pageRequest.getPageSize());
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    if (!isAllEvents && !isSurvey) {
      params.put(NAMED_DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
    }
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  public QueryJobConfiguration buildCountQuery(Long participantId,
                                               DomainType domain) {
    boolean isSurvey = DomainType.SURVEY.equals(domain);
    boolean isAllEvents = DomainType.ALL_EVENTS.equals(domain);
    String finalSql = isSurvey ? COUNT_SURVEY_TEMPLATE : COUNT_TEMPLATE;
    if (!isAllEvents && !isSurvey) {
      finalSql = finalSql + DOMAIN_SQL;
    }
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    if (!isAllEvents && !isSurvey) {
      params.put(NAMED_DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
    }
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  public QueryJobConfiguration buildChartDataQuery(Long participantId,
                                                   DomainType domain,
                                                   Integer limit) {
    String tableName = QueryBuilderConstants.REVIEW_TABLE;
    String finalSql = String.format(CHART_DATA_TEMPLATE, tableName, tableName);
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
    params.put(NAMED_DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
    params.put(NAMED_LIMIT_PARAM, QueryParameterValue.int64(limit));
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }
}
