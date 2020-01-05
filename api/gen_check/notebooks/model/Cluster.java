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
import org.pmiops.workbench.notebooks.model.ClusterError;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.Instance;
import org.pmiops.workbench.notebooks.model.MachineConfig;
import java.io.Serializable;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:10:53.265-05:00")
public class Cluster implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("id")
  private String id = null;

  @SerializedName("clusterName")
  private String clusterName = null;

  @SerializedName("googleId")
  private String googleId = null;

  @SerializedName("googleProject")
  private String googleProject = null;

  @SerializedName("googleServiceAccount")
  private String googleServiceAccount = null;

  @SerializedName("machineConfig")
  private MachineConfig machineConfig = null;

  @SerializedName("operationName")
  private String operationName = null;

  @SerializedName("status")
  private ClusterStatus status = null;

  @SerializedName("hostIp")
  private String hostIp = null;

  @SerializedName("createdDate")
  private String createdDate = null;

  @SerializedName("destroyedDate")
  private String destroyedDate = null;

  @SerializedName("labels")
  private Object labels = null;

  @SerializedName("errors")
  private List<ClusterError> errors = null;

  @SerializedName("instances")
  private List<Instance> instances = null;

  @SerializedName("dateAccessed")
  private String dateAccessed = null;

  @SerializedName("autopauseThreshold")
  private Integer autopauseThreshold = null;

  @SerializedName("defaultClientId")
  private String defaultClientId = null;

  @SerializedName("scopes")
  private List<String> scopes = new ArrayList<String>();

  public Cluster id(String id) {
    this.id = id;
    return this;
  }

   /**
   * The internally-referenced ID of the cluster
   * @return id
  **/
  @ApiModelProperty(required = true, value = "The internally-referenced ID of the cluster")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Cluster clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

   /**
   * The user-supplied name for the cluster
   * @return clusterName
  **/
  @ApiModelProperty(required = true, value = "The user-supplied name for the cluster")
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public Cluster googleId(String googleId) {
    this.googleId = googleId;
    return this;
  }

   /**
   * Google&#39;s UUID for the cluster
   * @return googleId
  **/
  @ApiModelProperty(value = "Google's UUID for the cluster")
  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  public Cluster googleProject(String googleProject) {
    this.googleProject = googleProject;
    return this;
  }

   /**
   * The Google Project used to create the cluster
   * @return googleProject
  **/
  @ApiModelProperty(required = true, value = "The Google Project used to create the cluster")
  public String getGoogleProject() {
    return googleProject;
  }

  public void setGoogleProject(String googleProject) {
    this.googleProject = googleProject;
  }

  public Cluster googleServiceAccount(String googleServiceAccount) {
    this.googleServiceAccount = googleServiceAccount;
    return this;
  }

   /**
   * The Google Service Account used to create the cluster
   * @return googleServiceAccount
  **/
  @ApiModelProperty(required = true, value = "The Google Service Account used to create the cluster")
  public String getGoogleServiceAccount() {
    return googleServiceAccount;
  }

  public void setGoogleServiceAccount(String googleServiceAccount) {
    this.googleServiceAccount = googleServiceAccount;
  }

  public Cluster machineConfig(MachineConfig machineConfig) {
    this.machineConfig = machineConfig;
    return this;
  }

   /**
   * The machine configurations for the master and worker nodes
   * @return machineConfig
  **/
  @ApiModelProperty(required = true, value = "The machine configurations for the master and worker nodes")
  public MachineConfig getMachineConfig() {
    return machineConfig;
  }

  public void setMachineConfig(MachineConfig machineConfig) {
    this.machineConfig = machineConfig;
  }

  public Cluster operationName(String operationName) {
    this.operationName = operationName;
    return this;
  }

   /**
   * Google&#39;s operation ID for the cluster
   * @return operationName
  **/
  @ApiModelProperty(value = "Google's operation ID for the cluster")
  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public Cluster status(ClusterStatus status) {
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(required = true, value = "")
  public ClusterStatus getStatus() {
    return status;
  }

  public void setStatus(ClusterStatus status) {
    this.status = status;
  }

  public Cluster hostIp(String hostIp) {
    this.hostIp = hostIp;
    return this;
  }

   /**
   * The IP address of the cluster master node
   * @return hostIp
  **/
  @ApiModelProperty(value = "The IP address of the cluster master node")
  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public Cluster createdDate(String createdDate) {
    this.createdDate = createdDate;
    return this;
  }

   /**
   * The date and time the cluster was created, in ISO-8601 format
   * @return createdDate
  **/
  @ApiModelProperty(required = true, value = "The date and time the cluster was created, in ISO-8601 format")
  public String getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  public Cluster destroyedDate(String destroyedDate) {
    this.destroyedDate = destroyedDate;
    return this;
  }

   /**
   * The date and time the cluster was destroyed, in ISO-8601 format
   * @return destroyedDate
  **/
  @ApiModelProperty(value = "The date and time the cluster was destroyed, in ISO-8601 format")
  public String getDestroyedDate() {
    return destroyedDate;
  }

  public void setDestroyedDate(String destroyedDate) {
    this.destroyedDate = destroyedDate;
  }

  public Cluster labels(Object labels) {
    this.labels = labels;
    return this;
  }

   /**
   * The labels to be placed on the cluster. Of type Map[String,String]
   * @return labels
  **/
  @ApiModelProperty(required = true, value = "The labels to be placed on the cluster. Of type Map[String,String]")
  public Object getLabels() {
    return labels;
  }

  public void setLabels(Object labels) {
    this.labels = labels;
  }

  public Cluster errors(List<ClusterError> errors) {
    this.errors = errors;
    return this;
  }

  public Cluster addErrorsItem(ClusterError errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<ClusterError>();
    }
    this.errors.add(errorsItem);
    return this;
  }

   /**
   * The list of errors that were encountered on cluster create. Each error consists of the error message, code and timestamp
   * @return errors
  **/
  @ApiModelProperty(value = "The list of errors that were encountered on cluster create. Each error consists of the error message, code and timestamp")
  public List<ClusterError> getErrors() {
    return errors;
  }

  public void setErrors(List<ClusterError> errors) {
    this.errors = errors;
  }

  public Cluster instances(List<Instance> instances) {
    this.instances = instances;
    return this;
  }

  public Cluster addInstancesItem(Instance instancesItem) {
    if (this.instances == null) {
      this.instances = new ArrayList<Instance>();
    }
    this.instances.add(instancesItem);
    return this;
  }

   /**
   * Array of instances belonging to this cluster
   * @return instances
  **/
  @ApiModelProperty(value = "Array of instances belonging to this cluster")
  public List<Instance> getInstances() {
    return instances;
  }

  public void setInstances(List<Instance> instances) {
    this.instances = instances;
  }

  public Cluster dateAccessed(String dateAccessed) {
    this.dateAccessed = dateAccessed;
    return this;
  }

   /**
   * The date and time the cluster was last accessed, in ISO-8601 format. Date accessed is defined as the last time the cluster was created, modified, or accessed via the proxy. 
   * @return dateAccessed
  **/
  @ApiModelProperty(required = true, value = "The date and time the cluster was last accessed, in ISO-8601 format. Date accessed is defined as the last time the cluster was created, modified, or accessed via the proxy. ")
  public String getDateAccessed() {
    return dateAccessed;
  }

  public void setDateAccessed(String dateAccessed) {
    this.dateAccessed = dateAccessed;
  }

  public Cluster autopauseThreshold(Integer autopauseThreshold) {
    this.autopauseThreshold = autopauseThreshold;
    return this;
  }

   /**
   * The number of minutes of idle time to elapse before the cluster is autopaused. A value of 0 is equivalent to autopause being turned off.
   * @return autopauseThreshold
  **/
  @ApiModelProperty(required = true, value = "The number of minutes of idle time to elapse before the cluster is autopaused. A value of 0 is equivalent to autopause being turned off.")
  public Integer getAutopauseThreshold() {
    return autopauseThreshold;
  }

  public void setAutopauseThreshold(Integer autopauseThreshold) {
    this.autopauseThreshold = autopauseThreshold;
  }

  public Cluster defaultClientId(String defaultClientId) {
    this.defaultClientId = defaultClientId;
    return this;
  }

   /**
   * The default Google Client ID.
   * @return defaultClientId
  **/
  @ApiModelProperty(required = true, value = "The default Google Client ID.")
  public String getDefaultClientId() {
    return defaultClientId;
  }

  public void setDefaultClientId(String defaultClientId) {
    this.defaultClientId = defaultClientId;
  }

  public Cluster scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public Cluster addScopesItem(String scopesItem) {
    this.scopes.add(scopesItem);
    return this;
  }

   /**
   * The scopes for the cluster.
   * @return scopes
  **/
  @ApiModelProperty(required = true, value = "The scopes for the cluster.")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Cluster cluster = (Cluster) o;
    return Objects.equals(this.id, cluster.id) &&
        Objects.equals(this.clusterName, cluster.clusterName) &&
        Objects.equals(this.googleId, cluster.googleId) &&
        Objects.equals(this.googleProject, cluster.googleProject) &&
        Objects.equals(this.googleServiceAccount, cluster.googleServiceAccount) &&
        Objects.equals(this.machineConfig, cluster.machineConfig) &&
        Objects.equals(this.operationName, cluster.operationName) &&
        Objects.equals(this.status, cluster.status) &&
        Objects.equals(this.hostIp, cluster.hostIp) &&
        Objects.equals(this.createdDate, cluster.createdDate) &&
        Objects.equals(this.destroyedDate, cluster.destroyedDate) &&
        Objects.equals(this.labels, cluster.labels) &&
        Objects.equals(this.errors, cluster.errors) &&
        Objects.equals(this.instances, cluster.instances) &&
        Objects.equals(this.dateAccessed, cluster.dateAccessed) &&
        Objects.equals(this.autopauseThreshold, cluster.autopauseThreshold) &&
        Objects.equals(this.defaultClientId, cluster.defaultClientId) &&
        Objects.equals(this.scopes, cluster.scopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, clusterName, googleId, googleProject, googleServiceAccount, machineConfig, operationName, status, hostIp, createdDate, destroyedDate, labels, errors, instances, dateAccessed, autopauseThreshold, defaultClientId, scopes);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Cluster {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    googleId: ").append(toIndentedString(googleId)).append("\n");
    sb.append("    googleProject: ").append(toIndentedString(googleProject)).append("\n");
    sb.append("    googleServiceAccount: ").append(toIndentedString(googleServiceAccount)).append("\n");
    sb.append("    machineConfig: ").append(toIndentedString(machineConfig)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    hostIp: ").append(toIndentedString(hostIp)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    destroyedDate: ").append(toIndentedString(destroyedDate)).append("\n");
    sb.append("    labels: ").append(toIndentedString(labels)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("    instances: ").append(toIndentedString(instances)).append("\n");
    sb.append("    dateAccessed: ").append(toIndentedString(dateAccessed)).append("\n");
    sb.append("    autopauseThreshold: ").append(toIndentedString(autopauseThreshold)).append("\n");
    sb.append("    defaultClientId: ").append(toIndentedString(defaultClientId)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
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

