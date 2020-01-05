/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaMenuOptionsListResponse;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

@Api(value = "CohortBuilder", description = "the CohortBuilder API")
public interface CohortBuilderApi {

    @ApiOperation(value = "", notes = "Searches for participants based on criteria, criteria specific parameters, and modifiers.", response = Long.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A count of subjects", response = Long.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/search",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Long> countParticipants(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "object of parameters by which to perform the search" ,required=true )  @Valid @RequestBody SearchRequest request);


    @ApiOperation(value = "", notes = "Returns a collection of criteria per search term ", response = CriteriaListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/{domain}/search/term",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaListResponse> findCriteriaByDomainAndSearchTerm(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "the specific type of domain",required=true ) @PathVariable("domain") String domain, @NotNull@ApiParam(value = "the term to search for", required = true) @RequestParam(value = "term", required = true) String term,@ApiParam(value = "number of criteria matches to return") @RequestParam(value = "limit", required = false) Integer limit);


    @ApiOperation(value = "", notes = "Returns criteria menu options ", response = CriteriaMenuOptionsListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria menu options", response = CriteriaMenuOptionsListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/menu",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaMenuOptionsListResponse> findCriteriaMenuOptions(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId);


    @ApiOperation(value = "", notes = "Returns criteria tree with the specified name", response = CriteriaAttributeListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaAttributeListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/attribute/{conceptId}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaAttributeListResponse> getCriteriaAttributeByConceptId(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "conceptId of brand",required=true ) @PathVariable("conceptId") Long conceptId);


    @ApiOperation(value = "", notes = "Returns matches on criteria table by code or name", response = CriteriaListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/{domain}/search",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaListResponse> getCriteriaAutoComplete(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "the specific domain of criteria to get",required=true ) @PathVariable("domain") String domain, @NotNull@ApiParam(value = "the term to search for", required = true) @RequestParam(value = "term", required = true) String term,@ApiParam(value = "the type of the criteria were search for") @RequestParam(value = "type", required = false) String type,@ApiParam(value = "the type of the criteria were search for", defaultValue = "false") @RequestParam(value = "standard", required = false, defaultValue="false") Boolean standard,@ApiParam(value = "number of criteria matches to return") @RequestParam(value = "limit", required = false) Integer limit);


    @ApiOperation(value = "", notes = "Returns a collection of criteria by optional query parameters of the following: type, subtype, parentId and/or allChildren ", response = CriteriaListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/{domain}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaListResponse> getCriteriaBy(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "the specific domain of criteria to get",required=true ) @PathVariable("domain") String domain,@ApiParam(value = "the specific type of criteria to get") @RequestParam(value = "type", required = false) String type,@ApiParam(value = "reveals if source or standard", defaultValue = "false") @RequestParam(value = "standard", required = false, defaultValue="false") Boolean standard,@ApiParam(value = "fetch direct children of parentId") @RequestParam(value = "parentId", required = false) Long parentId);


    @ApiOperation(value = "", notes = "Searches for demographic info about subjects.", response = DemoChartInfoListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = DemoChartInfoListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/chartinfo",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<DemoChartInfoListResponse> getDemoChartInfo(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "object of parameters by which to perform the search" ,required=true )  @Valid @RequestBody SearchRequest request);


    @ApiOperation(value = "", notes = "Returns criteria tree matching value", response = CriteriaListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/drug",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaListResponse> getDrugBrandOrIngredientByValue(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId, @NotNull@ApiParam(value = "matches name or code of drug", required = true) @RequestParam(value = "value", required = true) String value,@ApiParam(value = "number of criteria matches to return") @RequestParam(value = "limit", required = false) Integer limit);


    @ApiOperation(value = "", notes = "Returns criteria tree with the specified name", response = CriteriaListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/drug/ingredient/{conceptId}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaListResponse> getDrugIngredientByConceptId(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "conceptId of brand",required=true ) @PathVariable("conceptId") Long conceptId);


    @ApiOperation(value = "", notes = "Will return a list all values for gender, race and ethnicity.", response = ParticipantDemographics.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "An object which contains a list of all values for gender, race and ethnicity.", response = ParticipantDemographics.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/demographics",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ParticipantDemographics> getParticipantDemographics(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId);


    @ApiOperation(value = "", notes = "Returns a collection of criteria per concept id ", response = CriteriaListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cohortBuilder", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A collection of criteria", response = CriteriaListResponse.class) })
    
    @RequestMapping(value = "/v1/cohortbuilder/{cdrVersionId}/criteria/{domain}/{conceptId}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CriteriaListResponse> getStandardCriteriaByDomainAndConceptId(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,@ApiParam(value = "the specific type of domain",required=true ) @PathVariable("domain") String domain,@ApiParam(value = "the concept id to search for",required=true ) @PathVariable("conceptId") Long conceptId);

}
