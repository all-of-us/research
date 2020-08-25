package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.collections4.CollectionUtils;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptSetsController implements ConceptSetsApiDelegate {

  private final WorkspaceService workspaceService;
  private final ConceptSetService conceptSetService;
  private final UserRecentResourceService userRecentResourceService;
  private final Provider<DbUser> userProvider;

  @Autowired
  ConceptSetsController(
      WorkspaceService workspaceService,
      ConceptSetService conceptSetService,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider) {
    this.workspaceService = workspaceService;
    this.conceptSetService = conceptSetService;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<ConceptSet> createConceptSet(
      String workspaceNamespace, String workspaceId, CreateConceptSetRequest request) {
    // Fail fast if request is not valid
    validateCreateConceptSetRequest(request);
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    ConceptSet conceptSet =
        conceptSetService.save(request, userProvider.get(), workspace.getWorkspaceId());
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), userProvider.get().getUserId(), conceptSet.getId());
    return ResponseEntity.ok(conceptSetService.toHydratedConcepts(conceptSet));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    conceptSetService.delete(conceptSetId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ConceptSet> getConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        conceptSetService.toHydratedConcepts(conceptSetService.findOne(conceptSetId)));
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<ConceptSet> conceptSets =
        conceptSetService.findByWorkspaceId(workspace.getWorkspaceId()).stream()
            .sorted(Comparator.comparing(ConceptSet::getName))
            .collect(Collectors.toList());
    return ResponseEntity.ok(new ConceptSetListResponse().items(conceptSets));
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getSurveyConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId, String surveyName) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    short surveyId = DbStorageEnums.surveysToStorage(Surveys.fromValue(surveyName.toUpperCase()));
    List<ConceptSet> conceptSets =
        conceptSetService.findByWorkspaceIdAndSurvey(workspace.getWorkspaceId(), surveyId).stream()
            .sorted(Comparator.comparing(ConceptSet::getName))
            .collect(Collectors.toList());
    return ResponseEntity.ok(new ConceptSetListResponse().items(conceptSets));
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId, ConceptSet conceptSet) {
    // Fail fast if etag isn't provided
    validateUpdateConceptSet(conceptSet);
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        conceptSetService.toHydratedConcepts(
            conceptSetService.updateConceptSet(conceptSetId, conceptSet)));
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSetConcepts(
      String workspaceNamespace,
      String workspaceId,
      Long conceptSetId,
      UpdateConceptSetRequest request) {
    // Fail fast if request isn't valid
    validateUpdateConceptSetConcepts(request);

    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        conceptSetService.toHydratedConcepts(
            conceptSetService.updateConceptSetConcepts(conceptSetId, request)));
  }

  @Override
  public ResponseEntity<ConceptSet> copyConceptSet(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromConceptSetId,
      CopyRequest copyRequest) {
    DbWorkspace fromWorkspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER);
    DbWorkspace toWorkspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            copyRequest.getToWorkspaceNamespace(),
            copyRequest.getToWorkspaceName(),
            WorkspaceAccessLevel.WRITER);
    if (toWorkspace.getCdrVersion().getCdrVersionId()
        != fromWorkspace.getCdrVersion().getCdrVersionId()) {
      throw new BadRequestException(
          "Target workspace does not have the same CDR version as current workspace");
    }

    ConceptSet conceptSet =
        conceptSetService.copyAndSave(
            Long.valueOf(fromConceptSetId),
            copyRequest.getNewName(),
            userProvider.get(),
            toWorkspace.getWorkspaceId());
    userRecentResourceService.updateConceptSetEntry(
        toWorkspace.getWorkspaceId(), userProvider.get().getUserId(), conceptSet.getId());
    return ResponseEntity.ok(conceptSetService.toHydratedConcepts(conceptSet));
  }

  private void validateCreateConceptSetRequest(CreateConceptSetRequest request) {
    Optional.ofNullable(request.getConceptSet().getDomain())
        .orElseThrow(() -> new BadRequestException("Domain cannot be null"));
    if (CollectionUtils.isEmpty(request.getAddedIds())) {
      throw new BadRequestException("Cannot create a concept set with no concepts");
    }
  }

  private void validateUpdateConceptSet(ConceptSet conceptSet) {
    if (Strings.isNullOrEmpty(conceptSet.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    Optional.ofNullable(conceptSet.getDomain())
        .orElseThrow(() -> new BadRequestException("Domain cannot be null"));
  }

  private void validateUpdateConceptSetConcepts(UpdateConceptSetRequest request) {
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
  }
}
