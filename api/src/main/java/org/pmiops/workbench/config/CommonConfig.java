package org.pmiops.workbench.config;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {
  @Bean
  JsonFactory jsonFactory() {
    return new JacksonFactory();
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  Random random() {
    return new SecureRandom();
  }

  @Bean
  Javers javers() {
    return JaversBuilder.javers().build();
  }
}
