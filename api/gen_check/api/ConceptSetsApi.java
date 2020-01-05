/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateConceptSetRequest;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

@Api(value = "ConceptSets", description = "the ConceptSets API")
public interface ConceptSetsApi {

    @ApiOperation(value = "", notes = "Copy specified concept set in path to specified workspace in body", response = ConceptSet.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful copy", response = ConceptSet.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets/{conceptSetId}/copy",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<ConceptSet> copyConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "",required=true ) @PathVariable("conceptSetId") String conceptSetId,@ApiParam(value = "" ,required=true )  @Valid @RequestBody CopyRequest copyConceptSetRequest);


    @ApiOperation(value = "", notes = "Creates a concept set in a workspace.", response = ConceptSet.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The concept set that was created.", response = ConceptSet.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<ConceptSet> createConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "create concept set request"  )  @Valid @RequestBody CreateConceptSetRequest request);


    @ApiOperation(value = "", notes = "Deletes the concept set with the specified ID", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 202, message = "ACCEPTED", response = EmptyResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets/{conceptSetId}",
        produces = { "application/json" }, 
        method = RequestMethod.DELETE)
    ResponseEntity<EmptyResponse> deleteConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId);


    @ApiOperation(value = "", notes = "Returns the concept set definition with the specified ID", response = ConceptSet.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A concept set definition", response = ConceptSet.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets/{conceptSetId}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ConceptSet> getConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId);


    @ApiOperation(value = "", notes = "Returns all concept sets in a workspace", response = ConceptSetListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A list of concept sets.", response = ConceptSetListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId);


    @ApiOperation(value = "", notes = "Returns all survey concept sets in a workspace", response = ConceptSetListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A list of concept sets of type surveys.", response = ConceptSetListResponse.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/survey-concept-sets/{surveyName}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ConceptSetListResponse> getSurveyConceptSetsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "",required=true ) @PathVariable("surveyName") String surveyName);


    @ApiOperation(value = "", notes = "Modifies the name or description of the concept set with the specified ID; fields that are omitted will not be modified ", response = ConceptSet.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The updated concept set.", response = ConceptSet.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets/{conceptSetId}",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.PATCH)
    ResponseEntity<ConceptSet> updateConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId,@ApiParam(value = "concept set definition"  )  @Valid @RequestBody ConceptSet conceptSet);


    @ApiOperation(value = "", notes = "Adds or removes concepts from the concept set. ", response = ConceptSet.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "conceptSets", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The updated concept set.", response = ConceptSet.class) })
    
    @RequestMapping(value = "/v1/workspaces/{workspaceNamespace}/{workspaceId}/concept-sets/{conceptSetId}/concepts",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<ConceptSet> updateConceptSetConcepts(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,@ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,@ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId,@ApiParam(value = "update concept set request"  )  @Valid @RequestBody UpdateConceptSetRequest request);

}
