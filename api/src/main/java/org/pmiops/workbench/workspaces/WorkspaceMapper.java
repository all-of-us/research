package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMapper {

  public WorkspaceAccessLevel toApiWorkspaceAccessLevel(String firecloudAccessLevel) {
    if (firecloudAccessLevel.equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(firecloudAccessLevel);
    }
  }

  public Workspace toApiWorkspace(
      org.pmiops.workbench.db.model.Workspace workspace,
      Map<org.pmiops.workbench.db.model.User, WorkspaceAccessEntry> firecloudAcls) {
    ResearchPurpose researchPurpose = createResearchPurpose(workspace);
    FirecloudWorkspaceId workspaceId = workspace.getFirecloudWorkspaceId();

    Workspace result =
        new Workspace()
            .etag(Etags.fromVersion(workspace.getVersion()))
            .lastModifiedTime(workspace.getLastModifiedTime().getTime())
            .creationTime(workspace.getCreationTime().getTime())
            .dataAccessLevel(workspace.getDataAccessLevelEnum())
            .name(workspace.getName())
            .id(workspaceId.getWorkspaceName())
            .namespace(workspaceId.getWorkspaceNamespace())
            .researchPurpose(researchPurpose);
    if (workspace.getCreator() != null) {
      result.setCreator(workspace.getCreator().getEmail());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    result.setUserRoles(
        firecloudAcls.entrySet().stream().map(this::toApiUserRole).collect(Collectors.toList()));

    return result;
  }

  public Workspace toApiWorkspace(
      org.pmiops.workbench.db.model.Workspace workspace,
      org.pmiops.workbench.firecloud.model.Workspace fcWorkspace,
      Map<org.pmiops.workbench.db.model.User, WorkspaceAccessEntry> firecloudAcls) {
    ResearchPurpose researchPurpose = createResearchPurpose(workspace);
    if (workspace.getPopulation()) {
      researchPurpose.setPopulationDetails(new ArrayList<>(workspace.getSpecificPopulationsEnum()));
    }

    Workspace result =
        new Workspace()
            .etag(Etags.fromVersion(workspace.getVersion()))
            .lastModifiedTime(workspace.getLastModifiedTime().getTime())
            .creationTime(workspace.getCreationTime().getTime())
            .dataAccessLevel(workspace.getDataAccessLevelEnum())
            .name(workspace.getName())
            .id(fcWorkspace.getName())
            .namespace(fcWorkspace.getNamespace())
            .researchPurpose(researchPurpose)
            .googleBucketName(fcWorkspace.getBucketName());
    if (fcWorkspace.getCreatedBy() != null) {
      result.setCreator(fcWorkspace.getCreatedBy());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }

    result.setUserRoles(
        firecloudAcls.entrySet().stream().map(this::toApiUserRole).collect(Collectors.toList()));

    return result;
  }

  public org.pmiops.workbench.db.model.Workspace toDbWorkspace(Workspace workspace) {
    org.pmiops.workbench.db.model.Workspace result = new org.pmiops.workbench.db.model.Workspace();

    if (workspace.getDataAccessLevel() != null) {
      result.setDataAccessLevelEnum(workspace.getDataAccessLevel());
    }

    result.setName(workspace.getName());

    if (workspace.getResearchPurpose() != null) {
      setResearchPurposeDetails(result, workspace.getResearchPurpose());
      result.setReviewRequested(workspace.getResearchPurpose().getReviewRequested());
      if (workspace.getResearchPurpose().getTimeRequested() != null) {
        result.setTimeRequested(new Timestamp(workspace.getResearchPurpose().getTimeRequested()));
      }
      result.setApproved(workspace.getResearchPurpose().getApproved());
    }

    return result;
  }

  public UserRole toApiUserRole(
      Map.Entry<org.pmiops.workbench.db.model.User, WorkspaceAccessEntry> aclEntry) {
    org.pmiops.workbench.db.model.User user = aclEntry.getKey();
    UserRole result = new UserRole();
    result.setEmail(user.getEmail());
    result.setGivenName(user.getGivenName());
    result.setFamilyName(user.getFamilyName());
    result.setRole(WorkspaceAccessLevel.fromValue(aclEntry.getValue().getAccessLevel()));
    return result;
  }

  /**
   * This probably doesn't belong in a mapper service but it makes the refactoring easier atm. Sets
   * user-editable research purpose detail fields.
   */
  public static void setResearchPurposeDetails(
      org.pmiops.workbench.db.model.Workspace dbWorkspace, ResearchPurpose purpose) {
    dbWorkspace.setDiseaseFocusedResearch(purpose.getDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(purpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(purpose.getMethodsDevelopment());
    dbWorkspace.setControlSet(purpose.getControlSet());
    dbWorkspace.setAncestry(purpose.getAncestry());
    dbWorkspace.setCommercialPurpose(purpose.getCommercialPurpose());
    dbWorkspace.setPopulation(purpose.getPopulation());
    if (purpose.getPopulation()) {
      dbWorkspace.setSpecificPopulationsEnum(new HashSet<>(purpose.getPopulationDetails()));
    }
    dbWorkspace.setSocialBehavioral(purpose.getSocialBehavioral());
    dbWorkspace.setPopulationHealth(purpose.getPopulationHealth());
    dbWorkspace.setEducational(purpose.getEducational());
    dbWorkspace.setDrugDevelopment(purpose.getDrugDevelopment());
    dbWorkspace.setOtherPurpose(purpose.getOtherPurpose());
    dbWorkspace.setOtherPurposeDetails(purpose.getOtherPurposeDetails());
    dbWorkspace.setAdditionalNotes(purpose.getAdditionalNotes());
    dbWorkspace.setReasonForAllOfUs(purpose.getReasonForAllOfUs());
    dbWorkspace.setIntendedStudy(purpose.getIntendedStudy());
    dbWorkspace.setAnticipatedFindings(purpose.getAnticipatedFindings());
    dbWorkspace.setOtherPopulationDetails(purpose.getOtherPopulationDetails());
  }

  private final ResearchPurpose createResearchPurpose(
      org.pmiops.workbench.db.model.Workspace workspace) {
    ResearchPurpose researchPurpose =
        new ResearchPurpose()
            .diseaseFocusedResearch(workspace.getDiseaseFocusedResearch())
            .diseaseOfFocus(workspace.getDiseaseOfFocus())
            .methodsDevelopment(workspace.getMethodsDevelopment())
            .controlSet(workspace.getControlSet())
            .ancestry(workspace.getAncestry())
            .commercialPurpose(workspace.getCommercialPurpose())
            .socialBehavioral(workspace.getSocialBehavioral())
            .educational(workspace.getEducational())
            .drugDevelopment(workspace.getDrugDevelopment())
            .populationHealth(workspace.getPopulationHealth())
            .otherPurpose(workspace.getOtherPurpose())
            .otherPurposeDetails(workspace.getOtherPurposeDetails())
            .population(workspace.getPopulation())
            .reasonForAllOfUs(workspace.getReasonForAllOfUs())
            .intendedStudy(workspace.getIntendedStudy())
            .anticipatedFindings(workspace.getAnticipatedFindings())
            .additionalNotes(workspace.getAdditionalNotes())
            .reviewRequested(workspace.getReviewRequested())
            .approved(workspace.getApproved())
            .otherPopulationDetails(workspace.getOtherPopulationDetails());
    if (workspace.getTimeRequested() != null) {
      researchPurpose.timeRequested(workspace.getTimeRequested().getTime());
    }
    return researchPurpose;
  }
}
