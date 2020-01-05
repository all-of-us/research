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
 * Specification of Jupyter Extensions to be installed on the cluster
 */
@ApiModel(description = "Specification of Jupyter Extensions to be installed on the cluster")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-01-05T11:54:38.230-05:00")
public class UserJupyterExtensionConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName("nbExtensions")
  private Object nbExtensions = null;

  @SerializedName("serverExtensions")
  private Object serverExtensions = null;

  @SerializedName("combinedExtensions")
  private Object combinedExtensions = null;

  @SerializedName("labExtensions")
  private Object labExtensions = null;

  public UserJupyterExtensionConfig nbExtensions(Object nbExtensions) {
    this.nbExtensions = nbExtensions;
    return this;
  }

   /**
   * Optional, map of extension name and nbExtension. The nbExtension can either be a tar.gz or .js file, either on google storage or at a URL, or a python package. An archive must not include a parent directory, and must have an entry point named &#39;main&#39;. For more information on notebook extensions, see http://jupyter-notebook.readthedocs.io/en/latest/extending/frontend_extensions.html. Example, {\&quot;ext1\&quot;:\&quot;gs://bucket/extension.tar.gz\&quot;, \&quot;ext2\&quot;:\&quot;python-package\&quot;,  \&quot;ext3\&quot;:\&quot;http://foo.com/extension.js\&quot;} 
   * @return nbExtensions
  **/
  @ApiModelProperty(value = "Optional, map of extension name and nbExtension. The nbExtension can either be a tar.gz or .js file, either on google storage or at a URL, or a python package. An archive must not include a parent directory, and must have an entry point named 'main'. For more information on notebook extensions, see http://jupyter-notebook.readthedocs.io/en/latest/extending/frontend_extensions.html. Example, {\"ext1\":\"gs://bucket/extension.tar.gz\", \"ext2\":\"python-package\",  \"ext3\":\"http://foo.com/extension.js\"} ")
  public Object getNbExtensions() {
    return nbExtensions;
  }

  public void setNbExtensions(Object nbExtensions) {
    this.nbExtensions = nbExtensions;
  }

  public UserJupyterExtensionConfig serverExtensions(Object serverExtensions) {
    this.serverExtensions = serverExtensions;
    return this;
  }

   /**
   * Optional, map of extension name and server extension. The serverExtensions can either be a tar.gz file on google storage or a python package. Example, {\&quot;ext1\&quot;:\&quot;gs://bucket/extension.tar.gz\&quot;, \&quot;ext2\&quot;:\&quot;python-package\&quot;} 
   * @return serverExtensions
  **/
  @ApiModelProperty(value = "Optional, map of extension name and server extension. The serverExtensions can either be a tar.gz file on google storage or a python package. Example, {\"ext1\":\"gs://bucket/extension.tar.gz\", \"ext2\":\"python-package\"} ")
  public Object getServerExtensions() {
    return serverExtensions;
  }

  public void setServerExtensions(Object serverExtensions) {
    this.serverExtensions = serverExtensions;
  }

  public UserJupyterExtensionConfig combinedExtensions(Object combinedExtensions) {
    this.combinedExtensions = combinedExtensions;
    return this;
  }

   /**
   * Optional, map of extension name and notebook plus server extension. The extension can either be a tar.gz file on google storage or a python package. Example, {\&quot;ext1\&quot;:\&quot;gs://bucket/extension.tar.gz\&quot;, \&quot;ext2\&quot;:\&quot;python-package\&quot;} 
   * @return combinedExtensions
  **/
  @ApiModelProperty(value = "Optional, map of extension name and notebook plus server extension. The extension can either be a tar.gz file on google storage or a python package. Example, {\"ext1\":\"gs://bucket/extension.tar.gz\", \"ext2\":\"python-package\"} ")
  public Object getCombinedExtensions() {
    return combinedExtensions;
  }

  public void setCombinedExtensions(Object combinedExtensions) {
    this.combinedExtensions = combinedExtensions;
  }

  public UserJupyterExtensionConfig labExtensions(Object labExtensions) {
    this.labExtensions = labExtensions;
    return this;
  }

   /**
   * Optional, map of extension name and lab extension. The extension should be a verified jupyterlab extension that is uploaded to npm (list of public extensions here: https://github.com/search?utf8&#x3D;%E2%9C%93&amp;q&#x3D;topic%3Ajupyterlab-extension&amp;type&#x3D;Repositories), a gzipped tarball made using &#39;npm pack&#39;, a folder structured by &#39;jlpm build&#39;, a JS file to be inserted into an JL extension template (see https://github.com/jupyterlab/extension-cookiecutter-js), or a URL to one of the last three options. 
   * @return labExtensions
  **/
  @ApiModelProperty(value = "Optional, map of extension name and lab extension. The extension should be a verified jupyterlab extension that is uploaded to npm (list of public extensions here: https://github.com/search?utf8=%E2%9C%93&q=topic%3Ajupyterlab-extension&type=Repositories), a gzipped tarball made using 'npm pack', a folder structured by 'jlpm build', a JS file to be inserted into an JL extension template (see https://github.com/jupyterlab/extension-cookiecutter-js), or a URL to one of the last three options. ")
  public Object getLabExtensions() {
    return labExtensions;
  }

  public void setLabExtensions(Object labExtensions) {
    this.labExtensions = labExtensions;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserJupyterExtensionConfig userJupyterExtensionConfig = (UserJupyterExtensionConfig) o;
    return Objects.equals(this.nbExtensions, userJupyterExtensionConfig.nbExtensions) &&
        Objects.equals(this.serverExtensions, userJupyterExtensionConfig.serverExtensions) &&
        Objects.equals(this.combinedExtensions, userJupyterExtensionConfig.combinedExtensions) &&
        Objects.equals(this.labExtensions, userJupyterExtensionConfig.labExtensions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nbExtensions, serverExtensions, combinedExtensions, labExtensions);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserJupyterExtensionConfig {\n");
    
    sb.append("    nbExtensions: ").append(toIndentedString(nbExtensions)).append("\n");
    sb.append("    serverExtensions: ").append(toIndentedString(serverExtensions)).append("\n");
    sb.append("    combinedExtensions: ").append(toIndentedString(combinedExtensions)).append("\n");
    sb.append("    labExtensions: ").append(toIndentedString(labExtensions)).append("\n");
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

