package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * levels of access to data in the curated data repository
 */
public enum DataAccessLevel {
  
  UNREGISTERED("unregistered"),
  
  REGISTERED("registered"),
  
  PROTECTED("protected");

  private String value;

  DataAccessLevel(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static DataAccessLevel fromValue(String text) {
    for (DataAccessLevel b : DataAccessLevel.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

