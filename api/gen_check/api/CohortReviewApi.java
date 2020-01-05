/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortReviewListResponse;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.VocabularyListResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

@Api(value = "CohortReview", description = "the CohortReview API")
public interface CohortReviewApi {

    @ApiOperation(value = "", notes = "This endpoint will create an cohort review which is a participant cohort sample specified by the review size parameter. ", response = CohortReview.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A cohortReviewId and cohort count", response = CohortReview.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortId}/{cdrVersionId}",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<CohortReview> createCohortReview(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "cohort review creation request body" ,required=true )  @Valid @RequestBody CreateReviewRequest request);


    @ApiOperation(value = "", notes = "This endpoint will create a ParticipantCohortAnnotation.", response = ParticipantCohortAnnotation.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A ParticipantCohortAnnotation.", response = ParticipantCohortAnnotation.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}/annotations",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,@ApiParam(value = "ParticipantCohortAnnotation creation request body" ,required=true )  @Valid @RequestBody ParticipantCohortAnnotation request);


    @ApiOperation(value = "", notes = "Deletes the cohort review with the specified ID", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 202, message = "ACCEPTED", response = EmptyResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohort-reviews/{cohortReviewId}",
        produces = { "application/json" }, 
        method = RequestMethod.DELETE)
    ResponseEntity<EmptyResponse> deleteCohortReview(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "specifies which cohort review",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId);


    @ApiOperation(value = "", notes = "Deletes the ParticipantCohortAnnotation with the specified ID", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 202, message = "ParticipantCohortAnnotation deletion request accepted", response = EmptyResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}/annotations/{annotationId}",
        produces = { "application/json" }, 
        method = RequestMethod.DELETE)
    ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,@ApiParam(value = "specifies which annotation",required=true ) @PathVariable("annotationId") Long annotationId);


    @ApiOperation(value = "", notes = "Returns a collection of CohortChartData for UI charting in cohort review.", response = CohortChartDataListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of CohortChartData", response = CohortChartDataListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/charts/{domain}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CohortChartDataListResponse> getCohortChartData(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which domain the CohortChartData should belong to.",required=true ) @PathVariable("domain") String domain,@ApiParam(value = "the limit search results to") @RequestParam(value = "limit", required = false) Integer limit);


    @ApiOperation(value = "", notes = "Returns all cohort reviews in a workspace", response = CohortReviewListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A list of cohort definitions.", response = CohortReviewListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohort-reviews",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CohortReviewListResponse> getCohortReviewsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId);


    @ApiOperation(value = "", notes = "This endpoint will return a ParticipantCohortStatus", response = ParticipantChartDataListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The Participant Chart data", response = ParticipantChartDataListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}/charts/{domain}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ParticipantChartDataListResponse> getParticipantChartData(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,@ApiParam(value = "specifies which domain the chart data should belong to.",required=true ) @PathVariable("domain") String domain,@ApiParam(value = "the limit search results to") @RequestParam(value = "limit", required = false) Integer limit);


    @ApiOperation(value = "", notes = "This endpoint will get a collection of ParticipantCohortAnnotations.", response = ParticipantCohortAnnotationListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of ParticipantCohortAnnotation.", response = ParticipantCohortAnnotationListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}/annotations",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId);


    @ApiOperation(value = "", notes = "This endpoint will return a ParticipantCohortStatus", response = ParticipantCohortStatus.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The ParticipantCohortStatus definition", response = ParticipantCohortStatus.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ParticipantCohortStatus> getParticipantCohortStatus(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId);


    @ApiOperation(value = "", notes = "Returns a collection of participants for the specified cohortId and cdrVersionId. This endpoint does pagination based on page, limit, order and column. ", response = CohortReview.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of participants", response = CohortReview.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortId}/{cdrVersionId}/participants",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<CohortReview> getParticipantCohortStatuses(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "request body for getting list of ParticipantCohortStatuses." ,required=true )  @Valid @RequestBody PageFilterRequest request);


    @ApiOperation(value = "", notes = "Returns a collection of participant data for the specified params based off the PageFilterRequest. This endpoint does pagination based on page, limit, order and column. ", response = ParticipantDataListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of ParticipantConditions.", response = ParticipantDataListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}/data",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<ParticipantDataListResponse> getParticipantData(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,@ApiParam(value = "request body for getting list of participant data." ,required=true )  @Valid @RequestBody PageFilterRequest request);


    @ApiOperation(value = "", notes = "This endpoint will get a collection of OMOP vocabularies per cdrVersionId.", response = VocabularyListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of OMOP vocabularies.", response = VocabularyListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/vocabularies",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<VocabularyListResponse> getVocabularies(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId);


    @ApiOperation(value = "", notes = "Modifies the cohort review with the specified ID; fields that are omitted will not be modified ", response = CohortReview.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The updated cohort review", response = CohortReview.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohort-reviews/{cohortReviewId}",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.PATCH)
    ResponseEntity<CohortReview> updateCohortReview(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "specifies which cohort review",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "cohort review"  )  @Valid @RequestBody CohortReview cohortReview);


    @ApiOperation(value = "", notes = "This endpoint will modify a ParticipantCohortAnnotation.", response = ParticipantCohortAnnotation.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A ParticipantCohortAnnotation.", response = ParticipantCohortAnnotation.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}/annotations/{annotationId}",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.PUT)
    ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,@ApiParam(value = "specifies which annotation",required=true ) @PathVariable("annotationId") Long annotationId,@ApiParam(value = "ParticipantCohortAnnotation modification request body" ,required=true )  @Valid @RequestBody ModifyParticipantCohortAnnotationRequest request);


    @ApiOperation(value = "", notes = "Modifies the ParticipantCohortStatus status", response = ParticipantCohortStatus.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortReview", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The updated ParticipantCohortStatus definition", response = ParticipantCohortStatus.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/review/{cohortReviewId}/participants/{participantId}",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.PUT)
    ResponseEntity<ParticipantCohortStatus> updateParticipantCohortStatus(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,@ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,@ApiParam(value = "Contains the new review status"  )  @Valid @RequestBody ModifyCohortStatusRequest cohortStatusRequest);

}
