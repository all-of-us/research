package org.pmiops.workbench.workspaceadmin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cohortreview.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class WorkspaceAdminControllerTest {

  private static final long DB_WORKSPACE_ID = 2222L;
  private static final String FIRECLOUD_WORKSPACE_CREATOR_USERNAME = "jay@allofus.biz";
  private static final String WORKSPACE_NAME = "Gone with the Wind";
  private static final String DB_WORKSPACE_FIRECLOUD_NAME = "gonewiththewind";
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String NONSENSE_NAMESPACE = "wharrgarbl_wharrgarbl";

  @MockBean private CloudMonitoringService mockCloudMonitoringService;
  @MockBean private FireCloudService mockFirecloudService;
  @MockBean private LeonardoNotebooksClient mockLeonardoNotebooksClient;
  @MockBean private WorkspaceAdminService mockWorkspaceAdminService;
  @MockBean private WorkspaceService mockWorkspaceService;

  @Autowired private TestMockFactory testMockFactory;
  @Autowired private WorkspaceAdminController workspaceAdminController;

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    TestMockFactory.class,
    WorkspaceAdminController.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    CloudStorageService.class,
    NotebooksService.class,
  })
  static class Configuration {
    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.enableBillingLockout = false;
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    when(mockWorkspaceAdminService.getFirstWorkspaceByNamespace(anyString()))
        .thenReturn(Optional.empty());

    final Workspace workspace =
        testMockFactory.makeCreatedWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME);
    final DbWorkspace dbWorkspace = createDbWorkspaceStub(workspace);
    when(mockWorkspaceAdminService.getFirstWorkspaceByNamespace(WORKSPACE_NAMESPACE))
        .thenReturn(Optional.of(dbWorkspace));

    final UserRole collaborator =
        new UserRole().email("test@test.test").role(WorkspaceAccessLevel.WRITER);
    final List<UserRole> collaborators = ImmutableList.of(collaborator);
    when(mockWorkspaceService.getFirecloudUserRoles(
            WORKSPACE_NAMESPACE, DB_WORKSPACE_FIRECLOUD_NAME))
        .thenReturn(collaborators);

    final AdminWorkspaceObjectsCounts adminWorkspaceObjectsCounts =
        new AdminWorkspaceObjectsCounts().cohortCount(1).conceptSetCount(2).datasetCount(3);
    when(mockWorkspaceAdminService.getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId()))
        .thenReturn(adminWorkspaceObjectsCounts);

    final AdminWorkspaceCloudStorageCounts cloudStorageCounts =
        new AdminWorkspaceCloudStorageCounts()
            .notebookFileCount(1)
            .nonNotebookFileCount(2)
            .storageBytesUsed(123456789L);
    when(mockWorkspaceAdminService.getAdminWorkspaceCloudStorageCounts(
            WORKSPACE_NAMESPACE, dbWorkspace.getFirecloudName()))
        .thenReturn(cloudStorageCounts);

    org.pmiops.workbench.notebooks.model.ListClusterResponse firecloudListClusterResponse =
        testMockFactory.createFirecloudListClusterResponse();
    List<org.pmiops.workbench.notebooks.model.ListClusterResponse> clusters =
        ImmutableList.of(firecloudListClusterResponse);
    when(mockLeonardoNotebooksClient.listClustersByProjectAsService(WORKSPACE_NAMESPACE))
        .thenReturn(clusters);

    FirecloudWorkspace fcWorkspace =
        testMockFactory.createFirecloudWorkspace(
            WORKSPACE_NAMESPACE, DB_WORKSPACE_FIRECLOUD_NAME, FIRECLOUD_WORKSPACE_CREATOR_USERNAME);
    FirecloudWorkspaceResponse fcWorkspaceResponse =
        new FirecloudWorkspaceResponse().workspace(fcWorkspace);
    when(mockFirecloudService.getWorkspaceAsService(
            WORKSPACE_NAMESPACE, DB_WORKSPACE_FIRECLOUD_NAME))
        .thenReturn(fcWorkspaceResponse);
  }

  @Test
  public void getFederatedWorkspaceDetails() {
    ResponseEntity<AdminFederatedWorkspaceDetailsResponse> response =
        workspaceAdminController.getFederatedWorkspaceDetails(WORKSPACE_NAMESPACE);
    assertThat(response.getStatusCodeValue()).isEqualTo(200);

    AdminFederatedWorkspaceDetailsResponse workspaceDetailsResponse = response.getBody();
    assertThat(workspaceDetailsResponse.getWorkspace().getNamespace())
        .isEqualTo(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getName()).isEqualTo(WORKSPACE_NAME);

    AdminWorkspaceResources resources = workspaceDetailsResponse.getResources();
    AdminWorkspaceObjectsCounts objectsCounts = resources.getWorkspaceObjects();
    assertThat(objectsCounts.getCohortCount()).isEqualTo(1);
    assertThat(objectsCounts.getConceptSetCount()).isEqualTo(2);
    assertThat(objectsCounts.getDatasetCount()).isEqualTo(3);

    AdminWorkspaceCloudStorageCounts cloudStorageCounts = resources.getCloudStorage();
    assertThat(cloudStorageCounts.getNotebookFileCount()).isEqualTo(1);
    assertThat(cloudStorageCounts.getNonNotebookFileCount()).isEqualTo(2);
    assertThat(cloudStorageCounts.getStorageBytesUsed()).isEqualTo(123456789L);

    List<ListClusterResponse> clusters = resources.getClusters();
    assertThat(clusters.size()).isEqualTo(1);
    ListClusterResponse cluster = clusters.get(0);
    assertThat(cluster.getClusterName()).isEqualTo("cluster");
    assertThat(cluster.getGoogleProject()).isEqualTo("google-project");
    assertThat(cluster.getStatus()).isEqualTo(ClusterStatus.STOPPED);
  }

  @Test
  public void getFederatedWorkspaceDetails_404sWhenNotFound() {
    ResponseEntity<AdminFederatedWorkspaceDetailsResponse> response =
        workspaceAdminController.getFederatedWorkspaceDetails(NONSENSE_NAMESPACE);
    assertThat(response.getStatusCodeValue()).isEqualTo(404);
  }

  @Test
  public void getCloudStorageTraffic_sortsPointsByTimestamp() {
    TimeSeries timeSeries =
        TimeSeries.newBuilder()
            .addPoints(
                Point.newBuilder()
                    .setInterval(TimeInterval.newBuilder().setEndTime(Timestamps.fromMillis(2000)))
                    .setValue(TypedValue.newBuilder().setDoubleValue(1234)))
            .addPoints(
                Point.newBuilder()
                    .setInterval(TimeInterval.newBuilder().setEndTime(Timestamps.fromMillis(1000)))
                    .setValue(TypedValue.newBuilder().setDoubleValue(1234)))
            .build();

    when(mockCloudMonitoringService.getCloudStorageReceivedBytes(anyString(), any(Duration.class)))
        .thenReturn(Arrays.asList(timeSeries));

    CloudStorageTraffic cloudStorageTraffic =
        workspaceAdminController.getCloudStorageTraffic(WORKSPACE_NAMESPACE).getBody();

    assertThat(
            cloudStorageTraffic.getReceivedBytes().stream()
                .map(timeSeriesPoint -> timeSeriesPoint.getTimestamp())
                .collect(Collectors.toList()))
        .containsExactly(1000L, 2000L);
  }

  // TODO(jaycarlton) use  WorkspaceMapper.toDbWorkspace() once it's available RW 4803
  private DbWorkspace createDbWorkspaceStub(Workspace workspace) {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceId(DB_WORKSPACE_ID);
    dbWorkspace.setName(workspace.getName());
    dbWorkspace.setWorkspaceNamespace(workspace.getNamespace());
    // a.k.a. FirecloudWorkspace.name
    dbWorkspace.setFirecloudName(workspace.getId()); // DB_WORKSPACE_FIRECLOUD_NAME);
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
}
