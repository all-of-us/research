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
 * report status of systems Orchestration calls out to
 */
@ApiModel(description = "report status of systems Orchestration calls out to")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:48:20.657-05:00")
public class FirecloudSystemStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("ok")
  private Boolean ok = null;

  @SerializedName("systems")
  private Object systems = null;

  public FirecloudSystemStatus ok(Boolean ok) {
    this.ok = ok;
    return this;
  }

   /**
   * whether any system(s) need attention
   * @return ok
  **/
  @ApiModelProperty(example = "false", required = true, value = "whether any system(s) need attention")
  public Boolean getOk() {
    return ok;
  }

  public void setOk(Boolean ok) {
    this.ok = ok;
  }

  public FirecloudSystemStatus systems(Object systems) {
    this.systems = systems;
    return this;
  }

   /**
   * Map[String, SubsystemStatus]
   * @return systems
  **/
  @ApiModelProperty(example = "{\"Agora\":{\"ok\":true},\"Google\":{\"ok\":true},\"Consent\":{\"ok\":false,\"messages\":[\"ClusterHealth is RED\"]},\"Rawls\":{\"ok\":true},\"Ontology\":{\"ok\":true},\"Search\":{\"ok\":true},\"Thurloe\":{\"ok\":false,\"messages\":[\"Thurloe misbehavior message\"]}}", required = true, value = "Map[String, SubsystemStatus]")
  public Object getSystems() {
    return systems;
  }

  public void setSystems(Object systems) {
    this.systems = systems;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudSystemStatus systemStatus = (FirecloudSystemStatus) o;
    return Objects.equals(this.ok, systemStatus.ok) &&
        Objects.equals(this.systems, systemStatus.systems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ok, systems);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudSystemStatus {\n");
    
    sb.append("    ok: ").append(toIndentedString(ok)).append("\n");
    sb.append("    systems: ").append(toIndentedString(systems)).append("\n");
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

