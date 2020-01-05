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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:10:52.701-05:00")
public class FirecloudBillingProjectMembership implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("projectName")
  private String projectName = null;

  @SerializedName("role")
  private String role = null;

  /**
   * Gets or Sets creationStatus
   */
  @JsonAdapter(CreationStatusEnum.Adapter.class)
  public enum CreationStatusEnum {
    CREATING("Creating"),
    
    READY("Ready"),
    
    ERROR("Error");

    private String value;

    CreationStatusEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static CreationStatusEnum fromValue(String text) {
      for (CreationStatusEnum b : CreationStatusEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<CreationStatusEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final CreationStatusEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public CreationStatusEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return CreationStatusEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("creationStatus")
  private CreationStatusEnum creationStatus = null;

  public FirecloudBillingProjectMembership projectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

   /**
   * the name of the project to create
   * @return projectName
  **/
  @ApiModelProperty(required = true, value = "the name of the project to create")
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public FirecloudBillingProjectMembership role(String role) {
    this.role = role;
    return this;
  }

   /**
   * the role of the current user in the project
   * @return role
  **/
  @ApiModelProperty(required = true, value = "the role of the current user in the project")
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public FirecloudBillingProjectMembership creationStatus(CreationStatusEnum creationStatus) {
    this.creationStatus = creationStatus;
    return this;
  }

   /**
   * Get creationStatus
   * @return creationStatus
  **/
  @ApiModelProperty(value = "")
  public CreationStatusEnum getCreationStatus() {
    return creationStatus;
  }

  public void setCreationStatus(CreationStatusEnum creationStatus) {
    this.creationStatus = creationStatus;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudBillingProjectMembership billingProjectMembership = (FirecloudBillingProjectMembership) o;
    return Objects.equals(this.projectName, billingProjectMembership.projectName) &&
        Objects.equals(this.role, billingProjectMembership.role) &&
        Objects.equals(this.creationStatus, billingProjectMembership.creationStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectName, role, creationStatus);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudBillingProjectMembership {\n");
    
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    creationStatus: ").append(toIndentedString(creationStatus)).append("\n");
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

