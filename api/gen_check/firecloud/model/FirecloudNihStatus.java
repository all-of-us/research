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
import java.io.Serializable;

/**
 * FirecloudNihStatus
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-12-26T15:08:18.272-06:00")
public class FirecloudNihStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("linkedNihUsername")
  private String linkedNihUsername = null;

  @SerializedName("datasetPermissions")
  private List<Object> datasetPermissions = null;

  @SerializedName("linkExpireTime")
  private Long linkExpireTime = 0l;

  public FirecloudNihStatus linkedNihUsername(String linkedNihUsername) {
    this.linkedNihUsername = linkedNihUsername;
    return this;
  }

   /**
   * The user&#39;s NIH username
   * @return linkedNihUsername
  **/
  @ApiModelProperty(required = true, value = "The user's NIH username")
  public String getLinkedNihUsername() {
    return linkedNihUsername;
  }

  public void setLinkedNihUsername(String linkedNihUsername) {
    this.linkedNihUsername = linkedNihUsername;
  }

  public FirecloudNihStatus datasetPermissions(List<Object> datasetPermissions) {
    this.datasetPermissions = datasetPermissions;
    return this;
  }

  public FirecloudNihStatus addDatasetPermissionsItem(Object datasetPermissionsItem) {
    if (this.datasetPermissions == null) {
      this.datasetPermissions = new ArrayList<Object>();
    }
    this.datasetPermissions.add(datasetPermissionsItem);
    return this;
  }

   /**
   * Array of FireCloud dataset permissions
   * @return datasetPermissions
  **/
  @ApiModelProperty(value = "Array of FireCloud dataset permissions")
  public List<Object> getDatasetPermissions() {
    return datasetPermissions;
  }

  public void setDatasetPermissions(List<Object> datasetPermissions) {
    this.datasetPermissions = datasetPermissions;
  }

  public FirecloudNihStatus linkExpireTime(Long linkExpireTime) {
    this.linkExpireTime = linkExpireTime;
    return this;
  }

   /**
   * The FireCloud-calculated expiration time, in Epoch seconds
   * @return linkExpireTime
  **/
  @ApiModelProperty(value = "The FireCloud-calculated expiration time, in Epoch seconds")
  public Long getLinkExpireTime() {
    return linkExpireTime;
  }

  public void setLinkExpireTime(Long linkExpireTime) {
    this.linkExpireTime = linkExpireTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudNihStatus nihStatus = (FirecloudNihStatus) o;
    return Objects.equals(this.linkedNihUsername, nihStatus.linkedNihUsername) &&
        Objects.equals(this.datasetPermissions, nihStatus.datasetPermissions) &&
        Objects.equals(this.linkExpireTime, nihStatus.linkExpireTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(linkedNihUsername, datasetPermissions, linkExpireTime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudNihStatus {\n");
    
    sb.append("    linkedNihUsername: ").append(toIndentedString(linkedNihUsername)).append("\n");
    sb.append("    datasetPermissions: ").append(toIndentedString(datasetPermissions)).append("\n");
    sb.append("    linkExpireTime: ").append(toIndentedString(linkExpireTime)).append("\n");
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

