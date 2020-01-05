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
import java.io.Serializable;

/**
 * LocalizationEntry
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:10:53.265-05:00")
public class LocalizationEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("sourceUri")
  private String sourceUri = null;

  @SerializedName("localDestinationPath")
  private String localDestinationPath = null;

  public LocalizationEntry sourceUri(String sourceUri) {
    this.sourceUri = sourceUri;
    return this;
  }

   /**
   * Get sourceUri
   * @return sourceUri
  **/
  @ApiModelProperty(value = "")
  public String getSourceUri() {
    return sourceUri;
  }

  public void setSourceUri(String sourceUri) {
    this.sourceUri = sourceUri;
  }

  public LocalizationEntry localDestinationPath(String localDestinationPath) {
    this.localDestinationPath = localDestinationPath;
    return this;
  }

   /**
   * Get localDestinationPath
   * @return localDestinationPath
  **/
  @ApiModelProperty(value = "")
  public String getLocalDestinationPath() {
    return localDestinationPath;
  }

  public void setLocalDestinationPath(String localDestinationPath) {
    this.localDestinationPath = localDestinationPath;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalizationEntry localizationEntry = (LocalizationEntry) o;
    return Objects.equals(this.sourceUri, localizationEntry.sourceUri) &&
        Objects.equals(this.localDestinationPath, localizationEntry.localDestinationPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceUri, localDestinationPath);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LocalizationEntry {\n");
    
    sb.append("    sourceUri: ").append(toIndentedString(sourceUri)).append("\n");
    sb.append("    localDestinationPath: ").append(toIndentedString(localDestinationPath)).append("\n");
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

