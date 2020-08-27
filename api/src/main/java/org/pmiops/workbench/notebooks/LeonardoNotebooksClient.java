package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.model.Cluster;
import org.pmiops.workbench.leonardo.model.ListClusterResponse;
import org.pmiops.workbench.notebooks.model.StorageLink;

/**
 * Encapsulate Leonardo's Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface LeonardoNotebooksClient {
  List<ListClusterResponse> listClustersByProject(String googleProject);

  /** lists all notebook clusters as the appengine SA, to be used only for admin operations */
  List<ListClusterResponse> listClustersByProjectAsService(String googleProject);

  /**
   * Creates a notebooks cluster owned by the current authenticated user.
   *
   * @param googleProject the google project that will be used for this notebooks cluster
   * @param clusterName the user assigned/auto-generated name for this notebooks cluster
   * @param workspaceFirecloudName the firecloudName of the workspace this cluster is associated
   *     with
   */
  Cluster createCluster(String googleProject, String clusterName, String workspaceFirecloudName)
      throws WorkbenchException;

  /** Deletes a notebook cluster */
  void deleteCluster(String googleProject, String clusterName) throws WorkbenchException;

  /** Deletes a notebook cluster as the appengine SA, to be used only for admin operations */
  void deleteClusterAsService(String googleProject, String clusterName) throws WorkbenchException;

  /** Gets information about a notebook cluster */
  Cluster getCluster(String googleProject, String clusterName) throws WorkbenchException;

  /** Send files over to notebook Cluster */
  void localize(String googleProject, String clusterName, Map<String, String> fileList)
      throws WorkbenchException;

  /** Create a new data synchronization storage link on a Welder-enabled cluster. */
  StorageLink createStorageLink(String googleProject, String clusterName, StorageLink storageLink);

  /** @return true if notebooks is okay, false if notebooks are down. */
  boolean getNotebooksStatus();
}
