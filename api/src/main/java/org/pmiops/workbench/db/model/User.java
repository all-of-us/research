package org.pmiops.workbench.db.model;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;

@Entity
@Table(name = "user",
    indexes = {  @Index(name = "idx_user_email", columnList = "email", unique = true)})
public class User {

  private long userId;
  // The Google email address that the user signs in with.
  private String email;
  // The email address that can be used to contact the user.
  private String contactEmail;
  private DataAccessLevel dataAccessLevel;
  private String fullName;
  private String givenName;
  private String familyName;
  private String phoneNumber;
  private String freeTierBillingProjectName;
  private Set<Authority> authorities = new HashSet<Authority>();

  @Id
  @GeneratedValue
  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Column(name = "email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Column(name = "contact_email")
  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Column(name = "data_access_level")
  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @Column(name = "full_name")
  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  @Column(name = "given_name")
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @Column(name = "family_name")
  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  @Column(name = "phone_number")
  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @Column(name = "free_tier_billing_project_name")
  public String getFreeTierBillingProjectName() {
    return freeTierBillingProjectName;
  }

  public void setFreeTierBillingProjectName(String freeTierBillingProjectName) {
    this.freeTierBillingProjectName = freeTierBillingProjectName;
  }

  // Authorities (special permissions) are granted using api/project.rb set-authority.
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "authority", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "authority")
  public Set<Authority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Authority> newAuthorities) {
    this.authorities = newAuthorities;
  }
}
