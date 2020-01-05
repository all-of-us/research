/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

@Api(value = "AuthDomain", description = "the AuthDomain API")
public interface AuthDomainApi {

    @ApiOperation(value = "", notes = "This endpoint will create the registered auth domain.", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "authDomain", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully created group", response = EmptyResponse.class) })
    
    @RequestMapping(value = "/v1/auth-domain/{groupName}",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<EmptyResponse> createAuthDomain(@ApiParam(value = "groupName",required=true ) @PathVariable("groupName") String groupName);


    @ApiOperation(value = "enable/disable a user to an auth domain if you have ID verification authority", notes = "", response = Void.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "authDomain", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Successfully Updated User In Group", response = Void.class),
        @ApiResponse(code = 403, message = "You must be an admin of this group in order to enable/disable members", response = ErrorResponse.class),
        @ApiResponse(code = 404, message = "User not found", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "FireCloud Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/auth-domain/users",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Void> updateUserDisabledStatus(@ApiParam(value = "Request containing user email to update and a disabled status to update the user to."  )  @Valid @RequestBody UpdateUserDisabledRequest request);

}
