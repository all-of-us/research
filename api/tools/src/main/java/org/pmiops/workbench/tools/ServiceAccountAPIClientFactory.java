package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.Arrays;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;

public class ServiceAccountAPIClientFactory {

  final String apiUrl;

  private static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
      };

  public ServiceAccountAPIClientFactory(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  private ApiClient newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(FC_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    return apiClient;
  }

  public WorkspacesApi workspacesApi() throws IOException {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  public BillingApi billingApi() throws IOException {
    BillingApi api = new BillingApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }
}
