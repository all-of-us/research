package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.UnmodifiableIterator;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.codeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.codeSubtypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.codeTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.domainBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.domainInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.paramChild;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.paramParent;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.subtypeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeICD;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CODE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.DOMAIN;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SUBTYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * CodesQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the following criteria types:
 * ICD9, ICD10 and CPT.
 */
@Service
public class CodesQueryBuilder extends AbstractQueryBuilder {

  private static final String GROUP = "group";
  private static final String NOT_GROUP = "notGroup";

  //If the querybuilder will use modifiers then this sql statement has to have
  //the distinct and ${modifierColumns}

  private static final String CHILD_CODE_IN_CLAUSE_TEMPLATE =
    "in unnest(${conceptIds})\n" +
      "${encounterSql}";  private static final String CODES_SQL_TEMPLATE =
    "select distinct a.person_id, ${modifierColumns}\n" +
      "from `${projectId}.${dataSetId}.${tableName}` a\n" +
      "where a.${tableId}\n";


  private static final String GROUP_CODE_LIKE_TEMPLATE =
    "in (select concept_id \n" +
      "from `${projectId}.${dataSetId}.criteria` \n" +
      "where is_group = 0\n" +
      "and is_selectable = 1\n" +
      "and path in (\n" +
      "  select CONCAT( path, '.', CAST(id as STRING)) as path\n" +
      "  from `${projectId}.${dataSetId}.criteria`\n" +
      "  where type = ${type}\n" +
      "  and subtype = ${subtype}\n" +
      "  and REGEXP_CONTAINS(code, ${code})\n" +
      "  and is_group = 1\n" +
      "  and is_selectable = 1\n" +
      "))\n" +
      "${encounterSql}";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams, QueryParameters params) {
    from(parametersEmpty()).test(params.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    ListMultimap<MultiKey, SearchParameter> paramMap = getMappedParameters(params.getParameters());
    List<String> queryParts = new ArrayList<String>();

    for (MultiKey key : paramMap.keySet()) {
        final List<SearchParameter> paramList = paramMap.get(key);
        final SearchParameter parameter = paramList.get(0);
        if (key.getKey().contains(NOT_GROUP)) {
          List<Long> conceptIds =
            paramList.stream().map(SearchParameter::getConceptId).collect(Collectors.toList());
          buildInnerQuery(parameter.getType(),
            parameter.getSubtype(),
            queryParts,
            queryParams,
            parameter.getDomainId(),
            QueryParameterValue.array(conceptIds.stream().toArray(Long[]::new), Long.class),
            CHILD_CODE_IN_CLAUSE_TEMPLATE);
        } else {
          List<String> codes =
            paramList.stream().map(SearchParameter::getValue).collect(Collectors.toList());
          String codeParam = "^(" + String.join("|", codes) + ")";
          buildInnerQuery(parameter.getType(),
            parameter.getSubtype(),
            queryParts,
            queryParams,
            parameter.getDomainId(),
            QueryParameterValue.string(codeParam),
            GROUP_CODE_LIKE_TEMPLATE);
        }
    }

    String codesSql = String.join(UNION_TEMPLATE, queryParts);
    String finalSql = buildModifierSql(codesSql, queryParams, params.getModifiers());

    return finalSql;
  }

  private void validateSearchParameter(SearchParameter param) {
    from(typeBlank().or(codeTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(typeICD().and(subtypeBlank().or(codeSubtypeInvalid()))).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, param.getSubtype());
    from(domainBlank().or(domainInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, DOMAIN, param.getDomainId());
    from(paramChild().and(conceptIdNull())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
    from(paramParent().and(codeBlank())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CODE, param.getValue());
  }

  private void buildInnerQuery(String type,
                               String subtype,
                               List<String> queryParts,
                               Map<String, QueryParameterValue> queryParams,
                               String domain, QueryParameterValue codes,
                               String groupOrChildSql) {
    ImmutableMap.Builder<String, String> paramNames = ImmutableMap.<String, String>builder()
      .put("${tableName}", DomainTableEnum.getTableName(domain))
        .put("${modifierColumns}", DomainTableEnum.getEntryDate(domain) +
            " as entry_date, " + DomainTableEnum.getSourceConceptId(domain))
        .put("${tableId}", TreeType.SNOMED.name().equalsIgnoreCase(type) ?
          DomainTableEnum.getConceptId(domain) :
          DomainTableEnum.getSourceConceptId(domain));

    if (codes.getType().equals(StandardSQLTypeName.ARRAY)) {
      String conceptIdsNamedParameter = addQueryParameterValue(queryParams, codes);
      paramNames.put("${conceptIds}", "@" + conceptIdsNamedParameter);
    } else {
      String codeNamedParameter = addQueryParameterValue(queryParams, codes);
      paramNames.put("${code}", "@" + codeNamedParameter);
      String typeNamedParameter = addQueryParameterValue(queryParams, QueryParameterValue.string(type));
      paramNames.put("${type}", "@" + typeNamedParameter);
      String subtypeNamedParameter = addQueryParameterValue(queryParams, QueryParameterValue.string(subtype));
      paramNames.put("${subtype}", "@" + subtypeNamedParameter);
    }

    queryParts.add(filterSql(CODES_SQL_TEMPLATE + groupOrChildSql, paramNames.build()));
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.CODES;
  }

  protected ListMultimap<MultiKey, SearchParameter> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<MultiKey, SearchParameter> fullMap = ArrayListMultimap.create();
    searchParameters
      .forEach(param -> {
          validateSearchParameter(param);
          fullMap.put(new MultiKey(param), param);
        }
      );
    return fullMap;
  }

  private String filterSql(String sqlStatement, ImmutableMap replacements) {
    String returnSql = sqlStatement;
    for (UnmodifiableIterator iterator = replacements.keySet().iterator(); iterator.hasNext(); ) {
      String key = (String) iterator.next();
      returnSql = returnSql.replace(key, replacements.get(key).toString());
    }
    return returnSql;

  }

  public class MultiKey {
    private String group;
    private String type;
    private String domain;

    public MultiKey(SearchParameter searchParameter) {
      this.group = searchParameter.getGroup() ? GROUP : NOT_GROUP;
      this.type = searchParameter.getType();
      this.domain = searchParameter.getDomainId();
    }

    public String getKey() {
      return this.group + this.type + this.domain;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MultiKey multiKey = (MultiKey) o;
      return Objects.equals(group, multiKey.group) &&
        Objects.equals(type, multiKey.type) &&
        Objects.equals(domain, multiKey.domain);
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, type, domain);
    }
  }
}
