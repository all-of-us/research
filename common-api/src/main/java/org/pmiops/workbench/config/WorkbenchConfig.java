package org.pmiops.workbench.config;

import java.util.ArrayList;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheSpringConfiguration}. This should be kept in sync with files in the config/
 * directory.
 */
public class WorkbenchConfig {

  public FireCloudConfig firecloud;
  public AuthConfig auth;
  public CdrConfig cdr;
  public GoogleCloudStorageServiceConfig googleCloudStorageService;
  public GoogleDirectoryServiceConfig googleDirectoryService;
  public ServerConfig server;
  public AdminConfig admin;
  public MandrillConfig mandrill;
  public ElasticsearchConfig elasticsearch;
  public MoodleConfig moodle;
  public AccessConfig access;
  public CohortBuilderConfig cohortbuilder;
  public FeatureFlagsConfig featureFlags;

  /** Creates a config with non-null-but-empty member variables, for use in testing. */
  public static WorkbenchConfig createEmptyConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.auth = new AuthConfig();
    config.auth.serviceAccountApiUsers = new ArrayList();
    config.cdr = new CdrConfig();
    config.googleCloudStorageService = new GoogleCloudStorageServiceConfig();
    config.googleDirectoryService = new GoogleDirectoryServiceConfig();
    config.server = new ServerConfig();
    config.admin = new AdminConfig();
    config.mandrill = new MandrillConfig();
    config.elasticsearch = new ElasticsearchConfig();
    config.moodle = new MoodleConfig();
    config.access = new AccessConfig();
    config.cohortbuilder = new CohortBuilderConfig();
    return config;
  }

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String baseUrl;
    public String billingAccountId;
    public String billingProjectPrefix;
    public Integer clusterMaxAgeDays;
    public Integer clusterIdleMaxAgeDays;
    public String registeredDomainName;
    public boolean enforceRegistered;
    public String leoBaseUrl;
    public Integer billingRetryCount;
    public Integer billingProjectBufferCapacity;
    // This value specifies the information we hand to Terra as our AppId header.
    // It is primarily used for metrics gathering information.
    public String xAppIdValue;
  }

  public static class AuthConfig {
    // A list of GCP service accounts (not affiliated with researchers) that can be used to
    // make API calls.
    public ArrayList<String> serviceAccountApiUsers;
  }

  public static class CdrConfig {
    public boolean debugQueries;
  }

  public static class GoogleCloudStorageServiceConfig {
    public String clusterResourcesBucketName;
    public String credentialsBucketName;
    public String emailImagesBucketName;
    public String demosBucketName;
  }

  public static class GoogleDirectoryServiceConfig {
    public String gSuiteDomain;
  }

  public static class ServerConfig {
    public String apiBaseUrl;
    public String publicApiKeyForErrorReports;
    public String projectId;
    public String shortName;
    public String oauthClientId;
  }

  public static class AdminConfig {
    public String adminIdVerification;
    public String loginUrl;
  }

  public static class MandrillConfig {
    public String fromEmail;
    public int sendRetries;
  }

  public static class ElasticsearchConfig {
    public String baseUrl;
    public boolean enableBasicAuth;
    public boolean enableElasticsearchBackend;
  }

  public static class MoodleConfig {
    public String host;
    public boolean enableMoodleBackend;
  }

  // The access object specifies whether each of the following access requirements block access
  // to the workbench.
  public static class AccessConfig {
    // Allows a user to bypass their own access modules. This is used for testing purposes so that
    // We can give control over 3rd party access modules
    public boolean unsafeAllowSelfBypass;
    public boolean enableComplianceTraining;
    public boolean enableEraCommons;
    public boolean enableDataUseAgreement;
    public boolean enableBetaAccess;
  }

  public static class CohortBuilderConfig {
    public boolean enableListSearch;
  }

  public static class FeatureFlagsConfig {
    public boolean useBillingProjectBuffer = false;
    public boolean notebooksUIV2 = false;
    // Allows a user to delete their own account. This is used for testing purposes so that
    // We can clean up after ourselves. This should never go to prod.
    public boolean unsafeAllowDeleteUser;
  }
}
