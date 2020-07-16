package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.util.BQParameterUtil;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.utils.mappers.AuditLogEntryMapper;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private static final long MAX_QUERY_LIMIT = 1000L;
  private static final String QUERY_FORMAT =
      "SELECT\n"
          + "  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) as event_time,\n"
          + "  jsonPayload.agent_type AS agent_type,\n"
          + "  CAST(jsonPayload.agent_id AS INT64) AS agent_id,\n"
          + "  jsonPayload.agent_email AS agent_username,\n"
          + "  jsonPayload.action_id AS action_id,\n"
          + "  jsonPayload.action_type AS action_type,\n"
          + "  jsonPayload.target_type AS target_type,\n"
          + "  CAST(jsonPayload.target_id AS INT64) AS target_id,\n"
          + "  jsonPayload.target_property AS target_property,\n"
          + "  jsonPayload.prev_value AS prev_value,\n"
          + "  jsonPayload.new_value AS new_value\n"
          + "FROM %s\n"
          + "WHERE %s\n"
          + "  AND @after <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64))\n"
          + "  AND TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) < @before\n"
          + "  AND @after_partition_time <= _PARTITIONTIME\n"
          + "  AND _PARTITIONTIME < @before_partition_time\n"
          + "ORDER BY event_time, agent_id, action_id\n"
          + "LIMIT @limit;";

  private final AuditLogEntryMapper auditLogEntryMapper;
  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private static final Duration PARTITION_BUFFER = Duration.ofDays(1);

  public ActionAuditQueryServiceImpl(
      AuditLogEntryMapper auditLogEntryMapper,
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.auditLogEntryMapper = auditLogEntryMapper;
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public WorkspaceAuditLogQueryResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, Instant after, Instant before) {
    final String whereClausePrefix =
        "jsonPayload.target_id = @workspace_db_id AND\n"
            + "  jsonPayload.target_type = 'WORKSPACE'\n";
    final String queryString = String.format(QUERY_FORMAT, getTableName(), whereClausePrefix);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(
                getNamedParameterMapBuilder(limit, after, before)
                    .put("workspace_db_id", QueryParameterValue.int64(workspaceDatabaseId))
                    .build())
            .build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);

    final List<AuditLogEntry> logEntries = auditLogEntryMapper.tableResultToLogEntries(tableResult);
    final String queryHeader =
        String.format(
            "Audit trail for workspace DB ID %d\nafter %s and before %s",
            workspaceDatabaseId, after.toString(), before.toString());

    return new WorkspaceAuditLogQueryResponse()
        .logEntries(logEntries)
        .query(BQParameterUtil.replaceNamedParameters(queryJobConfiguration, queryHeader))
        .workspaceDatabaseId(workspaceDatabaseId)
        .actions(auditLogEntryMapper.logEntriesToActions(logEntries));
  }

  private String getTableName() {
    final ActionAuditConfig actionAuditConfig = workbenchConfigProvider.get().actionAudit;
    return String.format(
        "`%s.%s.%s`",
        workbenchConfigProvider.get().server.projectId,
        actionAuditConfig.bigQueryDataset,
        actionAuditConfig.bigQueryTable);
  }

  @Override
  public UserAuditLogQueryResponse queryEventsForUser(
      long userDatabaseId, long limit, Instant after, Instant before) {

    final String whereClausePrefix =
        "((jsonPayload.target_id = @user_db_id AND jsonPayload.target_type = 'USER') OR\n"
            + "  (jsonPayload.agent_id = @user_db_id AND jsonPayload.agent_type = 'USER'))";
    final String queryString = String.format(QUERY_FORMAT, getTableName(), whereClausePrefix);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(
                getNamedParameterMapBuilder(limit, after, before)
                    .put("user_db_id", QueryParameterValue.int64(userDatabaseId))
                    .build())
            .build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);

    final List<AuditLogEntry> logEntries = auditLogEntryMapper.tableResultToLogEntries(tableResult);
    final String queryHeader =
        String.format(
            "Audit trail for user DB ID %d\nafter %s and before %s",
            userDatabaseId, after.toString(), before.toString());
    final String formattedQuery =
        BQParameterUtil.formatQuery(
            BQParameterUtil.replaceNamedParameters(queryJobConfiguration, queryHeader));

    return new UserAuditLogQueryResponse()
        .actions(auditLogEntryMapper.logEntriesToActions(logEntries))
        .logEntries(logEntries)
        .query(formattedQuery)
        .userDatabaseId(userDatabaseId);
  }

  private ImmutableMap.Builder<String, QueryParameterValue> getNamedParameterMapBuilder(
      long limit, Instant after, Instant before) {
    final Instant afterPartitionTime = after.minus(PARTITION_BUFFER);
    final Instant beforePartitionTime = before.plus(PARTITION_BUFFER);

    return ImmutableMap.<String, QueryParameterValue>builder()
        .put("limit", QueryParameterValue.int64(Math.max(limit, MAX_QUERY_LIMIT)))
        .put("after", BQParameterUtil.instantToQPValue(after))
        .put("before", BQParameterUtil.instantToQPValue(before))
        .put("after_partition_time", BQParameterUtil.instantToQPValue(afterPartitionTime))
        .put("before_partition_time", BQParameterUtil.instantToQPValue(beforePartitionTime));
  }
}
