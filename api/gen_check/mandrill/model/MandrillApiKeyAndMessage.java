/*
 * Mandrill
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.pmiops.workbench.mandrill.model;

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
 * MandrillApiKeyAndMessage
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:10:53.090-05:00")
public class MandrillApiKeyAndMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("key")
  private String key = null;

  @SerializedName("message")
  private Object message = null;

  public MandrillApiKeyAndMessage key(String key) {
    this.key = key;
    return this;
  }

   /**
   * API key
   * @return key
  **/
  @ApiModelProperty(required = true, value = "API key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public MandrillApiKeyAndMessage message(Object message) {
    this.message = message;
    return this;
  }

   /**
   * Mandrill Message
   * @return message
  **/
  @ApiModelProperty(required = true, value = "Mandrill Message")
  public Object getMessage() {
    return message;
  }

  public void setMessage(Object message) {
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
    MandrillApiKeyAndMessage mandrillApiKeyAndMessage = (MandrillApiKeyAndMessage) o;
    return Objects.equals(this.key, mandrillApiKeyAndMessage.key) &&
        Objects.equals(this.message, mandrillApiKeyAndMessage.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, message);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MandrillApiKeyAndMessage {\n");
    
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
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

