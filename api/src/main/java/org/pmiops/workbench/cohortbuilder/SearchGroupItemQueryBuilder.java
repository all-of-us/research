package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.cohortbuilder.util.Validation.from;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.betweenOperator;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.notBetweenAndNotInOperator;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.notZeroAndNotOne;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsEmpty;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotDates;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotOne;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotTwo;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operatorNull;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.temporalGroupNull;

import com.google.api.client.util.Sets;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.utils.OperatorUtils;

/** SearchGroupItemQueryBuilder builds BigQuery queries for search group items. */
public final class SearchGroupItemQueryBuilder {

  private static final int STANDARD = 1;
  private static final int SOURCE = 0;
  private static final int GROUP = 1;
  private static final int NOT_GROUP = 0;

  private static final ImmutableMap<AttrName, String> AGE_COLUMN_SQL_MAP =
      ImmutableMap.of(
          AttrName.AGE, "CAST(FLOOR(DATE_DIFF(CURRENT_DATE, dob, MONTH)/12) as INT64)",
          AttrName.AGE_AT_CONSENT, "age_at_consent",
          AttrName.AGE_AT_CDR, "age_at_cdr");

  private static final ImmutableMap<CriteriaType, String> DEMO_COLUMN_SQL_MAP =
      ImmutableMap.of(
          CriteriaType.RACE, "race_concept_id",
          CriteriaType.GENDER, "gender_concept_id",
          CriteriaType.SEX, "sex_at_birth_concept_id",
          CriteriaType.ETHNICITY, "ethnicity_concept_id");

  // sql parts to help construct BigQuery sql statements
  private static final String OR = " or\n";
  private static final String AND = " and ";
  private static final String UNION_TEMPLATE = "union all\n";
  private static final String DESC = " desc";
  private static final String BASE_SQL =
      "select distinct person_id, entry_date, concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_search_all_events`\n"
          + "where ";
  private static final String STANDARD_OR_SOURCE_SQL =
      "is_standard = %s and concept_id in unnest(%s)";
  private static final String PARENT_STANDARD_OR_SOURCE_SQL =
      "is_standard = %s and concept_id in (select distinct c.concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (${childLookup}) a\n"
          + "on (c.path like concat('%%.', a.id, '.%%') or c.path like concat('%%.', a.id) or c.path like concat(a.id, '.%%') or c.path = a.id)\n"
          + "where domain_id = %s\n"
          + "and is_standard = %s\n"
          + "and is_selectable = 1)";
  private static final String DRUG_SQL =
      "is_standard = %s and concept_id in (select distinct ca.descendant_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria_ancestor` ca\n"
          + "join (select distinct c.concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (${childLookup}) a\n"
          + "on (c.path like concat('%%.', a.id, '.%%') or c.path like concat('%%.', a.id) or c.path like concat(a.id, '.%%') or c.path = a.id)\n"
          + "where domain_id = %s\n"
          + "and is_standard = %s\n"
          + "and is_selectable = 1) b on (ca.ancestor_id = b.concept_id))";
  private static final String CHILD_LOOKUP =
      "select cast(cr.id as string) as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "where domain_id = %s\n"
          + "and is_standard = %s\n"
          + "and concept_id in unnest(%s)\n"
          + "and is_group = %s\n"
          + "and is_selectable = 1\n";
  private static final String VALUE_AS_NUMBER =
      "is_standard = %s and concept_id = %s and value_as_number %s %s\n";
  private static final String VALUE_AS_CONCEPT_ID =
      "is_standard = %s and concept_id = %s and value_as_concept_id %s unnest(%s)\n";
  private static final String VALUE_SOURCE_CONCEPT_ID =
      "is_standard = %s and concept_id = %s and value_source_concept_id %s unnest(%s)\n";
  private static final String BP_SQL = "is_standard = %s and concept_id in unnest(%s)";
  private static final String SYSTOLIC_SQL = " and systolic %s %s";
  private static final String DIASTOLIC_SQL = " and diastolic %s %s";

  // sql parts to help construct Temporal BigQuery sql
  private static final String SAME_ENC =
      "temp1.person_id = temp2.person_id and temp1.visit_occurrence_id = temp2.visit_occurrence_id\n";
  private static final String X_DAYS_BEFORE =
      "temp1.person_id = temp2.person_id and temp1.entry_date <= DATE_SUB(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String X_DAYS_AFTER =
      "temp1.person_id = temp2.person_id and temp1."
          + "entry_date >= DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String WITHIN_X_DAYS_OF =
      "temp1.person_id = temp2.person_id and temp1.entry_date between "
          + "DATE_SUB(temp2.entry_date, INTERVAL %s DAY) and DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String TEMPORAL_EXIST =
      "select temp1.person_id\n"
          + "from (%s) temp1\n"
          + "where exists (select 1\n"
          + "from (%s) temp2\n"
          + "where (%s))\n";
  private static final String TEMPORAL_JOIN =
      "select temp1.person_id\n"
          + "from (%s) temp1\n"
          + "join (select person_id, visit_occurrence_id, entry_date\n"
          + "from (%s)\n"
          + ") temp2 on (%s)\n";
  private static final String TEMPORAL_SQL =
      "select person_id, visit_occurrence_id, entry_date%s\n"
          + "from `${projectId}.${dataSetId}.cb_search_all_events`\n"
          + "where %s\n"
          + "and person_id in (%s)\n";
  private static final String RANK_1_SQL =
      ", rank() over (partition by person_id order by entry_date%s) rn";
  private static final String TEMPORAL_RANK_1_SQL =
      "select person_id, visit_occurrence_id, entry_date\n" + "from (%s) a\n" + "where rn = 1\n";

  // sql parts to help construct Modifiers BigQuery sql
  private static final String MODIFIER_SQL_TEMPLATE =
      "select criteria.person_id from (%s) criteria\n";
  private static final String OCCURRENCES_SQL_TEMPLATE =
      "group by criteria.person_id, criteria.concept_id\n" + "having count(criteria.person_id) ";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE = "and age_at_event ";
  private static final String EVENT_DATE_SQL_TEMPLATE = "and entry_date ";
  private static final String ENCOUNTERS_SQL_TEMPLATE = "and visit_concept_id ";

  // sql parts to help construct demographic BigQuery sql
  private static final String DEC_SQL =
      "exists (\n"
          + "SELECT 'x' FROM `${projectId}.${dataSetId}.death` d\n"
          + "where d.person_id = p.person_id)\n";
  private static final String DEMO_BASE =
      "select person_id\n" + "from `${projectId}.${dataSetId}.person` p\nwhere\n";
  private static final String AGE_SQL =
      "select person_id\n"
          + "from `${projectId}.${dataSetId}.cb_search_person` p\nwhere %s %s %s\n"
          + "and not "
          + DEC_SQL;
  private static final String DEMO_IN_SQL = "%s in unnest(%s)\n";

  /** Build the inner most sql using search parameters, modifiers and attributes. */
  public static void buildQuery(
      Map<String, QueryParameterValue> queryParams,
      List<String> queryParts,
      SearchGroup searchGroup) {
    if (searchGroup.getTemporal()) {
      // build the outer temporal sql statement
      String query = buildOuterTemporalQuery(queryParams, searchGroup);
      queryParts.add(query);
    } else {
      for (SearchGroupItem searchGroupItem : searchGroup.getItems()) {
        // build regular sql statement
        String query = buildBaseQuery(queryParams, searchGroupItem, searchGroup.getMention());
        queryParts.add(query);
      }
    }
  }

  /** Build the inner most sql */
  private static String buildBaseQuery(
      Map<String, QueryParameterValue> queryParams,
      SearchGroupItem searchGroupItem,
      TemporalMention mention) {
    Set<SearchParameter> standardSearchParameters = new HashSet<>();
    Set<SearchParameter> sourceSearchParameters = new HashSet<>();
    List<String> queryParts = new ArrayList<>();
    String domain = searchGroupItem.getType();

    // When building sql for demographics - we query against the person table
    if (DomainType.PERSON.toString().equals(domain)) {
      return buildDemoSql(queryParams, searchGroupItem);
    }
    // Otherwise build sql against flat denormalized search table
    for (SearchParameter param : searchGroupItem.getSearchParameters()) {
      if (param.getAttributes().isEmpty()) {
        if (param.getStandard()) {
          standardSearchParameters.add(param);
        } else {
          sourceSearchParameters.add(param);
        }
      } else {
        StringBuilder bpSql = new StringBuilder(BP_SQL);
        List<Long> bpConceptIds = new ArrayList<>();
        for (Attribute attribute : param.getAttributes()) {
          validateAttribute(attribute);
          if (attribute.getConceptId() != null) {
            // attribute.conceptId is unique to blood pressure attributes
            // this indicates we need to build a blood pressure sql statement
            bpConceptIds.add(attribute.getConceptId());
            processBloodPressureSql(queryParams, bpSql, attribute);
          } else if (AttrName.NUM.equals(attribute.getName())) {
            queryParts.add(processNumericalSql(queryParams, param, attribute));
          } else {
            queryParts.add(processCategoricalSql(queryParams, param, attribute));
          }
        }
        if (!bpConceptIds.isEmpty()) {
          String standardParam =
              QueryParameterUtil.addQueryParameterValue(
                  queryParams, QueryParameterValue.int64(param.getStandard() ? 1 : 0));
          // if blood pressure we need to add named parameters for concept ids
          QueryParameterValue cids =
              QueryParameterValue.array(bpConceptIds.toArray(new Long[0]), Long.class);
          String conceptIdsParam = QueryParameterUtil.addQueryParameterValue(queryParams, cids);
          queryParts.add(String.format(bpSql.toString(), standardParam, conceptIdsParam) + "\n");
        }
      }
    }
    addParamValueAndFormat(domain, queryParams, standardSearchParameters, queryParts, STANDARD);
    addParamValueAndFormat(domain, queryParams, sourceSearchParameters, queryParts, SOURCE);
    // need to OR all query parts together since they exist in the same search group item
    String queryPartsSql = "(" + String.join(OR, queryParts) + ")";
    // format the base sql with all query parts
    String baseSql = BASE_SQL + queryPartsSql;
    // build modifier sql if modifiers exists
    String modifiedSql = buildModifierSql(baseSql, queryParams, searchGroupItem.getModifiers());
    // build the inner temporal sql if this search group item is temporal
    // otherwise return modifiedSql
    return buildInnerTemporalQuery(
        modifiedSql, queryPartsSql, queryParams, searchGroupItem.getModifiers(), mention);
  }

  /** Build sql statement for demographics */
  private static String buildDemoSql(
      Map<String, QueryParameterValue> queryParams, SearchGroupItem searchGroupItem) {
    List<SearchParameter> parameters = searchGroupItem.getSearchParameters();
    SearchParameter param = parameters.get(0);
    switch (CriteriaType.valueOf(param.getType())) {
      case AGE:
        Attribute attribute = param.getAttributes().get(0);
        String ageNamedParameter1 =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.int64(new Long(attribute.getOperands().get(0))));
        if (attribute.getOperands().size() > 1) {
          String ageNamedParameter2 =
              QueryParameterUtil.addQueryParameterValue(
                  queryParams, QueryParameterValue.int64(new Long(attribute.getOperands().get(1))));
          ageNamedParameter1 = ageNamedParameter1 + AND + ageNamedParameter2;
        }
        return String.format(
            AGE_SQL,
            AGE_COLUMN_SQL_MAP.get(attribute.getName()),
            OperatorUtils.getSqlOperator(attribute.getOperator()),
            ageNamedParameter1);
      case GENDER:
      case SEX:
      case ETHNICITY:
      case RACE:
        // Gender, Sex, Ethnicity and Race all share the same implementation
        Long[] conceptIds =
            searchGroupItem.getSearchParameters().stream()
                .map(SearchParameter::getConceptId)
                .toArray(Long[]::new);
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.array(conceptIds, Long.class));

        CriteriaType criteriaType = CriteriaType.fromValue(param.getType());
        return DEMO_BASE
            + String.format(DEMO_IN_SQL, DEMO_COLUMN_SQL_MAP.get(criteriaType), namedParameter);
      case DECEASED:
        return DEMO_BASE + DEC_SQL;
      default:
        throw new BadRequestException(
            "Search unsupported for demographics type " + param.getType());
    }
  }

  /**
   * Implementation of temporal CB queries. Please reference the following google doc for details:
   * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
   */
  private static String buildInnerTemporalQuery(
      String modifiedSql,
      String conditionsSql,
      Map<String, QueryParameterValue> queryParams,
      List<Modifier> modifiers,
      TemporalMention mention) {
    if (mention == null) {
      return modifiedSql;
    }
    // if modifiers exists we need to add them again to the inner temporal sql
    conditionsSql = conditionsSql + getAgeDateAndEncounterSql(queryParams, modifiers);
    if (TemporalMention.ANY_MENTION.equals(mention)) {
      return String.format(TEMPORAL_SQL, "", conditionsSql, modifiedSql);
    } else if (TemporalMention.FIRST_MENTION.equals(mention)) {
      String rank1Sql = String.format(RANK_1_SQL, "");
      String temporalSql = String.format(TEMPORAL_SQL, rank1Sql, conditionsSql, modifiedSql);
      return String.format(TEMPORAL_RANK_1_SQL, temporalSql);
    }
    String rank1Sql = String.format(RANK_1_SQL, DESC);
    String temporalSql = String.format(TEMPORAL_SQL, rank1Sql, conditionsSql, modifiedSql);
    return String.format(TEMPORAL_RANK_1_SQL, temporalSql);
  }

  /**
   * The temporal group functionality description is here:
   * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
   */
  private static String buildOuterTemporalQuery(
      Map<String, QueryParameterValue> params, SearchGroup searchGroup) {
    List<String> temporalQueryParts1 = new ArrayList<>();
    List<String> temporalQueryParts2 = new ArrayList<>();
    ListMultimap<Integer, SearchGroupItem> temporalGroups = getTemporalGroups(searchGroup);
    for (Integer key : temporalGroups.keySet()) {
      List<SearchGroupItem> tempGroups = temporalGroups.get(key);
      // key of zero indicates belonging to the first temporal group
      // key of one indicates belonging to the second temporal group
      boolean isFirstGroup = key == 0;
      for (SearchGroupItem tempGroup : tempGroups) {
        String query = buildBaseQuery(params, tempGroup, searchGroup.getMention());
        if (isFirstGroup) {
          temporalQueryParts1.add(query);
        } else {
          temporalQueryParts2.add(query);
        }
      }
    }
    String conditions = SAME_ENC;
    if (TemporalTime.WITHIN_X_DAYS_OF.equals(searchGroup.getTime())) {
      String parameterName =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(WITHIN_X_DAYS_OF, parameterName, parameterName);
    } else if (TemporalTime.X_DAYS_BEFORE.equals(searchGroup.getTime())) {
      String parameterName =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(X_DAYS_BEFORE, parameterName);
    } else if (TemporalTime.X_DAYS_AFTER.equals(searchGroup.getTime())) {
      String parameterName =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(X_DAYS_AFTER, parameterName);
    }
    return String.format(
        temporalQueryParts2.size() == 1 ? TEMPORAL_EXIST : TEMPORAL_JOIN,
        String.join(UNION_TEMPLATE, temporalQueryParts1),
        String.join(UNION_TEMPLATE, temporalQueryParts2),
        conditions);
  }

  /**
   * Helper method to collect search groups into 2 temporal groups. Key of zero indicates belonging
   * to the first temporal group. Key of one indicates belonging to the second temporal group.
   */
  private static ListMultimap<Integer, SearchGroupItem> getTemporalGroups(SearchGroup searchGroup) {
    ListMultimap<Integer, SearchGroupItem> itemMap = ArrayListMultimap.create();
    searchGroup
        .getItems()
        .forEach(
            item -> {
              from(temporalGroupNull())
                  .test(item)
                  .throwException(
                      "Bad Request: search group item temporal group {0} is not valid.",
                      item.getTemporalGroup());
              itemMap.put(item.getTemporalGroup(), item);
            });
    from(notZeroAndNotOne())
        .test(itemMap)
        .throwException(
            "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).");
    return itemMap;
  }

  /** Helper method to build blood pressure sql. */
  private static void processBloodPressureSql(
      Map<String, QueryParameterValue> queryParams, StringBuilder sqlBuilder, Attribute attribute) {
    if (!AttrName.ANY.equals(attribute.getName())) {
      // this makes an assumption that the UI adds systolic attribute first. Otherwise we will have
      // to hard code the conceptId which is not optimal.
      String sqlTemplate =
          sqlBuilder.toString().contains("systolic") ? DIASTOLIC_SQL : SYSTOLIC_SQL;
      sqlBuilder.append(
          String.format(
              sqlTemplate,
              OperatorUtils.getSqlOperator(attribute.getOperator()),
              getOperandsExpression(queryParams, attribute)));
    }
  }

  /** Helper method to create sql statement for attributes of numerical type. */
  private static String processNumericalSql(
      Map<String, QueryParameterValue> queryParams,
      SearchParameter parameter,
      Attribute attribute) {
    String standardParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.int64(parameter.getStandard() ? 1 : 0));
    String conceptIdParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.int64(parameter.getConceptId()));
    return String.format(
        VALUE_AS_NUMBER,
        standardParam,
        conceptIdParam,
        OperatorUtils.getSqlOperator(attribute.getOperator()),
        getOperandsExpression(queryParams, attribute));
  }

  /** Helper method to create sql statement for attributes of categorical type. */
  private static String processCategoricalSql(
      Map<String, QueryParameterValue> queryParams,
      SearchParameter parameter,
      Attribute attribute) {
    String standardParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.int64(parameter.getStandard() ? 1 : 0));
    String conceptIdParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.int64(parameter.getConceptId()));
    String operandsParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams,
            QueryParameterValue.array(
                attribute.getOperands().stream().map(Long::parseLong).toArray(Long[]::new),
                Long.class));
    // if the search parameter is ppi/survey then we need to use different column.
    return String.format(
        (DomainType.SURVEY.toString().equals(parameter.getDomain())
            ? VALUE_SOURCE_CONCEPT_ID
            : VALUE_AS_CONCEPT_ID),
        standardParam,
        conceptIdParam,
        OperatorUtils.getSqlOperator(attribute.getOperator()),
        operandsParam);
  }

  /** Helper method to build the operand sql expression. */
  private static String getOperandsExpression(
      Map<String, QueryParameterValue> queryParams, Attribute attribute) {
    String operandsParam1 =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.float64(new Double(attribute.getOperands().get(0))));
    String valueExpression;
    if (attribute.getOperator().equals(Operator.BETWEEN)) {
      String operandsParam2 =
          QueryParameterUtil.addQueryParameterValue(
              queryParams, QueryParameterValue.float64(new Double(attribute.getOperands().get(1))));
      valueExpression = operandsParam1 + AND + operandsParam2;
    } else {
      valueExpression = operandsParam1;
    }
    return valueExpression;
  }

  /** Collect all child nodes per specified search parameters. */
  private static Set<Long> childConceptIds(
      Map<SearchParameter, Set<Long>> criteriaLookup, List<SearchParameter> params) {
    Set<Long> out = Sets.newHashSet();
    for (SearchParameter param : params) {
      if (param.getGroup() || param.getAncestorData()) {
        out.addAll(criteriaLookup.get(param));
      }
      if (param.getConceptId() != null) {
        // not all SearchParameter have a concept id, so attributes/modifiers
        // are used to find matches in those scenarios.
        out.add(param.getConceptId());
      }
    }
    return out;
  }

  /** Helper method to build modifier sql if needed. */
  private static String buildModifierSql(
      String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    validateModifiers(modifiers);
    String ageDateAndEncounterSql = getAgeDateAndEncounterSql(queryParams, modifiers);
    // Number of Occurrences has to be last because of the group by
    String occurrenceSql =
        buildOccurrencesSql(queryParams, getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES));
    return String.format(MODIFIER_SQL_TEMPLATE, baseSql + ageDateAndEncounterSql) + occurrenceSql;
  }

  /**
   * Helper method to build all modifiers together except occurrences since it has to be last
   * because of the group by.
   */
  private static String getAgeDateAndEncounterSql(
      Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    List<Modifier> ageDateAndEncounterModifiers = new ArrayList<>();
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.AGE_AT_EVENT));
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.EVENT_DATE));
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.ENCOUNTERS));
    StringBuilder modifierSql = new StringBuilder();
    for (Modifier modifier : ageDateAndEncounterModifiers) {
      if (modifier == null) {
        continue;
      }
      List<String> modifierParamList = new ArrayList<>();
      for (String operand : modifier.getOperands()) {
        String modifierParameter =
            QueryParameterUtil.addQueryParameterValue(
                queryParams,
                (isAgeAtEvent(modifier) || isEncounters(modifier))
                    ? QueryParameterValue.int64(new Long(operand))
                    : QueryParameterValue.date(operand));
        modifierParamList.add(modifierParameter);
      }
      if (isAgeAtEvent(modifier)) {
        modifierSql.append(AGE_AT_EVENT_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" ")
            .append(String.join(AND, modifierParamList))
            .append("\n");
      } else if (isEncounters(modifier)) {
        modifierSql.append(ENCOUNTERS_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" (")
            .append(modifierParamList.get(0))
            .append(")\n");
      } else {
        modifierSql.append(EVENT_DATE_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" ")
            .append(String.join(AND, modifierParamList))
            .append("\n");
      }
    }
    return modifierSql.toString();
  }

  /** Helper method to build occurrences modifier sql. */
  private static String buildOccurrencesSql(
      Map<String, QueryParameterValue> queryParams, Modifier occurrences) {
    StringBuilder modifierSql = new StringBuilder();
    if (occurrences != null) {
      List<String> modifierParamList = new ArrayList<>();
      for (String operand : occurrences.getOperands()) {
        String modifierParameter =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.int64(new Long(operand)));
        modifierParamList.add(modifierParameter);
      }
      modifierSql
          .append(OCCURRENCES_SQL_TEMPLATE)
          .append(OperatorUtils.getSqlOperator(occurrences.getOperator()))
          .append(" ")
          .append(String.join(AND, modifierParamList))
          .append("\n");
    }
    return modifierSql.toString();
  }

  /** Add source or standard concept ids and set params * */
  private static void addParamValueAndFormat(
      String domain,
      Map<String, QueryParameterValue> queryParams,
      Set<SearchParameter> searchParameters,
      List<String> queryParts,
      int standardOrSource) {
    if (!searchParameters.isEmpty()) {
      String domainParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams, QueryParameterValue.string(domain));
      String standardOrSourceParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams, QueryParameterValue.int64(standardOrSource));

      List<String> lookupSqlParts = new ArrayList<>();
      Map<Boolean, List<SearchParameter>> parentsAndChildren =
          searchParameters.stream().collect(Collectors.partitioningBy(SearchParameter::getGroup));
      List<Long> parents =
          parentsAndChildren.get(true).stream()
              .map(SearchParameter::getConceptId)
              .collect(Collectors.toList());
      List<Long> children =
          parentsAndChildren.get(false).stream()
              .map(SearchParameter::getConceptId)
              .collect(Collectors.toList());
      // Only children exist so no need to do lookups
      if (!parents.isEmpty() || DomainType.DRUG.toString().equals(domain)) {
        // Parent nodes exist need to do lookups
        if (!parents.isEmpty()) {
          lookupSqlParts.add(
              generateLookupSql(queryParams, parents, domainParam, standardOrSourceParam, GROUP));
        }
        if (!children.isEmpty()) {
          lookupSqlParts.add(
              generateLookupSql(
                  queryParams, children, domainParam, standardOrSourceParam, NOT_GROUP));
        }

        String lookupSql =
            String.format(
                DomainType.DRUG.toString().equals(domain)
                    ? DRUG_SQL
                    : PARENT_STANDARD_OR_SOURCE_SQL,
                standardOrSourceParam,
                domainParam,
                standardOrSourceParam);
        queryParts.add(
            lookupSql.replace("${childLookup}", String.join(UNION_TEMPLATE, lookupSqlParts)));
      } else {
        String conceptIdsParam =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.array(children.toArray(new Long[0]), Long.class));
        queryParts.add(
            String.format(STANDARD_OR_SOURCE_SQL, standardOrSourceParam, conceptIdsParam));
      }
    }
  }

  private static String generateLookupSql(
      Map<String, QueryParameterValue> queryParams,
      List<Long> conceptIds,
      String domainParam,
      String standardOrSourceParam,
      int i) {
    String conceptIdsParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.array(conceptIds.toArray(new Long[0]), Long.class));
    String isGroupParam =
        QueryParameterUtil.addQueryParameterValue(queryParams, QueryParameterValue.int64(i));
    return String.format(
        CHILD_LOOKUP, domainParam, standardOrSourceParam, conceptIdsParam, isGroupParam);
  }

  /** Helper method to return a modifier. */
  private static Modifier getModifier(List<Modifier> modifiers, ModifierType modifierType) {
    List<Modifier> modifierList =
        modifiers.stream()
            .filter(modifier -> modifier.getName().equals(modifierType))
            .collect(Collectors.toList());
    if (modifierList.isEmpty()) {
      return null;
    }
    return modifierList.get(0);
  }

  private static boolean isAgeAtEvent(Modifier modifier) {
    return modifier.getName().equals(ModifierType.AGE_AT_EVENT);
  }

  private static boolean isEncounters(Modifier modifier) {
    return modifier.getName().equals(ModifierType.ENCOUNTERS);
  }

  /** Validate attributes */
  private static void validateAttribute(Attribute attr) {
    if (!AttrName.ANY.equals(attr.getName())) {
      from(operatorNull())
          .test(attr)
          .throwException("Bad Request: attribute operator {0} is not valid.", attr.getOperator());
      from(operandsEmpty()).test(attr).throwException("Bad Request: attribute operands are empty.");
      from(notBetweenAndNotInOperator().and(operandsNotOne()))
          .test(attr)
          .throwException(
              "Bad Request: attribute {0} must have one operand when using the {1} operator.",
              attr.getName().toString(), attr.getOperator().toString());
      from(betweenOperator().and(operandsNotTwo()))
          .test(attr)
          .throwException(
              "Bad Request: attribute {0} can only have 2 operands when using the {1} operator",
              attr.getName().toString(), attr.getOperator().toString());
      from(operandsNotNumbers())
          .test(attr)
          .throwException(
              "Bad Request: attribute {0} operands must be numeric.", attr.getName().toString());
    }
  }

  private static void validateModifiers(List<Modifier> modifiers) {
    modifiers.forEach(
        modifier -> {
          from(operatorNull())
              .test(modifier)
              .throwException(
                  "Bad Request: modifier operator {0} is not valid.", modifier.getOperator());
          from(operandsEmpty())
              .test(modifier)
              .throwException("Bad Request: modifier operands are empty.");
          from(notBetweenAndNotInOperator().and(operandsNotOne()))
              .test(modifier)
              .throwException(
                  "Bad Request: modifier {0} must have one operand when using the {1} operator.",
                  modifier.getName().toString(), modifier.getOperator().toString());
          from(betweenOperator().and(operandsNotTwo()))
              .test(modifier)
              .throwException(
                  "Bad Request: modifier {0} can only have 2 operands when using the {1} operator",
                  modifier.getName().toString(), modifier.getOperator().toString());
          if (ModifierType.EVENT_DATE.equals(modifier.getName())) {
            from(operandsNotDates())
                .test(modifier)
                .throwException(
                    "Bad Request: modifier {0} must be a valid date.",
                    modifier.getName().toString());
          } else {
            from(operandsNotNumbers())
                .test(modifier)
                .throwException(
                    "Bad Request: modifier {0} operands must be numeric.",
                    modifier.getName().toString());
          }
        });
  }
}
