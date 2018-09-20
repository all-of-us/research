package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.cohortbuilder.querybuilder.validation.Validation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

  public static final String EMPTY_MESSAGE = "Bad Request: Search {0} are empty.";
  public static final String NOT_VALID_MESSAGE = "Bad Request: {0} \"{1}\" is not valid.";
  public static final String ONE_OPERAND_MESSAGE = "Bad Request: Provide one operand.";
  public static final String TWO_OPERAND_MESSAGE = "Bad Request: Provide two operands.";
  public static final String OPERANDS_NUMERIC_MESSAGE = "Bad Request: Operands must be numeric.";
  public static final String PARAMETERS = "Parameters";
  public static final String ATTRIBUTES = "Attributes";
  public static final String OPERANDS = "Operands";
  public static final String OPERATOR = "Operator";
  public static final String TYPE = "Type";
  public static final String SUBTYPE = "Subtype";
  public static final String DOMAIN = "Domain";
  public static final String CONCEPT_ID = "Concept Id";
  public static final String CODE = "Code";
  public static final String NAME = "Name";
  public static final String VALUE = "Value";

  ImmutableMap<ModifierType, String> exceptionText = ImmutableMap.<ModifierType, String>builder()
    .put(ModifierType.AGE_AT_EVENT, "age at event")
    .put(ModifierType.EVENT_DATE, "event date")
    .put(ModifierType.NUM_OF_OCCURRENCES, "number of occurrences")
    .put(ModifierType.ENCOUNTERS, "visit type")
    .build();
  private static final List<Operator> OPERATOR_ANY_EQUALS =
    Arrays.asList(Operator.LESS_THAN_OR_EQUAL_TO,
      Operator.GREATER_THAN_OR_EQUAL_TO, Operator.GREATER_THAN,
      Operator.LESS_THAN, Operator.EQUAL, Operator.NOT_EQUAL);

  public static final String AGE_AT_EVENT_PREFIX = "age";
  public static final String EVENT_DATE_PREFIX = "event";
  public static final String OCCURRENCES_PREFIX = "occ";
  public static final String ENCOUNTER_PREFIX = "enc";
  public static final String ANY = "ANY";

  public static final String WHERE = " where ";
  public static final String AND = " and ";
  private static final String MODIFIER_SQL_TEMPLATE = "select criteria.person_id from (${innerSql}) criteria\n";
  private static final String AGE_AT_EVENT_JOIN_TEMPLATE =
    "join `${projectId}.${dataSetId}.person` p on (criteria.person_id = p.person_id)\n";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE =
    "CAST(FLOOR(DATE_DIFF(criteria.entry_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64)\n";
  private static final String EVENT_DATE_SQL_TEMPLATE = "criteria.entry_date\n";
  private static final String OCCURRENCES_SQL_TEMPLATE = "group by criteria.person_id\n" +
    "having count(criteria.person_id) ";
  private static final String ENCOUNTERS_SQL_TEMPLATE = "and visit_occurrence_id in (\n" +
    "select visit_occurrence_id from `${projectId}.${dataSetId}.visit_occurrence`\n" +
    "where visit_concept_id in (\n" +
    "select descendant_concept_id\n" +
    "from `${projectId}.${dataSetId}.concept_ancestor`\n" +
    "where ancestor_concept_id ${encounterOperator} unnest(${encounterConceptId})))\n";

  /**
   * Build a {@link QueryJobConfiguration} from the specified
   * {@link QueryParameters} provided.
   *
   * @param parameters
   * @return
   */
  public abstract QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters);

  public abstract FactoryKey getType();

  public String buildModifierSql(String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    String modifierSql = "";
    String encounterSql = "";
    if (!modifiers.isEmpty()) {
      Modifier ageAtEvent = getModifier(modifiers, ModifierType.AGE_AT_EVENT);
      Modifier eventDate = getModifier(modifiers, ModifierType.EVENT_DATE);
      Modifier occurrences = getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES);
      Modifier encounters = getModifier(modifiers, ModifierType.ENCOUNTERS);
      encounterSql = buildEncountersSql(queryParams, encounters);
      modifierSql = buildAgeAtEventAndEventDateModifierSql(queryParams, Arrays.asList(ageAtEvent, eventDate));
      //Number of Occurrences has to be last because of the group by
      modifierSql = modifierSql + buildNumOfOccurrencesModifierSql(queryParams, occurrences);
    }
    return MODIFIER_SQL_TEMPLATE.replace("${innerSql}", baseSql).replace("${encounterSql}", encounterSql) + modifierSql;
  }

  protected static boolean isNameAny(Attribute attribute) {
    return attribute.getName() != null
      && attribute.getName().equals(ANY);
  }

  protected static boolean isOperatorBetween(Attribute attribute) {
    return attribute.getOperator() != null
      && attribute.getOperator().equals(Operator.BETWEEN)
      && attribute.getOperands().size() == 2
      && NumberUtils.isNumber(attribute.getOperands().get(0))
      && NumberUtils.isNumber(attribute.getOperands().get(1));
  }

  protected static boolean isOperatorAnyEquals(Attribute attribute) {
    return attribute.getOperator() != null
      && OPERATOR_ANY_EQUALS.contains(attribute.getOperator())
      && attribute.getOperands().size() == 1
      && NumberUtils.isNumber(attribute.getOperands().get(0));
  }

  protected static boolean isOperatorIn(Attribute attribute) {
    return attribute.getOperator() != null
      && attribute.getOperator().equals(Operator.IN)
      && attribute.getOperands().size() >= 0
      && attribute.getOperands().stream()
      .filter(o -> NumberUtils.isNumber(o))
      .collect(Collectors.toList()).size() > 0;
  }

  protected String getUniqueNamedParameterPostfix() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  private Modifier getModifier(List<Modifier> modifiers, ModifierType modifierType) {
    List<Modifier> modifierList =  modifiers.stream()
      .filter(modifier -> modifier.getName().equals(modifierType))
      .collect(Collectors.toList());
    if (modifierList.isEmpty()) {
      return null;
    } else if (modifierList.size() == 1){
      return modifierList.get(0);
    }
    throw new BadRequestException(String.format("Please provide one %s modifier.",
      exceptionText.get(modifierType)));
  }

  private String buildAgeAtEventAndEventDateModifierSql(Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    String modifierSql = "";
    for (Modifier modifier : modifiers) {
      if (modifier != null) {
        validateOperands(modifier);
        boolean isAgeAtEvent = modifier.getName().equals(ModifierType.AGE_AT_EVENT);
        List<String> modifierParamList = new ArrayList<>();
        for (String operand : modifier.getOperands()) {
          String modifierParameter = isAgeAtEvent ? AGE_AT_EVENT_PREFIX : EVENT_DATE_PREFIX +
            getUniqueNamedParameterPostfix();
          modifierParamList.add("@" + modifierParameter);
          queryParams.put(modifierParameter, isAgeAtEvent ?
            QueryParameterValue.int64(new Long(operand)) : QueryParameterValue.date(operand));
        }
        if (isAgeAtEvent) {
          if (modifierSql.isEmpty()) {
            modifierSql = AGE_AT_EVENT_JOIN_TEMPLATE + WHERE + AGE_AT_EVENT_SQL_TEMPLATE;
          } else {
            modifierSql = modifierSql + AND + AGE_AT_EVENT_SQL_TEMPLATE;
          }
        } else {
          if (modifierSql.isEmpty()) {
            modifierSql = WHERE + EVENT_DATE_SQL_TEMPLATE;
          } else {
            modifierSql = modifierSql + AND + EVENT_DATE_SQL_TEMPLATE;
          }
        }
        modifierSql = modifierSql +
          OperatorUtils.getSqlOperator(modifier.getOperator()) + " " +
          String.join(AND, modifierParamList) + "\n";
      }
    }
    return modifierSql;
  }

  private  String buildNumOfOccurrencesModifierSql(Map<String, QueryParameterValue> queryParams, Modifier occurrences) {
    String modifierSql = "";
    if (occurrences != null) {
      List<String> modifierParamList = new ArrayList<>();
      validateOperands(occurrences);
      for (String operand : occurrences.getOperands()) {
        String modifierParameter = OCCURRENCES_PREFIX + getUniqueNamedParameterPostfix();
        modifierParamList.add("@" + modifierParameter);
        queryParams.put(modifierParameter, QueryParameterValue.int64(new Long(operand)));
      }
      modifierSql = OCCURRENCES_SQL_TEMPLATE +
        OperatorUtils.getSqlOperator(occurrences.getOperator()) + " " +
        String.join(AND, modifierParamList) + "\n";
    }
    return modifierSql;
  }

  private String buildEncountersSql(Map<String, QueryParameterValue> queryParams, Modifier modifier) {
    if (modifier == null) {
      return "";
    } else if (modifier.getName().equals(ModifierType.ENCOUNTERS)) {
      if (!modifier.getOperator().equals(Operator.IN)) {
        throw new BadRequestException(String.format(
          "Please provide IN operator for %s.",
          exceptionText.get(modifier.getName())));
      }
      try {
        modifier.getOperands().stream()
          .map(Long::new).collect(Collectors.toList());
      } catch (NumberFormatException nfe) {
        throw new BadRequestException(String.format(
          "Please provide valid conceptId for %s.",
          exceptionText.get(modifier.getName())));
      }
    }
    String modifierParameter = ENCOUNTER_PREFIX + getUniqueNamedParameterPostfix();
    Long[] operands = modifier.getOperands().stream().map(Long::new).toArray(Long[]::new);
    queryParams.put(modifierParameter, QueryParameterValue.array(operands, Long.class));
    return ENCOUNTERS_SQL_TEMPLATE
      .replace("${encounterOperator}", OperatorUtils.getSqlOperator(modifier.getOperator()))
      .replace( "${encounterConceptId}", "@" + modifierParameter);
  }

  private void validateOperands(Modifier modifier) {
    if (modifier.getOperator().equals(Operator.BETWEEN) &&
      modifier.getOperands().size() != 2) {
      throw new BadRequestException(String.format(
        "Modifier: %s can only have 2 operands when using the %s operator",
        exceptionText.get(modifier.getName()),
        modifier.getOperator().name()));
    } else if (!modifier.getOperator().equals(Operator.BETWEEN) &&
      modifier.getOperands().size() != 1) {
      throw new BadRequestException(String.format(
        "Modifier: %s can only have 1 operand when using the %s operator",
        exceptionText.get(modifier.getName()),
        modifier.getOperator().name()));
    } else if (modifier.getName().equals(ModifierType.AGE_AT_EVENT)
      || modifier.getName().equals(ModifierType.NUM_OF_OCCURRENCES)) {
      try {
        modifier.getOperands().stream()
          .map(Long::new).collect(Collectors.toList());
      } catch (NumberFormatException nfe) {
        throw new BadRequestException(String.format(
          "Please provide valid number for %s.",
          exceptionText.get(modifier.getName())));
      }
    } else if (modifier.getName().equals(ModifierType.EVENT_DATE)) {
      modifier.getOperands().stream()
        .map(date -> {
          try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
          } catch (ParseException pe) {
            throw new BadRequestException(String.format(
              "Please provide valid date for %s.",
              exceptionText.get(modifier.getName())));
          }
        }).collect(Collectors.toList());
    }
  }

}
