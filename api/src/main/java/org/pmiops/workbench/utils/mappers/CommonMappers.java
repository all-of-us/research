package org.pmiops.workbench.utils.mappers;

import java.sql.Timestamp;
import java.util.Optional;
import javax.inject.Provider;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.stereotype.Service;

@Service
public class CommonMappers {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public CommonMappers(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }

    return null;
  }

  public Timestamp timestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  public String dbUserToCreatorEmail(DbUser creator) {
    return Optional.ofNullable(creator).map(DbUser::getUsername).orElse(null);
  }

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

  /////////////////////////////////////////////////////////////////////////////
  //                                  ENUMS                                  //
  /////////////////////////////////////////////////////////////////////////////

  public DataAccessLevel dataAccessLevelFromStorage(Short dataAccessLevel) {
    return DbStorageEnums.dataAccessLevelFromStorage(dataAccessLevel);
  }

  public Short dataAccessLevelToStorage(DataAccessLevel dataAccessLevel) {
    return DbStorageEnums.dataAccessLevelToStorage(dataAccessLevel);
  }

  public BillingStatus billingStatus(BillingStatus billingStatus) {
    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout) {
      return BillingStatus.ACTIVE;
    } else {
      return billingStatus;
    }
  }
}
