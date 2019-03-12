package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.notebooks.model.Cluster;

/**
 * Encapsulate Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface NotebooksService {
  String DEFAULT_CLUSTER_NAME = "all-of-us";

  /**
   * Creates a notebooks cluster owned by the current authenticated user.
   * @param googleProject the google project that will be used for this notebooks cluster
   * @param clusterName the user assigned/auto-generated name for this notebooks cluster
   */
  Cluster createCluster(String googleProject, String clusterName)
      throws WorkbenchException;

  /**
   * Deletes a notebook cluster
   */
  void deleteCluster(String googleProject, String clusterName) throws WorkbenchException;

  /**
   * Lists all existing clusters
   */
  List<Cluster> listClusters(String labels, boolean includeDeleted) throws WorkbenchException;

  /**
   * Gets information about a notebook cluster
   */
  Cluster getCluster(String googleProject, String clusterName) throws WorkbenchException;

  /**
   * Send files over to notebook Cluster
   */
  void localize(String googleProject, String clusterName, Map<String, String> fileList)
      throws WorkbenchException;

  /**
   * @return true if notebooks is okay, false if notebooks are down.
   */
  boolean getNotebooksStatus();
}
