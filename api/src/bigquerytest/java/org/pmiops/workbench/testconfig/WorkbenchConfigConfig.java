package org.pmiops.workbench.testconfig;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.CdrConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.db.model.User;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WorkbenchConfigConfig {

  @Bean
  public WorkbenchConfig workbenchConfig() {
    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.cdr = new CdrConfig();
    workbenchConfig.cdr.debugQueries = true;
    workbenchConfig.firecloud = new FireCloudConfig();
    workbenchConfig.firecloud.registeredDomainName = "all-of-us-registered-test";
    return workbenchConfig;
  }

  @Bean
  public User user() {
    User user = new User();
    user.setEmail("all-of-us-workbench-test@appspot.gserviceaccount.com");
    return user;
  }
}
