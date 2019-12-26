/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.AuditBigQueryResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

@Api(value = "OfflineAudit", description = "the OfflineAudit API")
public interface OfflineAuditApi {

    @ApiOperation(value = "", notes = "Endpoint meant to be called offline to trigger BigQuery auditing; may be slow to execute. Only executable via App Engine cronjob. ", response = AuditBigQueryResponse.class, tags={ "offlineAudit","cron", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Audit was successful.", response = AuditBigQueryResponse.class) })
    
    @RequestMapping(value = "/v1/cron/auditBigQuery",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<AuditBigQueryResponse> auditBigQuery();

}
