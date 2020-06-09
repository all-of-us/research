package org.pmiops.workbench.utils;

import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

/** Utility class for working with FieldValueLists, FieldValues, and Fields */
public final class FieldValues {

  public static final int MICROSECONDS_IN_MILLISECOND = 1000;

  private FieldValues() {}

  /** Return an Optional<FieldValue> which is empty if the value is null and present if not. */
  public static Optional<FieldValue> getValue(FieldValueList row, int index) {
    final FieldValue value = row.get(index);
    if (value.isNull()) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }

  /** Return an Optional<FieldValue> which is empty if the value is null and present if not. */
  public static Optional<FieldValue> getValue(FieldValueList row, String fieldName) {
    final FieldValue value = row.get(fieldName);
    if (value.isNull()) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }

  public static Optional<Boolean> getBoolean(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getBooleanValue);
  }

  public static Optional<Boolean> getBoolean(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getBooleanValue);
  }

  public static Optional<byte[]> getBytes(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getBytesValue);
  }

  public static Optional<byte[]> getBytes(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getBytesValue);
  }

  public static Optional<Double> getDouble(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getDoubleValue);
  }

  public static Optional<Double> getDouble(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getDoubleValue);
  }

  public static Optional<Long> getLong(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getLongValue);
  }

  public static Optional<Long> getLong(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getLongValue);
  }

  public static Optional<BigDecimal> getNumeric(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getNumericValue);
  }

  public static Optional<BigDecimal> getNumeric(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getNumericValue);
  }

  public static Optional<FieldValueList> getRecord(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getRecordValue);
  }

  public static Optional<FieldValueList> getRecord(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getRecordValue);
  }

  public static Optional<List<FieldValue>> getRepeated(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getRepeatedValue);
  }

  public static Optional<List<FieldValue>> getRepeated(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getRepeatedValue);
  }

  public static Optional<String> getString(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getStringValue);
  }

  public static Optional<String> getString(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getStringValue);
  }

  public static Optional<Long> getTimestampMicroseconds(FieldValueList row, int index) {
    return FieldValues.getValue(row, index).map(FieldValue::getTimestampValue);
  }

  public static Optional<Long> getTimestampMicroseconds(FieldValueList row, String fieldName) {
    return FieldValues.getValue(row, fieldName).map(FieldValue::getTimestampValue);
  }

  public static Optional<DateTime> getDateTime(FieldValueList row, int index) {
    return getTimestampMicroseconds(row, index)
        .map(FieldValues::microsecondsToMillis)
        .map(DateTime::new);
  }

  public static Optional<DateTime> getDateTime(FieldValueList row, String fieldName) {
    return getTimestampMicroseconds(row, fieldName)
        .map(FieldValues::microsecondsToMillis)
        .map(DateTime::new);
  }

  @VisibleForTesting
  public static FieldValueList buildFieldValueList(FieldList schemaFieldList, List<Object> values) {
    return FieldValueList.of(
        values.stream()
            .map(value -> FieldValue.of(Attribute.PRIMITIVE, value))
            .collect(Collectors.toList()),
        schemaFieldList);
  }

  @NotNull
  private static long microsecondsToMillis(long microseconds) {
    return microseconds / MICROSECONDS_IN_MILLISECOND;
  }
}
