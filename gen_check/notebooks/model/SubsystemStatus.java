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
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * status of a subsystem Leonardo depends on
 */
@ApiModel(description = "status of a subsystem Leonardo depends on")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-12-26T14:53:15.651-06:00")
public class SubsystemStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("ok")
  private Boolean ok = null;

  @SerializedName("messages")
  private List<String> messages = null;

  public SubsystemStatus ok(Boolean ok) {
    this.ok = ok;
    return this;
  }

   /**
   * whether this system is up or down from Leonardo&#39;s point of view
   * @return ok
  **/
  @ApiModelProperty(required = true, value = "whether this system is up or down from Leonardo's point of view")
  public Boolean getOk() {
    return ok;
  }

  public void setOk(Boolean ok) {
    this.ok = ok;
  }

  public SubsystemStatus messages(List<String> messages) {
    this.messages = messages;
    return this;
  }

  public SubsystemStatus addMessagesItem(String messagesItem) {
    if (this.messages == null) {
      this.messages = new ArrayList<String>();
    }
    this.messages.add(messagesItem);
    return this;
  }

   /**
   * Get messages
   * @return messages
  **/
  @ApiModelProperty(value = "")
  public List<String> getMessages() {
    return messages;
  }

  public void setMessages(List<String> messages) {
    this.messages = messages;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubsystemStatus subsystemStatus = (SubsystemStatus) o;
    return Objects.equals(this.ok, subsystemStatus.ok) &&
        Objects.equals(this.messages, subsystemStatus.messages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ok, messages);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubsystemStatus {\n");
    
    sb.append("    ok: ").append(toIndentedString(ok)).append("\n");
    sb.append("    messages: ").append(toIndentedString(messages)).append("\n");
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

