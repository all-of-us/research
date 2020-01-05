/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ErrorResponse;

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

@Api(value = "OfflineUser", description = "the OfflineUser API")
public interface OfflineUserApi {

    @ApiOperation(value = "", notes = "Audits project access for all users", response = Void.class, tags={ "offlineUser","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "All users' project access were audited.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/bulkAuditProjectAccess",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> bulkAuditProjectAccess();


    @ApiOperation(value = "", notes = "sync moodle badge/training status for all users.", response = Void.class, tags={ "offlineUser","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "The user table is updated with training status.", response = Void.class),
        @ApiResponse(code = 404, message = "User not found while retrieving  badge.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/bulkSyncComplianceTrainingStatus",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> bulkSyncComplianceTrainingStatus();


    @ApiOperation(value = "", notes = "sync eRA Commons linkage status for all users.", response = Void.class, tags={ "offlineUser","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "All users' eRA Commons statuses were updated.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/bulkSyncEraCommonsStatus",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> bulkSyncEraCommonsStatus();


    @ApiOperation(value = "", notes = "sync 2FA status for all users", response = Void.class, tags={ "offlineUser","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "All users' 2FA statuses were updated.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/bulkSyncTwoFactorAuthStatus",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> bulkSyncTwoFactorAuthStatus();

}
