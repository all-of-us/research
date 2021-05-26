package org.pmiops.workbench.utils.mappers;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import joptsimple.internal.Strings;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.stereotype.Service;

@Service
public class CommonMappers {

  private final Clock clock;

  public CommonMappers(Clock clock) {
    this.clock = clock;
  }

  public Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }
    return null;
  }

  public static OffsetDateTime offsetDateTimeUtc(Timestamp timestamp) {
    if (timestamp != null) {
      return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
    }
    return null;
  }

  public static Timestamp timestamp(OffsetDateTime offsetDateTime) {
    if (offsetDateTime != null) {
      return Timestamp.from(offsetDateTime.toInstant());
    }
    return null;
  }

  @Named("toTimestampCurrentIfNull")
  public Timestamp timestampCurrentIfNull(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }
    return Timestamp.from(clock.instant());
  }

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

  public Timestamp timestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  @Named("dateToString")
  public String dateToString(Date date) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30"
    if (date != null) {
      return date.toString();
    }
    return null;
  }

  public String dbUserToCreatorEmail(DbUser creator) {
    if (creator != null) {
      return creator.getUsername();
    }
    return null;
  }

  public String cdrVersionToId(DbCdrVersion cdrVersion) {
    if (cdrVersion != null) {
      return Long.toString(cdrVersion.getCdrVersionId());
    }
    return null;
  }

  @Named("cdrVersionToEtag")
  public String cdrVersionToEtag(int cdrVersion) {
    return Etags.fromVersion(cdrVersion);
  }

  @Named("etagToCdrVersion")
  public int etagToCdrVersion(String etag) {
    return Strings.isNullOrEmpty(etag) ? 1 : Etags.toVersion(etag);
  }
}
