package org.pmiops.workbench.cdr;

import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.CHILD_LOOKUP_SQL;
import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.DRUG_CHILD_LOOKUP_SQL;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private static final ImmutableList<Domain> CHILD_LOOKUP_DOMAINS =
      ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE, Domain.MEASUREMENT, Domain.DRUG);
  private static final String SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE =
      "select DISTINCT(question_concept_id) as concept_id \n"
          + "from `${projectId}.${dataSetId}.ds_survey`\n";

  @Autowired
  public ConceptBigQueryService(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
  }

  public int getParticipantCountForConcepts(
      Domain domain, String omopTable, Set<DbConceptSetConceptId> dbConceptSetConceptIds) {
    ConceptColumns conceptColumns = cdrBigQuerySchemaConfigService.getConceptColumns(omopTable);
    Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
        dbConceptSetConceptIds.stream()
            .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
    List<Long> standardList =
        partitionSourceAndStandard.get(true).stream()
            .map(DbConceptSetConceptId::getConceptId)
            .collect(Collectors.toList());
    List<Long> sourceList =
        partitionSourceAndStandard.get(false).stream()
            .map(DbConceptSetConceptId::getConceptId)
            .collect(Collectors.toList());
    StringBuilder innerSql = new StringBuilder("select count(distinct person_id) person_count\n");
    innerSql.append("from ");
    innerSql.append(String.format("`${projectId}.${dataSetId}.%s`", omopTable));
    innerSql.append(" where ");
    ImmutableMap.Builder<String, QueryParameterValue> paramMap = ImmutableMap.builder();
    if (!standardList.isEmpty()) {
      innerSql.append(conceptColumns.getStandardConceptColumn().name);
      generateParentChildLookupSql(
          innerSql, domain, "standardConceptIds", 1, standardList, paramMap);
      if (!sourceList.isEmpty()) {
        innerSql.append(" or ");
      }
    }
    if (!sourceList.isEmpty()) {
      if (Domain.SURVEY.equals(domain)) {
        innerSql.append("observation_source_concept_id");
      } else {
        innerSql.append(conceptColumns.getSourceConceptColumn().name);
      }
      generateParentChildLookupSql(innerSql, domain, "sourceConceptIds", 0, sourceList, paramMap);
    }
    QueryJobConfiguration jobConfiguration =
        QueryJobConfiguration.newBuilder(innerSql.toString())
            .setNamedParameters(paramMap.build())
            .setUseLegacySql(false)
            .build();
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration));
    return (int) result.iterateAll().iterator().next().get(0).getLongValue();
  }

  private void generateParentChildLookupSql(
      StringBuilder sqlBuilder,
      Domain domain,
      String conceptIdsParam,
      int standardOrSource,
      List<Long> conceptIds,
      ImmutableMap.Builder<String, QueryParameterValue> paramMap) {
    if (CHILD_LOOKUP_DOMAINS.contains(domain)) {
      String domainParam = (standardOrSource == 1 ? "standardDomain" : "sourceDomain");
      String standardParam = (standardOrSource == 1 ? "standard" : "source");
      sqlBuilder.append(
          " in "
              + String.format(
                  Domain.DRUG.equals(domain) ? DRUG_CHILD_LOOKUP_SQL : CHILD_LOOKUP_SQL,
                  "@" + domainParam,
                  "@" + standardParam,
                  "@" + conceptIdsParam,
                  "@" + domainParam,
                  "@" + standardParam));
      paramMap.put(
          conceptIdsParam, QueryParameterValue.array(conceptIds.toArray(new Long[0]), Long.class));
      paramMap.put(domainParam, QueryParameterValue.string(domain.toString()));
      paramMap.put(standardParam, QueryParameterValue.int64(standardOrSource));
    } else {
      sqlBuilder.append(" in unnest(@" + conceptIdsParam + ")");
      paramMap.put(
          conceptIdsParam, QueryParameterValue.array(conceptIds.toArray(new Long[0]), Long.class));
    }
  }

  public List<Long> getSurveyQuestionConceptIds() {
    QueryJobConfiguration qjc =
        QueryJobConfiguration.newBuilder(SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE)
            .setUseLegacySql(false)
            .build();
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(qjc), 360000L);
    List<Long> conceptIdList = new ArrayList<>();
    result
        .getValues()
        .forEach(
            surveyValue -> {
              conceptIdList.add(Long.parseLong(surveyValue.get(0).getValue().toString()));
            });
    return conceptIdList;
  }
}
