package org.pmiops.workbench.api;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Provider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.dataset.DataSetMapper;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetCodeResponse;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetListResponse;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.DomainValuesResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.MarkDataSetRequest;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSetController implements DataSetApiDelegate {

  private BigQueryService bigQueryService;
  private final Clock clock;
  private DataSetService dataSetService;

  private Provider<DbUser> userProvider;
  private Provider<String> prefixProvider;
  private final WorkspaceService workspaceService;

  // See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
  private static long APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC = 55000l;

  private static final String DATE_FORMAT_STRING = "yyyy/MM/dd HH:mm:ss";
  public static final String EMPTY_CELL_MARKER = "";

  private static final Logger log = Logger.getLogger(DataSetController.class.getName());

  private final CdrVersionService cdrVersionService;
  private final ConceptSetService conceptSetService;
  private final DataDictionaryEntryDao dataDictionaryEntryDao;
  private final DataSetMapper dataSetMapper;
  private final FireCloudService fireCloudService;
  private final NotebooksService notebooksService;
  private final CohortService cohortService;

  @Autowired
  DataSetController(
      BigQueryService bigQueryService,
      Clock clock,
      CohortService cohortService,
      CdrVersionService cdrVersionService,
      ConceptSetService conceptSetService,
      DataDictionaryEntryDao dataDictionaryEntryDao,
      DataSetMapper dataSetMapper,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      NotebooksService notebooksService,
      Provider<DbUser> userProvider,
      @Qualifier(DatasetConfig.DATASET_PREFIX_CODE) Provider<String> prefixProvider,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.clock = clock;
    this.cohortService = cohortService;
    this.cdrVersionService = cdrVersionService;
    this.conceptSetService = conceptSetService;
    this.dataDictionaryEntryDao = dataDictionaryEntryDao;
    this.dataSetMapper = dataSetMapper;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.prefixProvider = prefixProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<DataSet> createDataSet(
      String workspaceNamespace, String workspaceFirecloudName, DataSetRequest dataSetRequest) {
    validateDataSetCreateRequest(dataSetRequest);
    final Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceFirecloudName, WorkspaceAccessLevel.WRITER);
    final long workspaceId =
        workspaceService.get(workspaceNamespace, workspaceFirecloudName).getWorkspaceId();
    final ImmutableList<DbDatasetValue> dataSetValueList =
        dataSetRequest.getDomainValuePairs().stream()
            .map(this::getDataSetValuesFromDomainValueSet)
            .collect(toImmutableList());
    DbDataset savedDataSet =
        dataSetService.saveDataSet(
            dataSetRequest.getName(),
            dataSetRequest.getIncludesAllParticipants(),
            dataSetRequest.getDescription(),
            workspaceId,
            dataSetRequest.getCohortIds(),
            dataSetRequest.getConceptSetIds(),
            dataSetValueList,
            dataSetRequest.getPrePackagedConceptSet(),
            userProvider.get().getUserId(),
            now);
    return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(savedDataSet));
  }

  private DbDatasetValue getDataSetValuesFromDomainValueSet(DomainValuePair domainValuePair) {
    return new DbDatasetValue(
        DbStorageEnums.domainToStorage(domainValuePair.getDomain()).toString(),
        domainValuePair.getValue());
  }

  private void validateDataSetCreateRequest(DataSetRequest dataSetRequest) {
    boolean includesAllParticipants =
        Optional.ofNullable(dataSetRequest.getIncludesAllParticipants()).orElse(false);
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (dataSetRequest.getConceptSetIds() == null
        || (dataSetRequest.getConceptSetIds().isEmpty()
            && dataSetRequest.getPrePackagedConceptSet().equals(PrePackagedConceptSetEnum.NONE))) {
      throw new BadRequestException("Missing concept set ids");
    } else if ((dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().isEmpty())
        && !includesAllParticipants) {
      throw new BadRequestException("Missing cohort ids");
    } else if (dataSetRequest.getDomainValuePairs() == null
        || dataSetRequest.getDomainValuePairs().isEmpty()) {
      throw new BadRequestException("Missing values");
    }
  }

  private final Function<DbDataset, DataSet> TO_CLIENT_DATA_SET =
      new Function<DbDataset, DataSet>() {
        @Override
        public DataSet apply(DbDataset dataSet) {
          final DataSet result =
              new DataSet()
                  .name(dataSet.getName())
                  .includesAllParticipants(dataSet.getIncludesAllParticipants())
                  .id(dataSet.getDataSetId())
                  .etag(Etags.fromVersion(dataSet.getVersion()))
                  .description(dataSet.getDescription())
                  .prePackagedConceptSet(dataSet.getPrePackagedConceptSetEnum());
          if (dataSet.getLastModifiedTime() != null) {
            result.setLastModifiedTime(dataSet.getLastModifiedTime().getTime());
          }
          result.setConceptSets(
              conceptSetService.findAll(dataSet.getConceptSetIds()).stream()
                  .map(conceptSet -> conceptSetService.toClientConceptSet(conceptSet))
                  .collect(Collectors.toList()));
          result.setCohorts(
              cohortService.findAll(dataSet.getCohortIds()).stream()
                  .map(CohortsController.TO_CLIENT_COHORT)
                  .collect(Collectors.toList()));
          result.setDomainValuePairs(
              dataSet.getValues().stream()
                  .map(TO_CLIENT_DOMAIN_VALUE)
                  .collect(Collectors.toList()));
          return result;
        }
      };

  // TODO(jaycarlton): move into helper methods in one or both of these classes
  private static final Function<DbDatasetValue, DomainValuePair> TO_CLIENT_DOMAIN_VALUE =
      dataSetValue -> {
        DomainValuePair domainValuePair = new DomainValuePair();
        domainValuePair.setValue(dataSetValue.getValue());
        domainValuePair.setDomain(dataSetValue.getDomainEnum());
        return domainValuePair;
      };

  @VisibleForTesting
  public String generateRandomEightCharacterQualifier() {
    return prefixProvider.get();
  }

  public ResponseEntity<DataSetCodeResponse> generateCode(
      String workspaceNamespace,
      String workspaceId,
      String kernelTypeEnumString,
      DataSetRequest dataSetRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    final KernelTypeEnum kernelTypeEnum = KernelTypeEnum.fromValue(kernelTypeEnumString);

    // Generate query per domain for the selected concept set, cohort and values
    // TODO(jaycarlton): return better error information form this function for common validation
    // scenarios
    final Map<String, QueryJobConfiguration> bigQueryJobConfigsByDomain =
        dataSetService.domainToBigQueryConfig(dataSetRequest);

    if (bigQueryJobConfigsByDomain.isEmpty()) {
      log.warning("Empty query map generated for this DataSetRequest");
    }

    String qualifier = generateRandomEightCharacterQualifier();

    final ImmutableList<String> codeCells =
        ImmutableList.copyOf(
            dataSetService.generateCodeCells(
                kernelTypeEnum, dataSetRequest.getName(), qualifier, bigQueryJobConfigsByDomain));
    final String generatedCode = String.join("\n\n", codeCells);

    return ResponseEntity.ok(
        new DataSetCodeResponse().code(generatedCode).kernelType(kernelTypeEnum));
  }

  @Override
  public ResponseEntity<DataSetPreviewResponse> previewDataSetByDomain(
      String workspaceNamespace, String workspaceId, DataSetPreviewRequest dataSetPreviewRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DataSetPreviewResponse previewQueryResponse = new DataSetPreviewResponse();
    List<DataSetPreviewValueList> valuePreviewList = new ArrayList<>();

    QueryJobConfiguration previewBigQueryJobConfig =
        dataSetService.previewBigQueryJobConfig(dataSetPreviewRequest);

    TableResult queryResponse =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(previewBigQueryJobConfig),
            APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC);

    if (queryResponse.getTotalRows() != 0) {
      valuePreviewList.addAll(
          queryResponse.getSchema().getFields().stream()
              .map(fields -> new DataSetPreviewValueList().value(fields.getName()))
              .collect(Collectors.toList()));

      queryResponse
          .getValues()
          .forEach(
              fieldValueList ->
                  addFieldValuesFromBigQueryToPreviewList(valuePreviewList, fieldValueList));

      queryResponse
          .getSchema()
          .getFields()
          .forEach(fields -> formatTimestampValues(valuePreviewList, fields));

      Collections.sort(
          valuePreviewList,
          Comparator.comparing(item -> dataSetPreviewRequest.getValues().indexOf(item.getValue())));
    }

    previewQueryResponse.setDomain(dataSetPreviewRequest.getDomain());
    previewQueryResponse.setValues(valuePreviewList);
    return ResponseEntity.ok(previewQueryResponse);
  }

  @VisibleForTesting
  public void addFieldValuesFromBigQueryToPreviewList(
      List<DataSetPreviewValueList> valuePreviewList, FieldValueList fieldValueList) {
    IntStream.range(0, fieldValueList.size())
        .forEach(
            columnNumber ->
                valuePreviewList
                    .get(columnNumber)
                    .addQueryValueItem(
                        Optional.ofNullable(fieldValueList.get(columnNumber).getValue())
                            .map(Object::toString)
                            .orElse(EMPTY_CELL_MARKER)));
  }

  // Iterates through all values associated with a specific field, and converts all timestamps
  // to a timestamp formatted string.
  private void formatTimestampValues(List<DataSetPreviewValueList> valuePreviewList, Field field) {
    DataSetPreviewValueList previewValue =
        valuePreviewList.stream()
            .filter(preview -> preview.getValue().equalsIgnoreCase(field.getName()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Value should be present when it is not in dataset preview request"));
    if (field.getType() == LegacySQLTypeName.TIMESTAMP) {
      List<String> queryValues = new ArrayList<>();
      DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
      previewValue
          .getQueryValue()
          .forEach(
              value -> {
                if (!value.equals(EMPTY_CELL_MARKER)) {
                  Double fieldValue = Double.parseDouble(value);
                  queryValues.add(
                      dateFormat.format(Date.from(Instant.ofEpochSecond(fieldValue.longValue()))));
                } else {
                  queryValues.add(value);
                }
              });
      previewValue.setQueryValue(queryValues);
    }
  }

  @Override
  public ResponseEntity<EmptyResponse> exportToNotebook(
      String workspaceNamespace, String workspaceId, DataSetExportRequest dataSetExportRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    workspaceService.validateActiveBilling(workspaceNamespace, workspaceId);
    // This suppresses 'may not be initialized errors. We will always init to something else before
    // used.
    JSONObject notebookFile = new JSONObject();
    FirecloudWorkspaceResponse workspace =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
    JSONObject metaData = new JSONObject();

    if (!dataSetExportRequest.getNewNotebook()) {
      notebookFile =
          notebooksService.getNotebookContents(
              workspace.getWorkspace().getBucketName(), dataSetExportRequest.getNotebookName());
      try {
        String language =
            Optional.of(notebookFile.getJSONObject("metadata"))
                .flatMap(metaDataObj -> Optional.of(metaDataObj.getJSONObject("kernelspec")))
                .map(kernelSpec -> kernelSpec.getString("language"))
                .orElse("Python");
        if ("R".equals(language)) {
          dataSetExportRequest.setKernelType(KernelTypeEnum.R);
        } else {
          dataSetExportRequest.setKernelType(KernelTypeEnum.PYTHON);
        }
      } catch (JSONException e) {
        // If we can't find metadata to parse, default to python.
        dataSetExportRequest.setKernelType(KernelTypeEnum.PYTHON);
      }
    } else {
      switch (dataSetExportRequest.getKernelType()) {
        case PYTHON:
          break;
        case R:
          metaData
              .put(
                  "kernelspec",
                  new JSONObject().put("display_name", "R").put("language", "R").put("name", "ir"))
              .put(
                  "language_info",
                  new JSONObject()
                      .put("codemirror_mode", "r")
                      .put("file_extension", ".r")
                      .put("mimetype", "text/x-r-source")
                      .put("name", "r")
                      .put("pygments_lexer", "r")
                      .put("version", "3.4.4"));
          break;
        default:
          throw new BadRequestException(
              "Kernel Type " + dataSetExportRequest.getKernelType() + " is not supported");
      }
    }

    Map<String, QueryJobConfiguration> queriesByDomain =
        dataSetService.domainToBigQueryConfig(dataSetExportRequest.getDataSetRequest());

    String qualifier = generateRandomEightCharacterQualifier();

    List<String> queriesAsStrings =
        dataSetService.generateCodeCells(
            dataSetExportRequest.getKernelType(),
            dataSetExportRequest.getDataSetRequest().getName(),
            qualifier,
            queriesByDomain);

    if (dataSetExportRequest.getNewNotebook()) {
      notebookFile =
          new JSONObject()
              .put("cells", new JSONArray())
              .put("metadata", metaData)
              // nbformat and nbformat_minor are the notebook major and minor version we are
              // creating.
              // Specifically, here we create notebook version 4.2 (I believe)
              // See https://nbformat.readthedocs.io/en/latest/api.html
              .put("nbformat", 4)
              .put("nbformat_minor", 2);
    }
    for (String query : queriesAsStrings) {
      notebookFile.getJSONArray("cells").put(createNotebookCodeCellWithString(query));
    }

    notebooksService.saveNotebook(
        workspace.getWorkspace().getBucketName(),
        dataSetExportRequest.getNotebookName(),
        notebookFile);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<DataSetListResponse> getDataSetsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<DbDataset> dataSets = dataSetService.getInvalidDataSetsByWorkspace(workspace);
    DataSetListResponse response = new DataSetListResponse();

    response.setItems(
        dataSets.stream()
            .map(TO_CLIENT_DATA_SET)
            .sorted(Comparator.comparing(DataSet::getName))
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Boolean> markDirty(
      String workspaceNamespace, String workspaceId, MarkDataSetRequest markDataSetRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    dataSetService.markDirty(markDataSetRequest.getResourceType(), markDataSetRequest.getId());
    return ResponseEntity.ok(true);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    dataSetService.deleteDataSet(workspace, dataSetId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<DataSet> updateDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId, DataSetRequest request) {
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbDataset dbDataSet = dataSetService.getDbDataSet(workspace, dataSetId).get();

    int version = Etags.toVersion(request.getEtag());
    if (dbDataSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated data set version");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbDataSet.setLastModifiedTime(now);
    dbDataSet.setIncludesAllParticipants(request.getIncludesAllParticipants());
    dbDataSet.setCohortIds(request.getCohortIds());
    dbDataSet.setConceptSetIds(request.getConceptSetIds());
    dbDataSet.setDescription(request.getDescription());
    dbDataSet.setName(request.getName());
    dbDataSet.setPrePackagedConceptSetEnum(request.getPrePackagedConceptSet());
    dbDataSet.setValues(
        request.getDomainValuePairs().stream()
            .map(this::getDataSetValuesFromDomainValueSet)
            .collect(Collectors.toList()));

    dataSetService.saveDataSet(dbDataSet);

    return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(dbDataSet));
  }

  @Override
  public ResponseEntity<DataSet> getDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    DbDataset dataSet = dataSetService.getDbDataSet(workspace, dataSetId).get();
    return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(dataSet));
  }

  @Override
  public ResponseEntity<DataSetListResponse> getDataSetByResourceId(
      String workspaceNamespace, String workspaceId, ResourceType resourceType, Long id) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<DbDataset> dbDataSets = dataSetService.getDataSets(resourceType, id);
    DataSetListResponse dataSetResponse =
        new DataSetListResponse()
            .items(dbDataSets.stream().map(TO_CLIENT_DATA_SET).collect(Collectors.toList()));
    return ResponseEntity.ok(dataSetResponse);
  }

  @Override
  public ResponseEntity<DataDictionaryEntry> getDataDictionaryEntry(
      Long cdrVersionId, String domain, String domainValue) {
    DbCdrVersion cdrVersion =
        cdrVersionService
            .findByCdrVersionId(cdrVersionId)
            .<BadRequestException>orElseThrow(
                () -> {
                  throw new BadRequestException("Invalid CDR Version");
                });

    String omopTable = conceptSetService.getOmpTable(domain);
    if (omopTable == null) {
      throw new BadRequestException("Invalid Domain");
    }

    List<DbDataDictionaryEntry> dataDictionaryEntries =
        dataDictionaryEntryDao.findByFieldNameAndCdrVersion(domainValue, cdrVersion);

    if (dataDictionaryEntries.isEmpty()) {
      throw new NotFoundException();
    }

    return ResponseEntity.ok(dataSetMapper.dbModelToClient(dataDictionaryEntries.get(0)));
  }

  @Override
  public ResponseEntity<DomainValuesResponse> getValuesFromDomain(
      String workspaceNamespace, String workspaceId, String domainValue) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DomainValuesResponse response = new DomainValuesResponse();

    FieldList fieldList = bigQueryService.getTableFieldsFromDomain(Domain.valueOf(domainValue));
    response.setItems(
        fieldList.stream()
            .map(field -> new DomainValue().value(field.getName().toLowerCase()))
            .collect(Collectors.toList()));

    return ResponseEntity.ok(response);
  }

  // TODO(jaycarlton) create a class that knows about code cells and their properties,
  // then give it a toJson() method to replace this one.
  private JSONObject createNotebookCodeCellWithString(String cellInformation) {
    return new JSONObject()
        .put("cell_type", "code")
        .put("metadata", new JSONObject())
        .put("execution_count", JSONObject.NULL)
        .put("outputs", new JSONArray())
        .put("source", new JSONArray().put(cellInformation));
  }
}
