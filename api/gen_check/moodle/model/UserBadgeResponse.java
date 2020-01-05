/*
 * Moodle
 * Service for Moodle Interactions. 
 *
 * OpenAPI spec version: 0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.pmiops.workbench.moodle.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import java.io.Serializable;

/**
 * UserBadgeResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:54:38.149-05:00")
public class UserBadgeResponse implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("badges")
  private List<BadgeDetails> badges = new ArrayList<BadgeDetails>();

  @SerializedName("exception")
  private String exception = null;

  @SerializedName("errorcode")
  private String errorcode = null;

  @SerializedName("message")
  private String message = null;

  public UserBadgeResponse badges(List<BadgeDetails> badges) {
    this.badges = badges;
    return this;
  }

  public UserBadgeResponse addBadgesItem(BadgeDetails badgesItem) {
    this.badges.add(badgesItem);
    return this;
  }

   /**
   * Get badges
   * @return badges
  **/
  @ApiModelProperty(required = true, value = "")
  public List<BadgeDetails> getBadges() {
    return badges;
  }

  public void setBadges(List<BadgeDetails> badges) {
    this.badges = badges;
  }

  public UserBadgeResponse exception(String exception) {
    this.exception = exception;
    return this;
  }

   /**
   * Get exception
   * @return exception
  **/
  @ApiModelProperty(value = "")
  public String getException() {
    return exception;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public UserBadgeResponse errorcode(String errorcode) {
    this.errorcode = errorcode;
    return this;
  }

   /**
   * Get errorcode
   * @return errorcode
  **/
  @ApiModelProperty(value = "")
  public String getErrorcode() {
    return errorcode;
  }

  public void setErrorcode(String errorcode) {
    this.errorcode = errorcode;
  }

  public UserBadgeResponse message(String message) {
    this.message = message;
    return this;
  }

   /**
   * Get message
   * @return message
  **/
  @ApiModelProperty(value = "")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserBadgeResponse userBadgeResponse = (UserBadgeResponse) o;
    return Objects.equals(this.badges, userBadgeResponse.badges) &&
        Objects.equals(this.exception, userBadgeResponse.exception) &&
        Objects.equals(this.errorcode, userBadgeResponse.errorcode) &&
        Objects.equals(this.message, userBadgeResponse.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(badges, exception, errorcode, message);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserBadgeResponse {\n");
    
    sb.append("    badges: ").append(toIndentedString(badges)).append("\n");
    sb.append("    exception: ").append(toIndentedString(exception)).append("\n");
    sb.append("    errorcode: ").append(toIndentedString(errorcode)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

