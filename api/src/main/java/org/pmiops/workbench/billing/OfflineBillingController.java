package org.pmiops.workbench.billing;

import org.pmiops.workbench.api.OfflineBillingApiDelegate;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingService freeTierBillingService;
  private final BillingProjectBufferService billingProjectBufferService;
  private final BillingGarbageCollectionService billingGarbageCollectionService;
  private LogsBasedMetricService logsBasedMetricService;

  @Autowired
  OfflineBillingController(
      FreeTierBillingService freeTierBillingService,
      BillingProjectBufferService billingProjectBufferService,
      BillingGarbageCollectionService billingGarbageCollectionService,
      LogsBasedMetricService logsBasedMetricService) {
    this.freeTierBillingService = freeTierBillingService;
    this.billingProjectBufferService = billingProjectBufferService;
    this.billingGarbageCollectionService = billingGarbageCollectionService;
    this.logsBasedMetricService = logsBasedMetricService;
  }

  @Override
  public ResponseEntity<Void> billingProjectGarbageCollection() {
    billingGarbageCollectionService.deletedWorkspaceGarbageCollection();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> bufferBillingProjects() {
    billingProjectBufferService.bufferBillingProjects();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> syncBillingProjectStatus() {
    billingProjectBufferService.syncBillingProjectStatus();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> cleanBillingBuffer() {
    billingProjectBufferService.cleanBillingBuffer();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    freeTierBillingService.checkFreeTierBillingUsage();
    return ResponseEntity.noContent().build();
  }
}
