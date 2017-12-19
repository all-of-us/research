package org.pmiops.workbench.api;

import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ModifyCohortAnnotationDefinitionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;

@RestController
public class CohortAnnotationDefinitionController implements CohortAnnotationDefinitionApiDelegate {

    private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
    private CohortDao cohortDao;
    private WorkspaceService workspaceService;

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<org.pmiops.workbench.db.model.CohortAnnotationDefinition, CohortAnnotationDefinition>
            TO_CLIENT_COHORT_ANNOTATION_DEFINITION =
            new Function<org.pmiops.workbench.db.model.CohortAnnotationDefinition, CohortAnnotationDefinition>() {
                @Override
                public CohortAnnotationDefinition apply(org.pmiops.workbench.db.model.CohortAnnotationDefinition cohortAnnotationDefinition) {
                    return new org.pmiops.workbench.model.CohortAnnotationDefinition()
                            .name(cohortAnnotationDefinition.getColumnName())
                            .cohortId(cohortAnnotationDefinition.getCohortId())
                            .annotationType(cohortAnnotationDefinition.getAnnotationType())
                            .cohortAnnotationDefinitionId(cohortAnnotationDefinition.getCohortAnnotationDefinitionId());
                }
            };

    private static final Function<CohortAnnotationDefinition, org.pmiops.workbench.db.model.CohortAnnotationDefinition>
            FROM_CLIENT_COHORT_ANNOTATION_DEFINITION =
            new Function<CohortAnnotationDefinition, org.pmiops.workbench.db.model.CohortAnnotationDefinition>() {
                @Override
                public org.pmiops.workbench.db.model.CohortAnnotationDefinition apply(CohortAnnotationDefinition cohortAnnotationDefinition) {
                    return new org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                            .cohortId(cohortAnnotationDefinition.getCohortId())
                            .columnName(cohortAnnotationDefinition.getName())
                            .annotationType(cohortAnnotationDefinition.getAnnotationType());
                }
            };

    @Autowired
    CohortAnnotationDefinitionController(CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao,
                                         CohortDao cohortDao,
                                         WorkspaceService workspaceService) {
        this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
        this.cohortDao = cohortDao;
        this.workspaceService = workspaceService;
    }

    @Override
    public ResponseEntity<CohortAnnotationDefinition> createCohortAnnotationDefinition(String workspaceNamespace,
                                                                                       String workspaceId,
                                                                                       Long cohortId,
                                                                                       CohortAnnotationDefinition request) {
        Cohort cohort = findCohort(cohortId);
        //this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId());
        request.setCohortId(cohortId);

        org.pmiops.workbench.db.model.CohortAnnotationDefinition cohortAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(FROM_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(request));

        return ResponseEntity.ok(TO_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(cohortAnnotationDefinition));
    }

    @Override
    public ResponseEntity<EmptyResponse> deleteCohortAnnotationDefinition(String workspaceNamespace,
                                                                          String workspaceId,
                                                                          Long cohortId,
                                                                          Long annotationDefinitionId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new EmptyResponse());
    }

    @Override
    public ResponseEntity<CohortAnnotationDefinitionListResponse> getCohortAnnotationDefinitions(String workspaceNamespace,
                                                                                                 String workspaceId,
                                                                                                 Long cohortId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortAnnotationDefinitionListResponse());
    }

    @Override
    public ResponseEntity<CohortAnnotationDefinition>
    updateCohortAnnotationDefinition(String workspaceNamespace,
                                     String workspaceId,
                                     Long cohortId,
                                     Long annotationDefinitionId,
                                     ModifyCohortAnnotationDefinitionRequest modifyCohortAnnotationDefinitionRequest) {
        Cohort cohort = findCohort(cohortId);
        //this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId());

        org.pmiops.workbench.db.model.CohortAnnotationDefinition cohortAnnotationDefinition =
                cohortAnnotationDefinitionDao.findByCohortIdAndAndCohortAnnotationDefinitionId(cohortId, annotationDefinitionId);

        if (cohortAnnotationDefinition == null) {
            throw new NotFoundException(
                    String.format("Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: %s",
                            annotationDefinitionId));
        }

        cohortAnnotationDefinition.columnName(modifyCohortAnnotationDefinitionRequest.getColumnName());

        cohortAnnotationDefinition =
                cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        return ResponseEntity.ok(TO_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(cohortAnnotationDefinition));
    }

    private Cohort findCohort(long cohortId) {
        Cohort cohort = cohortDao.findOne(cohortId);
        if (cohort == null) {
            throw new BadRequestException(
                    String.format("Invalid Request: No Cohort exists for cohortId: %s", cohortId));
        }
        return cohort;
    }

    private void validateMatchingWorkspace(String workspaceNamespace, String workspaceName, long workspaceId) {
        Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceName);
        if (workspace.getWorkspaceId() != workspaceId) {
            throw new NotFoundException(
                    String.format("Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                            workspaceNamespace, workspaceName));
        }
    }
}
