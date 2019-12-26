/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;

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

@Api(value = "Cluster", description = "the Cluster API")
public interface ClusterApi {

    @ApiOperation(value = "Delete a cluster by name.", notes = "", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cluster", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Deletion success", response = EmptyResponse.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/clusters/{clusterNamespace}/{clusterName}",
        produces = { "application/json" }, 
        method = RequestMethod.DELETE)
    ResponseEntity<EmptyResponse> deleteCluster(@ApiParam(value = "clusterNamespace",required=true ) @PathVariable("clusterNamespace") String clusterNamespace,@ApiParam(value = "clusterName",required=true ) @PathVariable("clusterName") String clusterName);


    @ApiOperation(value = "List available notebook clusters", notes = "Returns the clusters available to the current user in the given billing project. Currently there is a single default cluster supported per billing project and this cluster should always either exist or be in the process of being initialized. In a future where researchers have more control over cluster creation, this endpoint would be extended to return all clusters. ", response = ClusterListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cluster", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Available clusters", response = ClusterListResponse.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/clusters/{billingProjectId}/{workspaceFirecloudName}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<ClusterListResponse> listClusters(@ApiParam(value = "The unique identifier of the Google Billing Project containing the clusters",required=true ) @PathVariable("billingProjectId") String billingProjectId,@ApiParam(value = "The firecloudName of the workspace whose notebook we're looking at",required=true ) @PathVariable("workspaceFirecloudName") String workspaceFirecloudName);


    @ApiOperation(value = "Localize files from a workspace to notebook cluster. As a side-effect, JSON workspace environment files will also be localized to the cluster. ", notes = "Localize notebook files to the corresponding notebook cluster.", response = ClusterLocalizeResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cluster", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Success", response = ClusterLocalizeResponse.class),
        @ApiResponse(code = 404, message = "Cluster or Workspace not found", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/clusters/{clusterNamespace}/{clusterName}/localize",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<ClusterLocalizeResponse> localize(@ApiParam(value = "clusterNamespace",required=true ) @PathVariable("clusterNamespace") String clusterNamespace,@ApiParam(value = "clusterName",required=true ) @PathVariable("clusterName") String clusterName,@ApiParam(value = "Localization request."  )  @Valid @RequestBody ClusterLocalizeRequest body);


    @ApiOperation(value = "Sets default cluster creation request parameters for a user.", notes = "Clusters are created with a default machine configuration. Setting this override changes that configuration for subsequent cluster creations. This change only takes effect after a new cluster creation, e.g. due to standard cluster expiration (~2w) or via manual reset. Requires DEVELOPER authority. ", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "cluster", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "success", response = EmptyResponse.class) })
    
    @RequestMapping(value = "/v1/admin/clusters/updateConfig",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<EmptyResponse> updateClusterConfig(@ApiParam(value = "Cluster config update request."  )  @Valid @RequestBody UpdateClusterConfigRequest body);

}
