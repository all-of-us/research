package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.Domain;

@Entity
@Table(name = "domain_info")
public class DbDomainInfo {

  private long conceptId;
  private short domain;
  private String domainId;
  private String domainEnumValue;
  private String name;
  private String description;
  private long allConceptCount;
  private long standardConceptCount;
  private long participantCount;

  public DbDomainInfo() {}

  // Used from JQL queries in DomainInfoDao
  public DbDomainInfo(
      short domain,
      String domainId,
      String domainEnumValue,
      String name,
      String description,
      long conceptId,
      long allConceptCount,
      long standardConceptCount,
      long participantCount) {
    this.conceptId = conceptId;
    this.domain = domain;
    this.domainId = domainId;
    this.domainEnumValue = domainEnumValue;
    this.name = name;
    this.description = description;
    this.allConceptCount = allConceptCount;
    this.standardConceptCount = standardConceptCount;
    this.participantCount = participantCount;
  }

  @Column(name = "concept_id")
  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public DbDomainInfo conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

  @Column(name = "domain")
  public short getDomain() {
    return domain;
  }

  public void setDomain(short domain) {
    this.domain = domain;
  }

  public DbDomainInfo domain(short domain) {
    this.domain = domain;
    return this;
  }

  @Transient
  public Domain getDomainEnum() {
    return DbStorageEnums.domainFromStorage(domain);
  }

  public DbDomainInfo domainEnum(Domain domain) {
    this.domain = DbStorageEnums.domainToStorage(domain);
    return this;
  }

  @Id
  @Column(name = "domain_id")
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public DbDomainInfo domainId(String domainId) {
    this.domainId = domainId;
    return this;
  }

  @Column(name = "domain_enum")
  public String getDomainEnumValue() {
    return domainEnumValue;
  }

  public void setDomainEnumValue(String domainEnumValue) {
    this.domainEnumValue = domainEnumValue;
  }

  public DbDomainInfo domainEnumValue(String domainEnumValue) {
    this.domainEnumValue = domainEnumValue;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbDomainInfo name(String name) {
    this.name = name;
    return this;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DbDomainInfo description(String description) {
    this.description = description;
    return this;
  }

  @Column(name = "all_concept_count")
  public long getAllConceptCount() {
    return allConceptCount;
  }

  public void setAllConceptCount(Long allConceptCount) {
    this.allConceptCount = allConceptCount == null ? 0L : allConceptCount;
  }

  public DbDomainInfo allConceptCount(long allConceptCount) {
    this.allConceptCount = allConceptCount;
    return this;
  }

  @Column(name = "standard_concept_count")
  public long getStandardConceptCount() {
    return standardConceptCount;
  }

  public void setStandardConceptCount(Long standardConceptCount) {
    this.standardConceptCount = standardConceptCount == null ? 0L : standardConceptCount;
  }

  public DbDomainInfo standardConceptCount(long standardConceptCount) {
    this.standardConceptCount = standardConceptCount;
    return this;
  }

  @Column(name = "participant_count")
  public long getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Long participantCount) {
    this.participantCount = participantCount == null ? 0L : participantCount;
  }

  public DbDomainInfo participantCount(long participantCount) {
    this.participantCount = participantCount;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbDomainInfo domainInfo = (DbDomainInfo) o;
    return Objects.equals(domain, domainInfo.domain)
        && Objects.equals(name, domainInfo.name)
        && Objects.equals(description, domainInfo.description)
        && Objects.equals(conceptId, domainInfo.conceptId)
        && Objects.equals(allConceptCount, domainInfo.allConceptCount)
        && Objects.equals(standardConceptCount, domainInfo.standardConceptCount)
        && Objects.equals(participantCount, domainInfo.participantCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        domain,
        name,
        description,
        conceptId,
        allConceptCount,
        standardConceptCount,
        participantCount);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
