package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class FireCloudIntegrationTest {

  @Autowired private FireCloudService service;

  private static WorkbenchConfig config;
  private static final FireCloudConfig fireCloudConfig = new FireCloudConfig();

  @TestConfiguration
  @ComponentScan("org.pmiops.workbench.firecloud")
  @Import({FireCloudServiceImpl.class, IntegrationTestConfig.class})
  static class Configuration {
    // Because we want to be able to adjust the config from within individual test cases, we need
    // to provide a prototype-scoped bean override. This will cause the autowired service to call
    // this method as its Provider<WorkbenchConfig>.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return config;
    }

    // We need to redefine a bean for any API clients that are used within these integration tests.
    // This is because FireCloudConfig defines the beans as request-scoped, but there's no request
    // scope available from an integration test setting.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StatusApi statusApi() {
      return fireCloudConfig.statusApi(config);
    }
  }

  private WorkbenchConfig loadConfig(String filename) throws IOException {
    String testConfig =
        Resources.toString(Resources.getResource(filename), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
    workbenchConfig.firecloud.debugEndpoints = true;
    return workbenchConfig;
  }

  @Before
  public void setUp() throws IOException {
    config = loadConfig("config_test.json");
  }

  @Test
  public void testStatusProd() throws IOException {
    config = loadConfig("config_prod.json");
    assertThat(service.getBasePath()).isEqualTo("https://api.firecloud.org");
    assertThat(service.getFirecloudStatus()).isTrue();
  }

  @Test
  public void testStatusDev() {
    assertThat(service.getBasePath())
        .isEqualTo("https://firecloud-orchestration.dsde-dev.broadinstitute.org");
    assertThat(service.getFirecloudStatus()).isTrue();
  }

  /**
   * Ensures we can successfully use delegation of authority to make FireCloud API calls on behalf
   * of AoU users.
   *
   * <p>This test depends on there being an active account in FireCloud dev with the email address
   * integration-test-user@fake-research-aou.org.
   */
  @Test
  public void testImpersonatedProfileCall() throws Exception {
    ApiClient apiClient =
        service.getApiClientWithImpersonation("integration-test-user@fake-research-aou.org");

    // Run the most basic API call against the /me/ endpoint.
    ProfileApi profileApi = new ProfileApi(apiClient);
    FirecloudMe me = profileApi.me();
    assertThat(me.getUserInfo().getUserEmail())
        .isEqualTo("integration-test-user@fake-research-aou.org");
    assertThat(me.getUserInfo().getUserSubjectId()).isEqualTo("101727030557929965916");

    // Run a test against a different FireCloud endpoint. This is important, because the /me/
    // endpoint is accessible even by service accounts whose subject IDs haven't been whitelisted
    // by FireCloud devops.
    //
    // If we haven't had our "firecloud-admin" service account whitelisted,
    // then the following API call would result in a 401 error instead of a 404.
    NihApi nihApi = new NihApi(apiClient);
    int responseCode = 0;
    try {
      nihApi.nihStatus();
    } catch (ApiException e) {
      responseCode = e.getCode();
    }
    assertThat(responseCode).isEqualTo(404);
  }
}
