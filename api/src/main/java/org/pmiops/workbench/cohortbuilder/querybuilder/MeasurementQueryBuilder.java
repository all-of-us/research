package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.cohortbuilder.querybuilder.validation.Validation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class MeasurementQueryBuilder extends AbstractQueryBuilder {

  public static final String NUMERICAL = "NUM";
  public static final String CATEGORICAL = "CAT";
  public static final String BOTH = "BOTH";
  public static final String LAB = "LAB";

  private static final String UNION_ALL = " union all\n";
  private static final String AND = " and ";
  private static final String OR = " or ";
  private static final String MEASUREMENT_SQL_TEMPLATE =
    "select person_id, measurement_date as entry_date, measurement_source_concept_id\n" +
      "from `${projectId}.${dataSetId}.measurement`\n" +
      "where measurement_concept_id = ${conceptId}\n" +
      "${encounterSql}";
  private static final String VALUE_AS_NUMBER =
    "value_as_number ${operator} ${value}\n";
  private static final String VALUE_AS_CONCEPT_ID =
    "value_as_concept_id ${operator} unnest(${values})\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    for (SearchParameter parameter : parameters.getParameters()) {
      validateAttributes(parameter);
      if (parameter.getConceptId() == null) {
        throw new BadRequestException("Please provide valid concept id for Measurements.");
      }
      String baseSql = MEASUREMENT_SQL_TEMPLATE.replace("${conceptId}", parameter.getConceptId().toString());
      List<String> tempQueryParts = new ArrayList<String>();
      for (Attribute attribute : parameter.getAttributes()) {
        if (attribute.getName().equals(ANY)) {
          queryParts.add(baseSql);
        } else {
          if (attribute.getName().equals(NUMERICAL)) {
            processNumericalSql(queryParts, queryParams, baseSql + AND + VALUE_AS_NUMBER, attribute);
          } else if (attribute.getName().equals(CATEGORICAL)) {
            processCategoricalSql(queryParts, queryParams, baseSql + AND + VALUE_AS_CONCEPT_ID, attribute);
          } else if (attribute.getName().equals(BOTH) && attribute.getOperator().equals(Operator.IN)) {
            processCategoricalSql(tempQueryParts, queryParams, VALUE_AS_CONCEPT_ID, attribute);
          } else {
            processNumericalSql(tempQueryParts, queryParams, VALUE_AS_NUMBER, attribute);
          }
        }
      }
      if (!tempQueryParts.isEmpty()) {
        queryParts.add(baseSql + AND + "(" + String.join(OR, tempQueryParts) + ")");
      }
    }
    String measurementSql = String.join(UNION_ALL, queryParts);
    String finalSql = buildModifierSql(measurementSql, queryParams, parameters.getModifiers());
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.MEAS;
  }

  private void validateAttributes(SearchParameter parameter) {
    Predicate<Attribute> isCategoricalAndNotIn = a -> CATEGORICAL.equals(a.getName()) && !a.getOperator().equals(Operator.IN);
    Predicate<Attribute> isOperandsEmpty = a -> a.getOperands().isEmpty();
    Predicate<Attribute> isNameBlank = a -> StringUtils.isBlank(a.getName());
    Predicate<Attribute> isOperatorNull = a -> a.getOperator() == null;
    Predicate<Attribute> isBetweenAndNotTwoOperands = a -> a.getOperator().equals(Operator.BETWEEN) && a.getOperands().size() != 2;
    Predicate<Attribute> isOperandsInvalid =
      a -> !a
        .getOperands()
        .stream()
        .filter(
          o -> !NumberUtils.isNumber(o))
        .collect(Collectors.toList()).isEmpty();
    parameter
      .getAttributes()
      .stream()
      .filter(attribute -> !isNameAny(attribute))
      .forEach(attribute -> {
        Validation.from(isNameBlank).test(attribute).throwException("Please provide an attribute name.");
        Validation.from(isOperatorNull).test(attribute).throwException("Please provide an attribute operator");
        Validation.from(isOperandsEmpty).test(attribute).throwException("Please provide one or more operands.");
        Validation.from(isCategoricalAndNotIn).test(attribute).throwException("Please provide the in operator when searching categorical attributes.");
        Validation.from(isBetweenAndNotTwoOperands).test(attribute).throwException("Please provide two operands when using the between operator.");
        Validation.from(isOperandsInvalid).test(attribute).throwException("Please provide valid numeric operands.");
      });
  }

  private void processNumericalSql(List<String> queryParts,
                                   Map<String, QueryParameterValue> queryParams,
                                   String baseSql,
                                   Attribute attribute) {
    String namedParameter1 = LAB.toLowerCase() + getUniqueNamedParameterPostfix();
    String namedParameter2 = LAB.toLowerCase() + getUniqueNamedParameterPostfix();
    queryParts.add(baseSql
      .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
      .replace("${value}", attribute.getOperator().equals(Operator.BETWEEN)
        ? "@" + namedParameter1 + " and " + "@" + namedParameter2 : "@" + namedParameter1));
    queryParams.put(namedParameter1, QueryParameterValue.float64(new Double(attribute.getOperands().get(0))));
    if (attribute.getOperator().equals(Operator.BETWEEN)) {
      queryParams.put(namedParameter2, QueryParameterValue.float64(new Double(attribute.getOperands().get(1))));
    }
  }

  private void processCategoricalSql(List<String> queryParts,
                                     Map<String, QueryParameterValue> queryParams,
                                     String baseSql,
                                     Attribute attribute) {
    String namedParameter1 = LAB.toLowerCase() + getUniqueNamedParameterPostfix();
    queryParts.add(baseSql
      .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
      .replace("${values}", "@" + namedParameter1));
    queryParams.put(namedParameter1,
      QueryParameterValue.array(attribute.getOperands().stream().map(s -> Long.parseLong(s)).toArray(Long[]::new), Long.class)
    );
  }
}
