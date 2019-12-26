package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * a survey for concepts
 */
public enum Surveys {
  
  THE_BASICS("THE BASICS"),
  
  LIFESTYLE("LIFESTYLE"),
  
  OVERALL_HEALTH("OVERALL HEALTH");

  private String value;

  Surveys(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Surveys fromValue(String text) {
    for (Surveys b : Surveys.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

