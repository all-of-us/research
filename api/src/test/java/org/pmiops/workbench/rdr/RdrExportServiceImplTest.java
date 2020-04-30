package org.pmiops.workbench.rdr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RdrExportServiceImplTest {
  @Autowired private RdrExportService rdrExportService;

  @MockBean private ApiClient mockApiClient;
  @MockBean private RdrApi mockRdrApi;
  @MockBean private RdrExportDao rdrExportDao;
  @MockBean private UserDao mockUserDao;
  @MockBean private WorkspaceDao mockWorkspaceDao;
  @MockBean private WorkspaceService mockWorkspaceService;
  @MockBean private InstitutionService institutionService;
  @MockBean private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private static final Instant NOW = Instant.now();
  private static final Timestamp NOW_TIMESTAMP = Timestamp.from(NOW);
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  private DbUser dbUserWithEmail;
  private DbUser dbUserWithoutEmail;
  private DbWorkspace mockWorkspace;

  @TestConfiguration
  @Import({RdrExportServiceImpl.class})
  @MockBean({WorkspaceDao.class, WorkspaceService.class, VerifiedInstitutionalAffiliationDao.class})
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    rdrExportService = spy(rdrExportService);
    when(mockRdrApi.getApiClient()).thenReturn(mockApiClient);
    when(mockApiClient.setDebugging(true)).thenReturn(null);

    dbUserWithEmail = new DbUser();
    dbUserWithEmail.setUserId(1L);
    dbUserWithEmail.setCreationTime(NOW_TIMESTAMP);
    dbUserWithEmail.setLastModifiedTime(NOW_TIMESTAMP);
    dbUserWithEmail.setGivenName("icanhas");
    dbUserWithEmail.setFamilyName("email");
    dbUserWithEmail.setContactEmail("i.can.has.email@gmail.com");
    dbUserWithEmail.setDegreesEnum(Collections.singletonList(Degree.NONE));

    when(mockUserDao.findUserByUserId(1L)).thenReturn(dbUserWithEmail);
    dbUserWithoutEmail = new DbUser();
    dbUserWithoutEmail.setUserId(2L);
    dbUserWithoutEmail.setCreationTime(NOW_TIMESTAMP);
    dbUserWithoutEmail.setLastModifiedTime(NOW_TIMESTAMP);
    dbUserWithoutEmail.setGivenName("icannothas");
    dbUserWithoutEmail.setFamilyName("email");
    dbUserWithoutEmail.setDegreesEnum(Collections.singletonList(Degree.NONE));

    when(mockUserDao.findUserByUserId(2L)).thenReturn(dbUserWithoutEmail);

    when(rdrExportDao.findByEntityTypeAndEntityId(anyShort(), anyLong())).thenReturn(null);
    mockWorkspace =
        buildDbWorkspace(1, "workspace_name", "workspaceNS", WorkspaceActiveStatus.ACTIVE);
    mockWorkspace.setCreator(dbUserWithEmail);
    when(mockWorkspaceDao.findDbWorkspaceByWorkspaceId(1)).thenReturn(mockWorkspace);

    DbVerifiedInstitutionalAffiliation mockVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    mockVerifiedInstitutionalAffiliation.setInstitution(
        new DbInstitution().setShortName("mockInstitution"));
    mockVerifiedInstitutionalAffiliation.setInstitutionalRoleEnum(
        InstitutionalRole.PROJECT_PERSONNEL);
    when(verifiedInstitutionalAffiliationDao.findFirstByUser(dbUserWithEmail))
        .thenReturn(Optional.of(mockVerifiedInstitutionalAffiliation));
  }

  private DbWorkspace buildDbWorkspace(
      long dbId, String name, String namespace, WorkspaceActiveStatus activeStatus) {
    DbWorkspace workspace = new DbWorkspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    workspace.setLastModifiedTime(nowTimestamp);
    workspace.setCreationTime(nowTimestamp);
    workspace.setName(name);
    workspace.setWorkspaceId(dbId);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    workspace.setFirecloudName(name);
    workspace.setFirecloudUuid(Long.toString(dbId));
    workspace.setScientificApproach("Scientific Approach");
    workspace.setReasonForAllOfUs("Reason for AllOf Us");
    workspace.setEthics(false);
    workspace.setReviewRequested(true);
    return workspace;
  }

  @Test
  public void exportUsers_successful() throws ApiException {
    doNothing().when(mockRdrApi).exportResearchers(anyList());

    List<Long> userIds = new ArrayList<>();
    userIds.add(dbUserWithEmail.getUserId());
    userIds.add(dbUserWithoutEmail.getUserId());
    rdrExportService.exportUsers(userIds);

    verify(rdrExportService, times(1)).updateDbRdrExport(any(), anyList());
  }

  @Test
  public void exportUsers_unsuccessful_no_persist() throws ApiException {
    doThrow(new ApiException()).when(mockRdrApi).exportResearchers(anyList());

    List<Long> userIds = new ArrayList<>();
    userIds.add(dbUserWithEmail.getUserId());
    userIds.add(dbUserWithoutEmail.getUserId());
    rdrExportService.exportUsers(userIds);

    verify(rdrExportService, times(0)).updateDbRdrExport(any(), anyList());
  }

  @Test
  public void exportWorkspace() throws ApiException {
    List<Long> workspaceID = new ArrayList<>();
    workspaceID.add(1l);
    rdrExportService.exportWorkspaces(workspaceID);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(
            mockWorkspace.getWorkspaceNamespace(), mockWorkspace.getFirecloudName());
    verify(rdrExportDao, times(1)).save(anyList());

    RdrWorkspace rdrWorkspace = toRdrWorkspace(mockWorkspace);
    verify(mockRdrApi).exportWorkspaces(Arrays.asList(rdrWorkspace));
  }

  /**
   * In case workspace has any specific population FocusOnUnderrepresentedPopulations should be true
   *
   * @throws ApiException
   */
  @Test
  public void exportWorkspace_FocusOnUnderservedPopulation() throws ApiException {
    Set<SpecificPopulationEnum> specificPopulationEnumsSet = new HashSet<SpecificPopulationEnum>();
    specificPopulationEnumsSet.add(SpecificPopulationEnum.RACE_AA);
    mockWorkspace.setSpecificPopulationsEnum(specificPopulationEnumsSet);
    when(mockWorkspaceDao.findDbWorkspaceByWorkspaceId(1)).thenReturn(mockWorkspace);

    List<Long> workspaceID = new ArrayList<>();
    workspaceID.add(1l);
    rdrExportService.exportWorkspaces(workspaceID);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(
            mockWorkspace.getWorkspaceNamespace(), mockWorkspace.getFirecloudName());
    verify(rdrExportDao, times(1)).save(anyList());

    RdrWorkspace rdrWorkspace = toRdrWorkspace(mockWorkspace);
    rdrWorkspace
        .getWorkspaceDemographic()
        .setRaceEthnicity(Arrays.asList(RdrWorkspaceDemographic.RaceEthnicityEnum.AA));
    rdrWorkspace.setFocusOnUnderrepresentedPopulations(true);
    verify(mockRdrApi).exportWorkspaces(Arrays.asList(rdrWorkspace));
  }

  private RdrWorkspace toRdrWorkspace(DbWorkspace dbWorkspace) {
    ZoneOffset offset = OffsetDateTime.now().getOffset();
    RdrWorkspace rdrWorkspace = new RdrWorkspace();
    rdrWorkspace.setWorkspaceId(1);
    rdrWorkspace.setName(dbWorkspace.getName());

    rdrWorkspace.setCreationTime(dbWorkspace.getCreationTime().toLocalDateTime().atOffset(offset));
    rdrWorkspace.setModifiedTime(
        dbWorkspace.getLastModifiedTime().toLocalDateTime().atOffset(offset));
    rdrWorkspace.setStatus(RdrWorkspace.StatusEnum.ACTIVE);
    rdrWorkspace.setWorkspaceUsers(new ArrayList());
    rdrWorkspace.setExcludeFromPublicDirectory(false);
    rdrWorkspace.setDiseaseFocusedResearch(false);
    rdrWorkspace.setDiseaseFocusedResearchName(null);
    rdrWorkspace.setOtherPurpose(false);
    rdrWorkspace.setOtherPurposeDetails(null);
    rdrWorkspace.setMethodsDevelopment(false);
    rdrWorkspace.setControlSet(false);
    rdrWorkspace.setAncestry(false);
    rdrWorkspace.setSocialBehavioral(false);
    rdrWorkspace.setPopulationHealth(false);
    rdrWorkspace.setDrugDevelopment(false);
    rdrWorkspace.setCommercialPurpose(false);
    rdrWorkspace.setEducational(false);
    rdrWorkspace.setEthicalLegalSocialImplications(false);
    rdrWorkspace.setScientificApproaches("Scientific Approach");
    rdrWorkspace.setReviewRequested(true);

    rdrWorkspace.setFocusOnUnderrepresentedPopulations(false);
    RdrWorkspaceDemographic workspaceDemographic = new RdrWorkspaceDemographic();
    workspaceDemographic.setRaceEthnicity(
        Arrays.asList(RdrWorkspaceDemographic.RaceEthnicityEnum.UNSET));
    workspaceDemographic.setAge(Arrays.asList(RdrWorkspaceDemographic.AgeEnum.UNSET));
    workspaceDemographic.setSexAtBirth(RdrWorkspaceDemographic.SexAtBirthEnum.UNSET);
    workspaceDemographic.setGenderIdentity(RdrWorkspaceDemographic.GenderIdentityEnum.UNSET);
    workspaceDemographic.setSexualOrientation(RdrWorkspaceDemographic.SexualOrientationEnum.UNSET);
    workspaceDemographic.setGeography(RdrWorkspaceDemographic.GeographyEnum.UNSET);
    workspaceDemographic.setDisabilityStatus(RdrWorkspaceDemographic.DisabilityStatusEnum.UNSET);
    workspaceDemographic.setAccessToCare(RdrWorkspaceDemographic.AccessToCareEnum.UNSET);
    workspaceDemographic.setEducationLevel(RdrWorkspaceDemographic.EducationLevelEnum.UNSET);
    workspaceDemographic.setIncomeLevel(RdrWorkspaceDemographic.IncomeLevelEnum.UNSET);
    rdrWorkspace.setWorkspaceDemographic(workspaceDemographic);
    if (dbWorkspace.getSpecificPopulationsEnum().contains(SpecificPopulationEnum.OTHER)) {
      rdrWorkspace.getWorkspaceDemographic().setOthers(dbWorkspace.getOtherPopulationDetails());
    }

    return rdrWorkspace;
  }
}
