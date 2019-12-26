/*
 * Leonardo
 * Workbench notebooks service. 
 *
 * OpenAPI spec version: 0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.pmiops.workbench.notebooks.model;

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
 * Errors encountered on cluster create
 */
@ApiModel(description = "Errors encountered on cluster create")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-12-26T15:08:18.819-06:00")
public class ClusterError implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("errorMessage")
  private String errorMessage = null;

  @SerializedName("errorCode")
  private Integer errorCode = null;

  @SerializedName("timestamp")
  private String timestamp = null;

  public ClusterError errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

   /**
   * Error message
   * @return errorMessage
  **/
  @ApiModelProperty(value = "Error message")
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public ClusterError errorCode(Integer errorCode) {
    this.errorCode = errorCode;
    return this;
  }

   /**
   * Error code
   * @return errorCode
  **/
  @ApiModelProperty(value = "Error code")
  public Integer getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(Integer errorCode) {
    this.errorCode = errorCode;
  }

  public ClusterError timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

   /**
   * timestamp for error in ISO 8601 format
   * @return timestamp
  **/
  @ApiModelProperty(value = "timestamp for error in ISO 8601 format")
  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterError clusterError = (ClusterError) o;
    return Objects.equals(this.errorMessage, clusterError.errorMessage) &&
        Objects.equals(this.errorCode, clusterError.errorCode) &&
        Objects.equals(this.timestamp, clusterError.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, errorCode, timestamp);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterError {\n");
    
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
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

