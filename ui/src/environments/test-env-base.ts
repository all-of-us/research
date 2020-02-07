// The values are shared across the deployed test env as well as the local dev
// environments.
export const testEnvironmentBase = {
  allOfUsApiUrl: 'https://api-dot-all-of-us-workbench-test.appspot.com',
  clientId: '602460048110-5uk3vds3igc9qo0luevroc2uc3okgbkt.apps.googleusercontent.com',
  // Keep in sync with config_test.json.
  leoApiUrl: 'https://leonardo.dsde-dev.broadinstitute.org',
  publicUiUrl: 'https://aou-db-test.appspot.com',
  gaId: 'UA-112406425-1',
  gaUserAgentDimension: 'dimension2',
  gaLoggedInDimension: 'dimension3',
  trainingUrl: 'https://aoudev.nnlm.gov',
  zendeskHelpCenterUrl: 'https://aousupporthelp1580753096.zendesk.com/hc',
  createBillingAccountHelpUrl: 'https://aousupporthelp.zendesk.com/hc/en-us/articles/360039539411-How-to-Create-a-Billing-Account',
  zendeskWidgetKey: 'df0a2e39-f8a8-482b-baf5-af82e14d38f9',
  shibbolethUrl: 'http://mock-nih.dev.test.firecloud.org',
  shouldShowDisplayTag: true,
  inactivityTimeoutSeconds: 99999999999,
  inactivityWarningBeforeSeconds: 5 * 60,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: true,
  enableNewConceptTabs: false
};
