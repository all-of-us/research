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
 * a billing project status
 */
@ApiModel(description = "a billing project status")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:49:27.584-05:00")
public class FirecloudBillingProjectStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("projectName")
  private String projectName = null;

  /**
   * Gets or Sets creationStatus
   */
  @JsonAdapter(CreationStatusEnum.Adapter.class)
  public enum CreationStatusEnum {
    CREATING("Creating"),
    
    ADDINGTOPERIMETER("AddingToPerimeter"),
    
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

  public FirecloudBillingProjectStatus projectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

   /**
   * the name of the billing project
   * @return projectName
  **/
  @ApiModelProperty(required = true, value = "the name of the billing project")
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public FirecloudBillingProjectStatus creationStatus(CreationStatusEnum creationStatus) {
    this.creationStatus = creationStatus;
    return this;
  }

   /**
   * Get creationStatus
   * @return creationStatus
  **/
  @ApiModelProperty(required = true, value = "")
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
    FirecloudBillingProjectStatus billingProjectStatus = (FirecloudBillingProjectStatus) o;
    return Objects.equals(this.projectName, billingProjectStatus.projectName) &&
        Objects.equals(this.creationStatus, billingProjectStatus.creationStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectName, creationStatus);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudBillingProjectStatus {\n");
    
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
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

