package org.pmiops.workbench.access;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;

public interface AccessTierService {
  String REGISTERED_TIER_SHORT_NAME = "registered";

  /**
   * Return all access tiers in the database
   *
   * @return the List of all DbAccessTiers in the database
   */
  List<DbAccessTier> getAllTiers();

  /**
   * Return the access tier referred to by the shortName in the database
   *
   * @param shortName the short name of the access tier to look up in the database
   * @return an Option\<DbAccessTier\> if one matches the shortName passed in, EMPTY otherwise
   */
  Optional<DbAccessTier> getAccessTier(String shortName);

  /**
   * Return the Registered Tier if it exists in the database
   *
   * @return a DbAccessTier representing the Registered Tier
   * @throws ServerErrorException if there is no Registered Tier
   */
  DbAccessTier getRegisteredTier();

  /**
   * Add memberships to all tiers for a user if they don't exist by inserting DB row(s). For any
   * memberships which exist, update them (whether enabled or not) set them to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mappings we're updating
   */
  void addUserToAllTiers(DbUser user);

  /**
   * Remove a Registered Tier membership from a user if one exists by marking that membership as
   * DISABLED. Do nothing if no membership exists.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  void removeUserFromRegisteredTier(DbUser user);

  /**
   * Add a Registered Tier membership to a user if none exists by inserting a DB row. If such a
   * membership exists (whether enabled or not) set it to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  void addUserToRegisteredTier(DbUser user);

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
  List<DbAccessTier> getAccessTiersForUser(DbUser user);
}
