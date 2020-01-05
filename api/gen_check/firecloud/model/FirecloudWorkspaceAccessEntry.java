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
import java.io.Serializable;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:48:20.657-05:00")
public class FirecloudWorkspaceAccessEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("accessLevel")
  private String accessLevel = null;

  @SerializedName("pending")
  private Boolean pending = null;

  @SerializedName("canShare")
  private Boolean canShare = null;

  @SerializedName("canCompute")
  private Boolean canCompute = null;

  public FirecloudWorkspaceAccessEntry accessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
    return this;
  }

   /**
   * The access level granted to this user or group (OWNER, READER, WRITER, NO ACCESS)
   * @return accessLevel
  **/
  @ApiModelProperty(required = true, value = "The access level granted to this user or group (OWNER, READER, WRITER, NO ACCESS)")
  public String getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
  }

  public FirecloudWorkspaceAccessEntry pending(Boolean pending) {
    this.pending = pending;
    return this;
  }

   /**
   * The status of the users access
   * @return pending
  **/
  @ApiModelProperty(required = true, value = "The status of the users access")
  public Boolean getPending() {
    return pending;
  }

  public void setPending(Boolean pending) {
    this.pending = pending;
  }

  public FirecloudWorkspaceAccessEntry canShare(Boolean canShare) {
    this.canShare = canShare;
    return this;
  }

   /**
   * True if the user can share the workspace with others, false otherwise
   * @return canShare
  **/
  @ApiModelProperty(required = true, value = "True if the user can share the workspace with others, false otherwise")
  public Boolean getCanShare() {
    return canShare;
  }

  public void setCanShare(Boolean canShare) {
    this.canShare = canShare;
  }

  public FirecloudWorkspaceAccessEntry canCompute(Boolean canCompute) {
    this.canCompute = canCompute;
    return this;
  }

   /**
   * True if the user can launch compute in this workspace, false otherwise
   * @return canCompute
  **/
  @ApiModelProperty(required = true, value = "True if the user can launch compute in this workspace, false otherwise")
  public Boolean getCanCompute() {
    return canCompute;
  }

  public void setCanCompute(Boolean canCompute) {
    this.canCompute = canCompute;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudWorkspaceAccessEntry workspaceAccessEntry = (FirecloudWorkspaceAccessEntry) o;
    return Objects.equals(this.accessLevel, workspaceAccessEntry.accessLevel) &&
        Objects.equals(this.pending, workspaceAccessEntry.pending) &&
        Objects.equals(this.canShare, workspaceAccessEntry.canShare) &&
        Objects.equals(this.canCompute, workspaceAccessEntry.canCompute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessLevel, pending, canShare, canCompute);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudWorkspaceAccessEntry {\n");
    
    sb.append("    accessLevel: ").append(toIndentedString(accessLevel)).append("\n");
    sb.append("    pending: ").append(toIndentedString(pending)).append("\n");
    sb.append("    canShare: ").append(toIndentedString(canShare)).append("\n");
    sb.append("    canCompute: ").append(toIndentedString(canCompute)).append("\n");
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

