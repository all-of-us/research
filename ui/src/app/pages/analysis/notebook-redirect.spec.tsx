import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import {registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore, serverConfigStore, urlParamsStore, userProfileStore} from 'app/utils/navigation';
import {Kernels} from 'app/utils/notebook-kernels';
import {RuntimeApi, RuntimeStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {RuntimesApi as LeoRuntimesApi, JupyterApi, NotebooksApi} from 'notebooks-generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {JupyterApiStub} from 'testing/stubs/jupyter-api-stub';
import {NotebooksApiStub} from 'testing/stubs/notebooks-api-stub';
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

  const mountedComponent = () => {
    return mount(<NotebookRedirect/>);
  };

  async function awaitTickAndTimers(wrapper: ReactWrapper) {
    jest.runOnlyPendingTimers();
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
    registerApiClientNotebooks(NotebooksApi, new NotebooksApiStub());
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

    // mock timers
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mountedComponent();
    expect(wrapper).toBeTruthy();
  });

  it('should show redirect display before showing notebook', async() => {
    const wrapper = mountedComponent();
    expect(wrapper.exists('[data-test-id="notebook-redirect"]')).toBeTruthy();
  });

  it('should be "Initializing" until a Creating runtime for an existing notebook is running', async() => {
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: false});
    runtimeStub.runtime.status = RuntimeStatus.Creating;
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
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: true});
    runtimeStub.runtime.status = RuntimeStatus.Creating;
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
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: false});
    runtimeStub.runtime.status = RuntimeStatus.Stopped;
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
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: true});
    runtimeStub.runtime.status = RuntimeStatus.Stopped;
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
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: false});
    runtimeStub.runtime.status = RuntimeStatus.Running;
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });


  it('should be "Redirecting" when the runtime is initially Running for a new notebook', async() => {
    const wrapper = mountedComponent();

    wrapper.setState({creatingNewNotebook: true});
    runtimeStub.runtime.status = RuntimeStatus.Running;
    await awaitTickAndTimers(wrapper);

    expect(wrapper
      .exists(getCardSpinnerTestId(ProgressCardState.Redirecting)))
      .toBeTruthy();
    expect(currentCardText(wrapper))
      .toContain(progressStrings.get(Progress.Redirecting));
  });
});
