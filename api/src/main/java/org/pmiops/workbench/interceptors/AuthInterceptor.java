package org.pmiops.workbench.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.pmiops.workbench.db.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Authority;


/**
 * Intercepts all non-OPTIONS API requests to ensure they have an appropriate auth token.
 *
 * Checks handler methods for annotations like
 *     @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
 * to enforce granular permissions.
 */
@Service
public class AuthInterceptor extends HandlerInterceptorAdapter {
  private static final Logger log = Logger.getLogger(AuthInterceptor.class.getName());
  private static final String authName = "aou_oauth";

  private final UserInfoService userInfoService;
  private final Provider<User> userProvider;
  private final UserDao userDao;

  @Autowired
  public AuthInterceptor(UserInfoService userInfoService, Provider<User> userProvider,
      UserDao userDao) {
    this.userInfoService = userInfoService;
    // Note that this provider isn't usable until after we publish the security context below.
    this.userProvider = userProvider;
    this.userDao = userDao;
  }

  /**
   * Returns true iff the request is auth'd and should proceed. Publishes authenticated user info
   * using Spring's SecurityContext.
   * @param handler The Swagger-generated ApiController. It contains our handler as a private
   *     delegate.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // OPTIONS methods requests don't need authorization.
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }

    HandlerMethod method = (HandlerMethod) handler;

    boolean isAuthRequired = false;
    ApiOperation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), ApiOperation.class);
    if (apiOp != null) {
      for (Authorization auth : apiOp.authorizations()) {
        if (auth.value().equals(authName)) {
          isAuthRequired = true;
          break;
        }
      }
    }
    if (!isAuthRequired) {
      return true;
    }

    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.warning("No bearer token found in request");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    String token = authorizationHeader.substring("Bearer".length()).trim();
    Userinfoplus userInfo;
    try {
      userInfo = userInfoService.getUserInfo(token);
    } catch (HttpResponseException e) {
      log.log(Level.WARNING,
          "{0} response getting user info for bearer token {1}: {2}",
          new Object[] { e.getStatusCode(), token, e.getStatusMessage() });
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    // TODO: get token info and check that as well

    // TODO: check Google group membership to ensure user is in registered user group

    SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(userInfo, token));

    // TODO: setup this in the context, get rid of log statement
    log.log(Level.INFO, "{0} logged in", userInfo.getEmail());

    if (!hasRequiredAuthority(method.getMethod(), userProvider.get())) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    return true;
  }

  /**
   * Checks any @AuthorityRequired annotation on the controller's handler method.
   *
   * There is a hierarchy of Swagger-generated interfaces/wrappers around our controllers:
   *     FooApi (interface generated by Swagger)
   *     FooApiController (generated by Swagger, handed to AuthInterceptor)
   *       private FooApiDelegate delegate;
   *     FooApiDelegate (interface generated by Swagger)
   *     FooController implements FooApiDelegate (we implement this)
   * We can only annotate FooController methods, but are given a FooApiController, so we use
   * reflection to hack our way to FooController's method.
   *
   * @param method The ApiController (Swagger-generated) method which calls our annotated delegate.
   * @param user Database details of the authenticated user.
   */
  boolean hasRequiredAuthority(Method apiControllerMethod, User user) {
    // There's no concise way to find out what class implements the delegate interface, so instead
    // depend on naming conventions. Essentially, this removes "Api" from the class name.
    // If this becomes a bottleneck, consider caching the class mapping, or copying annotations
    // from our implementation to the Swagger wrapper at startup (locally it takes <1ms).
    Pattern apiControllerPattern = Pattern.compile("(.*\\.[^.]+)Api(Controller)");
    String apiControllerName = apiControllerMethod.getDeclaringClass().getName();
    String controllerName = apiControllerPattern.matcher(apiControllerName).replaceAll("$1$2");
    Class controllerClass;
    try {
      controllerClass = Class.forName(controllerName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "Missing " + controllerName + " by name derived from " + apiControllerName + ". "
          + " Cannot check @AuthorityRequired.",
          e);
    }

    Method controllerMethod;
    try {
      controllerMethod = controllerClass.getMethod(
          apiControllerMethod.getName(), apiControllerMethod.getParameterTypes());
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    String controllerMethodName =
        controllerMethod.getDeclaringClass().getName() + "." + controllerMethod.getName();

    AuthorityRequired req = controllerMethod.getAnnotation(AuthorityRequired.class);

    if (req != null) {
      // Fetch the user with authorities, since they aren't loaded during normal
      user = userDao.findUserWithAuthorities(user.getUserId());
      Collection<Authority> granted = user.getAuthorities();
      if (granted.containsAll(Arrays.asList(req.value()))) {
        return true;
      } else {
        log.log(
            Level.INFO,
            "{0} required authorities {1} but user had only {2}.",
            new Object[] {
                controllerMethodName,
                Arrays.toString(req.value()),
                Arrays.toString(granted.toArray())});
        return false;
      }
    }
    return true;  // No @AuthorityRequired annotation found at runtime, default to allowed.
  }
}
