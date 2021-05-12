import {profileApi} from 'app/services/swagger-fetch-clients';
import { BreadcrumbType } from 'app/utils/navigation';
import {atom, Atom} from 'app/utils/subscribable';
import {CdrVersionTiersResponse, ConfigResponse, Profile, Runtime} from 'generated/fetch';
import * as React from 'react';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

const {useEffect, useState} = React;

export interface RouteDataStore {
  title?: string;
  minimizeChrome?: boolean;
  pageKey?: string;
  breadcrumb?: BreadcrumbType;
  pathElementForTitle?: string;
  contentFullHeightOverride?: boolean;
}

export const routeDataStore = atom<RouteDataStore>({});

interface AuthStore {
  authLoaded: boolean;
  isSignedIn: boolean;
}

export const authStore = atom<AuthStore>({authLoaded: false, isSignedIn: false});

export const cdrVersionStore = atom<CdrVersionTiersResponse>({tiers: []});

export interface ProfileStore {
  profile?: Profile;
  load: Function;
  reload: Function;
  updateCache: Function;
}

export const profileStore = atom<ProfileStore>({
  profile: null,
  load: async() => {
    console.log('profileStore load: ' + !!profileStore.get().profile);
    if (!profileStore.get().profile) {
      await profileStore.get().reload();
    }
    return profileStore.get().profile;
  },
  reload: async() => {
    console.log('profileStore reload - await profileApi().getMe()');
    const newProfile = await profileApi().getMe();
    console.log('profileStore reload - received: ' + JSON.stringify(newProfile));
    profileStore.get().updateCache(newProfile);
    console.log('profileStore reload - cache updated');
    return profileStore.get().profile;
  },
  updateCache: (p: Profile): void => profileStore.set({
    ...profileStore.get(),
    profile: p
  })
});

export interface CompoundRuntimeOperation {
  pendingRuntime?: Runtime;
  aborter: AbortController;
}

export interface CompoundRuntimeOpStore {
  [workspaceNamespace: string]: CompoundRuntimeOperation;
}

// Store tracking any compound Runtime operations per workspace. Currently, this
// only pertains to applying a runtime configuration update via full recreate
// (compound operation of delete -> create).
export const compoundRuntimeOpStore = atom<CompoundRuntimeOpStore>({});

export const registerCompoundRuntimeOperation = (workspaceNamespace: string, runtimeOperation: CompoundRuntimeOperation) => {
  compoundRuntimeOpStore.set({
    ...compoundRuntimeOpStore.get(),
    [workspaceNamespace]: runtimeOperation
  });
};

export const markCompoundRuntimeOperationCompleted = (workspaceNamespace: string) => {
  const {[workspaceNamespace]: op, ...otherOps} = compoundRuntimeOpStore.get();
  if (op) {
    op.aborter.abort();
    compoundRuntimeOpStore.set(otherOps);
  }
};

export const clearCompoundRuntimeOperations = () => {
  const ops = compoundRuntimeOpStore.get();
  Object.keys(ops).forEach(k => ops[k].aborter.abort());
  compoundRuntimeOpStore.set({});
};

// runtime store states: undefined(initial state) -> Runtime (user selected) <--> null (delete only - no recreate)
export interface RuntimeStore {
  workspaceNamespace: string | null | undefined;
  runtime: Runtime | null | undefined;
}

export const runtimeStore = atom<RuntimeStore>({workspaceNamespace: undefined, runtime: undefined});

export interface StackdriverErrorReporterStore {
  reporter?: StackdriverErrorReporter;
}

export const stackdriverErrorReporterStore = atom<StackdriverErrorReporterStore>({});

export interface ServerConfigStore {
  config?: ConfigResponse;
}

export const serverConfigStore = atom<ServerConfigStore>({});


/**
 * @name useStore
 * @description React hook that will trigger a render when the corresponding store's value changes
 *              this should only be used in function components
 * @param {Atom<T>} theStore A container for the value to be updated
 */
export function useStore<T>(theStore: Atom<T>) {
  const [value, setValue] = useState(theStore.get());
  useEffect(() => {
    return theStore.subscribe(v => setValue(v)).unsubscribe;
  }, [theStore]);
  return value;
}

/**
 * HOC that injects the value of the given store as a prop. When the store changes, the wrapped
 * component will re-render
 */
export const withStore = (theStore, name) => WrappedComponent => {
  return (props) => {
    console.log('withStore ' + name);
    console.log('withStore props ' + JSON.stringify(props));
    const value = useStore(theStore);
    const storeProp = {[name]: value};
    return <WrappedComponent {...props} {...storeProp}/> ;
  };
};
