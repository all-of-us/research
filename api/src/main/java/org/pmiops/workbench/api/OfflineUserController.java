package org.pmiops.workbench.api;

import com.google.api.services.cloudresourcemanager.model.ResourceId;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to user management. */
@RestController
public class OfflineUserController implements OfflineUserApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineUserController.class.getName());
  private static final List<String> WHITELISTED_ORG_IDS =
      Arrays.asList(
          "400176686919", // test.firecloud.org
          "386193000800", // firecloud.org
          "394551486437" // pmi-ops.org
          );

  private final CloudResourceManagerService cloudResourceManagerService;
  private final UserService userService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public OfflineUserController(
      CloudResourceManagerService cloudResourceManagerService,
      UserService userService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Updates moodle information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   *
   * <p>This would only ever catch the following scenarios: 1. The user's compliance training
   * expires. The time scale on this expiration is O(years), so syncing nightly is entirely
   * acceptable.
   *
   * <p>2. The user completes compliance and doesn't return to the dashboard. We'd progress them in
   * the background. This won't make much of a difference and would have the same effect as if they
   * had navigated back to the dashboard.
   *
   * <p>3. Somehow the user manages to "uncomplete" training in Moodle. There is no direct process
   * for this today, but if it happened, it's certainly an edge case where nightly would be
   * acceptable latency
   */
  @Override
  public ResponseEntity<Void> bulkSyncComplianceTrainingStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsersExcludingDisabled()) {
      userCount++;
      try {
        Timestamp oldTime = user.getComplianceTrainingCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        DbUser updatedUser = userService.syncComplianceTrainingStatusV2(user, Agent.asSystem());

        Timestamp newTime = updatedUser.getComplianceTrainingCompletionTime();
        DataAccessLevel newLevel = updatedUser.getDataAccessLevelEnum();

        if (!Objects.equals(newTime, oldTime)) {
          log.info(
              String.format(
                  "Compliance training completion changed for user %s. Old %s, new %s",
                  user.getUsername(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(
              String.format(
                  "Data access level changed for user %s. Old %s, new %s",
                  user.getUsername(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (org.pmiops.workbench.moodle.ApiException | NotFoundException e) {
        errorCount++;
        log.log(
            Level.SEVERE,
            String.format(
                "Error syncing compliance training status for user %s", user.getUsername()),
            e);
      }
    }

    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during compliance training sync", errorCount));
    }

    return ResponseEntity.noContent().build();
  }

  /**
   * Updates eRA Commons information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncEraCommonsStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsersExcludingDisabled()) {
      userCount++;
      try {
        // User accounts are registered with Terra on first sign-in. Users who have never signed in
        // are therefore unusable for impersonated calls to Terra to check on their eRA commons
        // status.
        if (user.getFirstSignInTime() == null) {
          continue;
        }

        Timestamp oldTime = user.getEraCommonsCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        DbUser updatedUser =
            userService.syncEraCommonsStatusUsingImpersonation(user, Agent.asSystem());

        Timestamp newTime = updatedUser.getEraCommonsCompletionTime();
        DataAccessLevel newLevel = user.getDataAccessLevelEnum();

        if (!Objects.equals(newTime, oldTime)) {
          log.info(
              String.format(
                  "eRA Commons completion changed for user %s. Old %s, new %s",
                  user.getUsername(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(
              String.format(
                  "Data access level changed for user %s. Old %s, new %s",
                  user.getUsername(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (org.pmiops.workbench.firecloud.ApiException e) {
        errorCount++;
        log.severe(
            String.format(
                "Error syncing eRA Commons status for user %s: %s",
                user.getUsername(), e.getMessage()));
      } catch (IOException e) {
        errorCount++;
        log.severe(
            String.format(
                "Error fetching impersonated creds for user %s: %s",
                user.getUsername(), e.getMessage()));
      }
    }

    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during eRA Commons sync", errorCount));
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates 2FA information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncTwoFactorAuthStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsersExcludingDisabled()) {
      userCount++;
      try {
        Timestamp oldTime = user.getTwoFactorAuthCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        DbUser updatedUser = userService.syncTwoFactorAuthStatus(user, Agent.asSystem());

        Timestamp newTime = updatedUser.getTwoFactorAuthCompletionTime();
        DataAccessLevel newLevel = user.getDataAccessLevelEnum();

        if (!Objects.equals(newTime, oldTime)) {
          log.info(
              String.format(
                  "Two-factor auth completion changed for user %s. Old %s, new %s",
                  user.getUsername(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(
              String.format(
                  "Data access level changed for user %s. Old %s, new %s",
                  user.getUsername(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (Exception e) {
        errorCount++;
        log.severe(
            String.format(
                "Error syncing two-factor auth status for user %s: %s",
                user.getUsername(), e.getMessage()));
      }
    }

    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during two-factor auth sync", errorCount));
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Audits GCP access for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkAuditProjectAccess() {
    int errorCount = 0;
    // For now, continue checking both enabled and disabled users. If needed for performance, this
    // could be scoped down to just enabled users. However, access to other GCP resources could also
    // indicate general Google account abuse, which may be a concern regardless of whether or not
    // the user has been disabled in the Workbench.
    List<DbUser> users = userService.getAllUsers();
    for (DbUser user : users) {
      // TODO(RW-2062): Move to using the gcloud api for list all resources when it is available.
      try {
        List<String> unauthorizedLogs =
            cloudResourceManagerService.getAllProjectsForUser(user).stream()
                .filter(
                    project ->
                        project.getParent() == null
                            || !(WHITELISTED_ORG_IDS.contains(project.getParent().getId())))
                .map(
                    project ->
                        project.getName()
                            + " in organization "
                            + Optional.ofNullable(project.getParent())
                                .map(ResourceId::getId)
                                .orElse("[none]"))
                .collect(Collectors.toList());
        if (unauthorizedLogs.size() > 0) {
          log.warning(
              "User "
                  + user.getUsername()
                  + " has access to projects: "
                  + String.join(", ", unauthorizedLogs));
        }
      } catch (IOException e) {
        log.log(Level.SEVERE, "failed to audit project access for user " + user.getUsername(), e);
        errorCount++;
      }
    }
    if (errorCount > 0) {
      log.severe(String.format("encountered errors on %d/%d users", errorCount, users.size()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    log.info(String.format("successfully audited %d users", users.size()));
    return ResponseEntity.noContent().build();
  }
}
