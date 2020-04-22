package org.pmiops.workbench.utils.mappers;

import java.sql.Timestamp;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.stereotype.Service;

@Service
public class CommonMappers {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  public CommonMappers(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Nullable
  public Long longToSqlTimestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }

    return null;
  }

  @Nullable
  public String timestampToString(Timestamp timestamp) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30 18:31:50.0"
    if (timestamp != null) {
      return timestamp.toString();
    }
    return null;
  }

  @Nullable
  public Timestamp longToSqlTimestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  @Nullable
  public String dbUserToCreatorEmail(DbUser creator) {
    return Optional.ofNullable(creator).map(DbUser::getUsername).orElse(null);
  }

  @Nullable
  public String cdrVersionToId(DbCdrVersion cdrVersion) {
    return Optional.ofNullable(cdrVersion)
        .map(DbCdrVersion::getCdrVersionId)
        .map(id -> Long.toString(id))
        .orElse(null);
  }

  @Named("cdrVersionToEtag")
  public String cdrVersionToEtag(int cdrVersion) {
    return Etags.fromVersion(cdrVersion);
  }

  @Named("etagToCdrVersion")
  public int etagToCdrVersion(String etag) {
    return Etags.toVersion(etag);
  }

  // Note that DbStorageEnums is a valid class for use in a MapStruct mapper directly, so there's
  // no need to add wrapper methods here.
  public WorkspaceAccessLevel fcAccessLevelToApiAccessLevel(FirecloudWorkspaceAccessEntry acl) {
    return WorkspaceAccessLevel.fromValue(acl.getAccessLevel());
  }

  public WorkspaceAccessLevel fcWorkspaceResponseToApiWorkspaceAccessLevel(
      FirecloudWorkspaceResponse fcResponse) {
    if (fcResponse.getAccessLevel().equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel());
    }
  }

  // Self-mapper that validates a new feature flag
  public BillingStatus overrideIfBillingLockoutDisabled(BillingStatus billingStatus) {
    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout) {
      return BillingStatus.ACTIVE;
    } else {
      return billingStatus;
    }
  }
}
