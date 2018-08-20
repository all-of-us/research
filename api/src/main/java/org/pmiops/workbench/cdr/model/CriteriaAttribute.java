package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "criteria_attribute")
public class CriteriaAttribute {

  private long id;
  private long conceptId;
  private long valueAsConceptId;
  private String conceptName;
  private String type;
  private String estCount;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public CriteriaAttribute id(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "concept_id")
  public long getConceptId() {
    return conceptId;
  }

  public void setConceptId(long conceptId) {
    this.conceptId = conceptId;
  }

  public CriteriaAttribute conceptId(long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

  @Column(name = "value_as_concept_id")
  public long getValueAsConceptId() {
    return valueAsConceptId;
  }

  public void setValueAsConceptId(long valueAsConceptId) {
    this.valueAsConceptId = valueAsConceptId;
  }

  public CriteriaAttribute valueAsConceptId(long valueAsConceptId) {
    this.valueAsConceptId = valueAsConceptId;
    return this;
  }

  @Column(name = "concept_name")
  public String getConceptName() {
    return conceptName;
  }

  public void setConceptName(String conceptName) {
    this.conceptName = conceptName;
  }

  public CriteriaAttribute conceptName(String conceptName) {
    this.conceptName = conceptName;
    return this;
  }

  @Column(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public CriteriaAttribute type(String type) {
    this.type = type;
    return this;
  }

  @Column(name = "estCount")
  public String getEstCount() {
    return estCount;
  }

  public void setEstCount(String estCount) {
    this.estCount = estCount;
  }

  public CriteriaAttribute estCount(String estCount) {
    this.estCount = estCount;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CriteriaAttribute that = (CriteriaAttribute) o;
    return conceptId == that.conceptId &&
      valueAsConceptId == that.valueAsConceptId &&
      Objects.equals(conceptName, that.conceptName) &&
      Objects.equals(type, that.type) &&
      Objects.equals(estCount, that.estCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, valueAsConceptId, conceptName, type, estCount);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
