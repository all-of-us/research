import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';
import Iframe from 'react-iframe';

import {registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {act} from 'react-dom/test-utils';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore, serverConfigStore, urlParamsStore, userProfileStore, NavStore} from 'app/utils/navigation';
import {runtimeStore} from 'app/utils/stores';
import {Kernels} from 'app/utils/notebook-kernels';
import {RuntimeApi, RuntimeStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {RuntimesApi as LeoRuntimesApi, JupyterApi, ProxyApi} from 'notebooks-generated/fetch';
import {handleUseEffect, waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {JupyterApiStub} from 'testing/stubs/jupyter-api-stub';
import {ProxyApiStub} from 'testing/stubs/proxy-api-stub';
import {LeoRuntimesApiStub} from 'testing/stubs/leo-runtimes-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';

import {NotebookRedirect, Progress, ProgressCardState, progressStrings} from './notebook-redirect';

describe('NotebookRedirect', () => {
  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
  const profile = ProfileStubVariables.PROFILE_STUB;
  const reload = jest.fn();
  const updateCache = jest.fn();

  let runtimeStub: RuntimeApiStub;

  const component = async() => {
    const c = mount(<NotebookRedirect/>);
    await handleUseEffect(c);
    return c;
  };

  async function awaitTickAndTimers(wrapper: ReactWrapper) {
    act(() => jest.runOnlyPendingTimers());
    await waitOneTickAndUpdate(wrapper);
  }

  function currentCardText(wrapper: ReactWrapper) {
    return wrapper.find('[data-test-id="current-progress-card"]').first().text();
  }

  function getCardSpinnerTestId(cardState: ProgressCardState) {
    return '[data-test-id="progress-card-spinner-' + cardState.valueOf() + '"]';
  }

  beforeEach(() => {
    runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = RuntimeStatus.Creating;

    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClientNotebooks(JupyterApi, new JupyterApiStub());
    registerApiClientNotebooks(ProxyApi, new ProxyApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    serverConfigStore.next({gsuiteDomain: 'x'});
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      nbName: 'blah blah'
    });
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: true
    });
    currentWorkspaceStore.next(workspace);
    userProfileStore.next({profile, reload, updateCache});
    runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});

    // mock timers
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should render', async() => {
    const wrapper = await component();
    expect(wrapper).toBeTruthy();
  });

  it('should show redirect display before showing notebook', async() => {
    const wrapper = await component();
    expect(wrapper.exists('[data-test-id="notebook-redirect"]')).toBeTruthy();
  });

  it('should be "Initializing" until a Creating runtime for an existing notebook is running', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Creating;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Initializing));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Initializing" until a Creating runtime for a new notebook is running', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Creating;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Initializing));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Resuming" until a Stopped runtime for an existing notebook is running', async() => {
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Stopped;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Resuming));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Resuming" until a Stopped runtime for a new notebook is running', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Stopped;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.UnknownInitializingResuming)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Resuming));

    runtimeStub.runtime.status = RuntimeStatus.Running;
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should be "Redirecting" when the runtime is initially Running for an existing notebook', async() => {
    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });


  it('should be "Redirecting" when the runtime is initially Running for a new notebook', async() => {
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });

  it('should navigate away after runtime transitions to deleting', async() => {
    const navSpy = jest.fn();
    NavStore.navigate = navSpy;

    queryParamsStore.next({
      kernelType: Kernels.R,
      creating: false
    });
    runtimeStub.runtime.status = RuntimeStatus.Running;

    const wrapper = await component();
    await awaitTickAndTimers(wrapper);

    // Wait for the "redirecting" timer to elapse, rendering the iframe.
    act(() => jest.advanceTimersByTime(2000));
    await awaitTickAndTimers(wrapper);

    expect(wrapper.find(Iframe).exists()).toBeTruthy();
    expect(navSpy).not.toHaveBeenCalled();

    // Simulate transition to deleting - should navigate away.
    act(() => {
      runtimeStub.runtime = {...runtimeStub.runtime, status: RuntimeStatus.Deleting};
      runtimeStore.set({
        workspaceNamespace: workspace.namespace,
        runtime: runtimeStub.runtime
      });
    });
    await awaitTickAndTimers(wrapper);

    expect(navSpy).toHaveBeenCalled();
  });
});
