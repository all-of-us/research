/*
 * FireCloud
 * Genome analysis execution service. 
 *
 * OpenAPI spec version: 0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.pmiops.workbench.firecloud.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceSubmissionStats;
import java.io.Serializable;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T12:00:56.068-05:00")
public class FirecloudWorkspaceResponse implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("accessLevel")
  private String accessLevel = null;

  @SerializedName("canShare")
  private Boolean canShare = null;

  @SerializedName("catalog")
  private Boolean catalog = null;

  @SerializedName("workspace")
  private FirecloudWorkspace workspace = null;

  @SerializedName("workspaceSubmissionStats")
  private FirecloudWorkspaceSubmissionStats workspaceSubmissionStats = null;

  @SerializedName("owners")
  private List<String> owners = null;

  public FirecloudWorkspaceResponse accessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
    return this;
  }

   /**
   * Get accessLevel
   * @return accessLevel
  **/
  @ApiModelProperty(value = "")
  public String getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
  }

  public FirecloudWorkspaceResponse canShare(Boolean canShare) {
    this.canShare = canShare;
    return this;
  }

   /**
   * Get canShare
   * @return canShare
  **/
  @ApiModelProperty(value = "")
  public Boolean getCanShare() {
    return canShare;
  }

  public void setCanShare(Boolean canShare) {
    this.canShare = canShare;
  }

  public FirecloudWorkspaceResponse catalog(Boolean catalog) {
    this.catalog = catalog;
    return this;
  }

   /**
   * Get catalog
   * @return catalog
  **/
  @ApiModelProperty(value = "")
  public Boolean getCatalog() {
    return catalog;
  }

  public void setCatalog(Boolean catalog) {
    this.catalog = catalog;
  }

  public FirecloudWorkspaceResponse workspace(FirecloudWorkspace workspace) {
    this.workspace = workspace;
    return this;
  }

   /**
   * Get workspace
   * @return workspace
  **/
  @ApiModelProperty(value = "")
  public FirecloudWorkspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(FirecloudWorkspace workspace) {
    this.workspace = workspace;
  }

  public FirecloudWorkspaceResponse workspaceSubmissionStats(FirecloudWorkspaceSubmissionStats workspaceSubmissionStats) {
    this.workspaceSubmissionStats = workspaceSubmissionStats;
    return this;
  }

   /**
   * Get workspaceSubmissionStats
   * @return workspaceSubmissionStats
  **/
  @ApiModelProperty(value = "")
  public FirecloudWorkspaceSubmissionStats getWorkspaceSubmissionStats() {
    return workspaceSubmissionStats;
  }

  public void setWorkspaceSubmissionStats(FirecloudWorkspaceSubmissionStats workspaceSubmissionStats) {
    this.workspaceSubmissionStats = workspaceSubmissionStats;
  }

  public FirecloudWorkspaceResponse owners(List<String> owners) {
    this.owners = owners;
    return this;
  }

  public FirecloudWorkspaceResponse addOwnersItem(String ownersItem) {
    if (this.owners == null) {
      this.owners = new ArrayList<String>();
    }
    this.owners.add(ownersItem);
    return this;
  }

   /**
   * Get owners
   * @return owners
  **/
  @ApiModelProperty(value = "")
  public List<String> getOwners() {
    return owners;
  }

  public void setOwners(List<String> owners) {
    this.owners = owners;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudWorkspaceResponse workspaceResponse = (FirecloudWorkspaceResponse) o;
    return Objects.equals(this.accessLevel, workspaceResponse.accessLevel) &&
        Objects.equals(this.canShare, workspaceResponse.canShare) &&
        Objects.equals(this.catalog, workspaceResponse.catalog) &&
        Objects.equals(this.workspace, workspaceResponse.workspace) &&
        Objects.equals(this.workspaceSubmissionStats, workspaceResponse.workspaceSubmissionStats) &&
        Objects.equals(this.owners, workspaceResponse.owners);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessLevel, canShare, catalog, workspace, workspaceSubmissionStats, owners);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudWorkspaceResponse {\n");
    
    sb.append("    accessLevel: ").append(toIndentedString(accessLevel)).append("\n");
    sb.append("    canShare: ").append(toIndentedString(canShare)).append("\n");
    sb.append("    catalog: ").append(toIndentedString(catalog)).append("\n");
    sb.append("    workspace: ").append(toIndentedString(workspace)).append("\n");
    sb.append("    workspaceSubmissionStats: ").append(toIndentedString(workspaceSubmissionStats)).append("\n");
    sb.append("    owners: ").append(toIndentedString(owners)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
}

