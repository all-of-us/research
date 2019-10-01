package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This sole purpose of this class is to override the setting of projectId on the BigQuery service
 * instance so test cases are runnable inside IntelliJ.
 */
public class BigQueryTestService extends BigQueryService {

  @Autowired private BigQuery bigquery;

  protected BigQuery getBigQueryService() {
    return bigquery;
  }

  /** Execute the provided query using bigquery. */
  public TableResult executeQuery(QueryJobConfiguration query) {
    int count = 0;
    int maxTries = 3;
    while (true) {
      try {
        return executeQuery(query, 1000L);
      } catch (BigQueryException e) {
        // handle exception
        if (++count == maxTries) throw e;
      }
    }
  }
}
