/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConfigResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

@Api(value = "Config", description = "the Config API")
public interface ConfigApi {

    @ApiOperation(value = "", notes = "Returns some server configuration data.", response = ConfigResponse.class, tags={ "config", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Configuration data", response = ConfigResponse.class) })
    
    @RequestMapping(value = "/v1/config",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ConfigResponse> getConfig();

}
