package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class BigQueryService {

    private static final Logger logger = Logger.getLogger(BigQueryService.class.getName());

    @Autowired
    private BigQuery bigquery;

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfigProvider;

    /**
     * Execute the provided query using bigquery.
     */
    public QueryResult executeQuery(QueryJobConfiguration query) {

        // Execute the query
        QueryResponse response = null;
        if (workbenchConfigProvider.get().cdr.debugQueries) {
            logger.log(Level.INFO, "Executing query ({0}) with parameters ({1})",
                new Object[] { query.getQuery(), query.getNamedParameters()});
        }
        try {
            response = bigquery.query(query, BigQuery.QueryOption.of(BigQuery.QueryResultsOption.maxWaitTime(60000L)));
        } catch (InterruptedException e) {
            throw new BigQueryException(500, "Something went wrong with BigQuery: " + e.getMessage());
        }

        return response.getResult();
    }

    public QueryJobConfiguration filterBigQueryConfig(QueryJobConfiguration queryJobConfiguration) {
        CdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
        if (cdrVersion == null) {
            throw new ServerErrorException("No CDR version specified");
        }
        String returnSql = queryJobConfiguration.getQuery().replace("${projectId}",
            cdrVersion.getBigqueryProject());
        returnSql = returnSql.replace("${dataSetId}", cdrVersion.getBigqueryDataset());
        return queryJobConfiguration
                .toBuilder()
                .setQuery(returnSql)
                .build();
    }

    public Map<String, Integer> getResultMapper(QueryResult result) {
        AtomicInteger index = new AtomicInteger();
        return result.getSchema().getFields().stream().collect(
                Collectors.toMap(Field::getName, s -> index.getAndIncrement()));
    }

    public Long getLong(List<FieldValue> row, int index) {
        if (row.get(index).isNull()) {
            throw new BigQueryException(500, "FieldValue is null at position: " + index);
        }
        return row.get(index).getLongValue();
    }

    public boolean isNull(List<FieldValue> row, int index) {
      return row.get(index).isNull();
    }

    public String getString(List<FieldValue> row, int index) {
        return row.get(index).isNull() ? null : row.get(index).getStringValue();
    }

    public Boolean getBoolean(List<FieldValue> row, int index) {
        return row.get(index).getBooleanValue();
    }

    public Date getDate(List<FieldValue> row, int index) {
        if (row.get(index).isNull()) {
            throw new BigQueryException(500, "FieldValue is null at position: " + index);
        }
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            isoFormat.setTimeZone(TimeZone.getDefault());
            return isoFormat.parse(Instant.ofEpochMilli(
                    Double.valueOf(row.get(index).getStringValue()).longValue() * 1000).toString());
        } catch (Exception e) {
            throw new BigQueryException(500, "Failed to parse date: " + e.getMessage());
        }
    }
}
