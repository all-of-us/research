package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController implements ConfigApiDelegate {

  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  ConfigController(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public ResponseEntity<ConfigResponse> getConfig() {
    WorkbenchConfig config = configProvider.get();
    return ResponseEntity.ok(
        new ConfigResponse()
            .gsuiteDomain(config.googleDirectoryService.gSuiteDomain)
            .projectId(config.server.projectId)
            .firecloudURL(config.firecloud.baseUrl)
            .publicApiKeyForErrorReports(config.server.publicApiKeyForErrorReports)
            .shibbolethUiBaseUrl(config.firecloud.shibbolethUiBaseUrl)
            .defaultFreeCreditsDollarLimit(config.billing.defaultFreeCreditsDollarLimit)
            .enableComplianceTraining(config.access.enableComplianceTraining)
            .enableEraCommons(config.access.enableEraCommons)
            .enableDataUseAgreement(config.access.enableDataUseAgreement)
            .enableBetaAccess(config.access.enableBetaAccess)
            .unsafeAllowSelfBypass(config.access.unsafeAllowSelfBypass)
            .requireInvitationKey(config.access.requireInvitationKey)
            .enableBillingLockout(config.featureFlags.enableBillingLockout)
            .enableBillingUpgrade(config.featureFlags.enableBillingUpgrade)
            .requireInstitutionalVerification(config.featureFlags.requireInstitutionalVerification)
            .enableCBAgeTypeOptions(config.featureFlags.enableCBAgeTypeOptions)
            .enableV3DataUserCodeOfConduct(config.featureFlags.enableV3DataUserCodeOfConduct)
            .enableEventDateModifier(config.featureFlags.enableEventDateModifier)
            .useNewShibbolethService(config.featureFlags.useNewShibbolethService));
  }
}
