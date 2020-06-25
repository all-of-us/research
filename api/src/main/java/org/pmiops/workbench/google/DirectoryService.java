package org.pmiops.workbench.google;

import com.google.api.services.directory.model.User;
import java.util.Optional;

/**
 * Google APIs for handling GSuite user accounts.
 *
 * <p>Terminology used by this service: * username: The GSuite primary email address, e.g.
 * "jdoe@researchallofus.org". This is consistent with most usage in RW, where `username` refers to
 * the full email address. * user prefix: The user-specific prefix of a GSuite email address, e.g.
 * "jdoe". * contact email: The user's specified contact email address, which is stored in GSuite as
 * well as the RW database.
 */
public interface DirectoryService {
  /** Returns whether the given user prefix corresponds to an existing GSuite user account. */
  boolean isUsernameTaken(String userPrefix);

  /** Returns a user via username lookup. Returns null if no user was found. */
  User getUser(String username);

  /** Looks up a user by username and returns their stored contact email address, if available. */
  Optional<String> getContactEmail(String username);

  User createUser(String givenName, String familyName, String username, String contactEmail);

  User resetUserPassword(String username);

  void deleteUser(String username);
}
