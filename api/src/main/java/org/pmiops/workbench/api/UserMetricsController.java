package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class UserMetricsController implements UserMetricsApiDelegate {
  private Provider<User> userProvider;
  private UserRecentResourceService userRecentResourceService;
  private WorkspaceService workspaceService;
  private FireCloudService fireCloudService;
  private static final String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks/";
  private int distinctWorkspacelimit = 5;
  private Clock clock;

  //Converts DB model to client Model
  private final Function<UserRecentResource, RecentResource> TO_CLIENT =
      userRecentResource -> {
        RecentResource resource = new RecentResource();
        resource.setCohort(TO_CLIENT_COHORT.apply(userRecentResource.getCohort()));
        if (userRecentResource.getNotebookName() != null) {
          FileDetail fileDetail = new FileDetail();
          String[] notebookDetails = userRecentResource.getNotebookName().split(NOTEBOOKS_WORKSPACE_DIRECTORY);
          fileDetail.setPath(notebookDetails[0] + NOTEBOOKS_WORKSPACE_DIRECTORY);
          fileDetail.setName(notebookDetails[1]);
          resource.setNotebook(fileDetail);
        }
        resource.setModifiedTime(userRecentResource.getLastAccessDate().toString());
        resource.setWorkspaceId(userRecentResource.getWorkspaceId());
        return resource;
      };

  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          if (cohort == null) {
            return null;
          }
          Cohort result = new Cohort()
              .etag(Etags.fromVersion(cohort.getVersion()))
              .lastModifiedTime(cohort.getLastModifiedTime().getTime())
              .creationTime(cohort.getCreationTime().getTime())
              .criteria(cohort.getCriteria())
              .description(cohort.getDescription())
              .id(cohort.getCohortId())
              .name(cohort.getName())
              .type(cohort.getType());
          if (cohort.getCreator() != null) {
            result.setCreator(cohort.getCreator().getEmail());
          }
          return result;
        }
      };

  @Autowired
  UserMetricsController(Provider<User> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Clock clock) {
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.clock = clock;
  }

  @VisibleForTesting
  public void setDistinctWorkspaceLimit(int limit) {
    distinctWorkspacelimit = limit;
  }


  @Override
  public ResponseEntity<RecentResource> updateNotebookEntry(String workspaceNamespace, String workspaceId, String notebook) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    UserRecentResource addedEntry = userRecentResourceService.updateNotebookEntry(wId, userProvider.get().getUserId(), notebook, now);
    return ResponseEntity.ok(TO_CLIENT.apply(addedEntry));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteRecentResource(String workspaceNamespace, String workspaceId, String notebook) {
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    userRecentResourceService.deleteNotebookEntry(wId, userProvider.get().getUserId(), notebook);
    return ResponseEntity.ok(new EmptyResponse());
  }

  /**
   * Gets the list of all resources recently access by user in order of access date time
   *
   * @return
   */
  @Override
  public ResponseEntity<RecentResourceResponse> getUserRecentResources() {
    long userId = userProvider.get().getUserId();
    RecentResourceResponse recentResponse = new RecentResourceResponse();
    List<UserRecentResource> userRecentResourceList = userRecentResourceService.findAllResourcesByUser(userId);
    List<Long> workspaceIdList = userRecentResourceList
        .stream()
        .map(UserRecentResource::getWorkspaceId)
        .distinct()
        .limit(distinctWorkspacelimit)
        .collect(Collectors.toList());

    Map<Long, String> workspaceAccessMap = workspaceIdList.stream().collect(Collectors.toMap(id -> id, id -> {
      Workspace workspace = workspaceService.findByWorkspaceId((long) id);
      WorkspaceResponse workspaceResponse = fireCloudService
          .getWorkspace(workspace.getWorkspaceNamespace(),
              workspace.getFirecloudName());
      return workspaceResponse.getAccessLevel();
    }));

    userRecentResourceList.stream()
        .filter(userRecentResource -> {
          return workspaceAccessMap.containsKey(userRecentResource.getWorkspaceId());
        })
        .forEach(userRecentResource -> {
          RecentResource resource = TO_CLIENT.apply(userRecentResource);
          resource.setPermission(workspaceAccessMap.get(userRecentResource.getWorkspaceId()));
          recentResponse.add(resource);
        });
    return ResponseEntity.ok(recentResponse);
  }

  //Retrieves Database workspace ID
  private long getWorkspaceId(String workspaceNamespace, String workspaceId) {
    Workspace dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId);
    return dbWorkspace.getWorkspaceId();
  }
}


