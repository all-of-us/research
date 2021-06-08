package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.monitoring.views.Metric;

public class MeasurementBundleTest extends SpringTest {

  private static final long USER_COUNT = 1000L;

  @Test
  public void testBuild_singleMeasurementBundle() {
    final MeasurementBundle bundle =
        MeasurementBundle.builder().addMeasurement(GaugeMetric.USER_COUNT, USER_COUNT).build();

    assertThat(bundle.getMeasurements()).hasSize(1);
    assertThat(bundle.getMeasurements().get(GaugeMetric.USER_COUNT)).isEqualTo(USER_COUNT);
    assertThat(bundle.getTags()).isEmpty();
  }

  @Test
  public void testBuild_multipleMeasurementsNoAttachments() {
    final ImmutableMap<Metric, Number> measurementMap =
        ImmutableMap.of(
            GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 202L,
            GaugeMetric.COHORT_COUNT, 300L,
            GaugeMetric.USER_COUNT, USER_COUNT);
    final MeasurementBundle bundle =
        MeasurementBundle.builder().addAllMeasurements(measurementMap).build();
    assertThat(bundle.getMeasurements()).hasSize(measurementMap.size());
    assertThat(bundle.getMeasurements().get(GaugeMetric.USER_COUNT)).isEqualTo(USER_COUNT);
    assertThat(bundle.getTags()).isEmpty();
  }

  @Test
  public void testBuild_missingMeasurementsThrows() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          MeasurementBundle.builder()
              .addTag(MetricLabel.USER_DISABLED, Boolean.valueOf(true).toString())
              .build();
        });
  }

  @Test
  public void testBuild_incompatibleMetricAndAttachmentThrows() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          MeasurementBundle.builder()
              .addMeasurement(GaugeMetric.COHORT_COUNT, 101L)
              .addMeasurement(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 202L)
              .addTag(MetricLabel.BUFFER_ENTRY_STATUS, BufferEntryStatus.AVAILABLE.toString())
              .build();
        });
  }

  @Test
  public void testBuild_unsupportedAttachmentValueThrows() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          MeasurementBundle.builder()
              .addTag(MetricLabel.BUFFER_ENTRY_STATUS, "lost and gone forever")
              .build();
        });
  }

  @Test
  public void testGetTagValue() {
    final MeasurementBundle measurementBundle =
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 101L)
            .addTag(MetricLabel.BUFFER_ENTRY_STATUS, BufferEntryStatus.AVAILABLE.toString())
            .build();

    final Optional<String> labelValue =
        measurementBundle.getTagValue(MetricLabel.BUFFER_ENTRY_STATUS);
    assertThat(labelValue.isPresent()).isTrue();
    assertThat(labelValue.orElse("wrong")).isEqualTo(BufferEntryStatus.AVAILABLE.toString());

    final Optional<String> missingValue = measurementBundle.getTagValue(MetricLabel.METHOD_NAME);
    assertThat(missingValue.isPresent()).isFalse();
  }
}
