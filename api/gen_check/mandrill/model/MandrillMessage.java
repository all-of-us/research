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
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import java.io.Serializable;

/**
 * MandrillMessage
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-12-26T15:08:18.649-06:00")
public class MandrillMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("html")
  private String html = null;

  @SerializedName("subject")
  private String subject = null;

  @SerializedName("from_email")
  private String fromEmail = null;

  @SerializedName("to")
  private List<RecipientAddress> to = new ArrayList<RecipientAddress>();

  public MandrillMessage html(String html) {
    this.html = html;
    return this;
  }

   /**
   * html that makes up email message
   * @return html
  **/
  @ApiModelProperty(required = true, value = "html that makes up email message")
  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public MandrillMessage subject(String subject) {
    this.subject = subject;
    return this;
  }

   /**
   * subject of email
   * @return subject
  **/
  @ApiModelProperty(required = true, value = "subject of email")
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public MandrillMessage fromEmail(String fromEmail) {
    this.fromEmail = fromEmail;
    return this;
  }

   /**
   * the from address
   * @return fromEmail
  **/
  @ApiModelProperty(required = true, value = "the from address")
  public String getFromEmail() {
    return fromEmail;
  }

  public void setFromEmail(String fromEmail) {
    this.fromEmail = fromEmail;
  }

  public MandrillMessage to(List<RecipientAddress> to) {
    this.to = to;
    return this;
  }

  public MandrillMessage addToItem(RecipientAddress toItem) {
    this.to.add(toItem);
    return this;
  }

   /**
   * Get to
   * @return to
  **/
  @ApiModelProperty(required = true, value = "")
  public List<RecipientAddress> getTo() {
    return to;
  }

  public void setTo(List<RecipientAddress> to) {
    this.to = to;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MandrillMessage mandrillMessage = (MandrillMessage) o;
    return Objects.equals(this.html, mandrillMessage.html) &&
        Objects.equals(this.subject, mandrillMessage.subject) &&
        Objects.equals(this.fromEmail, mandrillMessage.fromEmail) &&
        Objects.equals(this.to, mandrillMessage.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(html, subject, fromEmail, to);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MandrillMessage {\n");
    
    sb.append("    html: ").append(toIndentedString(html)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    fromEmail: ").append(toIndentedString(fromEmail)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
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

