package org.pmiops.workbench.rdr;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import main.java.org.pmiops.workbench.db.model.RdrEntityEnums;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.*;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The purpose of this service is to export user/workspace data from workbench to Research Directory
 *
 * @author nsaxena
 */
@Service
public class RdrExportServiceImpl implements RdrExportService {

  private Clock clock;
  private FireCloudService fireCloudService;
  private Provider<RdrApi> rdrApiProvider;
  private RdrExportDao rdrExportDao;
  private WorkspaceDao workspaceDao;
  private UserDao userDao;
  private WorkspaceService workspaceService;

  private static final Logger log = Logger.getLogger(RdrExportService.class.getName());
  ZoneOffset offset = OffsetDateTime.now().getOffset();

  @Autowired
  public RdrExportServiceImpl(
      Clock clock,
      FireCloudService fireCloudService,
      Provider<RdrApi> RdrApiProvider,
      RdrExportDao rdrExportDao,
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService,
      UserDao userDao) {
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.rdrExportDao = rdrExportDao;
    this.rdrApiProvider = RdrApiProvider;
    this.workspaceDao = workspaceDao;
    this.workspaceService = workspaceService;
    this.userDao = userDao;
  }

  /**
   * Retrieve the list of all users ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (user table) > export_time (rdr_export table)
   *
   * @return list of User Ids
   */
  @Override
  public List<Long> findAllUserIdsToExport() {
    List<Long> userIdList = new ArrayList<Long>();
    try {
      userIdList =
          rdrExportDao.findDbUserIdsToExport().stream()
              .map(user -> user.longValue())
              .collect(Collectors.toList());
    } catch (Exception ex) {
      log.severe(
          String.format(
              "Error while trying to fetch modified/created user list: %s", ex.getMessage()));
    }
    return userIdList;
  }

  /**
   * Retrieve the list of all workspace ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (workspace table) > export_time (rdr_export table)
   *
   * @return list of Workspace Ids
   */
  @Override
  public List<Long> findAllWorkspacesIdsToExport() {
    List<Long> workspaceListToExport = new ArrayList<Long>();
    try {
      workspaceListToExport =
          rdrExportDao.findDbWorkspaceIdsToExport().stream()
              .map(workspaceId -> workspaceId.longValue())
              .collect(Collectors.toList());
    } catch (Exception ex) {
      log.severe(
          String.format(
              "Error while trying to fetch modified/created workspace list: %s", ex.getMessage()));
    }
    return workspaceListToExport;
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param userIds
   */
  @Override
  public void exportUsers(List<Long> userIds) {
    List<RdrResearcher> RdrResearchersList;
    try {
      RdrResearchersList =
          userIds.stream()
              .map(userId -> toRdrResearcher(userDao.findUserByUserId(userId)))
              .collect(Collectors.toList());
      rdrApiProvider.get().getApiClient().setDebugging(true);
      rdrApiProvider.get().exportResearcheres(RdrResearchersList);

      updateDBRdrExport(RdrEntity.USER, userIds);
    } catch (ApiException ex) {
      log.severe("Error while sending researcher data to RDR");
    }
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param workspaceIds
   */
  @Override
  public void exportWorkspaces(List<Long> workspaceIds) {
    List<RdrWorkspace> rdrWorkspacesList;
    try {
      rdrWorkspacesList =
          workspaceIds.stream()
              .map(
                  workspaceId ->
                      toRdrWorkspace(workspaceDao.findDbWorkspaceByWorkspaceId(workspaceId)))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (!rdrWorkspacesList.isEmpty()) {
        rdrApiProvider.get().getApiClient().setDebugging(true);
        rdrApiProvider.get().exportWorkspaces(rdrWorkspacesList);
        updateDBRdrExport(RdrEntity.WORKSPACE, workspaceIds);
      }
    } catch (ApiException ex) {
      log.severe("Error while sending workspace data to RDR");
    }
  }

  // Convert workbench DBUser to RDR Model
  private RdrResearcher toRdrResearcher(DbUser dbUser) {
    RdrResearcher researcher = new RdrResearcher();
    researcher.setUserId((int) dbUser.getUserId());
    // RDR will start accepting null creation Time and later once workbench works on story
    // https://precisionmedicineinitiative.atlassian.net/browse/RW-3741 RDR will create API to
    // backfill data
    if (null != researcher.getCreationTime()) {
      researcher.setCreationTime(dbUser.getCreationTime().toLocalDateTime().atOffset(offset));
    }
    researcher.setModifiedTime(dbUser.getLastModifiedTime().toLocalDateTime().atOffset(offset));

    researcher.setGivenName(dbUser.getGivenName());
    researcher.setFamilyName(dbUser.getFamilyName());
    researcher.setDegrees(
        dbUser.getDegreesEnum().stream()
            .map(RdrExportEnums::degreeToRdrDegree)
            .collect(Collectors.toList()));

    if (dbUser.getAddress() != null) {
      researcher.setStreetAddress1(dbUser.getAddress().getStreetAddress1());
      researcher.setStreetAddress2(dbUser.getAddress().getStreetAddress2());
      researcher.setCity(dbUser.getAddress().getCity());
      researcher.setState(dbUser.getAddress().getState());
      researcher.setCountry(dbUser.getAddress().getCountry());
      researcher.setZipCode(dbUser.getAddress().getZipCode());
    }
    DbDemographicSurvey dbDemographicSurvey = dbUser.getDemographicSurvey();
    if (null != dbDemographicSurvey) {
      researcher.setDisability(
          RdrExportEnums.disabilityToRdrDisability(dbDemographicSurvey.getDisabilityEnum()));
      researcher.setEducation(
          RdrExportEnums.educationToRdrEducation(dbDemographicSurvey.getEducationEnum()));
      researcher.setEthnicity(
          Optional.ofNullable(dbDemographicSurvey.getEthnicityEnum())
              .map(RdrExportEnums::ethnicityToRdrEthnicity)
              .orElse(null));

      researcher.setSexAtBirth(
          Optional.ofNullable(
                  dbDemographicSurvey.getSexAtBirthEnum().stream()
                      .map(RdrExportEnums::sexAtBirthToRdrSexAtBirth)
                      .collect(Collectors.toList()))
              .orElse(new ArrayList<SexAtBirth>()));
      researcher.setGender(
          Optional.ofNullable(
                  dbDemographicSurvey.getGenderIdentityEnumList().stream()
                      .map(RdrExportEnums::genderToRdrGender)
                      .collect(Collectors.toList()))
              .orElse(new ArrayList<Gender>()));

      researcher.setDisability(
          RdrExportEnums.disabilityToRdrDisability(dbDemographicSurvey.getDisabilityEnum()));

      researcher.setRace(
          Optional.ofNullable(
                  dbDemographicSurvey.getRaceEnum().stream()
                      .map(RdrExportEnums::raceToRdrRace)
                      .collect(Collectors.toList()))
              .orElse(new ArrayList<Race>()));

      researcher.setLgbtqIdentity(dbDemographicSurvey.getLgbtqIdentity());
      researcher.setIdentifiesAsLgbtq(dbDemographicSurvey.getIdentifiesAsLgbtq());
    }
    researcher.setAffiliations(
        dbUser.getInstitutionalAffiliations().stream()
            .map(
                inst -> {
                  return new ResearcherAffiliation()
                      .institution(inst.getInstitution())
                      .role(inst.getRole());
                })
            .collect(Collectors.toList()));
    return researcher;
  }

  private RdrWorkspace toRdrWorkspace(DbWorkspace dbWorkspace) {
    RdrWorkspace rdrWorkspace = new RdrWorkspace();
    rdrWorkspace.setWorkspaceId((int) dbWorkspace.getWorkspaceId());
    rdrWorkspace.setName(dbWorkspace.getName());

    rdrWorkspace.setCreationTime(dbWorkspace.getCreationTime().toLocalDateTime().atOffset(offset));
    rdrWorkspace.setModifiedTime(
        dbWorkspace.getLastModifiedTime().toLocalDateTime().atOffset(offset));
    rdrWorkspace.setStatus(
        org.pmiops.workbench.rdr.model.RdrWorkspace.StatusEnum.fromValue(
            dbWorkspace.getWorkspaceActiveStatusEnum().toString()));
    rdrWorkspace.setExcludeFromPublicDirectory(false);
    rdrWorkspace.setDiseaseFocusedResearch(dbWorkspace.getDiseaseFocusedResearch());
    rdrWorkspace.setDiseaseFocusedResearchName(dbWorkspace.getDiseaseOfFocus());
    rdrWorkspace.setOtherPurpose(dbWorkspace.getOtherPurpose());
    rdrWorkspace.setOtherPurposeDetails(dbWorkspace.getOtherPurposeDetails());
    rdrWorkspace.setMethodsDevelopment(dbWorkspace.getMethodsDevelopment());
    rdrWorkspace.setControlSet(dbWorkspace.getControlSet());
    rdrWorkspace.setAncestry(dbWorkspace.getAncestry());
    rdrWorkspace.setSocialBehavioral(dbWorkspace.getSocialBehavioral());
    rdrWorkspace.setPopulationHealth(dbWorkspace.getPopulationHealth());
    rdrWorkspace.setDrugDevelopment(dbWorkspace.getDrugDevelopment());
    rdrWorkspace.setCommercialPurpose(dbWorkspace.getCommercialPurpose());
    rdrWorkspace.setEducational(dbWorkspace.getEducational());
    rdrWorkspace.setEthicalLegalSocialImplications(dbWorkspace.getEthics());
    rdrWorkspace.setScientificApproaches(dbWorkspace.getScientificApproach());
    rdrWorkspace.setIntendToStudy(dbWorkspace.getIntendedStudy());
    rdrWorkspace.setFindingsFromStudy(dbWorkspace.getAnticipatedFindings());

    rdrWorkspace.setWorkspaceDemographic(
        toWorkspaceDemographics(dbWorkspace.getSpecificPopulationsEnum()));

    if (dbWorkspace.getSpecificPopulationsEnum().contains(SpecificPopulationEnum.OTHER)) {
      rdrWorkspace.getWorkspaceDemographic().setOthers(dbWorkspace.getOtherPopulationDetails());
    }

    try {
      // Call Firecloud to get a list of Collaborators
      List<UserRole> collaboratorsMap =
          workspaceService.getFirecloudUserRoles(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

      // Since the USERS cannot be deleted from workbench yet, hence sending the the status of
      // COLLABORATOR as ACTIVE
      collaboratorsMap.forEach(
          (userRole) -> {
            RdrWorkspaceUser workspaceUserMap = new RdrWorkspaceUser();
            workspaceUserMap.setUserId(
                (int) userDao.findUserByUsername(userRole.getEmail()).getUserId());
            workspaceUserMap.setRole(
                RdrWorkspaceUser.RoleEnum.fromValue(userRole.getRole().toString()));
            workspaceUserMap.setStatus(RdrWorkspaceUser.StatusEnum.ACTIVE);
            rdrWorkspace.addWorkspaceUsersItem(workspaceUserMap);
          });
    } catch (Exception ex) {
      log.warning(
          String.format(
              "Exception while retrieving workspace collaborators for workspace id %s, skipping this workspace for RDR Export",
              rdrWorkspace.getWorkspaceId()));
      return null;
    }

    return rdrWorkspace;
  }

  WorkspaceDemographic toWorkspaceDemographics(Set<SpecificPopulationEnum> dbPopulationEnumSet) {
    WorkspaceDemographic demographic = new WorkspaceDemographic();

    demographic.setAccessToCare(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.ACCESS_TO_CARE)
            ? WorkspaceDemographic.AccessToCareEnum.NOT_EASILY_ACCESS_CARE
            : WorkspaceDemographic.AccessToCareEnum.UNSET);

    demographic.setDisabilityStatus(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.DISABILITY_STATUS)
            ? WorkspaceDemographic.DisabilityStatusEnum.DISABILITY
            : WorkspaceDemographic.DisabilityStatusEnum.UNSET);

    demographic.setEducationLevel(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.EDUCATION_LEVEL)
            ? WorkspaceDemographic.EducationLevelEnum.LESS_THAN_HIGH_SCHOOL
            : WorkspaceDemographic.EducationLevelEnum.UNSET);

    demographic.setIncomeLevel(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.INCOME_LEVEL)
            ? WorkspaceDemographic.IncomeLevelEnum.BELOW_FEDERAL_POVERTY_LEVEL_200_PERCENT
            : WorkspaceDemographic.IncomeLevelEnum.UNSET);

    demographic.setGeography(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.GEOGRAPHY)
            ? WorkspaceDemographic.GeographyEnum.RURAL
            : WorkspaceDemographic.GeographyEnum.UNSET);

    demographic.setSexualOrientation(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.SEXUAL_ORIENTATION)
            ? WorkspaceDemographic.SexualOrientationEnum.OTHER_THAN_STRAIGHT
            : WorkspaceDemographic.SexualOrientationEnum.UNSET);

    demographic.setGenderIdentity(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.GENDER_IDENTITY)
            ? WorkspaceDemographic.GenderIdentityEnum.OTHER_THAN_MAN_WOMAN
            : WorkspaceDemographic.GenderIdentityEnum.UNSET);

    demographic.setSexAtBirth(
        dbPopulationEnumSet.contains(SpecificPopulationEnum.SEX)
            ? WorkspaceDemographic.SexAtBirthEnum.INTERSEX
            : WorkspaceDemographic.SexAtBirthEnum.UNSET);

    demographic.setRaceEthnicity(
        dbPopulationEnumSet.stream()
            .map(RdrExportEnums::specificPopulationToRaceEthnicity)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

    if (demographic.getRaceEthnicity().isEmpty()) {
      demographic.setRaceEthnicity(Arrays.asList(WorkspaceDemographic.RaceEthnicityEnum.UNSET));
    }

    demographic.setAge(
        dbPopulationEnumSet.stream()
            .map(RdrExportEnums::specificPopulationToAge)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));

    if (demographic.getAge().isEmpty()) {
      demographic.setAge(Arrays.asList(WorkspaceDemographic.AgeEnum.UNSET));
    }

    return demographic;
  }

  /**
   * For Each entityType and entity id update lastExportDate to current date time if it exist in
   * rdr_export table else add a new entry
   *
   * @param entity
   * @param idList
   */
  private void updateDBRdrExport(RdrEntity entity, List<Long> idList) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    List<DbRdrExport> exportList =
        idList.stream()
            .map(
                id -> {
                  DbRdrExport rd =
                      rdrExportDao.findByEntityTypeAndEntityId(
                          RdrEntityEnums.entityToStorage(entity), id);
                  // If Entry doesn't exist in rdr_export create an object else just update the
                  // export Date
                  // to right now
                  if (rd == null) {
                    rd = new DbRdrExport();
                    rd.setEntityTypeEnum(entity);
                    rd.setEntityId(id);
                  }
                  rd.setLastExportDate(now);
                  return rd;
                })
            .collect(Collectors.toList());
    rdrExportDao.save(exportList);
  }
}
