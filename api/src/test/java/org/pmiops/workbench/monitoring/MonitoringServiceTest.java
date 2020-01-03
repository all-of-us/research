package org.pmiops.workbench.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.monitoring.views.OpenCensusView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MonitoringServiceTest {

  @Mock private MeasureMap mockMeasureMap; // not injected
  // TODO(jaycarlton): figure out why IntelliJ isn't copacetic with this
  // annotation but things still work.
  @Autowired private StackdriverStatsExporterService mockInitService;
  @Autowired private ViewManager mockViewManager;
  @Autowired private StatsRecorder mockStatsRecorder;
  @Autowired private MonitoringService monitoringService;

  @TestConfiguration
  @Import({MonitoringServiceImpl.class})
  @MockBean({ViewManager.class, StatsRecorder.class, StackdriverStatsExporterService.class})
  static class Configuration {
    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      // Nothing should show up in any Metrics backend for these tests.
      workbenchConfig.server = new ServerConfig();
      workbenchConfig.server.shortName = "unit-test";
      workbenchConfig.server.projectId = "fake-project";
      return workbenchConfig;
    }
  }

  @Before
  public void setup() {
    initMockMeasureMap();
  }

  // We require that the measure map exists when created, and returns itself after any entry
  // is added, but we won't be using the result directly.
  private void initMockMeasureMap() {
    doReturn(mockMeasureMap).when(mockStatsRecorder).newMeasureMap();
    doReturn(mockMeasureMap).when(mockMeasureMap).put(any(MeasureLong.class), anyLong());
    doReturn(mockMeasureMap).when(mockMeasureMap).put(any(MeasureDouble.class), anyDouble());
  }

  @Test
  public void testRecordIncrement() {
    monitoringService.recordEvent(EventMetric.NOTEBOOK_SAVE);

    verify(mockInitService).createAndRegister();
    verify(mockViewManager, times(GaugeMetric.values().length)).registerView(any(View.class));
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap).put(EventMetric.NOTEBOOK_SAVE.getMeasureLong(), 1L);
    verify(mockMeasureMap).record();
  }

  @Test
  public void testRecordValue() {
    long value = 16L;
    monitoringService.recordValue(GaugeMetric.BILLING_BUFFER_CREATING_PROJECT_COUNT, value);

    verify(mockInitService).createAndRegister();
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap)
        .put(GaugeMetric.BILLING_BUFFER_CREATING_PROJECT_COUNT.getMeasureLong(), value);
    verify(mockMeasureMap).record();
  }

  @Test
  public void testRecordMap() {
    ImmutableMap.Builder<OpenCensusView, Number> signalToValueBuilder = ImmutableMap.builder();
    signalToValueBuilder.put(GaugeMetric.BILLING_BUFFER_SIZE, 99L);
    signalToValueBuilder.put(GaugeMetric.BILLING_BUFFER_CREATING_PROJECT_COUNT, 2L);
    signalToValueBuilder.put(GaugeMetric.DEBUG_RANDOM_DOUBLE, 3.14);

    monitoringService.recordValues(signalToValueBuilder.build());
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap, times(2)).put(any(MeasureLong.class), anyLong());
    verify(mockMeasureMap, times(1)).put(GaugeMetric.DEBUG_RANDOM_DOUBLE.getMeasureDouble(), 3.14);
    verify(mockMeasureMap).record();
  }

  @Test
  public void testRecordValue_noOpOnEmptyMap() {
    monitoringService.recordValues(Collections.emptyMap());
    verify(mockInitService).createAndRegister();
    verifyZeroInteractions(mockStatsRecorder);
    verifyZeroInteractions(mockMeasureMap);
  }
}
