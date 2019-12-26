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
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

import java.io.IOException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Gets or Sets ClusterStatus
 */
@JsonAdapter(ClusterStatus.Adapter.class)
public enum ClusterStatus {
  
  CREATING("Creating"),
  
  RUNNING("Running"),
  
  UPDATING("Updating"),
  
  ERROR("Error"),
  
  STOPPING("Stopping"),
  
  STOPPED("Stopped"),
  
  STARTING("Starting"),
  
  DELETING("Deleting"),
  
  DELETED("Deleted"),
  
  UNKNOWN("Unknown");

  private String value;

  ClusterStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static ClusterStatus fromValue(String text) {
    for (ClusterStatus b : ClusterStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }

  public static class Adapter extends TypeAdapter<ClusterStatus> {
    @Override
    public void write(final JsonWriter jsonWriter, final ClusterStatus enumeration) throws IOException {
      jsonWriter.value(enumeration.getValue());
    }

    @Override
    public ClusterStatus read(final JsonReader jsonReader) throws IOException {
      String value = jsonReader.nextString();
      return ClusterStatus.fromValue(String.valueOf(value));
    }
  }
}

