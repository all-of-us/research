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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:49:27.584-05:00")
public class FirecloudStackTraceElement implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("className")
  private String className = null;

  @SerializedName("methodName")
  private String methodName = null;

  @SerializedName("fileName")
  private String fileName = null;

  @SerializedName("lineNumber")
  private Integer lineNumber = null;

  public FirecloudStackTraceElement className(String className) {
    this.className = className;
    return this;
  }

   /**
   * class name
   * @return className
  **/
  @ApiModelProperty(required = true, value = "class name")
  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public FirecloudStackTraceElement methodName(String methodName) {
    this.methodName = methodName;
    return this;
  }

   /**
   * method name
   * @return methodName
  **/
  @ApiModelProperty(required = true, value = "method name")
  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public FirecloudStackTraceElement fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

   /**
   * source file name
   * @return fileName
  **/
  @ApiModelProperty(required = true, value = "source file name")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public FirecloudStackTraceElement lineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
    return this;
  }

   /**
   * line number
   * @return lineNumber
  **/
  @ApiModelProperty(required = true, value = "line number")
  public Integer getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudStackTraceElement stackTraceElement = (FirecloudStackTraceElement) o;
    return Objects.equals(this.className, stackTraceElement.className) &&
        Objects.equals(this.methodName, stackTraceElement.methodName) &&
        Objects.equals(this.fileName, stackTraceElement.fileName) &&
        Objects.equals(this.lineNumber, stackTraceElement.lineNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, methodName, fileName, lineNumber);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudStackTraceElement {\n");
    
    sb.append("    className: ").append(toIndentedString(className)).append("\n");
    sb.append("    methodName: ").append(toIndentedString(methodName)).append("\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    lineNumber: ").append(toIndentedString(lineNumber)).append("\n");
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

