import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

export const environment: Environment = {
  ...testEnvironmentBase,
  displayTag: 'Local->Test',
  debug: true,
  enableHomepageRestyle: true
};
