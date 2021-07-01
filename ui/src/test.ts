// This file is required by karma.conf.js and loads recursively all the .spec and framework files
// tslint:disable

import 'zone.js/dist/long-stack-trace-zone';
import 'zone.js/dist/proxy.js';
import 'zone.js/dist/sync-test';
import 'zone.js/dist/jasmine-patch';
import 'zone.js/dist/async-test';
import 'zone.js/dist/fake-async-test';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';
import {cohortReviewStore} from 'app/services/review-state.service';
import {
  currentWorkspaceStore,
  currentCohortStore,
  currentConceptSetStore,
  queryParamsStore,
  routeConfigDataStore,
  urlParamsStore
} from 'app/utils/navigation';
import {serverConfigStore} from "./app/utils/stores";

// Unfortunately there's no typing for the `__karma__` variable. Just declare it as any.
declare let __karma__: any;
declare let require: any;

// Prevent Karma from running prematurely.
__karma__.loaded = function () {};

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting()
);
beforeEach(() => {
  cohortReviewStore.next(undefined);
  currentWorkspaceStore.next(undefined);
  currentCohortStore.next(undefined);
  currentConceptSetStore.next(undefined);
  queryParamsStore.next({});
  routeConfigDataStore.next({});
  serverConfigStore.set({});
  urlParamsStore.next({});
});
// Then we find all the tests.
const context = require.context('./', true, /\.spec\.ts$/);
// And load the modules.
context.keys().map(context);
// Finally, start Karma to run the tests.
__karma__.start();
