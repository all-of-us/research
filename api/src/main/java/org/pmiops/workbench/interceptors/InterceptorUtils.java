package org.pmiops.workbench.interceptors;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.web.method.HandlerMethod;

public class InterceptorUtils {

  private static Map<String, String> implMap = ImmutableMap.of(
      "org.pmiops.workbench.api.WorkspacesApiController", "org.pmiops.workbench.workspaces.WorkspacesController"
  );

  private InterceptorUtils() {}

  public static Method getControllerMethod(HandlerMethod handlerMethod) {
    // There's no concise way to find out what class implements the delegate interface, so instead
    // depend on naming conventions. Essentially, this removes "Api" from the class name.
    // If this becomes a bottleneck, consider caching the class mapping, or copying annotations
    // from our implementation to the Swagger wrapper at startup (locally it takes <1ms).
    Pattern apiControllerPattern = Pattern.compile("(.*\\.[^.]+)Api(Controller)");
    Method apiControllerMethod = handlerMethod.getMethod();
    String apiControllerName = apiControllerMethod.getDeclaringClass().getName();

    String controllerName;
    if (implMap.containsKey(apiControllerName)) {
      controllerName = implMap.get(apiControllerName);
    } else {
      controllerName = apiControllerPattern.matcher(apiControllerName).replaceAll("$1$2");
    }

    Class<?> controllerClass;
    try {
      controllerClass = Class.forName(controllerName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "Missing " + controllerName + " by name derived from " + apiControllerName + ".", e);
    }

    try {
      return controllerClass.getMethod(
          apiControllerMethod.getName(), apiControllerMethod.getParameterTypes());
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
