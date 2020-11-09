import {runtimeApi} from 'app/services/swagger-fetch-clients';
import {switchCase} from 'app/utils';
import { withAsyncErrorHandling } from 'app/utils';
import {
  ExceededActionCountError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  markRuntimeOperationCompleteForWorkspace,
  runtimeStore,
  runtimeOpsStore,
  updateRuntimeOpsStoreForWorkspaceNamespace,
  useStore,
} from 'app/utils/stores';
import {Runtime, RuntimeStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';

import * as React from 'react';

const {useState, useEffect} = React;

export enum RuntimeStatusRequest {
  Delete = 'Delete'
}

// useRuntime hook is a simple hook to populate the runtime store.
// This is only used by other runtime hooks
const useRuntime = (currentWorkspaceNamespace) => {
  // No cleanup is being handled at the moment.
  // When the user initiates a runtime change we want that change to take place even if they navigate away
  useEffect(() => {
    const getRuntime = withAsyncErrorHandling(
      () => runtimeStore.set({workspaceNamespace: null, runtime: null}),
      async() => {
        const leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
        if (currentWorkspaceNamespace === runtimeStore.get().workspaceNamespace) {
          runtimeStore.set({
            workspaceNamespace: currentWorkspaceNamespace,
            runtime: leoRuntime
          });
        }
      });

    if (currentWorkspaceNamespace !== runtimeStore.get().workspaceNamespace) {
      runtimeStore.set({workspaceNamespace: currentWorkspaceNamespace, runtime: undefined});
      getRuntime();
    }
  }, []);
};

// useRuntimeStatus hook can be used to change the status of the runtime
// Only 'Delete' is supported at the moment. This setter returns a promise which
// resolves when any proximal fetch has completed, but does not wait for any
// polling, which may continue asynchronously.
export const useRuntimeStatus = (currentWorkspaceNamespace): [
  RuntimeStatus | undefined, (statusRequest: RuntimeStatusRequest) => Promise<void>]  => {
  const [runtimeStatus, setRuntimeStatus] = useState<RuntimeStatusRequest>();
  const {runtime} = useStore(runtimeStore);

  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    // Additional status changes can be put here
    if (!!runtimeStatus) {
      switchCase(runtimeStatus,
        [RuntimeStatusRequest.Delete, async() => {
          try {
            await LeoRuntimeInitializer.initialize({workspaceNamespace: currentWorkspaceNamespace, maxCreateCount: 0});
          } catch (e) {
            // ExceededActionCountError is expected, as we exceed our create limit of 0.
            if (!(e instanceof ExceededActionCountError)) {
              throw e;
            }
          }
        }]);
    }

  }, [runtimeStatus]);

  const setStatusRequest = async(req) => {
    await switchCase(req, [
      RuntimeStatusRequest.Delete, () => runtimeApi().deleteRuntime(currentWorkspaceNamespace)
    ]);
    setRuntimeStatus(req);
  };
  return [runtime ? runtime.status : undefined, setStatusRequest];
};

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useCustomRuntime = (currentWorkspaceNamespace):
    [{currentRuntime: Runtime, pendingRuntime: Runtime}, (runtime: Runtime) => void] => {
  const {runtime, workspaceNamespace} = useStore(runtimeStore);
  const runtimeOps = useStore(runtimeOpsStore);
  const {pendingRuntime = null} = runtimeOps[currentWorkspaceNamespace] || {};
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime>();

  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    const aborter = new AbortController();
    const runAction = async() => {
      // Only delete if the runtime already exists.
      // TODO: It is likely more correct here to use the LeoRuntimeInitializer wait for the runtime
      // to reach a terminal status before attempting deletion.
      try {
        if (runtime && runtime.status !== RuntimeStatus.Deleted) {
          await runtimeApi().deleteRuntime(currentWorkspaceNamespace, {
            signal: aborter.signal
          });
        }
        await LeoRuntimeInitializer.initialize({
          workspaceNamespace,
          targetRuntime: requestedRuntime,
          pollAbortSignal: aborter.signal
        });
      } finally {
        markRuntimeOperationCompleteForWorkspace(currentWorkspaceNamespace);
        setRequestedRuntime(undefined);
      }
    };

    if (requestedRuntime !== undefined && !fp.equals(requestedRuntime, runtime)) {
      runAction();
      updateRuntimeOpsStoreForWorkspaceNamespace(currentWorkspaceNamespace, {
        pendingRuntime: requestedRuntime,
        aborter
      });
    }
  }, [requestedRuntime]);

  return [{currentRuntime: runtime, pendingRuntime}, setRequestedRuntime];
};
