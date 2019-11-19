package org.pmiops.workbench.api;

import org.pmiops.workbench.actionaudit.adapters.AuthDomainAuditAdapter;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthDomainController implements AuthDomainApiDelegate {

  private final FireCloudService fireCloudService;
  private final UserService userService;
  private final UserDao userDao;
  private AuthDomainAuditAdapter authDomainAuditAdapter;

  @Autowired
  AuthDomainController(
      FireCloudService fireCloudService,
      UserService userService,
      UserDao userDao,
      AuthDomainAuditAdapter authDomainAuditAdapter) {
    this.fireCloudService = fireCloudService;
    this.userService = userService;
    this.userDao = userDao;
    this.authDomainAuditAdapter = authDomainAuditAdapter;
  }

  @AuthorityRequired({Authority.DEVELOPER})
  @Override
  public ResponseEntity<EmptyResponse> createAuthDomain(String groupName) {
    fireCloudService.createGroup(groupName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Void> updateUserDisabledStatus(UpdateUserDisabledRequest request) {
    final DbUser user = userDao.findUserByEmail(request.getEmail());
    final Boolean previousDisabled = user.getDisabled();
    final DbUser updatedUser =
        userService.setDisabledStatus(user.getUserId(), request.getDisabled());
    authDomainAuditAdapter.fireSetAccountEnabled(
        user.getUserId(), !request.getDisabled(), !previousDisabled);
    userService.logAdminUserAction(
        user.getUserId(), "updated user disabled state", previousDisabled, request.getDisabled());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
