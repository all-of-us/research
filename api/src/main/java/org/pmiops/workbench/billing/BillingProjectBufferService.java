package org.pmiops.workbench.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus.CreationStatusEnum;
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.pmiops.workbench.model.BillingProjectBufferStatusByTier;
import org.pmiops.workbench.model.BillingProjectBufferStatusByTierInner;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.utils.Comparables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectBufferService implements GaugeDataCollector {

  private static final Logger log = Logger.getLogger(BillingProjectBufferService.class.getName());

  private static final int PROJECT_BILLING_ID_SIZE = 8;
  @VisibleForTesting static final Duration CREATING_TIMEOUT = Duration.ofMinutes(60);
  @VisibleForTesting static final Duration ASSIGNING_TIMEOUT = Duration.ofMinutes(10);
  private static final ImmutableMap<BufferEntryStatus, Duration> STATUS_TO_GRACE_PERIOD =
      ImmutableMap.of(
          BufferEntryStatus.CREATING, CREATING_TIMEOUT,
          BufferEntryStatus.ASSIGNING, ASSIGNING_TIMEOUT);

  private final BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final AccessTierDao accessTierDao;

  @Autowired
  public BillingProjectBufferService(
      BillingProjectBufferEntryDao billingProjectBufferEntryDao,
      Clock clock,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      AccessTierDao accessTierDao) {
    this.billingProjectBufferEntryDao = billingProjectBufferEntryDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.accessTierDao = accessTierDao;
  }

  private Timestamp getCurrentTimestamp() {
    return new Timestamp(clock.instant().toEpochMilli());
  }

  /** Makes a configurable number of project creation attempts. */
  public void bufferBillingProjects() {
    WorkbenchConfig config = this.workbenchConfigProvider.get();
    for (int i = 0; i < config.billing.bufferRefillProjectsPerTask; i++) {
      accessTierDao.findAll().forEach(this::bufferBillingProject);
    }
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    final ImmutableMap<BufferEntryStatus, Long> entryStatusToCount =
        ImmutableMap.copyOf(billingProjectBufferEntryDao.getCountByStatusMap());

    return Arrays.stream(BufferEntryStatus.values())
        .map(
            status ->
                MeasurementBundle.builder()
                    .addMeasurement(
                        GaugeMetric.BILLING_BUFFER_PROJECT_COUNT,
                        entryStatusToCount.getOrDefault(status, 0L))
                    .addTag(MetricLabel.BUFFER_ENTRY_STATUS, status.toString())
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * Creates a new billing project in the buffer, and kicks off the FireCloud project creation.
   *
   * <p>No action is taken if the buffer is full.
   */
  private void bufferBillingProject(DbAccessTier accessTier) {
    if (getUnfilledBufferSpace(accessTier) <= 0) {
      log.fine(
          String.format(
              "Billing buffer for tier %s is at capacity: size = %d, capacity = %d",
              accessTier, getCurrentBufferSize(accessTier), getBufferMaxCapacity(accessTier)));
      return;
    }

    final DbBillingProjectBufferEntry creatingBufferEntry = makeCreatingBufferEntry(accessTier);
    fireCloudService.createAllOfUsBillingProject(
        creatingBufferEntry.getFireCloudProjectName(), accessTier.getServicePerimeter());
    log.info(
        String.format(
            "Created new project %s for access tier %s",
            creatingBufferEntry.toString(), accessTier.getDisplayName()));
  }

  @NotNull
  private DbBillingProjectBufferEntry makeCreatingBufferEntry(DbAccessTier accessTier) {
    final DbBillingProjectBufferEntry bufferEntry = new DbBillingProjectBufferEntry();
    bufferEntry.setFireCloudProjectName(createBillingProjectName());
    bufferEntry.setCreationTime(Timestamp.from(clock.instant()));
    bufferEntry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    bufferEntry.setAccessTier(accessTier.getShortName());
    return billingProjectBufferEntryDao.save(bufferEntry);
  }

  public void syncBillingProjectStatus() {
    List<DbBillingProjectBufferEntry> creatingEntriesToSync =
        billingProjectBufferEntryDao.findTop5ByStatusOrderByLastSyncRequestTimeAsc(
            DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.CREATING));
    if (creatingEntriesToSync.isEmpty()) {
      return;
    }

    int successfulSyncCount = 0;
    for (DbBillingProjectBufferEntry bufferEntry : creatingEntriesToSync) {
      //noinspection UnusedAssignment
      bufferEntry = syncBufferEntry(bufferEntry);
      successfulSyncCount += 1;
    }
    log.info(
        String.format(
            "Synchronized %d/%d billing projects.",
            successfulSyncCount, creatingEntriesToSync.size()));
  }

  private DbBillingProjectBufferEntry syncBufferEntry(DbBillingProjectBufferEntry bufferEntry) {
    bufferEntry.setLastSyncRequestTime(Timestamp.from(clock.instant()));

    try {
      final CreationStatusEnum fireCloudStatus =
          fireCloudService
              .getBillingProjectStatus(bufferEntry.getFireCloudProjectName())
              .getCreationStatus();
      switch (fireCloudStatus) {
        case READY:
          bufferEntry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
          log.info(
              String.format(
                  "SyncBillingProjectStatus: BillingProject %s available", bufferEntry.toString()));
          break;
        case ERROR:
          log.warning(
              String.format(
                  "SyncBillingProjectStatus: BillingProject %s creation failed",
                  bufferEntry.toString()));
          bufferEntry.setStatusEnum(BufferEntryStatus.ERROR, this::getCurrentTimestamp);
          break;
        case CREATING:
        case ADDINGTOPERIMETER:
        default:
          log.info(
              String.format(
                  "SyncBillingProjectStatus: BillingProject %s Firecloud status = %s",
                  bufferEntry.toString(), fireCloudStatus.toString()));
          break;
      }
    } catch (NotFoundException e) {
      log.log(
          Level.WARNING,
          String.format(
              "Failed to get Firecloud status for %s. Project not found.", bufferEntry.toString()),
          e);
    } catch (WorkbenchException e) {
      log.log(
          Level.WARNING, "Get BillingProjectStatus call failed for " + bufferEntry.toString(), e);
    }
    return billingProjectBufferEntryDao.save(bufferEntry);
  }

  /** Update any expired buffer entries in creating or assigning state to ERROR status. */
  public void cleanBillingBuffer() {
    Instant now = clock.instant();
    Iterables.concat(
            findExpiredCreatingEntries(now),
            // For the ASSIGNING status monitor, we can simply filter by the current time as this
            // status tracks internal state rather than the Firecloud status.
            findEntriesWithExpiredGracePeriod(now, BufferEntryStatus.ASSIGNING))
        .forEach(this::setBufferEntryErrorStatus);
  }

  private void setBufferEntryErrorStatus(DbBillingProjectBufferEntry bufferEntry) {
    log.warning(
        "CleanBillingBuffer: Setting status of "
            + bufferEntry.getFireCloudProjectName()
            + " to ERROR from "
            + bufferEntry.getStatusEnum());
    bufferEntry.setStatusEnum(BufferEntryStatus.ERROR, this::getCurrentTimestamp);
    //noinspection UnusedAssignment
    bufferEntry = billingProjectBufferEntryDao.save(bufferEntry);
  }

  private List<DbBillingProjectBufferEntry> findEntriesWithExpiredGracePeriod(
      Instant now, BufferEntryStatus bufferEntryStatus) {
    final Optional<Duration> gracePeriod =
        Optional.ofNullable(STATUS_TO_GRACE_PERIOD.get(bufferEntryStatus));

    return gracePeriod
        .map(p -> findEntriesWithExpiredGracePeriod(now, bufferEntryStatus, p))
        .orElse(Collections.emptyList());
  }

  private List<DbBillingProjectBufferEntry> findEntriesWithExpiredGracePeriod(
      Instant now, BufferEntryStatus bufferEntryStatus, Duration gracePeriod) {
    return billingProjectBufferEntryDao.findAllByStatusAndLastStatusChangedTimeLessThan(
        DbStorageEnums.billingProjectBufferEntryStatusToStorage(bufferEntryStatus),
        Timestamp.from(now.minus(gracePeriod)));
  }

  private List<DbBillingProjectBufferEntry> findExpiredCreatingEntries(Instant now) {
    return findEntriesWithExpiredGracePeriod(now, BufferEntryStatus.CREATING).stream()
        // Ensure that we've also synchronized the status since the deadline elapsed. This
        // covers degenerate cases where syncBillingProjectStatus might be backlogged and
        // hasn't caught up to check all the CREATING projects, or covers periods where
        // the Firecloud API is unresponsive. Ideally we'd push this logic directly down
        // into the DAO, but JPQL has poor support for date comparisons. Keep the above
        // filter as well as a first pass filter to limit what we pull into memory.
        .filter(entry -> entry.getLastStatusChangedTime() != null)
        .filter(entry -> entry.getLastSyncRequestTime() != null)
        .filter(
            entry ->
                Comparables.isLessThan(
                    CREATING_TIMEOUT, entry.getLastChangedToLastSyncRequestInterval()))
        .collect(Collectors.toList());
  }

  public DbBillingProjectBufferEntry assignBillingProject(DbUser dbUser, String accessTier) {
    DbBillingProjectBufferEntry bufferEntry = consumeBufferEntryForAssignment(accessTier);
    final String billingProject = bufferEntry.getFireCloudProjectName();

    fireCloudService.addOwnerToBillingProject(dbUser.getUsername(), billingProject);
    bufferEntry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    bufferEntry.setAssignedUser(dbUser);

    // Ensure entry reference isn't left in a dirty state by save().
    bufferEntry = billingProjectBufferEntryDao.save(bufferEntry);
    return bufferEntry;
  }

  private DbBillingProjectBufferEntry consumeBufferEntryForAssignment(String accessTier) {
    // Each call to acquire the lock will timeout in 1s if it is currently held
    while (true) {
      if (billingProjectBufferEntryDao.acquireAssigningLock() == 1) {
        break;
      }
    }

    DbBillingProjectBufferEntry entry;
    try {
      entry =
          billingProjectBufferEntryDao.findFirstByStatusAndAccessTierOrderByCreationTimeAsc(
              DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.AVAILABLE),
              accessTier);

      if (entry == null) {
        log.log(Level.SEVERE, "Consume Buffer call made while Billing Project Buffer was empty");
        throw new EmptyBufferException();
      }

      entry.setStatusEnum(BufferEntryStatus.ASSIGNING, this::getCurrentTimestamp);
      return billingProjectBufferEntryDao.save(entry);
    } finally {
      billingProjectBufferEntryDao.releaseAssigningLock();
    }
  }

  private String createBillingProjectName() {
    String randomString =
        Hashing.sha256()
            .hashUnencodedChars(UUID.randomUUID().toString())
            .toString()
            .substring(0, PROJECT_BILLING_ID_SIZE);

    String prefix = workbenchConfigProvider.get().billing.projectNamePrefix;
    if (!prefix.endsWith("-")) {
      prefix = prefix + "-";
    }

    return prefix + randomString;
  }

  private int getUnfilledBufferSpace(DbAccessTier accessTier) {
    return getBufferMaxCapacity(accessTier) - (int) getCurrentBufferSize(accessTier);
  }

  private long getCurrentBufferSize(DbAccessTier accessTier) {
    return billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(
        accessTier.getShortName());
  }

  // TODO: decide on tier proportion in the billing buffer (e.g. 60% RT, 40% CT).  Use config?
  // initial implementation: allocate the full capacity to each tier
  private int getBufferMaxCapacity(/* not used */ DbAccessTier accessTier) {
    return workbenchConfigProvider.get().billing.bufferCapacity;
  }

  public BillingProjectBufferStatus getStatus() {
    final long bufferSize =
        billingProjectBufferEntryDao.countByStatus(
            DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.AVAILABLE));
    return new BillingProjectBufferStatus().bufferSize(bufferSize);
  }

  public BillingProjectBufferStatusByTier getStatusByTier() {
    final BillingProjectBufferStatusByTier response = new BillingProjectBufferStatusByTier();
    response.addAll(
        StreamSupport.stream(accessTierDao.findAll().spliterator(), false)
            .map(
                accessTier -> {
                  final long bufferSize =
                      billingProjectBufferEntryDao.countByStatusAndAccessTier(
                          DbStorageEnums.billingProjectBufferEntryStatusToStorage(
                              BufferEntryStatus.AVAILABLE),
                          accessTier.getShortName());
                  return new BillingProjectBufferStatusByTierInner()
                      .accessTier(accessTier.getShortName())
                      .bufferSize(bufferSize);
                })
            .collect(Collectors.toList()));
    return response;
  }
}
