package org.pmiops.workbench.firecloud;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.auth.ServiceAccounts;
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
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.auth.OAuth;
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.firecloud.model.SystemStatus;
import org.pmiops.workbench.test.Providers;
import org.springframework.retry.backoff.NoBackOffPolicy;

public class FireCloudServiceImplTest {

  private static final String EMAIL_ADDRESS = "abc@fake-research-aou.org";

  private FireCloudServiceImpl service;

  private WorkbenchConfig workbenchConfig;

  @Mock private ProfileApi profileApi;
  @Mock private BillingApi billingApi;
  @Mock private WorkspacesApi workspacesApi;
  @Mock private WorkspacesApi workspaceAclsApi;
  @Mock private GroupsApi groupsApi;
  @Mock private NihApi nihApi;
  @Mock private StatusApi statusApi;
  @Mock private StaticNotebooksApi staticNotebooksApi;
  @Mock private GoogleCredential fireCloudCredential;
  @Mock private ServiceAccounts serviceAccounts;
  @Mock private GoogleCredential impersonatedCredential;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.firecloud.baseUrl = "https://api.firecloud.org";
    workbenchConfig.firecloud.debugEndpoints = true;
    workbenchConfig.firecloud.timeoutInSeconds = 20;
    workbenchConfig.billing.accountId = "test-billing-account";

    service =
        new FireCloudServiceImpl(
            Providers.of(workbenchConfig),
            Providers.of(profileApi),
            Providers.of(billingApi),
            Providers.of(groupsApi),
            Providers.of(nihApi),
            Providers.of(workspacesApi),
            Providers.of(workspaceAclsApi),
            Providers.of(statusApi),
            Providers.of(staticNotebooksApi),
            new FirecloudRetryHandler(new NoBackOffPolicy()),
            serviceAccounts,
            Providers.of(fireCloudCredential));
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
    when(groupsApi.getGroup("group")).thenReturn(new ManagedGroupWithMembers());
    assertThat(service.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_noNameMatch() throws Exception {
    ManagedGroupWithMembers group = new ManagedGroupWithMembers();
    group.setMembersEmails(Arrays.asList("asdf@fake-research-aou.org"));
    when(groupsApi.getGroup("group")).thenReturn(group);
    assertThat(service.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_matchInAdminList() throws Exception {
    ManagedGroupWithMembers group = new ManagedGroupWithMembers();
    group.setAdminsEmails(Arrays.asList(EMAIL_ADDRESS));

    when(groupsApi.getGroup("group")).thenReturn(group);
    assertThat(service.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isTrue();
  }

  @Test
  public void testIsUserMemberOfGroup_matchInMemberList() throws Exception {
    ManagedGroupWithMembers group = new ManagedGroupWithMembers();
    group.setMembersEmails(Arrays.asList(EMAIL_ADDRESS));

    when(groupsApi.getGroup("group")).thenReturn(group);
    assertThat(service.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isTrue();
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
    when(nihApi.nihCallback(any()))
        .thenReturn(new NihStatus().linkedNihUsername("test").linkExpireTime(500L));
    try {
      service.postNihCallback(any());
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = BadRequestException.class)
  public void testNihCallbackBadRequest() throws Exception {
    when(nihApi.nihCallback(any())).thenThrow(new ApiException(400, "Bad Request"));
    service.postNihCallback(any());
  }

  @Test(expected = ServerErrorException.class)
  public void testNihCallbackServerError() throws Exception {
    when(nihApi.nihCallback(any())).thenThrow(new ApiException(500, "Internal Server Error"));
    service.postNihCallback(any());
  }

  @Test
  public void testGetApiClientWithImpersonation() throws IOException {
    when(serviceAccounts.getImpersonatedCredential(any(), eq("asdf@fake-research-aou.org"), any()))
        .thenReturn(impersonatedCredential);

    // Pretend we retrieved the given access token.
    when(impersonatedCredential.getAccessToken()).thenReturn("impersonated-access-token");

    ApiClient apiClient = service.getApiClientWithImpersonation("asdf@fake-research-aou.org");

    // The impersonated access token should be assigned to the generated API client.
    OAuth oauth = (OAuth) apiClient.getAuthentication("googleoauth");
    assertThat(oauth.getAccessToken()).isEqualTo("impersonated-access-token");
  }

  @Test
  public void testCreateAllOfUsBillingProject() throws Exception {
    workbenchConfig.featureFlags.enableVpcFlowLogs = false;
    workbenchConfig.featureFlags.enableVpcServicePerimeter = false;
    workbenchConfig.firecloud.vpcServicePerimeterName = "this will be ignored";

    service.createAllOfUsBillingProject("project-name");

    ArgumentCaptor<CreateRawlsBillingProjectFullRequest> captor =
        ArgumentCaptor.forClass(CreateRawlsBillingProjectFullRequest.class);
    verify(billingApi).createBillingProjectFull(captor.capture());
    CreateRawlsBillingProjectFullRequest request = captor.getValue();

    // N.B. FireCloudServiceImpl doesn't add the project prefix; this is done by callers such
    // as BillingProjectBufferService.
    assertThat(request.getProjectName()).isEqualTo("project-name");
    // FireCloudServiceImpl always adds the "billingAccounts/" prefix to the billing account
    // from config.
    assertThat(request.getBillingAccount()).isEqualTo("billingAccounts/test-billing-account");
    assertThat(request.getEnableFlowLogs()).isFalse();
    assertThat(request.getHighSecurityNetwork()).isFalse();
    assertThat(request.getServicePerimeter()).isNull();
  }

  @Test
  public void testVpcFlowLogsParams() throws Exception {
    workbenchConfig.featureFlags.enableVpcFlowLogs = true;

    service.createAllOfUsBillingProject("project-name");

    ArgumentCaptor<CreateRawlsBillingProjectFullRequest> captor =
        ArgumentCaptor.forClass(CreateRawlsBillingProjectFullRequest.class);
    verify(billingApi).createBillingProjectFull(captor.capture());
    CreateRawlsBillingProjectFullRequest request = captor.getValue();

    assertThat(request.getEnableFlowLogs()).isTrue();
    assertThat(request.getHighSecurityNetwork()).isTrue();
  }

  @Test
  public void testVpcServicePerimeterParams() throws Exception {
    String servicePerimeter = "a-cloud-with-a-fence-around-it";

    workbenchConfig.featureFlags.enableVpcServicePerimeter = true;
    workbenchConfig.firecloud.vpcServicePerimeterName = servicePerimeter;

    service.createAllOfUsBillingProject("project-name");

    ArgumentCaptor<CreateRawlsBillingProjectFullRequest> captor =
        ArgumentCaptor.forClass(CreateRawlsBillingProjectFullRequest.class);
    verify(billingApi).createBillingProjectFull(captor.capture());
    CreateRawlsBillingProjectFullRequest request = captor.getValue();

    assertThat(request.getServicePerimeter()).isEqualTo(servicePerimeter);
  }
}
