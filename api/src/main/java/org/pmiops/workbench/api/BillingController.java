package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import org.pmiops.workbench.db.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@Component
@RestController
public class BillingController implements BillingApiDelegate {

  private static final Logger log = Logger.getLogger(BillingController.class.getName());

  private final BigQueryService bigQueryService;
  private final UserDao userDao;

  @Autowired
  BillingController(
      BigQueryService bigQueryService,
      UserDao userDao) {
    this.bigQueryService = bigQueryService;
    this.userDao = userDao;
  }

  @Override
  public ResponseEntity<Void> findUsersExceedingFreeTierBilling() {
    log.info("testing cron job");
    QueryJobConfiguration queryConfig = QueryJobConfiguration
        .newBuilder("SELECT project.id, SUM(cost) cost, MIN(DATE(usage_start_time)) start_time, MAX(DATE(usage_end_time)) end_time FROM `all-of-us-workbench-test-bd.billing_data.gcp_billing_export_v1_014D91_FCB792_33D2C0` GROUP BY project.id ORDER BY cost desc;")
        .build();

    TableResult result = bigQueryService.executeQuery(queryConfig);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
