package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao.StatusToCountResult;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BillingProjectBufferEntryDaoTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final ImmutableMap<BufferEntryStatus, Long> STATUS_TO_COUNT_INPUT =
      ImmutableMap.of(
          BufferEntryStatus.ASSIGNED, 3L,
          BufferEntryStatus.AVAILABLE, 2L,
          BufferEntryStatus.ERROR, 1L);

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private BillingProjectBufferEntryDao billingProjectBufferEntryDao;

  private final String TEST_TIER = "registered";
  private final String TEST_PERIMETER = "the/registered/tier/service/perimeter";
  private final DbAccessTier DB_TIER =
      accessTierDao.save(
          new DbAccessTier()
              .setShortName(TEST_TIER)
              .setDisplayName(TEST_TIER)
              .setServicePerimeter(TEST_PERIMETER));

  @Before
  public void setup() {
    insertEntriesWithCounts(STATUS_TO_COUNT_INPUT);
  }

  @Test
  public void testGetCountByStatusMap() {
    final Map<BufferEntryStatus, Long> result = billingProjectBufferEntryDao.getCountByStatusMap();
    assertThat(result).hasSize(STATUS_TO_COUNT_INPUT.size());
    assertThat(result.get(BufferEntryStatus.CREATING)).isNull();
    assertThat(result.get(BufferEntryStatus.AVAILABLE)).isEqualTo(2L);
    assertThat(result.get(BufferEntryStatus.ERROR)).isEqualTo(1L);
  }

  @Test
  public void testComputeProjectCountByStatus() {
    final List<StatusToCountResult> rows =
        billingProjectBufferEntryDao.computeProjectCountByStatus();
    assertThat(rows).hasSize(STATUS_TO_COUNT_INPUT.size());
    assertThat(
            rows.stream()
                .map(StatusToCountResult::getStatusEnum)
                .collect(ImmutableSet.toImmutableSet()))
        .hasSize(STATUS_TO_COUNT_INPUT.size());

    assertThat(
            rows.stream()
                .filter(r -> r.getStatusEnum().equals(BufferEntryStatus.AVAILABLE))
                .map(StatusToCountResult::getNumProjects)
                .findFirst()
                .orElse(-1L))
        .isEqualTo(2L);
  }

  @Test
  public void testGetCurrentBufferSizeForAccessTier_invalidTier() {
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(DB_TIER))
        .isEqualTo(0);
  }

  @Test
  public void testGetCurrentBufferSizeForAccessTier_defaultTier() {
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(DB_TIER))
        .isEqualTo(STATUS_TO_COUNT_INPUT.get(BufferEntryStatus.AVAILABLE));
  }

  @Test
  public void testGetCurrentBufferSizeForAccessTier_multiTier() {
    // add one available project in an alternate tier
    String altTierName = "other";
    DbAccessTier altTier =
        accessTierDao.save(
            new DbAccessTier()
                .setShortName(altTierName)
                .setDisplayName(altTierName)
                .setServicePerimeter("other perim"));

    DbBillingProjectBufferEntry altEntry = new DbBillingProjectBufferEntry();
    altEntry.setStatusEnum(BufferEntryStatus.AVAILABLE, () -> NOW);
    altEntry.setAccessTier(altTier.getShortName());
    billingProjectBufferEntryDao.save(altEntry);

    // no change to existing tier
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(DB_TIER))
        .isEqualTo(STATUS_TO_COUNT_INPUT.get(BufferEntryStatus.AVAILABLE));
    assertThat(billingProjectBufferEntryDao.getCurrentBufferSizeForAccessTier(altTier))
        .isEqualTo(1);
  }

  private void insertEntriesWithCounts(Map<BufferEntryStatus, Long> statusToCount) {
    statusToCount.forEach(
        (status, count) -> {
          for (int i = 0; i < count; ++i) {
            DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
            entry.setStatusEnum(status, () -> NOW);
            entry.setAccessTier(TEST_TIER);
            entry = billingProjectBufferEntryDao.save(entry);
          }
        });
  }
}
