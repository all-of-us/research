package org.pmiops.workbench.firecloud;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.auth.OAuth;
import org.pmiops.workbench.firecloud.model.ManagedGroupAccessResponse;
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.firecloud.model.SystemStatus;
import org.pmiops.workbench.test.Providers;
import org.springframework.retry.backoff.NoBackOffPolicy;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FireCloudServiceImplTest {

  private FireCloudServiceImpl service;

  @Mock
  private ProfileApi profileApi;
  @Mock
  private BillingApi billingApi;
  @Mock
  private WorkspacesApi workspacesApi;
  @Mock
  private GroupsApi groupsApi;
  @Mock
  private GroupsApi endUserGroupsApi;
  @Mock
  private NihApi nihApi;
  @Mock
  private StatusApi statusApi;
  @Mock
  private GoogleCredential fireCloudCredential;
  @Mock
  private GoogleCredential impersonatedCredential;
  @Mock
  private HttpTransport httpTransport;

  private WorkbenchConfig workbenchConfig;
  private GoogleCredential.Builder credentialBuilder;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    // We want to partially mock the credential builder: the builder methods should act as normal,
    // but we'll mock out .build() to instead return a pre-baked GoogleCredential mock.
    credentialBuilder = mock(GoogleCredential.Builder.class, Mockito.CALLS_REAL_METHODS);

    workbenchConfig = new WorkbenchConfig();
    workbenchConfig.firecloud = new WorkbenchConfig.FireCloudConfig();
    workbenchConfig.firecloud.baseUrl = "https://api.firecloud.org";
    workbenchConfig.firecloud.debugEndpoints = true;

    service = new FireCloudServiceImpl(Providers.of(workbenchConfig),
        Providers.of(profileApi), Providers.of(billingApi), Providers.of(groupsApi),
        Providers.of(endUserGroupsApi), Providers.of(nihApi), Providers.of(workspacesApi),
        Providers.of(statusApi), new FirecloudRetryHandler(new NoBackOffPolicy()),
        Providers.of(fireCloudCredential),
        Providers.of(credentialBuilder),
        httpTransport);
  }

  @Test
  public void testStatus_success() throws ApiException {
    when(statusApi.status()).thenReturn(new SystemStatus());
    assertThat(service.getFirecloudStatus()).isTrue();
  }

  @Test
  public void testStatus_handleApiException() throws ApiException {
    when(statusApi.status()).thenThrow(new ApiException(500, null, "{\"ok\": false}"));
    assertThat(service.getFirecloudStatus()).isFalse();
  }

  @Test
  public void testStatus_handleJsonException() throws ApiException {
    when(statusApi.status()).thenThrow(new ApiException(500, null, "unparseable response"));
    assertThat(service.getFirecloudStatus()).isFalse();
  }

  @Test(expected = NotFoundException.class)
  public void testGetMe_throwsNotFound() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(404, "blah"));
    service.getMe();
  }

  @Test(expected = ForbiddenException.class)
  public void testGetMe_throwsForbidden() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(403, "blah"));
    service.getMe();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetMe_throwsUnauthorized() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(401, "blah"));
    service.getMe();
  }

  @Test
  public void testIsUserMemberOfGroup_none() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(new ArrayList<ManagedGroupAccessResponse>());
    assertThat(service.isUserMemberOfGroup("group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_noNameMatch() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(
        Lists.newArrayList(new ManagedGroupAccessResponse().groupName("blah").role("Member")));
    assertThat(service.isUserMemberOfGroup("group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_noRoleMatch() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(
        Lists.newArrayList(new ManagedGroupAccessResponse().groupName("group").role("notmember")));
    assertThat(service.isUserMemberOfGroup("group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_match() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(
        Lists.newArrayList(new ManagedGroupAccessResponse().groupName("group").role("member")));
    assertThat(service.isUserMemberOfGroup("group")).isTrue();
  }

  @Test
  public void testNihStatus() throws Exception {
    NihStatus status = new NihStatus().linkedNihUsername("test").linkExpireTime(500L);
    when(nihApi.nihStatus()).thenReturn(status);
    assertThat(service.getNihStatus()).isNotNull();
    assertThat(service.getNihStatus()).isEqualTo(status);
  }

  @Test
  public void testNihStatusNotFound() throws Exception {
    when(nihApi.nihStatus()).thenThrow(new ApiException(404, "Not Found"));
    assertThat(service.getNihStatus()).isNull();
  }

  @Test(expected = ServerErrorException.class)
  public void testNihStatusException() throws Exception {
    when(nihApi.nihStatus()).thenThrow(new ApiException(500, "Internal Server Error"));
    service.getNihStatus();
  }

  @Test
  public void testNihCallback() throws Exception {
    doNothing().when(nihApi).nihCallback(any());
    try {
      service.postNihCallback(any());
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = BadRequestException.class)
  public void testNihCallbackBadRequest() throws Exception {
    doThrow(new ApiException(400, "Bad Request")).when(nihApi).nihCallback(any());
    service.postNihCallback(any());
  }

  @Test(expected = ServerErrorException.class)
  public void testNihCallbackServerError() throws Exception {
    doThrow(new ApiException(500, "Internal Server Error")).when(nihApi).nihCallback(any());
    service.postNihCallback(any());
  }

  @Test
  public void testGetApiClientWithImpersonation() throws IOException {
    // Avoid actually generating derived credentials by swapping in a mocked GoogleCredential when
    // the builder is invoked.
    doReturn(impersonatedCredential).when(credentialBuilder).build();
    // Skip the refreshToken process which actually calls OAuth endpoints.
    when(impersonatedCredential.refreshToken()).thenReturn(true);
    // Pretend we retrieved the given access token.
    when(impersonatedCredential.getAccessToken()).thenReturn("impersonated-access-token");

    ApiClient apiClient = service.getApiClientWithImpersonation("asdf@fake-research-aou.org");

    // The Credential builder should be called with the impersonated username.
    verify(credentialBuilder).setServiceAccountUser("asdf@fake-research-aou.org");

    // The impersonated access token should be assigned to the generated API client.
    OAuth oauth = (OAuth) apiClient.getAuthentication("googleoauth");
    assertThat(oauth.getAccessToken()).isEqualTo("impersonated-access-token");
  }

}
