package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.services.cloudresourcemanager.model.Project;
import java.util.List;
import org.junit.Test;
import org.pmiops.workbench.BaseIntegrationTest;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

public class CloudResourceManagerServiceImplIntegrationTest extends BaseIntegrationTest {
  @Autowired private CloudResourceManagerService service;

  @TestConfiguration
  @ComponentScan(basePackageClasses = CloudResourceManagerServiceImpl.class)
  @Import({CloudResourceManagerServiceImpl.class})
  static class Configuration {}

  // This is a single hand created user in the fake-research-aou.org gsuite.
  // It has one project that has been shared with it, AoU CRM Integration Test
  // in the firecloud dev domain.
  private final String CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL =
      "cloud-resource-manager-integration-test@fake-research-aou.org";

  @Test
  public void testGetAllProjectsForUser() throws Exception {
    DbUser testUser = new DbUser();
    testUser.setUsername(CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL);
    List<Project> projectList = service.getAllProjectsForUser(testUser);
    assertThat(projectList.size()).isEqualTo(1);
  }
}
