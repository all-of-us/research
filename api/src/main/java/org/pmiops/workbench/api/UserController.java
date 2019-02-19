package org.pmiops.workbench.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.UserResponse;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApiDelegate {

  private static final Logger log = Logger.getLogger(UserController.class.getName());
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final String DEFAULT_SORT_FIELD = "email";
  private static final Function<User, org.pmiops.workbench.model.User> TO_USER_RESPONSE_USER = user -> {
    org.pmiops.workbench.model.User modelUser = new org.pmiops.workbench.model.User();
    modelUser.setEmail(user.getEmail());
    modelUser.setGivenName(user.getGivenName());
    modelUser.setFamilyName(user.getFamilyName());
    return modelUser;
  };

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Updates moodle information for all User in user Database
   * @return
   */
  @Override
  public ResponseEntity<Void> bulkSyncTrainingStatus() {
    List<User> allUsers = userService.getAllUsers();
    allUsers.parallelStream().forEach(user -> {
      try {
        userService.syncUserTraining(user);
      } catch (NotFoundException ex){
        log.severe(String.format("User Not found Exception: %s For user id: %s", ex.getMessage(),
            user.getUserId()));
      } catch (ApiException ex) {
        log.severe(String.format("Exception: %s For user id: %s",
            ex.getMessage(), user.getUserId()));
      }
    });
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<UserResponse> user(String term, String pageToken, Integer size, String sortOrder) {
    UserResponse response = new UserResponse();
    response.setUsers(Collections.emptyList());
    response.setNextPageToken("");
    response.setQuery(term);

    if (null == term || term.isEmpty()) {
      return ResponseEntity.ok(response);
    }

    PaginationToken paginationToken;
    try {
      paginationToken = getPaginationTokenFromPageToken(pageToken);
    } catch (IllegalArgumentException | BadRequestException e) {
      return ResponseEntity.badRequest().body(response);
    }

    Sort.Direction direction = Optional
        .ofNullable(Sort.Direction.fromStringOrNull(sortOrder))
        .orElse(Sort.Direction.ASC);
    Sort sort = new Sort(new Sort.Order(direction, DEFAULT_SORT_FIELD));
    // We want to filter out users not initialized in firecloud yet to avoid sharing with a not yet existent user.
    List<User> users = userService.findUsersBySearchString(term, sort)
        .stream()
        .filter(user -> user.getFreeTierBillingProjectName() != null)
        .collect(Collectors.toList());
    int pageSize = Optional.ofNullable(size).orElse(DEFAULT_PAGE_SIZE);
    List<List<User>> pagedUsers = Lists.partition(users, pageSize);

    int pageOffset = Long.valueOf(paginationToken.getOffset()).intValue();

    if (pagedUsers.size() == 0) {
      return ResponseEntity.ok(response);
    }

    if (pageOffset < pagedUsers.size()) {
      boolean hasNext = pageOffset < pagedUsers.size() - 1;
      if (hasNext) {
        response.setNextPageToken(PaginationToken.of(pageOffset + 1).toBase64());
      }
      List<org.pmiops.workbench.model.User> modelUsers = pagedUsers
          .get(pageOffset)
          .stream()
          .map(TO_USER_RESPONSE_USER)
          .collect(Collectors.toList());
      response.setUsers(modelUsers);
    } else {
      log.warning(String.format("User attempted autocomplete for a paged result that doesn't exist. Term: %s. Page: %d", term, pageOffset));
      return ResponseEntity.badRequest().body(response);
    }
    return ResponseEntity.ok(response);
  }

  private PaginationToken getPaginationTokenFromPageToken(String pageToken) {
    return (null == pageToken) ?
        PaginationToken.of(0) :
        PaginationToken.fromBase64(pageToken);
  }

}
