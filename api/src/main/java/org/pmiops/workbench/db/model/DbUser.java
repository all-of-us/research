package org.pmiops.workbench.db.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.pmiops.workbench.db.dto.DtoUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.EmailVerificationStatus;

@Entity
@Table(name = "user")
public class DbUser implements DtoUser {

  private static final String CLUSTER_NAME_PREFIX = "all-of-us-";

  /**
   * This is a Gson compatible class for encoding a JSON blob which is stored in MySQL. This
   * represents cluster configuration overrides we support on a per-user basis for their notebook
   * cluster. Corresponds to Leonardo's MachineConfig model. All fields are optional.
   *
   * <p>Any changes to this class should produce backwards-compatible JSON.
   */
  public static class ClusterConfig {
    // Master persistent disk size in GB.
    public Integer masterDiskSize;
    // GCE machine type, e.g. n1-standard-2.
    public String machineType;
  }

  private long userId;
  private int version;
  // A nonce which can be used during the account creation flow to verify
  // unauthenticated API calls after account creation, but before initial login.
  private Long creationNonce;
  // The full G Suite email address that the user signs in with, e.g. "joe@researchallofus.org".
  private String username;
  // The email address that can be used to contact the user.
  private String contactEmail;
  private Short dataAccessLevel;
  private String givenName;
  private String familyName;
  private String phoneNumber;
  private String professionalUrl;
  private String currentPosition;
  private String organization;
  private Double freeTierCreditsLimitDollarsOverride = null;
  private Short freeTierCreditsLimitDaysOverride = null;
  private Timestamp lastFreeTierCreditsTimeCheck;
  private Timestamp firstSignInTime;
  private Timestamp firstRegistrationCompletionTime;
  private Set<Short> authorities = new HashSet<>();
  private Boolean idVerificationIsValid;
  private List<Short> degrees;
  private Timestamp demographicSurveyCompletionTime;
  private boolean disabled;
  private Short emailVerificationStatus;
  private Set<DbPageVisit> pageVisits = new HashSet<>();
  private String clusterConfigDefault;

  private List<DbInstitutionalAffiliation> institutionalAffiliations = new ArrayList<>();
  private String aboutYou;
  private String areaOfResearch;
  private Integer clusterCreateRetries;
  private Integer billingProjectRetries;
  private Integer moodleId;

  // Access module fields go here. See http://broad.io/aou-access-modules for docs.
  private String eraCommonsLinkedNihUsername;
  private Timestamp eraCommonsLinkExpireTime;
  private Timestamp eraCommonsCompletionTime;
  private Timestamp betaAccessRequestTime;
  private Timestamp betaAccessBypassTime;
  private Timestamp dataUseAgreementCompletionTime;
  private Timestamp dataUseAgreementBypassTime;
  private Integer dataUseAgreementSignedVersion;
  private Timestamp complianceTrainingCompletionTime;
  private Timestamp complianceTrainingBypassTime;
  private Timestamp complianceTrainingExpirationTime;
  private Timestamp eraCommonsBypassTime;
  private Timestamp emailVerificationCompletionTime;
  private Timestamp emailVerificationBypassTime;
  private Timestamp idVerificationCompletionTime;
  private Timestamp idVerificationBypassTime;
  private Timestamp twoFactorAuthCompletionTime;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private Timestamp twoFactorAuthBypassTime;
  private Set<DbDemographicSurvey> demographicSurveys;
  private Set<DbAddress> addresses;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  @Override
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Version
  @Column(name = "version")
  @Deprecated
  @Override
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Column(name = "creation_nonce")
  public Long getCreationNonce() {
    return creationNonce;
  }

  public void setCreationNonce(Long creationNonce) {
    this.creationNonce = creationNonce;
  }

  /**
   * Returns the user's full G Suite email address, e.g. "joe@researchallofus.org". This is named
   * "username" in this entity class to distinguish it from getContactEmail, which is the user's
   * designated contact email address.
   */
  @Column(name = "email")
  @Override
  public String getUsername() {
    return username;
  }

  public void setUsername(String userName) {
    this.username = userName;
  }

  /**
   * Returns the user's designated contact email address, e.g. "joe@gmail.com".
   */
  @Column(name = "contact_email")
  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Column(name = "data_access_level")
  public Short getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(Short dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @Transient
  public DataAccessLevel getDataAccessLevelEnum() {
    return DbStorageEnums.dataAccessLevelFromStorage(getDataAccessLevel());
  }

  public void setDataAccessLevelEnum(DataAccessLevel dataAccessLevel) {
    setDataAccessLevel(DbStorageEnums.dataAccessLevelToStorage(dataAccessLevel));
  }

  @Column(name = "given_name")
  @Override
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @Column(name = "family_name")
  @Override
  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  // TODO: consider dropping this (do we want researcher phone numbers?)
  @Column(name = "phone_number")
  @Override
  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @Column(name = "current_position")
  public String getCurrentPosition() {
    return currentPosition;
  }

  public void setCurrentPosition(String currentPosition) {
    this.currentPosition = currentPosition;
  }

  @Column(name = "organization")
  @Override
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Column(name = "free_tier_credits_limit_dollars_override")
  @Override
  public Double getFreeTierCreditsLimitDollarsOverride() {
    return freeTierCreditsLimitDollarsOverride;
  }

  public void setFreeTierCreditsLimitDollarsOverride(Double freeTierCreditsLimitDollarsOverride) {
    this.freeTierCreditsLimitDollarsOverride = freeTierCreditsLimitDollarsOverride;
  }

  @Deprecated
  @Column(name = "free_tier_credits_limit_days_override")
  @Override
  public Short getFreeTierCreditsLimitDaysOverride() {
    return freeTierCreditsLimitDaysOverride;
  }

  public void setFreeTierCreditsLimitDaysOverride(Short freeTierCreditsLimitDaysOverride) {
    this.freeTierCreditsLimitDaysOverride = freeTierCreditsLimitDaysOverride;
  }

  @Deprecated
  @Column(name = "last_free_tier_credits_time_check")
  @Override
  public Timestamp getLastFreeTierCreditsTimeCheck() {
    return lastFreeTierCreditsTimeCheck;
  }

  public void setLastFreeTierCreditsTimeCheck(Timestamp lastFreeTierCreditsTimeCheck) {
    this.lastFreeTierCreditsTimeCheck = lastFreeTierCreditsTimeCheck;
  }

  @Column(name = "first_sign_in_time")
  @Override
  public Timestamp getFirstSignInTime() {
    return firstSignInTime;
  }

  public void setFirstSignInTime(Timestamp firstSignInTime) {
    this.firstSignInTime = firstSignInTime;
  }

  @Column(name = "first_registration_completion_time")
  @Override
  public Timestamp getFirstRegistrationCompletionTime() {
    return firstRegistrationCompletionTime;
  }

  public void setFirstRegistrationCompletionTime() {
    setFirstRegistrationCompletionTime(Timestamp.from(Instant.now()));
  }

  @VisibleForTesting
  public void setFirstRegistrationCompletionTime(Timestamp registrationCompletionTime) {
    this.firstRegistrationCompletionTime = registrationCompletionTime;
  }

  // Authorities (special permissions) are granted using api/project.rb set-authority.
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "authority", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "authority")
  public Set<Short> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Short> newAuthorities) {
    this.authorities = newAuthorities;
  }

  @Transient
  public Set<Authority> getAuthoritiesEnum() {
    Set<Short> from = getAuthorities();
    if (from == null) {
      return null;
    }
    return from.stream().map(DbStorageEnums::authorityFromStorage).collect(Collectors.toSet());
  }

  public void setAuthoritiesEnum(Set<Authority> newAuthorities) {
    this.setAuthorities(
        newAuthorities.stream()
            .map(DbStorageEnums::authorityToStorage)
            .collect(Collectors.toSet()));
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_degree", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "degree")
  public List<Short> getDegrees() {
    return degrees;
  }

  public void setDegrees(List<Short> degree) {
    this.degrees = degree;
  }

  @Transient
  public List<Degree> getDegreesEnum() {
    if (degrees == null) {
      return null;
    }
    return this.degrees.stream()
        .map(
            DbStorageEnums::degreeFromStorage)
        .collect(Collectors.toList());
  }

  public void setDegreesEnum(List<Degree> degreeList) {
    this.degrees =
        degreeList.stream()
            .map(
                DbStorageEnums::degreeToStorage)
            .collect(Collectors.toList());
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public Set<DbPageVisit> getPageVisits() {
    return pageVisits;
  }

  public void setPageVisits(Set<DbPageVisit> newPageVisits) {
    this.pageVisits = newPageVisits;
  }

  @Column(name = "id_verification_is_valid")
  @Override
  public Boolean getIdVerificationIsValid() {
    return idVerificationIsValid;
  }

  public void setIdVerificationIsValid(Boolean value) {
    idVerificationIsValid = value;
  }

  @Column(name = "cluster_config_default")
  @Override
  public String getClusterConfigDefaultRaw() {
    return clusterConfigDefault;
  }

  public void setClusterConfigDefaultRaw(String value) {
    clusterConfigDefault = value;
  }

  @Transient
  public ClusterConfig getClusterConfigDefault() {
    if (clusterConfigDefault == null) {
      return null;
    }
    return new Gson().fromJson(clusterConfigDefault, ClusterConfig.class);
  }

  public void setClusterConfigDefault(ClusterConfig value) {
    String rawValue = null;
    if (value != null) {
      rawValue = new Gson().toJson(value);
    }
    setClusterConfigDefaultRaw(rawValue);
  }

  @Column(name = "demographic_survey_completion_time")
  @Override
  public Timestamp getDemographicSurveyCompletionTime() {
    return demographicSurveyCompletionTime;
  }

  public void setDemographicSurveyCompletionTime(Timestamp demographicSurveyCompletionTime) {
    this.demographicSurveyCompletionTime = demographicSurveyCompletionTime;
  }

  @Column(name = "disabled")
  @Override
  public boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @Column(name = "email_verification_status")
  @Override
  public Short getEmailVerificationStatus() {
    return emailVerificationStatus;
  }

  public void setEmailVerificationStatus(Short emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
  }

  @Transient
  public EmailVerificationStatus getEmailVerificationStatusEnum() {
    return DbStorageEnums.emailVerificationStatusFromStorage(getEmailVerificationStatus());
  }

  public void setEmailVerificationStatusEnum(EmailVerificationStatus emailVerificationStatus) {
    setEmailVerificationStatus(
        DbStorageEnums.emailVerificationStatusToStorage(emailVerificationStatus));
  }

  @OneToMany(
      fetch = FetchType.LAZY,
      orphanRemoval = true,
      mappedBy = "user",
      cascade = CascadeType.ALL)
  @OrderColumn(name = "order_index")
  public List<DbInstitutionalAffiliation> getInstitutionalAffiliations() {
    return institutionalAffiliations;
  }

  public void setInstitutionalAffiliations(
      List<DbInstitutionalAffiliation> newInstitutionalAffiliations) {
    this.institutionalAffiliations = newInstitutionalAffiliations;
  }

  public void clearInstitutionalAffiliations() {
    this.institutionalAffiliations.clear();
  }

  public void addInstitutionalAffiliation(DbInstitutionalAffiliation newInstitutionalAffiliation) {
    this.institutionalAffiliations.add(newInstitutionalAffiliation);
  }

  @Column(name = "about_you")
  @Override
  public String getAboutYou() {
    return aboutYou;
  }

  public void setAboutYou(String aboutYou) {
    this.aboutYou = aboutYou;
  }

  @Column(name = "area_of_research")
  public String getAreaOfResearch() {
    return areaOfResearch;
  }

  public void setAreaOfResearch(String areaOfResearch) {
    this.areaOfResearch = areaOfResearch;
  }

  @Column(name = "cluster_create_retries")
  @Override
  public Integer getClusterCreateRetries() {
    return clusterCreateRetries;
  }

  public void setClusterCreateRetries(Integer clusterCreateRetries) {
    this.clusterCreateRetries = clusterCreateRetries;
  }

  @Column(name = "billing_project_retries")
  @Override
  public Integer getBillingProjectRetries() {
    return billingProjectRetries;
  }

  public void setBillingProjectRetries(Integer billingProjectRetries) {
    this.billingProjectRetries = billingProjectRetries;
  }

  @Column(name = "beta_access_request_time")
  @Override
  public Timestamp getBetaAccessRequestTime() {
    return betaAccessRequestTime;
  }

  public void setBetaAccessRequestTime(Timestamp betaAccessRequestTime) {
    this.betaAccessRequestTime = betaAccessRequestTime;
  }

  @Column(name = "moodle_id")
  @Override
  public Integer getMoodleId() {
    return moodleId;
  }

  public void setMoodleId(Integer moodleId) {
    this.moodleId = moodleId;
  }

  @Column(name = "era_commons_linked_nih_username")
  @Override
  public String getEraCommonsLinkedNihUsername() {
    return eraCommonsLinkedNihUsername;
  }

  public void setEraCommonsLinkedNihUsername(String eraCommonsLinkedNihUsername) {
    this.eraCommonsLinkedNihUsername = eraCommonsLinkedNihUsername;
  }

  @Column(name = "era_commons_link_expire_time")
  @Override
  public Timestamp getEraCommonsLinkExpireTime() {
    return eraCommonsLinkExpireTime;
  }

  public void setEraCommonsLinkExpireTime(Timestamp eraCommonsLinkExpireTime) {
    this.eraCommonsLinkExpireTime = eraCommonsLinkExpireTime;
  }

  @Column(name = "era_commons_completion_time")
  @Override
  public Timestamp getEraCommonsCompletionTime() {
    return eraCommonsCompletionTime;
  }

  public void setEraCommonsCompletionTime(Timestamp eraCommonsCompletionTime) {
    this.eraCommonsCompletionTime = eraCommonsCompletionTime;
  }

  @Column(name = "data_use_agreement_completion_time")
  @Override
  public Timestamp getDataUseAgreementCompletionTime() {
    return dataUseAgreementCompletionTime;
  }

  public void setDataUseAgreementCompletionTime(Timestamp dataUseAgreementCompletionTime) {
    this.dataUseAgreementCompletionTime = dataUseAgreementCompletionTime;
  }

  @Column(name = "data_use_agreement_bypass_time")
  @Override
  public Timestamp getDataUseAgreementBypassTime() {
    return dataUseAgreementBypassTime;
  }

  public void setDataUseAgreementBypassTime(Timestamp dataUseAgreementBypassTime) {
    this.dataUseAgreementBypassTime = dataUseAgreementBypassTime;
  }

  @Column(name = "data_use_agreement_signed_version")
  @Override
  public Integer getDataUseAgreementSignedVersion() {
    return dataUseAgreementSignedVersion;
  }

  public void setDataUseAgreementSignedVersion(Integer dataUseAgreementSignedVersion) {
    this.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion;
  }

  @Column(name = "compliance_training_completion_time")
  @Override
  public Timestamp getComplianceTrainingCompletionTime() {
    return complianceTrainingCompletionTime;
  }

  public void setComplianceTrainingCompletionTime(Timestamp complianceTrainingCompletionTime) {
    this.complianceTrainingCompletionTime = complianceTrainingCompletionTime;
  }

  public void clearComplianceTrainingCompletionTime() {
    this.complianceTrainingCompletionTime = null;
  }

  @Column(name = "compliance_training_bypass_time")
  @Override
  public Timestamp getComplianceTrainingBypassTime() {
    return complianceTrainingBypassTime;
  }

  public void setComplianceTrainingBypassTime(Timestamp complianceTrainingBypassTime) {
    this.complianceTrainingBypassTime = complianceTrainingBypassTime;
  }

  @Column(name = "compliance_training_expiration_time")
  @Override
  public Timestamp getComplianceTrainingExpirationTime() {
    return complianceTrainingExpirationTime;
  }

  public void setComplianceTrainingExpirationTime(Timestamp complianceTrainingExpirationTime) {
    this.complianceTrainingExpirationTime = complianceTrainingExpirationTime;
  }

  public void clearComplianceTrainingExpirationTime() {
    this.complianceTrainingExpirationTime = null;
  }

  @Column(name = "beta_access_bypass_time")
  @Override
  public Timestamp getBetaAccessBypassTime() {
    return betaAccessBypassTime;
  }

  public void setBetaAccessBypassTime(Timestamp betaAccessBypassTime) {
    this.betaAccessBypassTime = betaAccessBypassTime;
  }

  @Column(name = "email_verification_completion_time")
  @Override
  public Timestamp getEmailVerificationCompletionTime() {
    return emailVerificationCompletionTime;
  }

  public void setEmailVerificationCompletionTime(Timestamp emailVerificationCompletionTime) {
    this.emailVerificationCompletionTime = emailVerificationCompletionTime;
  }

  @Column(name = "email_verification_bypass_time")
  @Override
  public Timestamp getEmailVerificationBypassTime() {
    return emailVerificationBypassTime;
  }

  public void setEmailVerificationBypassTime(Timestamp emailVerificationBypassTime) {
    this.emailVerificationBypassTime = emailVerificationBypassTime;
  }

  @Column(name = "era_commons_bypass_time")
  @Override
  public Timestamp getEraCommonsBypassTime() {
    return eraCommonsBypassTime;
  }

  public void setEraCommonsBypassTime(Timestamp eraCommonsBypassTime) {
    this.eraCommonsBypassTime = eraCommonsBypassTime;
  }

  @Column(name = "id_verification_completion_time")
  @Override
  public Timestamp getIdVerificationCompletionTime() {
    return idVerificationCompletionTime;
  }

  public void setIdVerificationCompletionTime(Timestamp idVerificationCompletionTime) {
    this.idVerificationCompletionTime = idVerificationCompletionTime;
  }

  @Column(name = "id_verification_bypass_time")
  @Override
  public Timestamp getIdVerificationBypassTime() {
    return idVerificationBypassTime;
  }

  public void setIdVerificationBypassTime(Timestamp idVerificationBypassTime) {
    this.idVerificationBypassTime = idVerificationBypassTime;
  }

  @Column(name = "two_factor_auth_completion_time")
  @Override
  public Timestamp getTwoFactorAuthCompletionTime() {
    return twoFactorAuthCompletionTime;
  }

  public void setTwoFactorAuthCompletionTime(Timestamp twoFactorAuthCompletionTime) {
    this.twoFactorAuthCompletionTime = twoFactorAuthCompletionTime;
  }

  @Column(name = "two_factor_auth_bypass_time")
  public Timestamp getTwoFactorAuthBypassTime() {
    return twoFactorAuthBypassTime;
  }

  public void setTwoFactorAuthBypassTime(Timestamp twoFactorAuthBypassTime) {
    this.twoFactorAuthBypassTime = twoFactorAuthBypassTime;
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public Set<DbDemographicSurvey> getDemographicSurveys() {
    return demographicSurveys;
  }

  public void setDemographicSurveys(Set<DbDemographicSurvey> demographicSurveys) {
    // Somehow Hibernate is giving me a set with a single null element. This
    // little trick is probably not a great idea long term, but I don't want to
    // run any migrations just yet.
    this.demographicSurveys = demographicSurveys;
  }

  @Transient
  public DbDemographicSurvey getDemographicSurvey() {
    return demographicSurveys.stream().findFirst().orElse(null);
  }

  @Transient
  public void setDemographicSurvey(DbDemographicSurvey demographicSurvey) {
    this.demographicSurveys.clear();
    this.demographicSurveys.add(demographicSurvey);
  }

  @Column(name = "last_modified_time")
  @Override
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  @Column(name = "creation_time")
  @Override
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "professional_url")
  @Override
  public String getProfessionalUrl() {
    return professionalUrl;
  }

  public void setProfessionalUrl(String professionalUrl) {
    this.professionalUrl = professionalUrl;
  }

  @Deprecated
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public Set<DbAddress> getAddresses() {
    return addresses;
  }

  @Deprecated
  public void setAddresses(Set<DbAddress> addresses) {
    this.addresses = addresses;
  }

  @Transient
  public DbAddress getAddress() {
    if (addresses == null) {
      return null;
    } else {
      return addresses.stream().findFirst().orElse(null);
    }
  }

  // Mock single-value semantics (which is how we use this anyway).
  public void setAddress(DbAddress address) {
    this.addresses.clear();
    if (address != null) {
      this.addresses.add(address);
    }
  }

  // null-friendly versions of equals() and hashCode() for DbVerifiedInstitutionalAffiliation
  // can be removed once we have a proper equals() / hashCode()

  public static boolean equalUsernames(DbUser a, DbUser b) {
    return Objects.equals(
        Optional.ofNullable(a).map(DbUser::getUsername),
        Optional.ofNullable(b).map(DbUser::getUsername));
  }

  public static int usernameHashCode(DbUser dbUser) {
    return (dbUser == null) ? 0 : Objects.hashCode(dbUser.getUsername());
  }

  /** Returns a name for the VM / cluster to be created for this user. */
  @Transient
  public String getClusterName() {
    return CLUSTER_NAME_PREFIX + getUserId();
  }
}
