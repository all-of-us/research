package org.pmiops.workbench.access;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.TierAccessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessTierServiceImpl implements AccessTierService {

  private final Provider<WorkbenchConfig> configProvider;

  private final AccessTierDao accessTierDao;
  private final UserAccessTierDao userAccessTierDao;

  @Autowired
  public AccessTierServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      AccessTierDao accessTierDao,
      UserAccessTierDao userAccessTierDao) {
    this.configProvider = configProvider;
    this.accessTierDao = accessTierDao;
    this.userAccessTierDao = userAccessTierDao;
  }

  /**
   * Return all access tiers in the database
   *
   * @return the List of all DbAccessTiers in the database
   */
  public List<DbAccessTier> getAllTiers() {
    return accessTierDao.findAll();
  }

  /**
   * Return the access tier referred to by the shortName in the database
   *
   * @param shortName the short name of the access tier to look up in the database
   * @return an {@code Option<DbAccessTier>} if one matches the shortName passed in, EMPTY otherwise
   */
  public Optional<DbAccessTier> getAccessTier(String shortName) {
    return accessTierDao.findOneByShortName(shortName);
  }

  /**
   * Return the Registered Tier if it exists in the database
   *
   * @return a DbAccessTier representing the Registered Tier
   * @throws ServerErrorException if there is no Registered Tier
   */
  public DbAccessTier getRegisteredTier() {
    return getAccessTier(REGISTERED_TIER_SHORT_NAME)
        .orElseThrow(() -> new ServerErrorException("Cannot find Registered Tier in database."));
  }

  /**
   * Add memberships to all tiers for a user if they don't exist by inserting DB row(s). For any
   * memberships which exist, update them (whether enabled or not) set them to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mappings we're updating
   */
  public void addUserToAllTiers(DbUser user) {
    getAllTiers().forEach(tier -> addUserToTier(user, tier));
  }

  /**
   * Add a Registered Tier membership to a user if none exists by inserting a DB row. If such a
   * membership exists (whether enabled or not) set it to ENABLED.
   *
   * <p>Currently, this does not synchronize Terra Auth Domain group membership, but it will do so
   * when the user_access_tier table is the source of truth for tier membership. The existing method
   * UserServiceImpl.addToRegisteredTierGroupIdempotent() continues to handle group membership until
   * then.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  public void addUserToRegisteredTier(DbUser user) {
    addUserToTier(user, getRegisteredTier());
  }

  /**
   * Remove a Registered Tier membership from a user if one exists by marking that membership as
   * DISABLED. Do nothing if no membership exists.
   *
   * <p>Currently, this does not synchronize Terra Auth Domain group membership, but it will do so
   * when the user_access_tier table is the source of truth for tier membership. The existing method
   * UserServiceImpl.removeFromRegisteredTierGroupIdempotent() continues to handle group membership
   * until then.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  public void removeUserFromRegisteredTier(DbUser user) {
    removeUserFromTier(user, getRegisteredTier());
  }

  /**
   * Add a tier membership to a user if none exists by inserting a DB row. If such a membership
   * exists (whether enabled or not) set it to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  private void addUserToTier(DbUser user, DbAccessTier accessTier) {
    Optional<DbUserAccessTier> existingEntryMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, accessTier);

    if (existingEntryMaybe.isPresent()) {
      final DbUserAccessTier entryToUpdate =
          existingEntryMaybe.get().setTierAccessStatus(TierAccessStatus.ENABLED).setLastUpdated();
      userAccessTierDao.save(entryToUpdate);
    } else {
      final DbUserAccessTier entryToInsert =
          new DbUserAccessTier()
              .setUser(user)
              .setAccessTier(accessTier)
              .setTierAccessStatus(TierAccessStatus.ENABLED)
              .setFirstEnabled()
              .setLastUpdated();
      userAccessTierDao.save(entryToInsert);
    }
  }

  /**
   * Remove a tier membership from a user if one exists by marking that membership as DISABLED. Do
   * nothing if no membership exists.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  private void removeUserFromTier(DbUser user, DbAccessTier accessTier) {
    userAccessTierDao
        .getByUserAndAccessTier(user, accessTier)
        .ifPresent(
            entryToSoftDelete ->
                userAccessTierDao.save(
                    entryToSoftDelete
                        .setTierAccessStatus(TierAccessStatus.DISABLED)
                        .setLastUpdated()));
  }

  /**
   * A placeholder implementation until we establish userAccessTierDao as the source of truth for
   * access tier membership.
   *
   * <p>For registered users, return the registered tier or all tiers if we're in an environment
   * which has enabled all tiers for registered users. Return no access tiers for unregistered
   * users.
   *
   * @param user the user whose access we're checking
   * @return The List of DbAccessTiers the DbUser has access to in this environment
   */
  public List<DbAccessTier> getAccessTiersForUser(DbUser user) {
    if (user.getDataAccessLevelEnum() == DataAccessLevel.REGISTERED) {
      if (configProvider.get().featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers) {
        return accessTierDao.findAll();
      } else {
        return ImmutableList.of(getRegisteredTier());
      }
    } else {
      return Collections.emptyList();
    }
  }
}
