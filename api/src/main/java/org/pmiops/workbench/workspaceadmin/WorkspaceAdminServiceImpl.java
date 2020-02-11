package org.pmiops.workbench.workspaceadmin;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.storage.BlobInfo;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {
  private static final String SELECT_ALL_WORKSPACE_AUDIT_EVENTS = "SELECT * "
      + "FROM `all-of-us-workbench-test.workbench_action_audit_test.workbench_action_audit_test` "
      + "WHERE "
      + "  (jsonPayload.target_type='WORKSPACE' AND "
      + "   jsonPayload.target_id=@workspaceId) "
      + "ORDER BY timestamp DESC "
      + "LIMIT 500;";

  private final BigQueryService bigQueryService;
  private final CloudStorageService cloudStorageService;
  private final CohortDao cohortDao;
  private final ConceptSetDao conceptSetDao;
  private final DataSetDao dataSetDao;
  private final FireCloudService fireCloudService;
  private final NotebooksService notebooksService;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public WorkspaceAdminServiceImpl(
      BigQueryService bigQueryService,
      CloudStorageService cloudStorageService,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      FireCloudService fireCloudService,
      NotebooksService notebooksService,
      WorkspaceDao workspaceDao) {
    this.bigQueryService = bigQueryService;
    this.cloudStorageService = cloudStorageService;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
    this.workspaceDao = workspaceDao;
  }

  @Override
  /** Returns the first workspace found for any given namespace. */
  public Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace) {
    return workspaceDao.findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(workspaceNamespace);
  }

  @Override
  public AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId) {
    int cohortCount = cohortDao.countByWorkspaceId(workspaceId);
    int conceptSetCount = conceptSetDao.countByWorkspaceId(workspaceId);
    int dataSetCount = dataSetDao.countByWorkspaceId(workspaceId);
    return new AdminWorkspaceObjectsCounts()
        .cohortCount(cohortCount)
        .conceptSetCount(conceptSetCount)
        .datasetCount(dataSetCount);
  }

  @Override
  public AdminWorkspaceCloudStorageCounts getAdminWorkspaceCloudStorageCounts(
      String workspaceNamespace, String workspaceName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    int notebookFilesCount =
        notebooksService.getNotebooks(workspaceNamespace, workspaceName).size();
    int nonNotebookFilesCount = getNonNotebookFileCount(bucketName);
    long storageSizeBytes = getStorageSizeBytes(bucketName);

    return new AdminWorkspaceCloudStorageCounts()
        .notebookFileCount(notebookFilesCount)
        .nonNotebookFileCount(nonNotebookFilesCount)
        .storageBytesUsed(storageSizeBytes);
  }

  private int getNonNotebookFileCount(String bucketName) {
    return cloudStorageService
        .getBlobListForPrefix(bucketName, NotebooksService.NOTEBOOKS_WORKSPACE_DIRECTORY).stream()
        .filter(blob -> !NotebooksService.NOTEBOOK_PATTERN.matcher(blob.getName()).matches())
        .collect(Collectors.toList())
        .size();
  }

  private long getStorageSizeBytes(String bucketName) {
    return cloudStorageService.getBlobList(bucketName).stream()
        .map(BlobInfo::getSize)
        .reduce(0L, Long::sum);
  }

  @Override
  public void getWorkspaceAuditEvents(long workspaceId) {
    final ImmutableMap<String, QueryParameterValue> queryNamesToValues = ImmutableMap.of(
        "workspaceId", QueryParameterValue.int64(workspaceId)
    );

    final String query = SELECT_ALL_WORKSPACE_AUDIT_EVENTS.replace("@workspaceId", Long.toString(workspaceId));
    final QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration.newBuilder(query)
            .setNamedParameters(queryNamesToValues)
            .setUseLegacySql(false)
            .build();
    final TableResult workspaceAuditEventsTablesResult = bigQueryService.executeQuery(queryJobConfiguration);
    System.err.println(workspaceAuditEventsTablesResult);
  }
}
