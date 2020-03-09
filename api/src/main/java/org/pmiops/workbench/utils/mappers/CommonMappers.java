package org.pmiops.workbench.utils.mappers;

import java.sql.Timestamp;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.NonAcademicAffiliation;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;

@Mapper(componentModel = "spring")
public class CommonMappers {

  public static Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }

    return null;
  }

  public static Timestamp timestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  public static Short booleanToShort(Boolean value) {
    if (value != null) {
      return value ? (short) 1 : (short) 0;
    } else {
      return null;
    }
  }

  public static Boolean shortToBoolean(Short value) {
    if (value != null) {
      return value == 1;
    } else {
      return null;
    }
  }

  public static String dbUserToCreatorEmail(DbUser creator) {
    return Optional.ofNullable(creator).map(DbUser::getUsername).orElse(null);
  }

  public static String cdrVersionToId(DbCdrVersion cdrVersion) {
    return Optional.ofNullable(cdrVersion)
        .map(DbCdrVersion::getCdrVersionId)
        .map(id -> Long.toString(id))
        .orElse(null);
  }

  @Named("cdrVersionToEtag")
  public static String cdrVersionToEtag(int cdrVersion) {
    return Etags.fromVersion(cdrVersion);
  }

  @Named("etagToCdrVersion")
  public static int etagToCdrVersion(String etag) {
    return Etags.toVersion(etag);
  }

  /////////////////////////////////////////////////////////////////////////////
  //                                  ENUMS                                  //
  /////////////////////////////////////////////////////////////////////////////

  public static DataAccessLevel dataAccessLevelFromStorage(Short dataAccessLevel) {
    return DbStorageEnums.dataAccessLevelFromStorage(dataAccessLevel);
  }

  public static Short dataAccessLevelToStorage(DataAccessLevel dataAccessLevel) {
    return DbStorageEnums.dataAccessLevelToStorage(dataAccessLevel);
  }
}
