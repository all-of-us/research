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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupRef;
import java.io.Serializable;

/**
 * FirecloudWorkspaceIngest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T12:07:34.443-05:00")
public class FirecloudWorkspaceIngest implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("namespace")
  private String namespace = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("attributes")
  private Map<String, String> attributes = new HashMap<String, String>();

  @SerializedName("authorizationDomain")
  private List<FirecloudManagedGroupRef> authorizationDomain = null;

  public FirecloudWorkspaceIngest namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

   /**
   * New workspace namespace
   * @return namespace
  **/
  @ApiModelProperty(required = true, value = "New workspace namespace")
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public FirecloudWorkspaceIngest name(String name) {
    this.name = name;
    return this;
  }

   /**
   * New workspace name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "New workspace name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FirecloudWorkspaceIngest attributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }

  public FirecloudWorkspaceIngest putAttributesItem(String key, String attributesItem) {
    this.attributes.put(key, attributesItem);
    return this;
  }

   /**
   * Map of attributes
   * @return attributes
  **/
  @ApiModelProperty(required = true, value = "Map of attributes")
  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public FirecloudWorkspaceIngest authorizationDomain(List<FirecloudManagedGroupRef> authorizationDomain) {
    this.authorizationDomain = authorizationDomain;
    return this;
  }

  public FirecloudWorkspaceIngest addAuthorizationDomainItem(FirecloudManagedGroupRef authorizationDomainItem) {
    if (this.authorizationDomain == null) {
      this.authorizationDomain = new ArrayList<FirecloudManagedGroupRef>();
    }
    this.authorizationDomain.add(authorizationDomainItem);
    return this;
  }

   /**
   * The list of groups to form the Authorization Domain (empty if no Authorization Domain is set)
   * @return authorizationDomain
  **/
  @ApiModelProperty(value = "The list of groups to form the Authorization Domain (empty if no Authorization Domain is set)")
  public List<FirecloudManagedGroupRef> getAuthorizationDomain() {
    return authorizationDomain;
  }

  public void setAuthorizationDomain(List<FirecloudManagedGroupRef> authorizationDomain) {
    this.authorizationDomain = authorizationDomain;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FirecloudWorkspaceIngest workspaceIngest = (FirecloudWorkspaceIngest) o;
    return Objects.equals(this.namespace, workspaceIngest.namespace) &&
        Objects.equals(this.name, workspaceIngest.name) &&
        Objects.equals(this.attributes, workspaceIngest.attributes) &&
        Objects.equals(this.authorizationDomain, workspaceIngest.authorizationDomain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, name, attributes, authorizationDomain);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FirecloudWorkspaceIngest {\n");
    
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
    sb.append("    authorizationDomain: ").append(toIndentedString(authorizationDomain)).append("\n");
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

