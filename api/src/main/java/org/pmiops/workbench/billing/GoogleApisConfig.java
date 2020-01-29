package org.pmiops.workbench.billing;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.inject.Provider;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class GoogleApisConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Cloudbilling googleCloudBillingApi(
      UserAuthentication userAuthentication,
      JsonFactory jsonFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider)
      throws GeneralSecurityException, IOException {
    GoogleCredentials credentials =
        new GoogleCredentials(new AccessToken(userAuthentication.getCredentials(), null));

    return new Cloudbilling.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            new HttpCredentialsAdapter(credentials))
        .setApplicationName(workbenchConfigProvider.get().server.projectId)
        .build();
  }
}
