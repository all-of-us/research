package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BillingControllerTest {

  @TestConfiguration
  @Import({
      BillingController.class
  })
  @MockBean({BigQueryService.class})
  static class Configuration {}

  @Autowired
  private BillingController billingController;

  @Test
  public void testBulkSyncTrainingStatus() throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    billingController.findUsersExceedingFreeTierBilling();
  }

}
