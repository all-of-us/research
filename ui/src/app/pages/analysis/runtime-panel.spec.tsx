import {mount} from 'enzyme';
import {act} from 'react-dom/test-utils';
import * as React from 'react';

import {Button, Link} from 'app/components/buttons';
import {Spinner} from 'app/components/spinners';
import {WarningMessage} from 'app/components/warning-message';
import {ConfirmDelete, RuntimePanel, Props} from 'app/pages/analysis/runtime-panel';
import {registerApiClient, runtimeApi} from 'app/services/swagger-fetch-clients';
import {ComputeType} from 'app/utils/machines';
import {clearCompoundRuntimeOperations} from 'app/utils/stores';
import {cdrVersionStore, serverConfigStore} from 'app/utils/navigation';
import {runtimePresets} from 'app/utils/runtime-presets';
import {runtimeStore} from 'app/utils/stores';
import {RuntimeConfigurationType, RuntimeStatus, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {RuntimeApi} from 'generated/fetch/api';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {cdrVersionListResponse, CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';
import {defaultDataprocConfig, RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {WorkspacesApiStub, workspaceStubs} from 'testing/stubs/workspaces-api-stub';

describe('RuntimePanel', () => {
  let props: Props;
  let runtimeApiStub: RuntimeApiStub;
  let workspacesApiStub: WorkspacesApiStub;

  const component = async() => {
    const c = mount(<RuntimePanel {...props}/>);
    await waitOneTickAndUpdate(c);
    return c;
  };

  beforeEach(() => {
    cdrVersionStore.next(cdrVersionListResponse);
    serverConfigStore.next({...defaultServerConfig, enableCustomRuntimes: true});

    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    runtimeStore.set({runtime: runtimeApiStub.runtime, workspaceNamespace: workspaceStubs[0].namespace});
    props = {
      workspace: {
        ...workspaceStubs[0],
        accessLevel: WorkspaceAccessLevel.WRITER,
        cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID
      },
      cdrVersionListResponse,
      onUpdate: () => {}
    };

    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.useRealTimers();
  });

  const pickDropdownOption = async(wrapper, id, label) => {
    wrapper.find(id).first().simulate('click');
    const item = wrapper.find(`${id} .p-dropdown-item`).find({'aria-label': label}).first();
    expect(item.exists()).toBeTruthy();

    item.simulate('click');

    // In some cases, picking an option may require some waiting, e.g. for
    // rerendering of RAM options based on CPU selection.
    await waitOneTickAndUpdate(wrapper);
  };

  const getInputValue = (wrapper, id) => {
    return wrapper.find(id).first().prop('value');
  };

  const enterNumberInput = async(wrapper, id, value) => {
    // TODO: Find a way to invoke this without props.
    act(() => {wrapper.find(id).first().prop('onChange')({value} as any);});
    await waitOneTickAndUpdate(wrapper);
  };

  const getMainCpu = (wrapper) => getInputValue(wrapper, '#runtime-cpu');
  const pickMainCpu = (wrapper, cpu) => pickDropdownOption(wrapper, '#runtime-cpu', cpu);

  const getMainRam = (wrapper) => getInputValue(wrapper, '#runtime-ram');
  const pickMainRam = (wrapper, ram) => pickDropdownOption(wrapper, '#runtime-ram', ram);

  const getMainDiskSize = (wrapper) => getInputValue(wrapper, '#runtime-disk');
  const pickMainDiskSize = (wrapper, diskSize) => enterNumberInput(wrapper, '#runtime-disk', diskSize);

  const pickComputeType = (wrapper, computeType) => pickDropdownOption(wrapper, '#runtime-compute', computeType);

  const getWorkerCpu = (wrapper) => getInputValue(wrapper, '#worker-cpu');
  const pickWorkerCpu = (wrapper, cpu) => pickDropdownOption(wrapper, '#worker-cpu', cpu);

  const getWorkerRam = (wrapper) => getInputValue(wrapper, '#worker-ram');
  const pickWorkerRam = (wrapper, ram) => pickDropdownOption(wrapper, '#worker-ram', ram);

  const getWorkerDiskSize = (wrapper) => getInputValue(wrapper, '#worker-disk');
  const pickWorkerDiskSize = (wrapper, diskSize) => enterNumberInput(wrapper, '#worker-disk', diskSize);

  const getNumWorkers = (wrapper) => getInputValue(wrapper, '#num-workers');
  const pickNumWorkers = (wrapper, n) => enterNumberInput(wrapper, '#num-workers', n);

  const getNumPreemptibleWorkers = (wrapper) => getInputValue(wrapper, '#num-preemptible');
  const pickNumPreemptibleWorkers = (wrapper, n) => enterNumberInput(wrapper, '#num-preemptible', n);

  const pickPreset = async(wrapper, {displayName}) => {
    wrapper.find({'data-test-id': 'runtime-presets-menu'}).first().simulate('click');
    act(() => {(document.querySelector(`#popup-root [aria-label="${displayName}"]`) as HTMLElement).click()});
    await waitOneTickAndUpdate(wrapper);
  };

  const mustClickButton = async(wrapper, label) => {
    const createButton = wrapper.find(Button).find({'aria-label': label}).first();
    expect(createButton.exists()).toBeTruthy();
    expect(createButton.prop('disabled')).toBeFalsy();

    createButton.simulate('click');
    await waitOneTickAndUpdate(wrapper);
  };

  it('should show loading spinner while loading', async() => {
    const wrapper = await component();

    // Check before ticking - stub returns the runtime asynchronously.
    expect(!wrapper.exists({'data-test-id': 'runtime-panel'}));
    expect(wrapper.exists(Spinner));

    // Now getRuntime returns.
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists({'data-test-id': 'runtime-panel'}));
    expect(!wrapper.exists(Spinner));
  });

  it('should show Create panel when no runtime', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    const computeDefaults = wrapper.find('#compute-resources').first();
    // defaults to generalAnalysis preset, which is a n1-standard-4 machine with a 50GB disk
    expect(computeDefaults.text()).toEqual('- Default: compute size of 4 CPUs, 15 GB memory, and a 50 GB disk')
  });

  it('should allow creation when no runtime exists with defaults', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.gceConfig.machineType).toEqual('n1-standard-4');
  });

  it('should allow creation when runtime has error status', async() => {
    runtimeApiStub.runtime.status = RuntimeStatus.Error;
    act(() => { runtimeStore.set({runtime: runtimeApiStub.runtime, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Create');

    // Kicks off a deletion to first clear the error status runtime.
    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should allow creation with GCE config', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}); });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 52);
    await pickMainDiskSize(wrapper, 75);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
    expect(runtimeApiStub.runtime.gceConfig).toEqual({
      machineType: 'n1-highmem-8',
      diskSize: 75
    });
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow creation with Dataproc config', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // master settings
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    await pickMainDiskSize(wrapper, 100);

    await pickComputeType(wrapper, ComputeType.Dataproc);

    // worker settings
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    await pickNumWorkers(wrapper, 10);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
    expect(runtimeApiStub.runtime.dataprocConfig).toEqual({
      masterMachineType: 'n1-standard-2',
      masterDiskSize: 100,
      workerMachineType: 'n1-standard-8',
      workerDiskSize: 300,
      numberOfWorkers: 10,
      numberOfPreemptibleWorkers: 20
    });
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should allow configuration via GCE preset', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Ensure set the form to something non-standard to start
    await pickMainCpu(wrapper, 8);
    await pickMainDiskSize(wrapper, 75);
    await pickComputeType(wrapper, ComputeType.Dataproc);

    await pickPreset(wrapper, runtimePresets.generalAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.GeneralAnalysis);
    expect(runtimeApiStub.runtime.gceConfig)
      .toEqual(runtimePresets.generalAnalysis.runtimeTemplate.gceConfig);
    expect(runtimeApiStub.runtime.dataprocConfig).toBeFalsy();
  });

  it('should allow configuration via dataproc preset', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.HailGenomicAnalysis);
    expect(runtimeApiStub.runtime.dataprocConfig)
      .toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should allow configuration via dataproc preset from modified form', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Configure the form - we expect all of the changes to be overwritten by
    // the Hail preset selection.
    await pickMainCpu(wrapper, 2);
    await pickMainRam(wrapper, 7.5);
    await pickMainDiskSize(wrapper, 100);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    await pickNumWorkers(wrapper, 10);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await pickPreset(wrapper, runtimePresets.hailAnalysis);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.HailGenomicAnalysis);
    expect(runtimeApiStub.runtime.dataprocConfig)
      .toEqual(runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig);
    expect(runtimeApiStub.runtime.gceConfig).toBeFalsy();
  });

  it('should tag as user override after preset modification', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();

    await mustClickButton(wrapper, 'Customize');

    // Take the preset but make a solitary modification.
    await pickPreset(wrapper, runtimePresets.hailAnalysis);
    await pickNumPreemptibleWorkers(wrapper, 20);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.UserOverride);
  });

  it('should tag as preset if configuration matches', async() => {
    runtimeApiStub.runtime = null;
    act(() => { runtimeStore.set({runtime: null, workspaceNamespace: workspaceStubs[0].namespace}) });

    const wrapper = await component();
    await mustClickButton(wrapper, 'Customize');

    // Take the preset, make a change, then revert.
    await pickPreset(wrapper, runtimePresets.generalAnalysis);
    await pickComputeType(wrapper, ComputeType.Dataproc);
    await pickWorkerCpu(wrapper, 2);
    await pickComputeType(wrapper, ComputeType.Standard);

    await mustClickButton(wrapper, 'Create');

    expect(runtimeApiStub.runtime.status).toEqual('Creating');
    expect(runtimeApiStub.runtime.configurationType)
      .toEqual(RuntimeConfigurationType.GeneralAnalysis);
  });

  it('should restrict memory options by cpu', async() => {
    const wrapper = await component();

    wrapper.find('div[id="runtime-cpu"]').first().simulate('click');
    wrapper.find('.p-dropdown-item').find({'aria-label': 8}).first().simulate('click');

    const memoryOptions = wrapper.find('#runtime-ram').first().find('.p-dropdown-item');
    expect(memoryOptions.exists()).toBeTruthy();

    // See app/utils/machines.ts, these are the valid memory options for an 8
    // CPU machine in GCE.
    expect(memoryOptions.map(m => m.text())).toEqual(['7.2', '30', '52']);
  });

  it('should disable the Next button if there are no changes and runtime is running', async() => {
    const wrapper = await component();

    expect(wrapper.find(Button).find({'aria-label': 'Next'}).first().prop('disabled')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - increase disk size', async() => {
    const wrapper = await component();

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 10);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - number of workers', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig(), configurationType: RuntimeConfigurationType.UserOverride};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();

    await pickNumWorkers(wrapper, getNumWorkers(wrapper) + 2);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - number of preemptible workers', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();

    await pickNumPreemptibleWorkers(wrapper, getNumPreemptibleWorkers(wrapper) + 2);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - CPU', async() => {
    const wrapper = await component();

    await pickMainCpu(wrapper, getMainCpu(wrapper) + 4);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about reboot if there are updates that require one - Memory', async() => {
    const wrapper = await component();

    // 15 GB -> 26 GB
    await pickMainRam(wrapper, 26);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('reboot')).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Compute Type', async() => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn user about deletion if there are updates that require one - Decrease Disk', async() => {
    const wrapper = await component();

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) - 5);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker CPU', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();

    // 4 -> 8
    await pickWorkerCpu(wrapper, 8);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker RAM', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();

    // 15 -> 26
    await pickWorkerRam(wrapper, 26);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should warn the user about deletion if there are updates that require one - Worker Disk', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();
    await pickWorkerDiskSize(wrapper, getWorkerDiskSize(wrapper) + 10);
    await mustClickButton(wrapper, 'Next');

    expect(wrapper.find(WarningMessage).text().includes('deletion')).toBeTruthy();
  });

  it('should retain original inputs when hitting cancel from the Confirm panel', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig()};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();

    await pickMainDiskSize(wrapper, 75);
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    await pickWorkerCpu(wrapper, 16);
    await pickWorkerRam(wrapper, 60);
    await pickNumPreemptibleWorkers(wrapper, 3);
    await pickNumWorkers(wrapper, 5);
    await pickWorkerDiskSize(wrapper, 100);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Cancel');

    expect(getMainDiskSize(wrapper)).toBe(75);
    expect(getMainCpu(wrapper)).toBe(8);
    expect(getMainRam(wrapper)).toBe(30);
    expect(getWorkerCpu(wrapper)).toBe(16);
    expect(getWorkerRam(wrapper)).toBe(60);
    expect(getNumPreemptibleWorkers(wrapper)).toBe(3);
    expect(getNumWorkers(wrapper)).toBe(5);
    expect(getWorkerDiskSize(wrapper)).toBe(100);
  });

  it('should disable Next button if Runtime is in between states', async() => {
    const runtime = {...runtimeApiStub.runtime, gceConfig: null, dataprocConfig: defaultDataprocConfig(), status: RuntimeStatus.Creating};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();
    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 20);

    expect(wrapper.find(Button).find({'aria-label': 'Next'}).first().prop('disabled')).toBeTruthy();
  });

  it('should send an updateRuntime API call if runtime changes do not require a delete', async() => {
    const wrapper = await component();

    const updateSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
    const deleteSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');

    await pickMainDiskSize(wrapper, getMainDiskSize(wrapper) + 20);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    expect(updateSpy).toHaveBeenCalled();
    expect(deleteSpy).toHaveBeenCalledTimes(0);
  });

  it('should send a delete call if an update requires delete', async() => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    await mustClickButton(wrapper, 'Next');
    await mustClickButton(wrapper, 'Update');

    expect(runtimeApiStub.runtime.status).toEqual('Deleting');
  });

  it('should show create button if runtime is deleted', async() => {
    const runtime = {...runtimeApiStub.runtime, status: RuntimeStatus.Deleted};
    runtimeStore.set({runtime: runtime, workspaceNamespace: workspaceStubs[0].namespace});

    const wrapper = await component();

    expect(wrapper.find(Button).find({'aria-label': 'Create'}).first().exists()).toBeTruthy();
  });

  it('should add additional options when the compute type changes', async() => {
    const wrapper = await component();

    await pickComputeType(wrapper, ComputeType.Dataproc);

    expect(wrapper.exists('span[id="num-workers"]')).toBeTruthy();
    expect(wrapper.exists('span[id="num-preemptible"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-cpu"]')).toBeTruthy();
    expect(wrapper.exists('div[id="worker-ram"]')).toBeTruthy();
    expect(wrapper.exists('span[id="worker-disk"]')).toBeTruthy();
  });

  it('should update the cost estimator when the compute profile changes', async() => {
    const wrapper = await component();

    const costEstimator = () => wrapper.find('[data-test-id="cost-estimator"]');
    expect(costEstimator()).toBeTruthy();

    // Default GCE machine, n1-standard-4, makes the running cost 20 cents an hour and the storage cost less than 1 cent an hour.
    const runningCost = () => costEstimator().find('[data-test-id="running-cost"]');
    const storageCost = () => costEstimator().find('[data-test-id="storage-cost"]');
    expect(runningCost().text()).toEqual('$0.20/hr');
    expect(storageCost().text()).toEqual('< $0.01/hr');

    // Change the machine to n1-standard-8 and bump the storage to 300GB. This should make the running cost 40 cents an hour and the storage cost 2 cents an hour.
    await pickMainCpu(wrapper, 8);
    await pickMainRam(wrapper, 30);
    await pickMainDiskSize(wrapper, 300);
    expect(runningCost().text()).toEqual('$0.40/hr');
    expect(storageCost().text()).toEqual('$0.02/hr');

    // Selecting the General Analysis preset should bring the machine back to n1-standard-4 with 50GB storage.
    await pickPreset(wrapper, {displayName: 'General Analysis'});
    expect(runningCost().text()).toEqual('$0.20/hr');
    expect(storageCost().text()).toEqual('< $0.01/hr');

    // After selecting Dataproc, the Dataproc defaults should make the running cost 71 cents an hour. The storage cost should remain unchanged.
    await pickComputeType(wrapper, ComputeType.Dataproc);
    expect(runningCost().text()).toEqual('$0.71/hr');
    expect(storageCost().text()).toEqual('< $0.01/hr');

    // Bump up all the worker values to increase the price on everything.
    await pickNumWorkers(wrapper, 4);
    await pickNumPreemptibleWorkers(wrapper, 4);
    await pickWorkerCpu(wrapper, 8);
    await pickWorkerRam(wrapper, 30);
    await pickWorkerDiskSize(wrapper, 300);
    expect(runningCost().text()).toEqual('$2.87/hr');
    expect(storageCost().text()).toEqual('$0.13/hr');
  });

  it('should allow runtime deletion', async() => {
    const wrapper = await component();

    wrapper.find(Link).find({'aria-label': 'Delete Environment'}).first().simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    await mustClickButton(wrapper, 'Delete');

    // Runtime should be deleting, and confirm page should no longer be visible.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Deleting);
    expect(wrapper.find(ConfirmDelete).exists()).toBeFalsy();
  });

  it('should allow cancelling runtime deletion', async() => {
    const wrapper = await component();

    wrapper.find(Link).find({'aria-label': 'Delete Environment'}).first().simulate('click');
    expect(wrapper.find(ConfirmDelete).exists()).toBeTruthy();

    // Click cancel
    await mustClickButton(wrapper, 'Cancel');

    // Runtime should still be active, and confirm page should no longer be visible.
    expect(runtimeApiStub.runtime.status).toEqual(RuntimeStatus.Running);
    expect(wrapper.find(ConfirmDelete).exists()).toBeFalsy();
  });
});
