package org.pmiops.workbench.config;

import com.google.api.client.http.HttpMethods;
import com.google.api.services.oauth2.model.Userinfoplus;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
