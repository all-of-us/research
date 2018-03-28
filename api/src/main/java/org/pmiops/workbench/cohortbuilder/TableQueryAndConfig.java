package org.pmiops.workbench.cohortbuilder;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.pmiops.workbench.config.CdrSchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrSchemaConfig.TableConfig;
import org.pmiops.workbench.model.TableQuery;

public class TableQueryAndConfig {

  private final TableQuery tableQuery;
  private final TableConfig tableConfig;
  private final Map<String, ColumnConfig> columnMap;

  public TableQueryAndConfig(TableQuery tableQuery, TableConfig tableConfig,
      Map<String, ColumnConfig> columnMap) {
    this.tableQuery = tableQuery;
    this.tableConfig = tableConfig;
    this.columnMap = columnMap;
  }

  public TableQuery getTableQuery() {
    return tableQuery;
  }

  public TableConfig getTableConfig() {
    return tableConfig;
  }

  public ColumnConfig getColumn(String columnName) {
    return columnMap.get(columnName);
  }
}
