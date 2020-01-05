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
import org.pmiops.workbench.notebooks.model.ErrorReport;
import org.pmiops.workbench.notebooks.model.StackTraceElement;
import java.io.Serializable;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:05:00.794-05:00")
public class ErrorReport implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("source")
  private String source = null;

  @SerializedName("message")
  private String message = null;

  @SerializedName("exceptionClass")
  private String exceptionClass = null;

  @SerializedName("statusCode")
  private Integer statusCode = null;

  @SerializedName("causes")
  private List<ErrorReport> causes = new ArrayList<ErrorReport>();

  @SerializedName("stackTrace")
  private List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();

  public ErrorReport source(String source) {
    this.source = source;
    return this;
  }

   /**
   * service causing error
   * @return source
  **/
  @ApiModelProperty(required = true, value = "service causing error")
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public ErrorReport message(String message) {
    this.message = message;
    return this;
  }

   /**
   * what went wrong
   * @return message
  **/
  @ApiModelProperty(required = true, value = "what went wrong")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ErrorReport exceptionClass(String exceptionClass) {
    this.exceptionClass = exceptionClass;
    return this;
  }

   /**
   * class of exception thrown
   * @return exceptionClass
  **/
  @ApiModelProperty(value = "class of exception thrown")
  public String getExceptionClass() {
    return exceptionClass;
  }

  public void setExceptionClass(String exceptionClass) {
    this.exceptionClass = exceptionClass;
  }

  public ErrorReport statusCode(Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

   /**
   * HTTP status code
   * @return statusCode
  **/
  @ApiModelProperty(value = "HTTP status code")
  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public ErrorReport causes(List<ErrorReport> causes) {
    this.causes = causes;
    return this;
  }

  public ErrorReport addCausesItem(ErrorReport causesItem) {
    this.causes.add(causesItem);
    return this;
  }

   /**
   * errors triggering this one
   * @return causes
  **/
  @ApiModelProperty(required = true, value = "errors triggering this one")
  public List<ErrorReport> getCauses() {
    return causes;
  }

  public void setCauses(List<ErrorReport> causes) {
    this.causes = causes;
  }

  public ErrorReport stackTrace(List<StackTraceElement> stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public ErrorReport addStackTraceItem(StackTraceElement stackTraceItem) {
    this.stackTrace.add(stackTraceItem);
    return this;
  }

   /**
   * stack trace
   * @return stackTrace
  **/
  @ApiModelProperty(required = true, value = "stack trace")
  public List<StackTraceElement> getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(List<StackTraceElement> stackTrace) {
    this.stackTrace = stackTrace;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorReport errorReport = (ErrorReport) o;
    return Objects.equals(this.source, errorReport.source) &&
        Objects.equals(this.message, errorReport.message) &&
        Objects.equals(this.exceptionClass, errorReport.exceptionClass) &&
        Objects.equals(this.statusCode, errorReport.statusCode) &&
        Objects.equals(this.causes, errorReport.causes) &&
        Objects.equals(this.stackTrace, errorReport.stackTrace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, message, exceptionClass, statusCode, causes, stackTrace);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorReport {\n");
    
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    exceptionClass: ").append(toIndentedString(exceptionClass)).append("\n");
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    causes: ").append(toIndentedString(causes)).append("\n");
    sb.append("    stackTrace: ").append(toIndentedString(stackTrace)).append("\n");
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

