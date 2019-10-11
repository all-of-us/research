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

  /**
   * BQ can be flaky sometimes throwing 'Read timed out' exceptions. This method overrides
   * executeQuery so on 'Read timed out' we can try again without failing the test case.
   */
  public TableResult executeQuery(QueryJobConfiguration query, long waitTime) {
    int count = 0;
    int maxTries = 3;
    while (true) {
      try {
        return super.executeQuery(query, 1000L);
      } catch (BigQueryException e) {
        if (e.getMessage().equals("Read timed out")) {
          // if 'Read timed out' throw exception after 3 tries.
          if (++count == maxTries) {
            throw e;
          }
        } else {
          // not a read timed out exception so throw
          throw e;
        }
      }
    }
  }
}
