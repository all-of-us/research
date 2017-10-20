package org.pmiops.workbench.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpMethods;
import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletContext;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.google.Utils;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@EnableWebMvc
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

  @Autowired
  private AuthInterceptor authInterceptor;

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public UserAuthentication userAuthentication() {
    return (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Userinfoplus userInfo(UserAuthentication userAuthentication) {
    return userAuthentication.getPrincipal();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public User user(Userinfoplus userInfo, UserDao userDao) {
    return userDao.findUserByEmail(userInfo.getEmail());
  }

  @Bean
  public WorkbenchEnvironment workbenchEnvironment() {
    return new WorkbenchEnvironment();
  }

  /**
   * Service account credentials for the AofU server. These are derived from a key JSON file
   * copied from GCS deployed to /WEB-INF/sa-key.json during the build step. They can be used
   * to make API calls on behalf of AofU (as opposed to using end user credentials.)
   *
   * We may in future rotate key files in production, but will be sure to keep the ones currently
   * in use in cloud environments working when that happens.
   */
  @Lazy
  @Bean
  public GoogleCredential serviceAccountCredential() {
    ServletContext context = Utils.getRequestServletContext();
    InputStream saFileAsStream = context.getResourceAsStream("/WEB-INF/sa-key.json");
    GoogleCredential credential = null;
    try {
      return GoogleCredential.fromStream(saFileAsStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        // TODO: change this to be just the domains appropriate for the environment.
        .allowedOrigins("*")
        .allowedMethods(HttpMethods.GET, HttpMethods.HEAD, HttpMethods.POST, HttpMethods.PUT,
            HttpMethods.DELETE, HttpMethods.PATCH, HttpMethods.TRACE, HttpMethods.OPTIONS)
        .allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.AUTHORIZATION,
            "X-Requested-With", "requestId", "Correlation-Id");
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor);
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }
}
