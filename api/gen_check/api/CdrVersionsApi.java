/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CdrVersionListResponse;

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

@Api(value = "CdrVersions", description = "the CdrVersions API")
public interface CdrVersionsApi {

    @ApiOperation(value = "", notes = "Returns all curated data repository (CDR) versions that the user has access to", response = CdrVersionListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cdrVersions", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A list of CDR versions.", response = CdrVersionListResponse.class) })
    
    @RequestMapping(value = "/v1/cdrVersions",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<CdrVersionListResponse> getCdrVersions();

}
