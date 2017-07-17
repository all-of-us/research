package org.pmiops.workbench.firecloud;

import org.pmiops.workbench.auth.UserAuthentication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class FireCloudConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient getFireCloudApiClient(UserAuthentication userAuthentication) {
    ApiClient apiClient = new ApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }
}
