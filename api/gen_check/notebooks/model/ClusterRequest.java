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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.notebooks.model.MachineConfig;
import org.pmiops.workbench.notebooks.model.UserJupyterExtensionConfig;
import java.io.Serializable;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T14:05:00.794-05:00")
public class ClusterRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("labels")
  private Object labels = null;

  @SerializedName("userJupyterExtensionConfig")
  private UserJupyterExtensionConfig userJupyterExtensionConfig = null;

  @SerializedName("jupyterExtensionUri")
  private String jupyterExtensionUri = null;

  @SerializedName("jupyterUserScriptUri")
  private String jupyterUserScriptUri = null;

  @SerializedName("jupyterStartUserScriptUri")
  private String jupyterStartUserScriptUri = null;

  @SerializedName("machineConfig")
  private MachineConfig machineConfig = null;

  @SerializedName("properties")
  private Map<String, String> properties = null;

  @SerializedName("stopAfterCreation")
  private Boolean stopAfterCreation = false;

  @SerializedName("autopause")
  private Boolean autopause = null;

  @SerializedName("autopauseThreshold")
  private Integer autopauseThreshold = null;

  @SerializedName("defaultClientId")
  private String defaultClientId = null;

  @SerializedName("jupyterDockerImage")
  private String jupyterDockerImage = null;

  @SerializedName("scopes")
  private List<String> scopes = null;

  @SerializedName("enableWelder")
  private Boolean enableWelder = false;

  @SerializedName("customClusterEnvironmentVariables")
  private Map<String, String> customClusterEnvironmentVariables = null;

  public ClusterRequest labels(Object labels) {
    this.labels = labels;
    return this;
  }

   /**
   * The labels to be placed on the cluster. Of type Map[String,String]
   * @return labels
  **/
  @ApiModelProperty(value = "The labels to be placed on the cluster. Of type Map[String,String]")
  public Object getLabels() {
    return labels;
  }

  public void setLabels(Object labels) {
    this.labels = labels;
  }

  public ClusterRequest userJupyterExtensionConfig(UserJupyterExtensionConfig userJupyterExtensionConfig) {
    this.userJupyterExtensionConfig = userJupyterExtensionConfig;
    return this;
  }

   /**
   * Jupyter extensions to be installed in the notebook
   * @return userJupyterExtensionConfig
  **/
  @ApiModelProperty(value = "Jupyter extensions to be installed in the notebook")
  public UserJupyterExtensionConfig getUserJupyterExtensionConfig() {
    return userJupyterExtensionConfig;
  }

  public void setUserJupyterExtensionConfig(UserJupyterExtensionConfig userJupyterExtensionConfig) {
    this.userJupyterExtensionConfig = userJupyterExtensionConfig;
  }

  public ClusterRequest jupyterExtensionUri(String jupyterExtensionUri) {
    this.jupyterExtensionUri = jupyterExtensionUri;
    return this;
  }

   /**
   * Optional bucket URI or URL to an archive containing Jupyter notebook extension files, or a .js file. The archive must be in tar.gz format, must not include a parent directory, and must have an entry point named &#39;main&#39;. For more information on notebook extensions, see http://jupyter-notebook.readthedocs.io/en/latest/extending/frontend_extensions.html. 
   * @return jupyterExtensionUri
  **/
  @ApiModelProperty(value = "Optional bucket URI or URL to an archive containing Jupyter notebook extension files, or a .js file. The archive must be in tar.gz format, must not include a parent directory, and must have an entry point named 'main'. For more information on notebook extensions, see http://jupyter-notebook.readthedocs.io/en/latest/extending/frontend_extensions.html. ")
  public String getJupyterExtensionUri() {
    return jupyterExtensionUri;
  }

  public void setJupyterExtensionUri(String jupyterExtensionUri) {
    this.jupyterExtensionUri = jupyterExtensionUri;
  }

  public ClusterRequest jupyterUserScriptUri(String jupyterUserScriptUri) {
    this.jupyterUserScriptUri = jupyterUserScriptUri;
    return this;
  }

   /**
   * Optional GCS object URI to a bash script the user wishes to run inside their jupyter docker. This script runs exactly once when the cluster is first initialized. Logs from this script can be found in the Leo staging bucket for the cluster. Script is run as root and docker --privileged. 
   * @return jupyterUserScriptUri
  **/
  @ApiModelProperty(value = "Optional GCS object URI to a bash script the user wishes to run inside their jupyter docker. This script runs exactly once when the cluster is first initialized. Logs from this script can be found in the Leo staging bucket for the cluster. Script is run as root and docker --privileged. ")
  public String getJupyterUserScriptUri() {
    return jupyterUserScriptUri;
  }

  public void setJupyterUserScriptUri(String jupyterUserScriptUri) {
    this.jupyterUserScriptUri = jupyterUserScriptUri;
  }

  public ClusterRequest jupyterStartUserScriptUri(String jupyterStartUserScriptUri) {
    this.jupyterStartUserScriptUri = jupyterStartUserScriptUri;
    return this;
  }

   /**
   * Optional GCS object URI to a bash script the user wishes to run on cluster start inside the jupyter docker. In contrast to jupyterUserScriptUri, this always runs before starting Jupyter, both on initial cluster creation and on cluster resume (jupyterUserScriptUri runs once on cluster creation). This script may be used to launch background processes which would not otherwise survive a cluster stop/start. The script is pulled once at cluster creation time; subsequent client changes to the user script at this URI do not affect the cluster. Timestamped logs for this script can be found in the Leo staging bucket for the cluster. Script is run as root and docker --privileged. 
   * @return jupyterStartUserScriptUri
  **/
  @ApiModelProperty(value = "Optional GCS object URI to a bash script the user wishes to run on cluster start inside the jupyter docker. In contrast to jupyterUserScriptUri, this always runs before starting Jupyter, both on initial cluster creation and on cluster resume (jupyterUserScriptUri runs once on cluster creation). This script may be used to launch background processes which would not otherwise survive a cluster stop/start. The script is pulled once at cluster creation time; subsequent client changes to the user script at this URI do not affect the cluster. Timestamped logs for this script can be found in the Leo staging bucket for the cluster. Script is run as root and docker --privileged. ")
  public String getJupyterStartUserScriptUri() {
    return jupyterStartUserScriptUri;
  }

  public void setJupyterStartUserScriptUri(String jupyterStartUserScriptUri) {
    this.jupyterStartUserScriptUri = jupyterStartUserScriptUri;
  }

  public ClusterRequest machineConfig(MachineConfig machineConfig) {
    this.machineConfig = machineConfig;
    return this;
  }

   /**
   * The machine configurations for the master and worker nodes
   * @return machineConfig
  **/
  @ApiModelProperty(value = "The machine configurations for the master and worker nodes")
  public MachineConfig getMachineConfig() {
    return machineConfig;
  }

  public void setMachineConfig(MachineConfig machineConfig) {
    this.machineConfig = machineConfig;
  }

  public ClusterRequest properties(Map<String, String> properties) {
    this.properties = properties;
    return this;
  }

  public ClusterRequest putPropertiesItem(String key, String propertiesItem) {
    if (this.properties == null) {
      this.properties = new HashMap<String, String>();
    }
    this.properties.put(key, propertiesItem);
    return this;
  }

   /**
   * Example {\&quot;spark:spark.executor.memory\&quot;: \&quot;10g\&quot;}. See https://cloud.google.com/dataproc/docs/concepts/configuring-clusters/cluster-properties for allowed property settings 
   * @return properties
  **/
  @ApiModelProperty(value = "Example {\"spark:spark.executor.memory\": \"10g\"}. See https://cloud.google.com/dataproc/docs/concepts/configuring-clusters/cluster-properties for allowed property settings ")
  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public ClusterRequest stopAfterCreation(Boolean stopAfterCreation) {
    this.stopAfterCreation = stopAfterCreation;
    return this;
  }

   /**
   * If true, Leo will immediately stop the cluster once it&#39;s created, with the end result being a a cluster in Stopped state. Otherwise, the end result will be a cluster in Running state. Defaults to false. 
   * @return stopAfterCreation
  **/
  @ApiModelProperty(value = "If true, Leo will immediately stop the cluster once it's created, with the end result being a a cluster in Stopped state. Otherwise, the end result will be a cluster in Running state. Defaults to false. ")
  public Boolean getStopAfterCreation() {
    return stopAfterCreation;
  }

  public void setStopAfterCreation(Boolean stopAfterCreation) {
    this.stopAfterCreation = stopAfterCreation;
  }

  public ClusterRequest autopause(Boolean autopause) {
    this.autopause = autopause;
    return this;
  }

   /**
   * Whether autopause feature is enabled for this specific cluster. If unset, autopause will be enabled and a system default threshold will be used.
   * @return autopause
  **/
  @ApiModelProperty(value = "Whether autopause feature is enabled for this specific cluster. If unset, autopause will be enabled and a system default threshold will be used.")
  public Boolean getAutopause() {
    return autopause;
  }

  public void setAutopause(Boolean autopause) {
    this.autopause = autopause;
  }

  public ClusterRequest autopauseThreshold(Integer autopauseThreshold) {
    this.autopauseThreshold = autopauseThreshold;
    return this;
  }

   /**
   * The number of minutes of idle time to elapse before the cluster is autopaused. If autopause is set to false, this value is disregarded. A value of 0 is equivalent to autopause being turned off. If autopause is enabled and this is unset, a system default threshold will be used.
   * @return autopauseThreshold
  **/
  @ApiModelProperty(value = "The number of minutes of idle time to elapse before the cluster is autopaused. If autopause is set to false, this value is disregarded. A value of 0 is equivalent to autopause being turned off. If autopause is enabled and this is unset, a system default threshold will be used.")
  public Integer getAutopauseThreshold() {
    return autopauseThreshold;
  }

  public void setAutopauseThreshold(Integer autopauseThreshold) {
    this.autopauseThreshold = autopauseThreshold;
  }

  public ClusterRequest defaultClientId(String defaultClientId) {
    this.defaultClientId = defaultClientId;
    return this;
  }

   /**
   * The default Google Client ID.
   * @return defaultClientId
  **/
  @ApiModelProperty(value = "The default Google Client ID.")
  public String getDefaultClientId() {
    return defaultClientId;
  }

  public void setDefaultClientId(String defaultClientId) {
    this.defaultClientId = defaultClientId;
  }

  public ClusterRequest jupyterDockerImage(String jupyterDockerImage) {
    this.jupyterDockerImage = jupyterDockerImage;
    return this;
  }

   /**
   * The Jupyter docker image to install. May be Dockerhub or GCR. If not set, then a default Jupyter image will be installed.
   * @return jupyterDockerImage
  **/
  @ApiModelProperty(value = "The Jupyter docker image to install. May be Dockerhub or GCR. If not set, then a default Jupyter image will be installed.")
  public String getJupyterDockerImage() {
    return jupyterDockerImage;
  }

  public void setJupyterDockerImage(String jupyterDockerImage) {
    this.jupyterDockerImage = jupyterDockerImage;
  }

  public ClusterRequest scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public ClusterRequest addScopesItem(String scopesItem) {
    if (this.scopes == null) {
      this.scopes = new ArrayList<String>();
    }
    this.scopes.add(scopesItem);
    return this;
  }

   /**
   * The scopes for the cluster. Defaults (userinfo.email, userinfo.profile, bigquery, source.read_only) will be used if left blank. Important: If you choose to specify custom scopes, the defaults will be overwritten. Thus, if you need the defaults, you will need to include the default scopes in your custom list of scopes. 
   * @return scopes
  **/
  @ApiModelProperty(value = "The scopes for the cluster. Defaults (userinfo.email, userinfo.profile, bigquery, source.read_only) will be used if left blank. Important: If you choose to specify custom scopes, the defaults will be overwritten. Thus, if you need the defaults, you will need to include the default scopes in your custom list of scopes. ")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  public ClusterRequest enableWelder(Boolean enableWelder) {
    this.enableWelder = enableWelder;
    return this;
  }

   /**
   * If set to true, sets up welder on the cluster. If unset, welder will not be put on the cluster.
   * @return enableWelder
  **/
  @ApiModelProperty(value = "If set to true, sets up welder on the cluster. If unset, welder will not be put on the cluster.")
  public Boolean getEnableWelder() {
    return enableWelder;
  }

  public void setEnableWelder(Boolean enableWelder) {
    this.enableWelder = enableWelder;
  }

  public ClusterRequest customClusterEnvironmentVariables(Map<String, String> customClusterEnvironmentVariables) {
    this.customClusterEnvironmentVariables = customClusterEnvironmentVariables;
    return this;
  }

  public ClusterRequest putCustomClusterEnvironmentVariablesItem(String key, String customClusterEnvironmentVariablesItem) {
    if (this.customClusterEnvironmentVariables == null) {
      this.customClusterEnvironmentVariables = new HashMap<String, String>();
    }
    this.customClusterEnvironmentVariables.put(key, customClusterEnvironmentVariablesItem);
    return this;
  }

   /**
   * A collection of key/value pairs of environment variable names and their desired value to be set in the cluster.
   * @return customClusterEnvironmentVariables
  **/
  @ApiModelProperty(value = "A collection of key/value pairs of environment variable names and their desired value to be set in the cluster.")
  public Map<String, String> getCustomClusterEnvironmentVariables() {
    return customClusterEnvironmentVariables;
  }

  public void setCustomClusterEnvironmentVariables(Map<String, String> customClusterEnvironmentVariables) {
    this.customClusterEnvironmentVariables = customClusterEnvironmentVariables;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterRequest clusterRequest = (ClusterRequest) o;
    return Objects.equals(this.labels, clusterRequest.labels) &&
        Objects.equals(this.userJupyterExtensionConfig, clusterRequest.userJupyterExtensionConfig) &&
        Objects.equals(this.jupyterExtensionUri, clusterRequest.jupyterExtensionUri) &&
        Objects.equals(this.jupyterUserScriptUri, clusterRequest.jupyterUserScriptUri) &&
        Objects.equals(this.jupyterStartUserScriptUri, clusterRequest.jupyterStartUserScriptUri) &&
        Objects.equals(this.machineConfig, clusterRequest.machineConfig) &&
        Objects.equals(this.properties, clusterRequest.properties) &&
        Objects.equals(this.stopAfterCreation, clusterRequest.stopAfterCreation) &&
        Objects.equals(this.autopause, clusterRequest.autopause) &&
        Objects.equals(this.autopauseThreshold, clusterRequest.autopauseThreshold) &&
        Objects.equals(this.defaultClientId, clusterRequest.defaultClientId) &&
        Objects.equals(this.jupyterDockerImage, clusterRequest.jupyterDockerImage) &&
        Objects.equals(this.scopes, clusterRequest.scopes) &&
        Objects.equals(this.enableWelder, clusterRequest.enableWelder) &&
        Objects.equals(this.customClusterEnvironmentVariables, clusterRequest.customClusterEnvironmentVariables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(labels, userJupyterExtensionConfig, jupyterExtensionUri, jupyterUserScriptUri, jupyterStartUserScriptUri, machineConfig, properties, stopAfterCreation, autopause, autopauseThreshold, defaultClientId, jupyterDockerImage, scopes, enableWelder, customClusterEnvironmentVariables);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterRequest {\n");
    
    sb.append("    labels: ").append(toIndentedString(labels)).append("\n");
    sb.append("    userJupyterExtensionConfig: ").append(toIndentedString(userJupyterExtensionConfig)).append("\n");
    sb.append("    jupyterExtensionUri: ").append(toIndentedString(jupyterExtensionUri)).append("\n");
    sb.append("    jupyterUserScriptUri: ").append(toIndentedString(jupyterUserScriptUri)).append("\n");
    sb.append("    jupyterStartUserScriptUri: ").append(toIndentedString(jupyterStartUserScriptUri)).append("\n");
    sb.append("    machineConfig: ").append(toIndentedString(machineConfig)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    stopAfterCreation: ").append(toIndentedString(stopAfterCreation)).append("\n");
    sb.append("    autopause: ").append(toIndentedString(autopause)).append("\n");
    sb.append("    autopauseThreshold: ").append(toIndentedString(autopauseThreshold)).append("\n");
    sb.append("    defaultClientId: ").append(toIndentedString(defaultClientId)).append("\n");
    sb.append("    jupyterDockerImage: ").append(toIndentedString(jupyterDockerImage)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
    sb.append("    enableWelder: ").append(toIndentedString(enableWelder)).append("\n");
    sb.append("    customClusterEnvironmentVariables: ").append(toIndentedString(customClusterEnvironmentVariables)).append("\n");
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

