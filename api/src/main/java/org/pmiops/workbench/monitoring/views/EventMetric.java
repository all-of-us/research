package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.monitoring.labels.MetricLabel;

/**
 * Metric enum values for events to be counted. The value associated with count metrics is thus 1.
 */
public enum EventMetric implements Metric {
  NOTEBOOK_CLONE("notebook_clone_2", "Clone (duplicate) a notebook"),
  NOTEBOOK_DELETE("notebook_delete_2", "Delete a notebook"),
  NOTEBOOK_SAVE("notebook_save_2", "Save (or create) a notebook");

  private final String name;
  private final String description;
  private final List<MetricLabel> labels;

  EventMetric(String name, String description) {
    this(name, description, Collections.emptyList());
  }

  EventMetric(String name, String description, List<MetricLabel> labels) {
    this.name = name;
    this.description = description;
    this.labels = labels;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getUnit() {
    return UnitOfMeasure.COUNT.getUcmSymbol();
  }

  @Override
  public Class getMeasureClass() {
    return MeasureLong.class;
  }

  @Override
  public Aggregation getAggregation() {
    return Aggregation.Count.create();
  }

  @Override
  public List<MetricLabel> getLabels() {
    return labels;
  }
}
