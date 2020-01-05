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
 * FirecloudEnabled
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T12:07:34.443-05:00")
public class FirecloudEnabled implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("google")
  private Boolean google = null;

  @SerializedName("ldap")
  private Boolean ldap = null;

  @SerializedName("allUsersGroup")
  private Boolean allUsersGroup = null;

  public FirecloudEnabled google(Boolean google) {
    this.google = google;
    return this;
  }

   /**
   * User enabled via Google?
   * @return google
  **/
  @ApiModelProperty(value = "User enabled via Google?")
  public Boolean getGoogle() {
    return google;
  }

  public void setGoogle(Boolean google) {
    this.google = google;
  }

  public FirecloudEnabled ldap(Boolean ldap) {
    this.ldap = ldap;
    return this;
  }

   /**
   * User enabled in LDAP?
   * @return ldap
  **/
  @ApiModelProperty(value = "User enabled in LDAP?")
  public Boolean getLdap() {
    return ldap;
  }

  public void setLdap(Boolean ldap) {
    this.ldap = ldap;
  }

  public FirecloudEnabled allUsersGroup(Boolean allUsersGroup) {
    this.allUsersGroup = allUsersGroup;
    return this;
  }

   /**
   * User is a member of the \&quot;All Users\&quot; group?
   * @return allUsersGroup
  **/
  @ApiModelProperty(value = "User is a member of the \"All Users\" group?")
  public Boolean getAllUsersGroup() {
    return allUsersGroup;
  }

  public void setAllUsersGroup(Boolean allUsersGroup) {
    this.allUsersGroup = allUsersGroup;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudEnabled enabled = (FirecloudEnabled) o;
    return Objects.equals(this.google, enabled.google) &&
        Objects.equals(this.ldap, enabled.ldap) &&
        Objects.equals(this.allUsersGroup, enabled.allUsersGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(google, ldap, allUsersGroup);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudEnabled {\n");
    
    sb.append("    google: ").append(toIndentedString(google)).append("\n");
    sb.append("    ldap: ").append(toIndentedString(ldap)).append("\n");
    sb.append("    allUsersGroup: ").append(toIndentedString(allUsersGroup)).append("\n");
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

