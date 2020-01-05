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
import java.time.OffsetDateTime;
import java.io.Serializable;

/**
 * Statistics about submissions in a workspace
 */
@ApiModel(description = "Statistics about submissions in a workspace")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T12:07:34.443-05:00")
public class FirecloudWorkspaceSubmissionStats implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("lastSuccessDate")
  private OffsetDateTime lastSuccessDate = null;

  @SerializedName("lastFailureDate")
  private OffsetDateTime lastFailureDate = null;

  @SerializedName("runningSubmissionsCount")
  private Integer runningSubmissionsCount = null;

  public FirecloudWorkspaceSubmissionStats lastSuccessDate(OffsetDateTime lastSuccessDate) {
    this.lastSuccessDate = lastSuccessDate;
    return this;
  }

   /**
   * The date of the last successful submission
   * @return lastSuccessDate
  **/
  @ApiModelProperty(value = "The date of the last successful submission")
  public OffsetDateTime getLastSuccessDate() {
    return lastSuccessDate;
  }

  public void setLastSuccessDate(OffsetDateTime lastSuccessDate) {
    this.lastSuccessDate = lastSuccessDate;
  }

  public FirecloudWorkspaceSubmissionStats lastFailureDate(OffsetDateTime lastFailureDate) {
    this.lastFailureDate = lastFailureDate;
    return this;
  }

   /**
   * The date of the last failed submission
   * @return lastFailureDate
  **/
  @ApiModelProperty(value = "The date of the last failed submission")
  public OffsetDateTime getLastFailureDate() {
    return lastFailureDate;
  }

  public void setLastFailureDate(OffsetDateTime lastFailureDate) {
    this.lastFailureDate = lastFailureDate;
  }

  public FirecloudWorkspaceSubmissionStats runningSubmissionsCount(Integer runningSubmissionsCount) {
    this.runningSubmissionsCount = runningSubmissionsCount;
    return this;
  }

   /**
   * Count of all the running submissions
   * @return runningSubmissionsCount
  **/
  @ApiModelProperty(required = true, value = "Count of all the running submissions")
  public Integer getRunningSubmissionsCount() {
    return runningSubmissionsCount;
  }

  public void setRunningSubmissionsCount(Integer runningSubmissionsCount) {
    this.runningSubmissionsCount = runningSubmissionsCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudWorkspaceSubmissionStats workspaceSubmissionStats = (FirecloudWorkspaceSubmissionStats) o;
    return Objects.equals(this.lastSuccessDate, workspaceSubmissionStats.lastSuccessDate) &&
        Objects.equals(this.lastFailureDate, workspaceSubmissionStats.lastFailureDate) &&
        Objects.equals(this.runningSubmissionsCount, workspaceSubmissionStats.runningSubmissionsCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastSuccessDate, lastFailureDate, runningSubmissionsCount);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudWorkspaceSubmissionStats {\n");
    
    sb.append("    lastSuccessDate: ").append(toIndentedString(lastSuccessDate)).append("\n");
    sb.append("    lastFailureDate: ").append(toIndentedString(lastFailureDate)).append("\n");
    sb.append("    runningSubmissionsCount: ").append(toIndentedString(runningSubmissionsCount)).append("\n");
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

