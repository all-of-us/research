package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TemporalQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the Temporal criteria groups.
 */
@Service
public class TemporalQueryBuilder {

  private static final String UNION_TEMPLATE = "union all\n";
  private static final String SAME_ENC =
    "temp1.person_id = temp2.person_id and temp1.visit_concept_id = temp2.visit_concept_id\n";
  private static final String X_DAYS_BEFORE =
    "temp1.person_id = temp2.person_id and temp1.entry_date < DATE_ADD(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String X_DAYS_AFTER =
    "temp1.person_id = temp2.person_id and temp1." +
      "entry_date > DATE_ADD(temp2.entry_date, INTERVAL ${timeValue} DAY)\n";
  private static final String WITHIN_X_DAYS_OF =
    "temp1.person_id = temp2.person_id and temp1.entry_date between " +
      "temp2.entry_date - ${timeValue} and temp2.entry_date + ${timeValue}\n";
  private static final String TEMPORAL_TEMPLATE =
    "select count(distinct temp1.person_id)\n" +
      "from (${query1}) temp1\n" +
      "where exists (select 1\n" +
      "from (${query2}) temp2\n" +
      "where (${conditions}))\n";

  public String buildQuery(Map<String, QueryParameterValue> params,
                           SearchGroup includeGroup) {
    List<String> temporalQueryParts1 = new ArrayList<>();
    List<String> temporalQueryParts2 = new ArrayList<>();
    ListMultimap<Integer, SearchGroupItem> temporalGroups = getTemporalGroups(includeGroup);
    for (Integer key : temporalGroups.keySet()) {
      List<SearchGroupItem> tempGroups = temporalGroups.get(key);
      //key of zero indicates belonging to the first temporal group
      //key of one indicates belonging to the seconn temporal group
      boolean isFirstGroup = key == 0;
      for (SearchGroupItem tempGroup : tempGroups) {
        String query = QueryBuilderFactory
          .getQueryBuilder(FactoryKey.getType(tempGroup.getType()))
          .buildQuery(params, tempGroup,
            isFirstGroup ? includeGroup.getMention() : TemporalMention.ANY_MENTION.name());
        if (isFirstGroup) {
          temporalQueryParts1.add(query);
        } else {
          temporalQueryParts2.add(query);
        }
      }
    }
    String conditions = SAME_ENC;
    if (includeGroup.getTime().equals(TemporalTime.DURING_SAME_ENCOUNTER_AS.name())) {
      conditions = WITHIN_X_DAYS_OF.replace("${timeValue}", includeGroup.getTimeValue().toString());
    } else if (includeGroup.getTime().equals(TemporalTime.X_DAYS_BEFORE.name())) {
      conditions = X_DAYS_BEFORE.replace("${timeValue}", includeGroup.getTimeValue().toString());
    } else if (includeGroup.getTime().equals(TemporalTime.X_DAYS_AFTER.name())) {
      conditions = X_DAYS_AFTER.replace("${timeValue}", includeGroup.getTimeValue().toString());
    }
    return TEMPORAL_TEMPLATE
      .replace("${query1}", String.join(UNION_TEMPLATE, temporalQueryParts1))
      .replace("${query2}", String.join(UNION_TEMPLATE, temporalQueryParts2))
      .replace("${conditions}", conditions);
  }

  private ListMultimap<Integer, SearchGroupItem> getTemporalGroups(SearchGroup searchGroup) {
    ListMultimap<Integer, SearchGroupItem> itemMap = ArrayListMultimap.create();
    searchGroup.getItems()
      .forEach(item -> itemMap.put(item.getTemporalGroup(), item));
    return itemMap;
  }

  private void validateSearchGroupItem() {

  }
}
