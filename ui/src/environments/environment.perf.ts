import {Environment} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Perf',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api-dot-all-of-us-rw-perf.appspot.com',
  captchaSiteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI',
  clientId: '63939010390-aj0r8hro7r8lkt7a45gissu3m73ietl2.apps.googleusercontent.com',
  leoApiUrl: 'https://leonardo.dsde-perf.broadinstitute.org',
  // There is no perf environment for the data browser
  publicUiUrl: 'https://aou-db-staging.appspot.com',
  debug: false,
  gaId: 'UA-112406425-2',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  trainingUrl: 'https://aoudev.nnlm.gov',
  zendeskHelpCenterUrl: 'https://aousupporthelp1580753096.zendesk.com/hc',
  createBillingAccountHelpUrl: 'https://aousupporthelp1580753096.zendesk.com/hc/en-us/articles/360039550031-Instructions-to-Create-a-Billing-Account',
  zendeskWidgetKey: 'df0a2e39-f8a8-482b-baf5-af82e14d38f9',
  shibbolethUrl: 'https://shibboleth.dsde-perf.broadinstitute.org',
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  enableCaptcha: true,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: false,
  enableNewConceptTabs: false,
  enableSignedInFooter: false
};
