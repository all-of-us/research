package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.ArchivalStatus;

@Entity
@Table(name = "cdr_version")
public class DbCdrVersion {
  private long cdrVersionId;
  private boolean isDefault;
  private String name;
  private DbAccessTier accessTier;
  private short releaseNumber;
  private short archivalStatus;
  private String bigqueryProject;
  private String bigqueryDataset;
  private Timestamp creationTime;
  private int numParticipants;
  private String cdrDbName;
  private String elasticIndexBaseName;
  private String wgsBigqueryDataset;
  private Boolean hasFitbitData;
  private Boolean hasCopeSurveyData;
  private Boolean hasMergedWgsData;

  @Id
  @Column(name = "cdr_version_id")
  public long getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  @Column(name = "is_default")
  public boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @ManyToOne
  @JoinColumn(name = "access_tier")
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public void setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
  }

  @Column(name = "archival_status")
  public Short getArchivalStatus() {
    return archivalStatus;
  }

  public void setArchivalStatus(Short archivalStatus) {
    this.archivalStatus = archivalStatus;
  }

  @Transient
  public ArchivalStatus getArchivalStatusEnum() {
    return DbStorageEnums.archivalStatusFromStorage(getArchivalStatus());
  }

  public void setArchivalStatusEnum(ArchivalStatus archivalStatus) {
    setArchivalStatus(DbStorageEnums.archivalStatusToStorage(archivalStatus));
  }

  @Column(name = "release_number")
  public short getReleaseNumber() {
    return releaseNumber;
  }

  public void setReleaseNumber(short releaseNumber) {
    this.releaseNumber = releaseNumber;
  }

  @Column(name = "bigquery_project")
  public String getBigqueryProject() {
    return bigqueryProject;
  }

  public void setBigqueryProject(String bigqueryProject) {
    this.bigqueryProject = bigqueryProject;
  }

  @Column(name = "bigquery_dataset")
  public String getBigqueryDataset() {
    return bigqueryDataset;
  }

  public void setBigqueryDataset(String bigqueryDataset) {
    this.bigqueryDataset = bigqueryDataset;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "num_participants")
  public int getNumParticipants() {
    return numParticipants;
  }

  public void setNumParticipants(int numParticipants) {
    this.numParticipants = numParticipants;
  }

  @Column(name = "cdr_db_name")
  public String getCdrDbName() {
    return cdrDbName;
  }

  public void setCdrDbName(String cdrDbName) {
    this.cdrDbName = cdrDbName;
  }

  @Column(name = "elastic_index_base_name")
  public String getElasticIndexBaseName() {
    return elasticIndexBaseName;
  }

  public void setElasticIndexBaseName(String elasticIndexBaseName) {
    this.elasticIndexBaseName = elasticIndexBaseName;
  }

  @Column(name = "wgs_bigquery_dataset")
  public String getWgsBigqueryDataset() {
    return wgsBigqueryDataset;
  }

  public void setWgsBigqueryDataset(String wgsBigqueryDataset) {
    this.wgsBigqueryDataset = wgsBigqueryDataset;
  }

  @Column(name = "has_fitbit_data")
  public Boolean getHasFitbitData() {
    return hasFitbitData == null ? false : hasFitbitData;
  }

  public void setHasFitbitData(Boolean hasFitbitData) {
    this.hasFitbitData = hasFitbitData;
  }

  @Column(name = "has_copesurvey_data")
  public Boolean getHasCopeSurveyData() {
    return hasCopeSurveyData == null ? false : hasCopeSurveyData;
  }

  public void setHasCopeSurveyData(Boolean hasCopeSurveyData) {
    this.hasCopeSurveyData = hasCopeSurveyData;
  }

  @Column(name = "has_merged_wgs_data")
  public Boolean getHasMergedWgsData() {
    return hasMergedWgsData;
  }

  public void setHasMergedWgsData(Boolean hasMergedWgsData) {
    this.hasMergedWgsData = hasMergedWgsData;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cdrVersionId,
        isDefault,
        name,
        accessTier,
        releaseNumber,
        archivalStatus,
        bigqueryProject,
        bigqueryDataset,
        creationTime,
        numParticipants,
        cdrDbName,
        elasticIndexBaseName,
        wgsBigqueryDataset,
        hasFitbitData,
        hasCopeSurveyData,
        hasMergedWgsData);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbCdrVersion that = (DbCdrVersion) o;
    return cdrVersionId == that.cdrVersionId
        && isDefault == that.isDefault
        && releaseNumber == that.releaseNumber
        && archivalStatus == that.archivalStatus
        && numParticipants == that.numParticipants
        && Objects.equals(name, that.name)
        && Objects.equals(accessTier, that.accessTier)
        && Objects.equals(bigqueryProject, that.bigqueryProject)
        && Objects.equals(bigqueryDataset, that.bigqueryDataset)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(cdrDbName, that.cdrDbName)
        && Objects.equals(elasticIndexBaseName, that.elasticIndexBaseName)
        && Objects.equals(wgsBigqueryDataset, that.wgsBigqueryDataset)
        && Objects.equals(hasFitbitData, that.hasFitbitData)
        && Objects.equals(hasCopeSurveyData, that.hasCopeSurveyData)
        && Objects.equals(hasMergedWgsData, that.hasMergedWgsData);
  }
}
