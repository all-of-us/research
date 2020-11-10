import { BreadcrumbType } from 'app/utils/navigation';
import {atom, Atom} from 'app/utils/subscribable';
import {Profile} from 'generated';
import {Runtime} from 'generated/fetch';
import * as React from 'react';

const {useEffect, useState} = React;

export interface RouteDataStore {
  title?: string;
  minimizeChrome?: boolean;
  helpContentKey?: string;
  breadcrumb?: BreadcrumbType;
  pathElementForTitle?: string;
  notebookHelpSidebarStyles?: boolean;
  contentFullHeightOverride?: boolean;
}

export const routeDataStore = atom<RouteDataStore>({});

interface AuthStore {
  authLoaded: boolean;
  isSignedIn: boolean;
}

export const authStore = atom<AuthStore>({authLoaded: false, isSignedIn: false});

interface ProfileStore {
  profile?: Profile;
}

export const profileStore = atom<ProfileStore>({});

export interface CompoundRuntimeOperation {
  pendingRuntime?: Runtime;
  aborter: AbortController;
}

// Store tracking any compound Runtime operations per workspace. Currently, this
// only pertains to applying a runtime configuration update via full recreate
// (compound operation of delete -> create).
export const compoundRuntimeOpStore = atom<{
  [workspaceNamespace: string]: CompoundRuntimeOperation;
}>({});

export const registerCompoundRuntimeOperation = (workspaceNamespace: string, runtimeOperation: CompoundRuntimeOperation) => {
  compoundRuntimeOpStore.set({
    ...compoundRuntimeOpStore.get(),
    [workspaceNamespace]: runtimeOperation
  });
};

export const markCompoundRuntimeOperationCompleted = (workspaceNamespace: string) => {
  const ops = compoundRuntimeOpStore.get();
  if (ops[workspaceNamespace]) {
    delete ops[workspaceNamespace];
    compoundRuntimeOpStore.set(ops);
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
    const value = useStore(theStore);
    const storeProp = {[name]: value};
    return <WrappedComponent {...props} {...storeProp}/> ;
  };
};
