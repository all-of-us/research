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
 * A contents object.  The content and format keys may be null if content is not contained.  If type is &#39;file&#39;, then the mimetype will be null.
 */
@ApiModel(description = "A contents object.  The content and format keys may be null if content is not contained.  If type is 'file', then the mimetype will be null.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:54:38.230-05:00")
public class JupyterContents implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("name")
  private String name = null;

  @SerializedName("path")
  private String path = null;

  /**
   * Type of content
   */
  @JsonAdapter(TypeEnum.Adapter.class)
  public enum TypeEnum {
    DIRECTORY("directory"),
    
    FILE("file"),
    
    NOTEBOOK("notebook");

    private String value;

    TypeEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static TypeEnum fromValue(String text) {
      for (TypeEnum b : TypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<TypeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final TypeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public TypeEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return TypeEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("type")
  private TypeEnum type = null;

  @SerializedName("writable")
  private Boolean writable = null;

  @SerializedName("created")
  private String created = null;

  @SerializedName("last_modified")
  private String lastModified = null;

  @SerializedName("mimetype")
  private String mimetype = null;

  @SerializedName("content")
  private String content = null;

  @SerializedName("format")
  private String format = null;

  public JupyterContents name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Name of file or directory, equivalent to the last part of the path
   * @return name
  **/
  @ApiModelProperty(required = true, value = "Name of file or directory, equivalent to the last part of the path")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JupyterContents path(String path) {
    this.path = path;
    return this;
  }

   /**
   * Full path for file or directory
   * @return path
  **/
  @ApiModelProperty(required = true, value = "Full path for file or directory")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public JupyterContents type(TypeEnum type) {
    this.type = type;
    return this;
  }

   /**
   * Type of content
   * @return type
  **/
  @ApiModelProperty(required = true, value = "Type of content")
  public TypeEnum getType() {
    return type;
  }

  public void setType(TypeEnum type) {
    this.type = type;
  }

  public JupyterContents writable(Boolean writable) {
    this.writable = writable;
    return this;
  }

   /**
   * indicates whether the requester has permission to edit the file
   * @return writable
  **/
  @ApiModelProperty(required = true, value = "indicates whether the requester has permission to edit the file")
  public Boolean getWritable() {
    return writable;
  }

  public void setWritable(Boolean writable) {
    this.writable = writable;
  }

  public JupyterContents created(String created) {
    this.created = created;
    return this;
  }

   /**
   * Creation timestamp
   * @return created
  **/
  @ApiModelProperty(required = true, value = "Creation timestamp")
  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public JupyterContents lastModified(String lastModified) {
    this.lastModified = lastModified;
    return this;
  }

   /**
   * Last modified timestamp
   * @return lastModified
  **/
  @ApiModelProperty(required = true, value = "Last modified timestamp")
  public String getLastModified() {
    return lastModified;
  }

  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  public JupyterContents mimetype(String mimetype) {
    this.mimetype = mimetype;
    return this;
  }

   /**
   * The mimetype of a file.  If content is not null, and type is &#39;file&#39;, this will contain the mimetype of the file, otherwise this will be null.
   * @return mimetype
  **/
  @ApiModelProperty(required = true, value = "The mimetype of a file.  If content is not null, and type is 'file', this will contain the mimetype of the file, otherwise this will be null.")
  public String getMimetype() {
    return mimetype;
  }

  public void setMimetype(String mimetype) {
    this.mimetype = mimetype;
  }

  public JupyterContents content(String content) {
    this.content = content;
    return this;
  }

   /**
   * The content, if requested (otherwise null).  Will be an array if type is &#39;directory&#39;
   * @return content
  **/
  @ApiModelProperty(required = true, value = "The content, if requested (otherwise null).  Will be an array if type is 'directory'")
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public JupyterContents format(String format) {
    this.format = format;
    return this;
  }

   /**
   * Format of content (one of null, &#39;text&#39;, &#39;base64&#39;, &#39;json&#39;)
   * @return format
  **/
  @ApiModelProperty(required = true, value = "Format of content (one of null, 'text', 'base64', 'json')")
  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JupyterContents jupyterContents = (JupyterContents) o;
    return Objects.equals(this.name, jupyterContents.name) &&
        Objects.equals(this.path, jupyterContents.path) &&
        Objects.equals(this.type, jupyterContents.type) &&
        Objects.equals(this.writable, jupyterContents.writable) &&
        Objects.equals(this.created, jupyterContents.created) &&
        Objects.equals(this.lastModified, jupyterContents.lastModified) &&
        Objects.equals(this.mimetype, jupyterContents.mimetype) &&
        Objects.equals(this.content, jupyterContents.content) &&
        Objects.equals(this.format, jupyterContents.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, path, type, writable, created, lastModified, mimetype, content, format);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JupyterContents {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    writable: ").append(toIndentedString(writable)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    lastModified: ").append(toIndentedString(lastModified)).append("\n");
    sb.append("    mimetype: ").append(toIndentedString(mimetype)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
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

