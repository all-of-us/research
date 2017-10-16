package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Provides counts of unique subjects
 * defined by the provided {@link SearchRequest}.
 */
@Service
public class SubjectCounter {

    private static final String COUNT_SQL_TEMPLATE =
            "select count(distinct person_id) as count\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "where\n";

    private static final String UNION_TEMPLATE =
            "union distinct\n";

    private static final String INCLUDE_SQL_TEMPLATE =
            "person.person_id in (${includeSql})\n";

    private static final String EXCLUDE_SQL_TEMPLATE =
            "not exists\n" +
                    "(select 'x' from\n" +
                    "(${excludeSql})\n" +
                    "x where x.person_id = person.person_id)\n";

    /**
     * Provides counts of unique subjects
     * defined by the provided {@link SearchRequest}.
     */
    public QueryJobConfiguration buildSubjectCounterQuery(SearchRequest request) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        List<String> queryParts = new ArrayList<>();

        // build query for included search groups
        StringJoiner joiner = new StringJoiner("and ");
        for (SearchGroup includeGroup : request.getIncludes()) {
            for (SearchGroupItem includeItem : includeGroup.getItems()) {
                QueryJobConfiguration queryRequest = QueryBuilderFactory
                        .getQueryBuilder(FactoryKey.getType(includeItem.getType()))
                        .buildQueryJobConfig(new QueryParameters()
                                .type(includeItem.getType())
                                .parameters(includeItem.getSearchParameters()));
                params.putAll(queryRequest.getNamedParameters());
                queryParts.add(queryRequest.getQuery());
            }
            joiner.add(INCLUDE_SQL_TEMPLATE.replace("${includeSql}", String.join(UNION_TEMPLATE, queryParts)));
            queryParts = new ArrayList<>();
        }

        // build query for excluded search groups
        for (SearchGroup excludeGroup : request.getExcludes()) {
            for (SearchGroupItem excludeItem : excludeGroup.getItems()) {
                QueryJobConfiguration queryRequest = QueryBuilderFactory
                        .getQueryBuilder(FactoryKey.getType(excludeItem.getType()))
                        .buildQueryJobConfig(new QueryParameters()
                                .type(excludeItem.getType())
                                .parameters(excludeItem.getSearchParameters()));
                params.putAll(queryRequest.getNamedParameters());
                queryParts.add(queryRequest.getQuery());
            }
            joiner.add(EXCLUDE_SQL_TEMPLATE.replace("${excludeSql}", String.join(UNION_TEMPLATE, queryParts)));
            queryParts = new ArrayList<>();
        }

        String finalSql = COUNT_SQL_TEMPLATE + joiner.toString();

        return QueryJobConfiguration
                        .newBuilder(finalSql)
                        .setNamedParameters(params)
                        .setUseLegacySql(false)
                        .build();
    }
}
