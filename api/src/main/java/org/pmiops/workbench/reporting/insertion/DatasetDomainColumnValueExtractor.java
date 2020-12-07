package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.int64;
import static com.google.cloud.bigquery.QueryParameterValue.string;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;

public enum DatasetDomainColumnValueExtractor
    implements ColumnValueExtractor<ReportingDatasetDomainIdValue> {
  DATASET_ID(
      "dataset_id", ReportingDatasetDomainIdValue::getDatasetId),
  DOMAIN_ID("domain_id", ReportingDatasetDomainIdValue::getDomainId),
  VALUE("value", ReportingDatasetDomainIdValue::getValue);

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private static final String TABLE_NAME = "dataset_domain_value";
  private final String parameterName;
  private final Function<ReportingDatasetDomainIdValue, Object> objectValueFunction;

  DatasetDomainColumnValueExtractor(
      String parameterName,
      Function<ReportingDatasetDomainIdValue, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getBigQueryTableName() {
    return TABLE_NAME;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingDatasetDomainIdValue, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
