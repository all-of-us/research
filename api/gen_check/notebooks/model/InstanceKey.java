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
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-12-26T15:08:18.819-06:00")
public class InstanceKey implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("project")
  private String project = null;

  @SerializedName("zone")
  private String zone = null;

  @SerializedName("name")
  private String name = null;

  public InstanceKey project(String project) {
    this.project = project;
    return this;
  }

   /**
   * The Google Project the instance belongs to
   * @return project
  **/
  @ApiModelProperty(required = true, value = "The Google Project the instance belongs to")
  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public InstanceKey zone(String zone) {
    this.zone = zone;
    return this;
  }

   /**
   * The Google zone the instance belongs to
   * @return zone
  **/
  @ApiModelProperty(required = true, value = "The Google zone the instance belongs to")
  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public InstanceKey name(String name) {
    this.name = name;
    return this;
  }

   /**
   * The name of the instance
   * @return name
  **/
  @ApiModelProperty(required = true, value = "The name of the instance")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstanceKey instanceKey = (InstanceKey) o;
    return Objects.equals(this.project, instanceKey.project) &&
        Objects.equals(this.zone, instanceKey.zone) &&
        Objects.equals(this.name, instanceKey.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(project, zone, name);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InstanceKey {\n");
    
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    zone: ").append(toIndentedString(zone)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

