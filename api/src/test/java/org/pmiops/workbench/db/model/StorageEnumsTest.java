package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.UnderservedPopulationEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

@RunWith(Parameterized.class)
public class StorageEnumsTest {
  @Parameters(name = "{0}")
  public static Object[][] data() {
    return new Object[][] {
      {
        Authority.class.getSimpleName(),
        Authority.values(),
        (Function<Short, Authority>) StorageEnums::authorityFromStorage,
        (Function<Authority, Short>) StorageEnums::authorityToStorage
      },
      {
        BillingProjectStatus.class.getSimpleName(),
        BillingProjectStatus.values(),
        (Function<Short, BillingProjectStatus>) StorageEnums::billingProjectStatusFromStorage,
        (Function<BillingProjectStatus, Short>) StorageEnums::billingProjectStatusToStorage
      },
      {
        DataAccessLevel.class.getSimpleName(),
        DataAccessLevel.values(),
        (Function<Short, DataAccessLevel>) StorageEnums::dataAccessLevelFromStorage,
        (Function<DataAccessLevel, Short>) StorageEnums::dataAccessLevelToStorage
      },
      {
        EmailVerificationStatus.class.getSimpleName(),
        EmailVerificationStatus.values(),
        (Function<Short, EmailVerificationStatus>) StorageEnums::emailVerificationStatusFromStorage,
        (Function<EmailVerificationStatus, Short>) StorageEnums::emailVerificationStatusToStorage
      },
      {
        UnderservedPopulationEnum.class.getSimpleName(),
        UnderservedPopulationEnum.values(),
        (Function<Short, UnderservedPopulationEnum>) StorageEnums::underservedPopulationFromStorage,
        (Function<UnderservedPopulationEnum, Short>) StorageEnums::underservedPopulationToStorage
      },
      {
        WorkspaceAccessLevel.class.getSimpleName(),
        WorkspaceAccessLevel.values(),
        (Function<Short, WorkspaceAccessLevel>) StorageEnums::workspaceAccessLevelFromStorage,
        (Function<WorkspaceAccessLevel, Short>) StorageEnums::workspaceAccessLevelToStorage
      },
    };
  }

  @Parameter()
  public String description;

  @Parameter(1)
  public Enum<?>[] enumValues;

  @Parameter(2)
  public Function<Short, Enum<?>> fromStorage;

  @Parameter(3)
  public Function<Enum<?>, Short> toStorage;

  @Test
  public void testBijectiveStorageMapping() {
    for (Enum<?> v : enumValues) {
      Short storageValue = toStorage.apply(v);
      assertWithMessage("unmapped enum value: " + v).that(storageValue).isNotNull();
      assertThat(v).isEqualTo(fromStorage.apply(storageValue));
    }
  }
}
