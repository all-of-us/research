package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.cohortreview.querybuilder.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewTabQueryBuilder {

    public QueryJobConfiguration buildQuery(String query,
                                            String sortColumnDatabaseColumnName,
                                            Long participantId,
                                            PageRequest pageRequest) {
        String finalSql = String.format(query,
          sortColumnDatabaseColumnName,
          pageRequest.getSortOrder().toString(),
          pageRequest.getPageSize(),
          pageRequest.getPageNumber() * pageRequest.getPageSize());

        return buildQuery(finalSql, participantId);
    }

    public QueryJobConfiguration buildCountQuery(String query, Long participantId) {
        return buildQuery(query, participantId);
    }

    private QueryJobConfiguration buildQuery(String query, Long participantId) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        params.put(ReviewQueryBuilder.NAMED_PARTICIPANTID_PARAM, QueryParameterValue.int64(participantId));
        return QueryJobConfiguration
                .newBuilder(query)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }
}
