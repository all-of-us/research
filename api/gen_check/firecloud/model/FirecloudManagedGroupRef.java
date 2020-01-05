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
 * a reference to a group that can be managed by users
 */
@ApiModel(description = "a reference to a group that can be managed by users")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T12:07:34.443-05:00")
public class FirecloudManagedGroupRef implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("membersGroupName")
  private String membersGroupName = null;

  public FirecloudManagedGroupRef membersGroupName(String membersGroupName) {
    this.membersGroupName = membersGroupName;
    return this;
  }

   /**
   * Get membersGroupName
   * @return membersGroupName
  **/
  @ApiModelProperty(required = true, value = "")
  public String getMembersGroupName() {
    return membersGroupName;
  }

  public void setMembersGroupName(String membersGroupName) {
    this.membersGroupName = membersGroupName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudManagedGroupRef managedGroupRef = (FirecloudManagedGroupRef) o;
    return Objects.equals(this.membersGroupName, managedGroupRef.membersGroupName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(membersGroupName);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudManagedGroupRef {\n");
    
    sb.append("    membersGroupName: ").append(toIndentedString(membersGroupName)).append("\n");
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

