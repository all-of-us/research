package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectBufferEntryDao
    extends CrudRepository<DbBillingProjectBufferEntry, Long> {

  String ASSIGNING_LOCK = "ASSIGNING_LOCK";

  DbBillingProjectBufferEntry findByFireCloudProjectName(String fireCloudProjectName);

  @Query("SELECT COUNT(*) FROM DbBillingProjectBufferEntry WHERE status IN (0, 2)")
  Long getCurrentBufferSize();

  List<DbBillingProjectBufferEntry> findAllByStatusAndLastStatusChangedTimeLessThan(
      short status, Timestamp timestamp);

  List<DbBillingProjectBufferEntry> findTop5ByStatusOrderByLastSyncRequestTimeAsc(short status);

  DbBillingProjectBufferEntry findFirstByStatusOrderByCreationTimeAsc(short status);

  Long countByStatus(short status);

  default Map<BufferEntryStatus, Long> getCountByStatusMap() {
    return computeProjectCountByStatus().stream()
        .collect(ImmutableMap.toImmutableMap(
            StatusToCountResult::getStatusEnum,
            StatusToCountResult::getNumProjects));
  }

  @Query(value = "select status, count(billing_project_buffer_entry_id) as numpNrojects\n"
      + "    from DbBillingProjectBufferEntry \n"
      + "group by status\n"
      + "order by status")
  List<StatusToCountResult> computeProjectCountByStatus();

  @Query(value = "SELECT GET_LOCK('" + ASSIGNING_LOCK + "', 1)", nativeQuery = true)
  int acquireAssigningLock();

  @Query(value = "SELECT RELEASE_LOCK('" + ASSIGNING_LOCK + "')", nativeQuery = true)
  int releaseAssigningLock();

  // get Billing Projects which are ASSIGNED to Workspaces which have been DELETED and have NEW
  // Billing Migration Status.  These are ready to be garbage-collected
  default List<String> findBillingProjectsForGarbageCollection() {
    return findByStatusAndActiveStatusAndBillingMigrationStatus(
        DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.ASSIGNED),
        DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.DELETED),
        DbStorageEnums.billingMigrationStatusToStorage(BillingMigrationStatus.NEW));
  }

  @Query(
      "SELECT p.fireCloudProjectName "
          + "FROM DbBillingProjectBufferEntry p "
          + "JOIN DbWorkspace w "
          + "ON w.workspaceNamespace = p.fireCloudProjectName "
          + "AND p.status = :billingStatus "
          + "AND w.activeStatus = :workspaceStatus "
          + "AND w.billingMigrationStatus = :migrationStatus")
  List<String> findByStatusAndActiveStatusAndBillingMigrationStatus(
      @Param("billingStatus") short billingStatus,
      @Param("workspaceStatus") short workspaceStatus,
      @Param("migrationStatus") short migrationStatus);

  interface StatusToCountResult {
    short getStatus();
    long getNumProjects();

    default BufferEntryStatus getStatusEnum() {
      return DbStorageEnums.billingProjectBufferEntryStatusFromStorage(getStatus());
    }
  }
}
