package org.pmiops.workbench.genomics;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.WgsCohortExtractionConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class GenomicExtractionService {

  private final DataSetService dataSetService;
  private final DataSetDao dataSetDao;
  private final FireCloudService fireCloudService;
  private final Provider<CloudStorageClient> extractionServiceAccountCloudStorageClientProvider;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao;
  private final GenomicExtractionMapper genomicExtractionMapper;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final Clock clock;

  @Autowired
  public GenomicExtractionService(
      DataSetService dataSetService,
      DataSetDao dataSetDao,
      FireCloudService fireCloudService,
      @Qualifier(StorageConfig.GENOMIC_EXTRACTION_STORAGE_CLIENT)
          Provider<CloudStorageClient> extractionServiceAccountCloudStorageClientProvider,
      Provider<SubmissionsApi> submissionsApiProvider,
      Provider<MethodConfigurationsApi> methodConfigurationsApiProvider,
      WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao,
      GenomicExtractionMapper genomicExtractionMapper,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceAuthService workspaceAuthService,
      Clock clock) {
    this.dataSetService = dataSetService;
    this.dataSetDao = dataSetDao;
    this.fireCloudService = fireCloudService;
    this.submissionApiProvider = submissionsApiProvider;
    this.extractionServiceAccountCloudStorageClientProvider =
        extractionServiceAccountCloudStorageClientProvider;
    this.methodConfigurationsApiProvider = methodConfigurationsApiProvider;
    this.wgsExtractCromwellSubmissionDao = wgsExtractCromwellSubmissionDao;
    this.genomicExtractionMapper = genomicExtractionMapper;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.clock = clock;
  }

  private Map<String, String> createRepoMethodParameter(
      WgsCohortExtractionConfig cohortExtractionConfig) {
    return new ImmutableMap.Builder<String, String>()
        .put("methodName", cohortExtractionConfig.extractionMethodConfigurationName)
        .put(
            "methodVersion", cohortExtractionConfig.extractionMethodConfigurationVersion.toString())
        .put("methodNamespace", cohortExtractionConfig.extractionMethodConfigurationNamespace)
        .put(
            "methodUri",
            "agora://"
                + cohortExtractionConfig.extractionMethodConfigurationNamespace
                + "/"
                + cohortExtractionConfig.extractionMethodConfigurationName
                + "/"
                + cohortExtractionConfig.extractionMethodConfigurationVersion)
        .put("sourceRepo", "agora")
        .build();
  }

  private boolean isTerminal(TerraJobStatus status) {
    return !(status == TerraJobStatus.RUNNING || status == TerraJobStatus.ABORTING);
  }

  public Optional<String> getExtractionDirectory(Long datasetId) {
    try {
      return Optional.of(wgsExtractCromwellSubmissionDao
          .findMostRecentValidExtractionByDataset(dataSetDao.findById(datasetId).get())
          .get()
          .getOutputDir());
    } catch (NoSuchElementException e) {
      return Optional.empty();
    }
  }

  public List<GenomicExtractionJob> getGenomicExtractionJobs(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return wgsExtractCromwellSubmissionDao.findAllByWorkspace(dbWorkspace).stream()
        .map(
            dbSubmission -> {
              try {
                // Don't bother checking if we already know the job is in a terminal status.
                if (dbSubmission.getTerraStatusEnum() == null
                    || !isTerminal(dbSubmission.getTerraStatusEnum())) {
                  WgsCohortExtractionConfig cohortExtractionConfig =
                      workbenchConfigProvider.get().wgsCohortExtraction;
                  FirecloudSubmission firecloudSubmission =
                      submissionApiProvider
                          .get()
                          .getSubmission(
                              cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                              cohortExtractionConfig.operationalTerraWorkspaceName,
                              dbSubmission.getSubmissionId());

                  TerraJobStatus status =
                      genomicExtractionMapper.convertWorkflowStatus(
                          // Extraction submissions should only have one workflow.
                          firecloudSubmission.getWorkflows().get(0).getStatus());
                  dbSubmission.setTerraStatusEnum(status);
                  if (isTerminal(status)) {
                    dbSubmission.setCompletionTime(
                        CommonMappers.timestamp(
                            firecloudSubmission.getWorkflows().get(0).getStatusLastChangedDate()));
                  }

                  wgsExtractCromwellSubmissionDao.save(dbSubmission);
                }
                return genomicExtractionMapper.toApi(dbSubmission);
              } catch (ApiException e) {
                throw new ServerErrorException("Could not fetch submission status from Terra", e);
              }
            })
        .collect(Collectors.toList());
  }

  public GenomicExtractionJob submitGenomicExtractionJob(DbWorkspace workspace, DbDataset dataSet)
      throws ApiException {
    WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    FirecloudWorkspace fcUserWorkspace =
        fireCloudService.getWorkspace(workspace).get().getWorkspace();

    String extractionUuid = UUID.randomUUID().toString();
    String extractionFolder = "genomic-extractions/" + extractionUuid;

    List<String> personIds = dataSetService.getPersonIdsWithWholeGenome(dataSet);
    if (personIds.isEmpty()) {
      throw new FailedPreconditionException(
          "provided cohort contains no participants with whole genome data");
    }

    Blob personIdsFile =
        extractionServiceAccountCloudStorageClientProvider
            .get()
            .writeFile(
                // It is critical that this file is written to a bucket that the user cannot write
                // to because its contents will feed into a SQL query with the cohort
                // extraction SA's permissions
                cohortExtractionConfig.operationalTerraWorkspaceBucket,
                extractionFolder + "/person_ids.txt",
                String.join("\n", personIds).getBytes(StandardCharsets.UTF_8));

    final String outputDir = "gs://" + fcUserWorkspace.getBucketName() + "/" + extractionFolder + "/vcfs/";

    FirecloudMethodConfiguration methodConfig =
        methodConfigurationsApiProvider
            .get()
            .createWorkspaceMethodConfig(
                new FirecloudMethodConfiguration()
                    .inputs(
                        new ImmutableMap.Builder<String, String>()
                            .put(
                                "WgsCohortExtract.participant_ids",
                                "\"gs://" // Cromwell string inputs require double quotes
                                    + personIdsFile.getBucket()
                                    + "/"
                                    + personIdsFile.getName()
                                    + "\"")
                            .put(
                                "WgsCohortExtract.query_project",
                                "\"" + workspace.getWorkspaceNamespace() + "\"")
                            .put("WgsCohortExtract.extraction_uuid", "\"" + extractionUuid + "\"")
                            .put(
                                "WgsCohortExtract.wgs_dataset",
                                "\""
                                    + workspace.getCdrVersion().getBigqueryProject()
                                    + "."
                                    + workspace.getCdrVersion().getWgsBigqueryDataset()
                                    + "\"")
                            .put(
                                "WgsCohortExtract.wgs_extraction_cohorts_dataset",
                                "\"" + cohortExtractionConfig.extractionCohortsDataset + "\"")
                            .put(
                                "WgsCohortExtract.wgs_extraction_destination_dataset",
                                "\"" + cohortExtractionConfig.extractionDestinationDataset + "\"")
                            .put(
                                "WgsCohortExtract.wgs_extraction_temp_tables_dataset",
                                "\"" + cohortExtractionConfig.extractionTempTablesDataset + "\"")
                            .put(
                                "WgsCohortExtract.wgs_intervals",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/wgs_calling_regions.hg38.interval_list\"")
                            // This value will need to be dynamically adjusted through testing
                            .put("WgsCohortExtract.scatter_count", "1000")
                            .put(
                                "WgsCohortExtract.reference",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/Homo_sapiens_assembly38.fasta\"")
                            .put(
                                "WgsCohortExtract.reference_index",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/Homo_sapiens_assembly38.fasta.fai\"")
                            .put(
                                "WgsCohortExtract.reference_dict",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/Homo_sapiens_assembly38.dict\"")
                            // Will produce files named "interval_1.vcf.gz", "interval_32.vcf.gz",
                            // etc
                            .put("WgsCohortExtract.output_file_base_name", "\"interval\"")
                            .put(
                                "WgsCohortExtract.output_gcs_dir",
                                "\"" + outputDir + "\"")
                            .put(
                                "WgsCohortExtract.gatk_override",
                                "\"gs://all-of-us-workbench-test-genomics/wgs/gatk-package-4.1.9.0-204-g6449d52-SNAPSHOT-local.jar\"")
                            .build())
                    .methodConfigVersion(
                        cohortExtractionConfig.extractionMethodConfigurationVersion)
                    .methodRepoMethod(createRepoMethodParameter(cohortExtractionConfig))
                    .name(extractionUuid)
                    .namespace(cohortExtractionConfig.extractionMethodConfigurationNamespace)
                    .outputs(new HashMap<>()),
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName)
            .getMethodConfiguration();

    FirecloudSubmissionResponse submissionResponse =
        submissionApiProvider
            .get()
            .createSubmission(
                new FirecloudSubmissionRequest()
                    .deleteIntermediateOutputFiles(false)
                    .methodConfigurationNamespace(methodConfig.getNamespace())
                    .methodConfigurationName(methodConfig.getName())
                    .useCallCache(false),
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName);

    // Note: if this save fails we may have an orphaned job. Will likely need a cleanup task to
    // check for such jobs.
    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setSubmissionId(submissionResponse.getSubmissionId());
    dbSubmission.setWorkspace(workspace);
    dbSubmission.setDataset(dataSet);
    dbSubmission.setCreator(userProvider.get());
    dbSubmission.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    dbSubmission.setTerraSubmissionDate(
        CommonMappers.timestamp(submissionResponse.getSubmissionDate()));
    dbSubmission.setSampleCount((long) personIds.size());
    dbSubmission.setOutputDir(outputDir);
    wgsExtractCromwellSubmissionDao.save(dbSubmission);

    methodConfigurationsApiProvider
        .get()
        .deleteWorkspaceMethodConfig(
            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
            cohortExtractionConfig.operationalTerraWorkspaceName,
            cohortExtractionConfig.extractionMethodConfigurationNamespace,
            methodConfig.getName());

    return new GenomicExtractionJob().status(TerraJobStatus.RUNNING);
  }

  public void abortGenomicExtractionJob(DbWorkspace dbWorkspace, String jobId) throws ApiException {
    Optional<DbWgsExtractCromwellSubmission> dbSubmission =
        wgsExtractCromwellSubmissionDao.findByWorkspaceWorkspaceIdAndWgsExtractCromwellSubmissionId(
            dbWorkspace.getWorkspaceId(), Long.valueOf(jobId));

    if (!dbSubmission.isPresent()) {
      throw new NotFoundException("Specified dataset is not in workspace " + dbWorkspace.getName());
    }

    WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    submissionApiProvider
        .get()
        .abortSubmission(
            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
            cohortExtractionConfig.operationalTerraWorkspaceName,
            dbSubmission.get().getSubmissionId());
  }
}
