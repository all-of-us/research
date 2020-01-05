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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

@Api(value = "OfflineBilling", description = "the OfflineBilling API")
public interface OfflineBillingApi {

    @ApiOperation(value = "", notes = "Trigger a bulk transfer of ownership of billing projects associated with deleted workspaces", response = Void.class, tags={ "offlineBilling","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Billing Project Garbage Collection was successful.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/billingProjectGarbageCollection",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> billingProjectGarbageCollection();


    @ApiOperation(value = "", notes = "If the AoU Billing Project buffer is not full, refill with one or more billing projects.", response = Void.class, tags={ "offlineBilling","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "No Error", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/bufferBillingProjects",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> bufferBillingProjects();


    @ApiOperation(value = "", notes = "Find and alert users that have exceeded their free tier billing usage", response = Void.class, tags={ "offlineBilling","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "No Error", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/checkFreeTierBillingUsage",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> checkFreeTierBillingUsage();


    @ApiOperation(value = "", notes = "Find BillingProjectBufferEntries that have failed during the creation or assignment step and set their statuses to ERROR", response = Void.class, tags={ "offlineBilling","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "No Error", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/cleanBillingBuffer",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> cleanBillingBuffer();


    @ApiOperation(value = "", notes = "Fetch a BillingProjectBufferEntry that is in the CREATING state and check its status on Firecloud", response = Void.class, tags={ "offlineBilling","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "No Error", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/cron/syncBillingProjectStatus",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Void> syncBillingProjectStatus();

}
