package org.pmiops.workbench.access;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.model.AccessModule;

/** Utilities for RW Access related functionalities. */
public class AccessUtils {
  private AccessUtils() {}

  private static final BiMap<AccessModule, AccessModuleName> CLIENT_TO_STORAGE_ACCESS_MODULE =
      ImmutableBiMap.<AccessModule, AccessModuleName>builder()
          .put(AccessModule.TWO_FACTOR_AUTH, AccessModuleName.TWO_FACTOR_AUTH)
          .put(AccessModule.ERA_COMMONS, AccessModuleName.ERA_COMMONS)
          .put(AccessModule.COMPLIANCE_TRAINING, AccessModuleName.RT_COMPLIANCE_TRAINING)
          .put(AccessModule.RAS_LINK_LOGIN_GOV, AccessModuleName.RAS_LOGIN_GOV)
          .put(AccessModule.DATA_USE_AGREEMENT, AccessModuleName.DATA_USER_CODE_OF_CONDUCT)
          .put(AccessModule.PUBLICATION_CONFIRMATION, AccessModuleName.PUBLICATION_CONFIRMATION)
          .put(AccessModule.PROFILE_CONFIRMATION, AccessModuleName.PROFILE_CONFIRMATION)
          .build();

  private static final BiMap<BypassTimeTargetProperty, AccessModuleName>
      AUDIT_TO_STORAGE_ACCESS_MODULE =
          ImmutableBiMap.<BypassTimeTargetProperty, AccessModuleName>builder()
              .put(BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME, AccessModuleName.ERA_COMMONS)
              .put(
                  BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
                  AccessModuleName.RT_COMPLIANCE_TRAINING)
              .put(
                  BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
                  AccessModuleName.TWO_FACTOR_AUTH)
              .put(BypassTimeTargetProperty.RAS_LINK_LOGIN_GOV, AccessModuleName.RAS_LOGIN_GOV)
              .put(
                  BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
                  AccessModuleName.DATA_USER_CODE_OF_CONDUCT)
              .build();

  public static AccessModuleName clientAccessModuleToStorage(AccessModule s) {
    return CLIENT_TO_STORAGE_ACCESS_MODULE.get(s);
  }

  public static AccessModule storageAccessModuleToClient(AccessModuleName s) {
    return CLIENT_TO_STORAGE_ACCESS_MODULE.inverse().get(s);
  }

  public static BypassTimeTargetProperty auditAccessModuleFromStorage(AccessModuleName s) {
    return AUDIT_TO_STORAGE_ACCESS_MODULE.inverse().get(s);
  }
}