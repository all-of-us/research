package org.pmiops.workbench.api;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.UserResponse;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApiDelegate {

  private static final Logger log = Logger.getLogger(UserController.class.getName());
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final String DEFAULT_SORT_FIELD = "email";
  private static final Function<User, org.pmiops.workbench.model.User> TO_USER_RESPONSE_USER =
      user -> {
        org.pmiops.workbench.model.User modelUser = new org.pmiops.workbench.model.User();
        modelUser.setEmail(user.getEmail());
        modelUser.setGivenName(user.getGivenName());
        modelUser.setFamilyName(user.getFamilyName());
        return modelUser;
      };

  private Provider<User> userProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final UserService userService;
  private final FireCloudService fireCloudService;

  @Autowired
  public UserController(
      Provider<User> userProvider,
      Provider<WorkbenchConfig> configProvider,
      FireCloudService fireCloudService,
      UserService userService) {
    this.userProvider = userProvider;
    this.configProvider = configProvider;
    this.userService = userService;
    this.fireCloudService = fireCloudService;
  }

  @Override
  public ResponseEntity<UserResponse> user(
      String term, String pageToken, Integer size, String sortOrder) {
    UserResponse response = new UserResponse();
    response.setUsers(Collections.emptyList());
    response.setNextPageToken("");

    if (null == term || term.isEmpty()) {
      return ResponseEntity.ok(response);
    }

    PaginationToken paginationToken;
    try {
      paginationToken = getPaginationTokenFromPageToken(pageToken);
    } catch (IllegalArgumentException | BadRequestException e) {
      return ResponseEntity.badRequest().body(response);
    }

    // See discussion on RW-2894. This may not be strictly necessary, especially if researchers
    // details will be published publicly, but it prevents arbitrary unregistered users from seeing
    // limited researcher profile details.
    WorkbenchConfig config = configProvider.get();
    if (config.firecloud.enforceRegistered
        && !fireCloudService.isUserMemberOfGroup(
            userProvider.get().getEmail(), config.firecloud.registeredDomainName)) {
      throw new ForbiddenException("user search requires registered data access");
    }
    Sort.Direction direction =
        Sort.Direction.fromOptionalString(sortOrder).orElse(Sort.Direction.ASC);
    Sort sort = new Sort(new Sort.Order(direction, DEFAULT_SORT_FIELD));

    // What we are really looking for here are users who have a FC account.
    // This should exist if they have signed in at least once
    List<User> users =
        userService.findUsersBySearchString(term, sort).stream()
            .filter(user -> user.getFirstSignInTime() != null)
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
      List<org.pmiops.workbench.model.User> modelUsers =
          pagedUsers.get(pageOffset).stream()
              .map(TO_USER_RESPONSE_USER)
              .collect(Collectors.toList());
      response.setUsers(modelUsers);
    } else {
      log.warning(
          String.format(
              "User attempted autocomplete for a paged result that doesn't exist. Term: %s. Page: %d",
              term, pageOffset));
      return ResponseEntity.badRequest().body(response);
    }
    return ResponseEntity.ok(response);
  }

  private PaginationToken getPaginationTokenFromPageToken(String pageToken) {
    return (null == pageToken) ? PaginationToken.of(0) : PaginationToken.fromBase64(pageToken);
  }
}
