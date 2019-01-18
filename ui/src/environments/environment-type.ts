export interface Environment {
  // Permanent environment variables.
  //
  // The URL to use when making API requests against the AoU API. This is used
  // by the core API / fetch modules and shouldn't be needed by most other components.
  // Example value: 'https://api-dot-all-of-us-rw-stable.appspot.com'
  allOfUsApiUrl: string;
  // The OAuth2 client ID. Used by the sign-in module to authenticate the user.
  // Example value: '56507752110-ovdus1lkreopsfhlovejvfgmsosveda6.apps.googleusercontent.com'
  clientId: string;
  // Indicates that the current server is a local server where client-side
  // debugging should be enabled (e.g. console.log, or devtools APIs).
  debug: boolean;
  // A prefix to add to the site title (shown in the tab title).
  // Example value: 'Test' would cause the following full title:
  // "Homepage | [Test] All of Us Research Workbench"
  displayTag: string;
  // The Google Analytics account ID for logging actions and page views.
  // Example value: 'UA-112406425-3'
  gaId: string;
  // API endpoint to use for Leonardo (notebook proxy) API calls.
  // Example value: 'https://notebooks.firecloud.org'
  leoApiUrl: string;
  // The URL to use when making requests to the "public" API (e.g. non-signed-in
  // endpoints.
  // Example value: 'https://public-api-dot-all-of-us-rw-stable.appspot.com'
  publicApiUrl: string;
  // The URL to forward users to for the public UI (aka Data Browser).
  // Example value: 'https://public-ui-dot-all-of-us-rw-stable.appspot.com'
  publicUiUrl: string;
  // The TCell API key. See RW-1682 for details.
  // Example value: 'AQEBBAEkx4iE2KxNyI7Wx08EwU1ycTM7E4FMSmaibbMUQxNU6uQvuAJt7fyABAtFYSYfgEE'
  tcellapikey: string;
  // The TCell app ID. See RW-1682 for details.
  // Example value: 'AoUNonProd-WZFW2'
  tcellappid: string;
  // The base URL for the Zendesk help center / user forum.
  // Example value: https://aousupporthelp.zendesk.com/hc/
  zendeskHelpCenterUrl: string;

  // Transient client-side flags.
  //
  // Whether temporal queries should be enabled in the cohort builder UI. See
  // RW-1443 for details.
  enableTemporal: boolean;
  // Whether Zendesk should be used for support requests & bug reports, instead
  // of Jira. See RW-1885 for details.
  // Exit criteria: remove flag and change all code to use Zendesk after Athens
  // release.
  useZendeskForSupport: boolean;
}
