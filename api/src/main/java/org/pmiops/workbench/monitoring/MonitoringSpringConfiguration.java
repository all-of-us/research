package org.pmiops.workbench.monitoring;

import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitoringSpringConfiguration {

  @Bean
  public ViewManager getViewManager() {
    return Stats.getViewManager();
  }

  @Bean
  public StatsRecorder getStatsRecorder() {
    return Stats.getStatsRecorder();
  }

  @Bean
  public Tagger getTagger() {
    return Tags.getTagger();
  }
}
