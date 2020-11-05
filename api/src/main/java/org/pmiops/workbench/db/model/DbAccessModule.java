package org.pmiops.workbench.db.model;

import java.util.HashSet;
import java.util.Set;
import javax.jdo.annotations.Unique;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.pmiops.workbench.accessmodules.AccessModuleEvaluatorKey;
import org.pmiops.workbench.accessmodules.AccessModuleType;

@Entity
@Table(name = "access_module")
public class DbAccessModule {

  private long accessModuleId;
  private String displayName;
  private AccessModuleType accessModuleType;
  private AccessModuleEvaluatorKey accessModuleEvaluatorKey;
//  private final Set<DbAccessPolicy> accessPolicies = new HashSet<>();

  public DbAccessModule() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "access_module_id")
  public long getAccessModuleId() {
    return accessModuleId;
  }

  public void setAccessModuleId(long accessModuleId) {
    this.accessModuleId = accessModuleId;
  }

  @Unique
  @Column(name = "display_name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Column(name = "access_module_evaluator_name")
  public AccessModuleEvaluatorKey getAccessModuleEvaluatorKey() {
    return accessModuleEvaluatorKey;
  }

  public void setAccessModuleEvaluatorKey(AccessModuleEvaluatorKey accessModuleEvaluatorName) {
    this.accessModuleEvaluatorKey = accessModuleEvaluatorName;
  }

  @Column(name = "access_module_type")
  public AccessModuleType getAccessModuleType() {
    return accessModuleType;
  }

  public void setAccessModuleType(AccessModuleType accessModuleType) {
    this.accessModuleType = accessModuleType;
  }

  //  @ManyToMany
  //  @JoinTable(
  //      name = "access_policy_module",
  //      joinColumns = @JoinColumn(name = "access_module_member_id.access_policy_id"),
  //      inverseJoinColumns = @JoinColumn(name = "access_module_id"))
  //  public Set<DbAccessPolicy> getAccessPolicies() {
  //    return accessPolicies;
  //  }
  //
  //  public void setAccessPolicies(Set<DbAccessPolicy> accessPolicies) {
  //    this.accessPolicies = accessPolicies;
  //  }
}
