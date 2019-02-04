import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

export const environment: Environment = {
  ...testEnvironmentBase,
  rootUrl: 'http://localhost:4200',
  displayTag: 'Local->Test',
  debug: true,
  enableTemporal: false,
  useZendeskForSupport: false,
  enableJupyterLab: true,
  enableComplianceLockout: true
};
