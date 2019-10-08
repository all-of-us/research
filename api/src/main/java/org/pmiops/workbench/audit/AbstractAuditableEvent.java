package org.pmiops.workbench.audit;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractAuditableEvent {
  private static final MonitoredResource MONITORED_RESOURCE = MonitoredResource.newBuilder("global").build();
  private static final String LOG_NAME = "action-audit-test-3";

  public abstract long timestamp();

  public abstract String actionId();

  public abstract AgentType agentType();

  public abstract long agentId();

  public abstract Optional<String> agentEmail();

  public abstract ActionType actionType();

  public abstract TargetType targetType();

  public abstract Optional<Long> targetId();

  public abstract Optional<String> targetProperty();

  public abstract Optional<String> previousValue();

  public abstract Optional<String> newValue();

  private JsonPayload toJsonPayload() {
    Map<String, Object> result = new HashMap<>(); // allow null values
    result.put(AuditColumn.TIMESTAMP.name(), timestamp());
    result.put(AuditColumn.ACTION_ID.name(), actionId());
    result.put(AuditColumn.ACTION_TYPE.name(), actionType());
    result.put(AuditColumn.AGENT_TYPE.name(), agentType());
    result.put(AuditColumn.AGENT_ID.name(), agentId());
    result.put(AuditColumn.AGENT_EMAIL.name(), toNullable(agentEmail()));
    result.put(AuditColumn.TARGET_TYPE.name(), targetType());
    result.put(AuditColumn.TARGET_ID.name(), toNullable(targetId()));
    result.put(AuditColumn.AGENT_ID.name(), agentId());
    result.put(AuditColumn.TARGET_PROPERTY.name(), toNullable(targetProperty()));
    result.put(AuditColumn.PREV_VALUE.name(), toNullable(previousValue()));
    result.put(AuditColumn.NEW_VALUE.name(), toNullable(newValue()));
    return JsonPayload.of(result);
  }

  public LogEntry toLogEntry() {

    return LogEntry.newBuilder(toJsonPayload())
        .setSeverity(Severity.INFO)
        .setLogName(LOG_NAME)
        .setResource(MONITORED_RESOURCE)
        .build();
  }

  // Inverse of Optional.ofNullable(). Used for JSON api which expects null values for empty
  // columns. Though perhaps we could just leave those out...
  private static <T> T toNullable(Optional<T> opt) {
    return opt.orElse(null);
  }
}
