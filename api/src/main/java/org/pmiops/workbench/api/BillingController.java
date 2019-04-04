package org.pmiops.workbench.api;

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
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
