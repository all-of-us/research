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
import org.pmiops.workbench.notebooks.model.UpdateMachineConfig;
import java.io.Serializable;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T12:07:34.879-05:00")
public class UpdateClusterRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("machineConfig")
  private UpdateMachineConfig machineConfig = null;

  @SerializedName("autopause")
  private Boolean autopause = null;

  @SerializedName("autopauseThreshold")
  private Integer autopauseThreshold = null;

  public UpdateClusterRequest machineConfig(UpdateMachineConfig machineConfig) {
    this.machineConfig = machineConfig;
    return this;
  }

   /**
   * The machine configurations for the master and worker nodes
   * @return machineConfig
  **/
  @ApiModelProperty(value = "The machine configurations for the master and worker nodes")
  public UpdateMachineConfig getMachineConfig() {
    return machineConfig;
  }

  public void setMachineConfig(UpdateMachineConfig machineConfig) {
    this.machineConfig = machineConfig;
  }

  public UpdateClusterRequest autopause(Boolean autopause) {
    this.autopause = autopause;
    return this;
  }

   /**
   * Whether autopause feature is enabled for this specific cluster. If unset, autopause will be enabled and a system default threshold will be used.
   * @return autopause
  **/
  @ApiModelProperty(value = "Whether autopause feature is enabled for this specific cluster. If unset, autopause will be enabled and a system default threshold will be used.")
  public Boolean getAutopause() {
    return autopause;
  }

  public void setAutopause(Boolean autopause) {
    this.autopause = autopause;
  }

  public UpdateClusterRequest autopauseThreshold(Integer autopauseThreshold) {
    this.autopauseThreshold = autopauseThreshold;
    return this;
  }

   /**
   * The number of minutes of idle time to elapse before the cluster is autopaused. If autopause is set to false, this value is disregarded. A value of 0 is equivalent to autopause being turned off. If autopause is enabled and this is unset, a system default threshold will be used.
   * @return autopauseThreshold
  **/
  @ApiModelProperty(value = "The number of minutes of idle time to elapse before the cluster is autopaused. If autopause is set to false, this value is disregarded. A value of 0 is equivalent to autopause being turned off. If autopause is enabled and this is unset, a system default threshold will be used.")
  public Integer getAutopauseThreshold() {
    return autopauseThreshold;
  }

  public void setAutopauseThreshold(Integer autopauseThreshold) {
    this.autopauseThreshold = autopauseThreshold;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateClusterRequest updateClusterRequest = (UpdateClusterRequest) o;
    return Objects.equals(this.machineConfig, updateClusterRequest.machineConfig) &&
        Objects.equals(this.autopause, updateClusterRequest.autopause) &&
        Objects.equals(this.autopauseThreshold, updateClusterRequest.autopauseThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(machineConfig, autopause, autopauseThreshold);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateClusterRequest {\n");
    
    sb.append("    machineConfig: ").append(toIndentedString(machineConfig)).append("\n");
    sb.append("    autopause: ").append(toIndentedString(autopause)).append("\n");
    sb.append("    autopauseThreshold: ").append(toIndentedString(autopauseThreshold)).append("\n");
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

