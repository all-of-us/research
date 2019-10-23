package org.pmiops.workbench.firecloud;

import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMembership;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudJWTWrapper;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;

/**
 * Encapsulate Firecloud API interaction details and provide a simple/mockable interface for
 * internal use.
 */
public interface FireCloudService {

  String WORKSPACE_DELIMITER = "__";

  /** @return true if firecloud is okay, false if firecloud is down. */
  boolean getFirecloudStatus();

  /** @return the FireCloud profile for the requesting user. */
  Me getMe();

  /**
   * Registers the user in Firecloud.
   *
   * @param contactEmail an email address that can be used to contact this user
   * @param firstName the user's first name
   * @param lastName the user's last name
   */
  void registerUser(String contactEmail, String firstName, String lastName);

  /** Creates a billing project owned by AllOfUs. */
  void createAllOfUsBillingProject(String projectName);

  /** Get Billing Project Status */
  BillingProjectStatus getBillingProjectStatus(String projectName);

  /** Adds the specified user to the specified billing project. */
  void addUserToBillingProject(String email, String projectName);

  /**
   * Removes the specified user from the specified billing project.
   *
   * <p>Only used for errored billing projects
   */
  void removeUserFromBillingProject(String email, String projectName);

  /** Adds the specified user as an owner to the specified billing project. */
  void addOwnerToBillingProject(String ownerEmail, String projectName);

  /**
   * Removes the specified user as an owner from the specified billing project. Since FireCloud
   * users cannot remove themselves, we need to supply the credential of a different user which will
   * retain ownership to make the call
   *
   * <p>Only used for billing project garbage collection
   */
  void removeOwnerFromBillingProject(
      String projectName, String ownerEmailToRemove, String callerAccessToken);

  /** Creates a new FC workspace. */
  void createWorkspace(String projectName, String workspaceName);

  void cloneWorkspace(String fromProject, String fromName, String toProject, String toName);

  /** Retrieves all billing project memberships for the user from FireCloud. */
  List<BillingProjectMembership> getBillingProjectMemberships();

  WorkspaceACL getWorkspaceAcl(String projectName, String workspaceName);

  WorkspaceACLUpdateResponseList updateWorkspaceACL(
      String projectName, String workspaceName, List<WorkspaceACLUpdate> aclUpdates);

  /**
   * Requested field options specified here:
   * https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#heading=h.xgjl2srtytjt
   */
  WorkspaceResponse getWorkspace(String projectName, String workspaceName);

  List<WorkspaceResponse> getWorkspaces(List<String> fields);

  void deleteWorkspace(String projectName, String workspaceName);

  ManagedGroupWithMembers getGroup(String groupname);

  ManagedGroupWithMembers createGroup(String groupName);

  void addUserToGroup(String email, String groupName);

  void removeUserFromGroup(String email, String groupName);

  boolean isUserMemberOfGroup(String email, String groupName);

  String staticNotebooksConvert(byte[] notebook);

  /**
   * Fetches the status of the currently-authenticated user's linkage to NIH's eRA Commons system.
   *
   * <p>Returns null if the FireCloud user is not found or if the user has no NIH linkage.
   */
  NihStatus getNihStatus();

  NihStatus postNihCallback(JWTWrapper wrapper);

  ApiClient getApiClientWithImpersonation(String email) throws IOException;
}
