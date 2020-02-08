package org.pmiops.workbench.monitoring;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Supplier;
import org.pmiops.workbench.monitoring.MeasurementBundle.Builder;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.springframework.stereotype.Service;

@Service
public class LogsBasedMetricServiceImpl implements LogsBasedMetricService {

  // We don't need to make this name different for each environment, as the
  // MonitoredResource will take care of that.
  private static final String METRICS_LOG_NAME = "debug-logs-based-metrics";
  private Logging logging;
  private StackdriverStatsExporterService stackdriverStatsExporterService;

  /**
   * TODO(jaycarlton) The dependency on the OpenCensus-specific StackdriverStatsExporterService is
   * temporrary until we have a Provider of MonitoredResource.
   */
  public LogsBasedMetricServiceImpl(
      Logging logging, StackdriverStatsExporterService stackdriverStatsExporterService) {
    this.logging = logging;
    this.stackdriverStatsExporterService = stackdriverStatsExporterService;
  }

  @Override
  public void record(MeasurementBundle measurementBundle) {
    final ImmutableSet<LogEntry> logEntries =
        measurementBundleToJsonPayloads(measurementBundle).stream()
            .map(this::payloadToLogEntry)
            .collect(ImmutableSet.toImmutableSet());
    // This list will never be empty because of the validation in the MeasurementBundle builder
    logging.write(logEntries);
  }

  @Override
  public void timeAndRecord(Builder measurementBundleBuilder,
      DistributionMetric distributionMetric, Runnable operation) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    operation.run();
    stopwatch.stop();
    record(
        measurementBundleBuilder
            .addMeasurement(distributionMetric, stopwatch.elapsed().toMillis())
            .build());
  }

  @Override
  public <T> T timeAndRecord(Builder measurementBundleBuilder,
      DistributionMetric distributionMetric, Supplier<T> operation) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final T result = operation.get();
    stopwatch.stop();
    record(
        measurementBundleBuilder
            .addMeasurement(distributionMetric, stopwatch.elapsed().toMillis())
            .build());
    return result;
  }

  /**
   * Each measurement bundle can contain multiple key/value pairs and a constant set of tags. We
   * need to create a separate JsonPayload for each measurement.
   *
   * <p>We drop several important fields from the Metric interface that are useful when talking to
   * OpenCensus and/or the Monitoring API. It's fairly bare bones, and we have to set up aggregation
   * on the Logging console.
   */
  private Set<JsonPayload> measurementBundleToJsonPayloads(MeasurementBundle measurementBundle) {
    final ImmutableMap<String, String> labelToValue =
        measurementBundle.getTags().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    e -> e.getKey().getName(), e -> e.getValue().asString()));

    // Make a JsonPayload for each measurement, and include all the metric labels.
    return measurementBundle.getMeasurements().entrySet().stream()
        .map(
            entry ->
                JsonPayload.of(
                    ImmutableMap.of(
                        MetricJsonKey.VALUE.getKeyName(), entry.getValue().doubleValue(),
                        MetricJsonKey.NAME.getKeyName(), entry.getKey().getName(),
                        MetricJsonKey.UNIT.getKeyName(), entry.getKey().getUnit(),
                        MetricJsonKey.LABELS.getKeyName(), labelToValue)))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * The LogEntry structure should only vary by payload within a single running of the application.
   * The MonitoredResource should not change during execution.
   *
   * @param jsonPayload - A JsonPayload structure with the three keys in LogsBasedMetricService.
   * @return
   */
  private LogEntry payloadToLogEntry(JsonPayload jsonPayload) {
    return LogEntry.newBuilder(jsonPayload)
        .setSeverity(Severity.INFO)
        .setLogName(METRICS_LOG_NAME)
        .setResource(stackdriverStatsExporterService.getLoggingMonitoredResource())
        .build();
  }
}
