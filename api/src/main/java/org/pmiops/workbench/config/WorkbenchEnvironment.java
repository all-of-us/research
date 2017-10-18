package org.pmiops.workbench.config;

import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;

public class WorkbenchEnvironment {

  private final boolean isDevelopment;
  private final String applicationId;


  public WorkbenchEnvironment() {
    this(SystemProperty.environment.value().equals(Value.Development),
         SystemProperty.applicationId.get());
  }

  public WorkbenchEnvironment(boolean isDevelopment, String applicationId) {
    this.isDevelopment = isDevelopment;
    this.applicationId = applicationId;
  }

  public boolean isDevelopment() {
    return isDevelopment;
  }

  public String getApplicationId() {
    return applicationId;
  }
}
