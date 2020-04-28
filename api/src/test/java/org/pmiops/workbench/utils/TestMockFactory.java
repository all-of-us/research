package org.pmiops.workbench.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.BillingAccount;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;

public class TestMockFactory {
  public static final String BUCKET_NAME = "workspace-bucket";
  private static final String CDR_VERSION_ID = "1";
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME = "billingAccounts/00000-AAAAA-BBBBB";

  public Workspace createWorkspace(String workspaceNameSpace, String workspaceName) {
    List<DisseminateResearchEnum> disseminateResearchEnumsList = new ArrayList<>();
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES);
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS);

    List<ResearchOutcomeEnum> ResearchOutcomeEnumsList = new ArrayList<>();
    ResearchOutcomeEnumsList.add(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT);

    return new Workspace()
        .id(workspaceNameToId(workspaceName))
        .etag("\"1\"")
        .name(workspaceName)
        .namespace(workspaceNameSpace)
        .dataAccessLevel(DataAccessLevel.PROTECTED)
        .cdrVersionId(CDR_VERSION_ID)
        .googleBucketName(BUCKET_NAME)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
        .billingAccountType(BillingAccountType.FREE_TIER)
        .creationTime(1588097211621L)
        .creator("jay@unit-test-research-aou.org")
        .googleBucketName("fc-secure-111111-2222-AAAA-BBBB-000000000000")
        .lastModifiedTime(1588097211621L)
        .published(false)
        .researchPurpose(
            new ResearchPurpose()
                .additionalNotes(null)
                .diseaseFocusedResearch(true)
                .diseaseOfFocus("cancer")
                .methodsDevelopment(true)
                .controlSet(true)
                .ancestry(true)
                .commercialPurpose(true)
                .socialBehavioral(true)
                .populationHealth(true)
                .educational(true)
                .drugDevelopment(true)
                .populationDetails(Collections.emptyList())
                .additionalNotes("additional notes")
                .reasonForAllOfUs("reason for aou")
                .intendedStudy("intended study")
                .anticipatedFindings("anticipated findings")
                .timeRequested(1000L)
                .timeReviewed(1500L)
                .reviewRequested(true)
                .disseminateResearchFindingList(disseminateResearchEnumsList)
                .researchOutcomeList(ResearchOutcomeEnumsList)
                .approved(false));
  }

  private String workspaceNameToId(String workspaceName) {
    return "mynewestworkspace";
  }

  public FirecloudWorkspace createFcWorkspace(String ns, String name, String creator) {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setWorkspaceId(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    fcWorkspace.setBucketName(BUCKET_NAME);
    return fcWorkspace;
  }

  public ListClusterResponse createFcListClusterResponse() {
    ListClusterResponse listClusterResponse =
        new ListClusterResponse()
            .clusterName("cluster")
            .googleProject("google-project")
            .status(ClusterStatus.STOPPED);
    return listClusterResponse;
  }

  public void stubCreateFcWorkspace(FireCloudService fireCloudService) {
    doAnswer(
            invocation -> {
              String capturedWorkspaceName = (String) invocation.getArguments()[1];
              String capturedWorkspaceNamespace = (String) invocation.getArguments()[0];
              FirecloudWorkspace fcWorkspace =
                  createFcWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null);

              FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
              fcResponse.setWorkspace(fcWorkspace);
              fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());

              doReturn(fcResponse)
                  .when(fireCloudService)
                  .getWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName);
              return fcWorkspace;
            })
        .when(fireCloudService)
        .createWorkspace(anyString(), anyString());
  }

  public void stubBufferBillingProject(BillingProjectBufferService billingProjectBufferService) {
    doAnswer(
            invocation -> {
              DbBillingProjectBufferEntry entry = mock(DbBillingProjectBufferEntry.class);
              doReturn(UUID.randomUUID().toString()).when(entry).getFireCloudProjectName();
              return entry;
            })
        .when(billingProjectBufferService)
        .assignBillingProject(any());
  }

  public static Cloudbilling createMockedCloudbilling() {
    Cloudbilling cloudbilling = mock(Cloudbilling.class);
    Cloudbilling.Projects projects = mock(Cloudbilling.Projects.class);

    try {
      doAnswer(
              invocation -> {
                ProjectBillingInfo projectBillingInfo = invocation.getArgument(1);

                Cloudbilling.Projects.UpdateBillingInfo updateBillingInfo =
                    mock(Cloudbilling.Projects.UpdateBillingInfo.class);
                doReturn(projectBillingInfo).when(updateBillingInfo).execute();

                return updateBillingInfo;
              })
          .when(projects)
          .updateBillingInfo(anyString(), any(ProjectBillingInfo.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    doReturn(projects).when(cloudbilling).projects();

    Cloudbilling.BillingAccounts billingAccounts = mock(Cloudbilling.BillingAccounts.class);
    Cloudbilling.BillingAccounts.Get getRequest = mock(Cloudbilling.BillingAccounts.Get.class);
    Cloudbilling.BillingAccounts.List listRequest = mock(Cloudbilling.BillingAccounts.List.class);

    try {
      doReturn(new BillingAccount().setOpen(true)).when(getRequest).execute();
      doReturn(getRequest).when(billingAccounts).get(anyString());

      doReturn(new ListBillingAccountsResponse()).when(listRequest).execute();
      doReturn(listRequest).when(billingAccounts).list();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    doReturn(billingAccounts).when(cloudbilling).billingAccounts();
    return cloudbilling;
  }
}
