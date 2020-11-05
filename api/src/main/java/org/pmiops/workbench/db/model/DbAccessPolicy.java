package org.pmiops.workbench.db.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "acccess_policy")
public class DbAccessPolicy {

  private long accessPolicyId;
  private String displayName;
  private final Set<DbAccessModule> accessModules = new HashSet<>();

  public DbAccessPolicy() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "access_policy_id")
  public long getAccessPolicyId() {
    return accessPolicyId;
  }

  public void setAccessPolicyId(long accessPolicyId) {
    this.accessPolicyId = accessPolicyId;
  }

  @Column(name = "display_name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbAccessPolicy)) {
      return false;
    }
    DbAccessPolicy that = (DbAccessPolicy) o;
    return accessPolicyId == that.accessPolicyId
        && Objects.equals(displayName, that.displayName)
        && Objects.equals(accessModules, that.accessModules);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessPolicyId, displayName, accessModules);
  }
  //  @OneToMany(mappedBy = "accessModuleId")
  //  public Set<DbAccessModule> getAccessModules() {
  //    return accessModules;
  //  }
  //
  //  public void setAccessModules(Set<DbAccessModule> accessModules) {
  //    this.accessModules = accessModules;
  //  }
}
