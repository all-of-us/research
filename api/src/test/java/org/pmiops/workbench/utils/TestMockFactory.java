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
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public class TestMockFactory {
  public static final String WORKSPACE_BUCKET_NAME = "fc-secure-111111-2222-AAAA-BBBB-000000000000";
  private static final String CDR_VERSION_ID = "1";
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME = "billingAccounts/00000-AAAAA-BBBBB";
  private static final String WORKSPACE_FIRECLOUD_NAME =
      "gonewiththewind"; // should match workspace name w/o spaces

  public Workspace createWorkspace(String workspaceNameSpace, String workspaceName) {
    List<DisseminateResearchEnum> disseminateResearchEnumsList = new ArrayList<>();
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES);
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS);

    List<ResearchOutcomeEnum> ResearchOutcomeEnumsList = new ArrayList<>();
    ResearchOutcomeEnumsList.add(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT);

    return new Workspace()
        .id(WORKSPACE_FIRECLOUD_NAME)
        .etag("\"1\"")
        .name(workspaceName)
        .namespace(workspaceNameSpace)
        .dataAccessLevel(DataAccessLevel.PROTECTED)
        .cdrVersionId(CDR_VERSION_ID)
        .googleBucketName(WORKSPACE_BUCKET_NAME)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
        .billingAccountType(BillingAccountType.FREE_TIER)
        .creationTime(1588097211621L)
        .creator("jay@unit-test-research-aou.org")
        .creationTime(Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli())
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

  public static FirecloudWorkspace createFirecloudWorkspace(
      String ns, String name, String creator) {
    return new FirecloudWorkspace()
        .namespace(ns)
        .workspaceId(ns)
        .name(name)
        .createdBy(creator)
        .bucketName(WORKSPACE_BUCKET_NAME);
  }

  public LeonardoListRuntimeResponse createLeonardoListRuntimesResponse() {
    return new LeonardoListRuntimeResponse()
        .runtimeName("runtime")
        .googleProject("google-project")
        .status(LeonardoRuntimeStatus.STOPPED);
  }

  public static void stubCreateFcWorkspace(FireCloudService fireCloudService) {
    doAnswer(
            invocation -> {
              String capturedWorkspaceName = (String) invocation.getArguments()[1];
              String capturedWorkspaceNamespace = (String) invocation.getArguments()[0];
              FirecloudWorkspace fcWorkspace =
                  createFirecloudWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null);

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

  // TODO(jaycarlton) use  WorkspaceMapper.toDbWorkspace() once it's available RW 4803
  public static DbWorkspace createDbWorkspaceStub(Workspace workspace, long workspaceDbId) {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceId(workspaceDbId);
    dbWorkspace.setName(workspace.getName());
    dbWorkspace.setWorkspaceNamespace(workspace.getNamespace());
    // a.k.a. FirecloudWorkspace.name
    dbWorkspace.setFirecloudName(workspace.getId()); // DB_WORKSPACE_FIRECLOUD_NAME
    ResearchPurpose researchPurpose = workspace.getResearchPurpose();
    dbWorkspace.setDiseaseFocusedResearch(researchPurpose.getDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(researchPurpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(researchPurpose.getMethodsDevelopment());
    dbWorkspace.setControlSet(researchPurpose.getControlSet());
    dbWorkspace.setAncestry(researchPurpose.getAncestry());
    dbWorkspace.setCommercialPurpose(researchPurpose.getCommercialPurpose());
    dbWorkspace.setSocialBehavioral(researchPurpose.getSocialBehavioral());
    dbWorkspace.setPopulationHealth(researchPurpose.getPopulationHealth());
    dbWorkspace.setEducational(researchPurpose.getEducational());
    dbWorkspace.setDrugDevelopment(researchPurpose.getDrugDevelopment());

    dbWorkspace.setSpecificPopulationsEnum(new HashSet<>(researchPurpose.getPopulationDetails()));
    dbWorkspace.setAdditionalNotes(researchPurpose.getAdditionalNotes());
    dbWorkspace.setReasonForAllOfUs(researchPurpose.getReasonForAllOfUs());
    dbWorkspace.setIntendedStudy(researchPurpose.getIntendedStudy());
    dbWorkspace.setAnticipatedFindings(researchPurpose.getAnticipatedFindings());
    return dbWorkspace;
  }

  // The Stopwatch class is final, so we can't create a fake implementation. The
  // next best thing is this helper method for setting all the method stubs in
  // a test's @Before method.
  // Deprecated in favor of Stopwatch.createStarted() et al.
  @Deprecated
  public static void stubStopwatch(Stopwatch mockStopwatch, Duration elapsed) {
    doReturn(elapsed).when(mockStopwatch).elapsed();

    // Just use millis unconditionally. This method is not recommended and not used currently,
    // so I'm not investing in making this correct yet.
    doReturn(elapsed.toMillis()).when(mockStopwatch).elapsed(any(TimeUnit.class));

    doReturn(mockStopwatch).when(mockStopwatch).start();
    doReturn(mockStopwatch).when(mockStopwatch).stop();
    doReturn(mockStopwatch).when(mockStopwatch).reset();
  }
}
