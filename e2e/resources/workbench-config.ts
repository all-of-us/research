require('dotenv').config();
import * as fp from 'lodash/fp';

const env = process.env.WORKBENCH_ENV || 'dev';

const userCredential = {
  userEmail: process.env.USER_NAME,
  userPassword: process.env.PASSWORD,
  institutionContactEmail: 'aou-dev-registration@broadinstitute.org',
};

const urlPath = {
  loginUrlPath: '/login',
  workspacesUrlPath: '/workspaces',
  profileUrlPath: '/profile',
  libraryUrlPath: '/library',
  adminUrlPath: '/admin/user',
};

// localhost development server
const local = {
  uiBaseUrl: process.env.DEV_LOGIN_URL || 'http://localhost:4200',
  apiBaseUrl: process.env.DEV_API_URL || 'http://localhost/v1',
  userEmailDomain: '@fake-research-aou.org',
  collaboratorUsername: 'puppetmaster@fake-research-aou.org'
};

// workbench test environment
const dev = {
  uiBaseUrl: process.env.TEST_LOGIN_URL || 'https://all-of-us-workbench-test.appspot.com',
  apiBaseUrl: process.env.TEST_API_URL || 'https://api-dot-all-of-us-workbench-test.appspot.com/v1',
  userEmailDomain: '@fake-research-aou.org',
  collaboratorUsername: 'puppetmaster@fake-research-aou.org'
};

// workbench staging environment
const staging = {
  uiBaseUrl: process.env.STAGING_LOGIN_URL || 'https://all-of-us-rw-staging.appspot.com',
  apiBaseUrl: process.env.STAGING_API_URL || 'https://api-dot-all-of-us-rw-staging.appspot.com/v1',
  userEmailDomain: '@staging.fake-research-aou.org',
};

// workbench stable environment
const stable = {
  uiBaseUrl: process.env.STABLE_LOGIN_URL,
  apiBaseUrl: process.env.STABLE_API_URL,
  userEmailDomain: '@stable.fake-research-aou.org',
};


const environment = {
  local,
  dev,
  staging,
  stable,
};

export const config = fp.mergeAll([environment[env], userCredential, urlPath]);
