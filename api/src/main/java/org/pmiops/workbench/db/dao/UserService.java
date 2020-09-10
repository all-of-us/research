package org.pmiops.workbench.db.dao;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.dao.projection.PrjReportingUser;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.springframework.data.domain.Sort;

public interface UserService {
  DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser dbUser, Agent agent);

  DbUser createServiceAccountUser(String email);

  // version used by DevUserRegistrationService
  DbUser createUser(
      Userinfoplus oAuth2Userinfo,
      String contactEmail,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser createUser(
      String givenName,
      String familyName,
      String userName,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch,
      String professionalUrl,
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser updateUserWithConflictHandling(DbUser user);

  // TODO(jaycarlton): Move compliance-related methods to a new UserComplianceService or similar
  DbUser submitDataUseAgreement(
      DbUser user, Integer dataUseAgreementSignedVersion, String initials);

  // Registers that a user has agreed to a given version of the Terms of Service.
  void submitTermsOfService(DbUser dbUser, Integer tosVersion);

  void setDataUseAgreementNameOutOfDate(String newGivenName, String newFamilyName);

  void setDataUseAgreementBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setComplianceTrainingBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setBetaAccessBypassTime(Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setEraCommonsBypassTime(Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setTwoFactorAuthBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  DbUser setDisabledStatus(Long userId, boolean disabled);

  List<DbUser> getAllUsers();

  @Deprecated // use or create an auditor in org.pmiops.workbench.actionaudit.auditors
  void logAdminUserAction(long targetUserId, String targetAction, Object oldValue, Object newValue);

  @Deprecated // use or create an auditor in org.pmiops.workbench.actionaudit.auditors
  void logAdminWorkspaceAction(
      long targetWorkspaceId, String targetAction, Object oldValue, Object newValue);

  List<DbUser> findUsersBySearchString(String term, Sort sort);

  @Deprecated
  DbUser syncComplianceTrainingStatusV1()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  @Deprecated
  DbUser syncComplianceTrainingStatusV1(DbUser user, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2(DbUser user, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncEraCommonsStatus();

  DbUser syncEraCommonsStatusUsingImpersonation(DbUser user, Agent agent)
      throws IOException, org.pmiops.workbench.firecloud.ApiException;

  void syncTwoFactorAuthStatus();

  DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent);

  int getCurrentDuccVersion();

  Optional<DbUser> getByUsername(String username);

  // same as the above, but throw NotFoundException if not found
  DbUser getByUsernameOrThrow(String username);

  Optional<DbUser> getByDatabaseId(long databaseId);

  void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest);

  boolean hasAuthority(long userId, Authority required);

  Set<DbUser> findAllUsersWithAuthoritiesAndPageVisits();

  Optional<DbUser> findUserWithAuthoritiesAndPageVisits(long userId);

  List<PrjReportingUser> getRepotingUsers();
}
