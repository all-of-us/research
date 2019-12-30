package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.apache.ApacheHttpTransport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.IntegrationTestConfig;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {IntegrationTestConfig.class})
public class DirectoryServiceImplIntegrationTest {
  private DirectoryServiceImpl service;
  @Autowired
  @Qualifier(Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
  private ServiceAccountCredentials gSuiteAdminCredentials;
  @Autowired
  private WorkbenchConfig workbenchConfig;
  @Autowired
  private ApacheHttpTransport httpTransport;
  @Autowired
  private ServiceAccounts serviceAccounts;

  @Before
  public void setUp() {
    service =
        new DirectoryServiceImpl(
            Providers.of(gSuiteAdminCredentials),
            Providers.of(workbenchConfig),
            httpTransport,
            new GoogleRetryHandler(new NoBackOffPolicy()),
            serviceAccounts);
  }

  @Test
  public void testDummyUsernameIsNotTaken() {
    assertThat(service.isUsernameTaken("username-that-should-not-exist")).isFalse();
  }

  @Test
  public void testDirectoryServiceUsernameIsTaken() {
    assertThat(service.isUsernameTaken("directory-service")).isTrue();
  }

  @Test
  public void testCreateAndDeleteTestUser() {
    String userName = String.format("integration.test.%d", Clock.systemUTC().millis());
    service.createUser("Integration", "Test", userName, "notasecret@gmail.com");
    assertThat(service.isUsernameTaken(userName)).isTrue();

    // As of ~6/25/19, customSchemas are sometimes unavailable on the initial call to Gsuite. This
    // data is likely not written with strong consistency. Retry until it is available.
    Map<String, Object> aouMeta =
        retryTemplate()
            .execute(
                c -> {
                  Map<String, Map<String, Object>> schemas =
                      service.getUserByUsername(userName).getCustomSchemas();
                  if (schemas == null) {
                    throw new RuntimeException("custom schemas is still null");
                  }
                  return schemas.get("All_of_Us_Workbench");
                });
    // Ensure our two custom schema fields are correctly set & re-fetched from GSuite.
    assertThat(aouMeta).containsEntry("Institution", "All of Us Research Workbench");
    assertThat(service.getContactEmailAddress(userName)).isEqualTo("notasecret@gmail.com");
    service.deleteUser(userName);
    assertThat(service.isUsernameTaken(userName)).isFalse();
  }

  private static RetryTemplate retryTemplate() {
    RetryTemplate tmpl = new RetryTemplate();
    ExponentialRandomBackOffPolicy backoff = new ExponentialRandomBackOffPolicy();
    tmpl.setBackOffPolicy(backoff);
    SimpleRetryPolicy retry = new SimpleRetryPolicy();
    retry.setMaxAttempts(10);
    tmpl.setRetryPolicy(retry);
    tmpl.setThrowLastExceptionOnExhausted(true);
    return tmpl;
  }
}
