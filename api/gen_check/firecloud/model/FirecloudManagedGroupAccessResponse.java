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
 * an element of a list of groups a user has access to
 */
@ApiModel(description = "an element of a list of groups a user has access to")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:10:52.701-05:00")
public class FirecloudManagedGroupAccessResponse implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("groupName")
  private String groupName = null;

  @SerializedName("groupEmail")
  private String groupEmail = null;

  @SerializedName("role")
  private String role = null;

  public FirecloudManagedGroupAccessResponse groupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

   /**
   * Get groupName
   * @return groupName
  **/
  @ApiModelProperty(required = true, value = "")
  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public FirecloudManagedGroupAccessResponse groupEmail(String groupEmail) {
    this.groupEmail = groupEmail;
    return this;
  }

   /**
   * Get groupEmail
   * @return groupEmail
  **/
  @ApiModelProperty(required = true, value = "")
  public String getGroupEmail() {
    return groupEmail;
  }

  public void setGroupEmail(String groupEmail) {
    this.groupEmail = groupEmail;
  }

  public FirecloudManagedGroupAccessResponse role(String role) {
    this.role = role;
    return this;
  }

   /**
   * Get role
   * @return role
  **/
  @ApiModelProperty(required = true, value = "")
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudManagedGroupAccessResponse managedGroupAccessResponse = (FirecloudManagedGroupAccessResponse) o;
    return Objects.equals(this.groupName, managedGroupAccessResponse.groupName) &&
        Objects.equals(this.groupEmail, managedGroupAccessResponse.groupEmail) &&
        Objects.equals(this.role, managedGroupAccessResponse.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupName, groupEmail, role);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudManagedGroupAccessResponse {\n");
    
    sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
    sb.append("    groupEmail: ").append(toIndentedString(groupEmail)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
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

