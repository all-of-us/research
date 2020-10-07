import {Button, Clickable, MenuItem} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {allMachineTypes, validLeonardoMachineTypes} from 'app/utils/machines';
import {useCustomRuntime} from 'app/utils/runtime-utils';
import {
  abortRuntimeOperationForWorkspace,
  markRuntimeOperationCompleteForWorkspace,
  RuntimeOperation,
  runtimeOpsStore,
  updateRuntimeOpsStoreForWorkspaceNamespace,
  useStore
} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Dropdown} from 'primereact/dropdown';
import {InputNumber} from 'primereact/inputnumber';

import { RuntimeStatus } from 'generated';
import * as fp from 'lodash/fp';
import * as React from 'react';

const {useState, useEffect, Fragment} = React;

const styles = reactStyles({
  sectionHeader: {
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 700,
    lineHeight: '1rem',
    marginBottom: '12px',
    marginTop: '12px'
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, .75)),
    borderRadius: '3px',
    padding: '.75rem',
    marginTop: '.75rem'
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px'
  }
});

const defaultMachineType = allMachineTypes.find(({name}) => name === 'n1-standard-4');

const ActiveRuntimeOp = ({operation, workspaceNamespace}) => {
  return <React.Fragment>
    <h3 style={styles.sectionHeader}>Active Runtime Operations</h3>
    <FlexRow style={{'alignItems': 'center'}}>
      <span style={{'marginRight': '1rem'}}>
        {operation} in progress
      </span>
      <Button
          onClick={() => abortRuntimeOperationForWorkspace(workspaceNamespace)}
          data-test-id='active-runtime-operation'
      >
        Cancel
      </Button>
    </FlexRow>
  </React.Fragment>;
};

export interface Props {
  workspace: WorkspaceData;
}

const MachineSelector = ({onChange, selectedMachine, currentRuntime}) => {
  const {dataprocConfig, gceConfig} = currentRuntime;
  const masterMachineName = !!dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const initialMachineType = fp.find(({name}) => name === masterMachineName, allMachineTypes) || defaultMachineType;
  const {cpu, memory} = selectedMachine || initialMachineType;
  const maybeGetMachine = machineRequested => fp.equals(machineRequested, initialMachineType) ? null : machineRequested;

  return <Fragment>
    <div>
      <label htmlFor='runtime-cpu'
            style={{marginRight: '.25rem'}}>CPUs</label>
      <Dropdown id='runtime-cpu'
                // disabled={true}
                options={fp.flow(
                  // Show all CPU options.
                  fp.map('cpu'),
                  // In the event that was remove a machine type from our set of valid
                  // configs, we want to continue to allow rendering of the value here.
                  // Union also makes the CPU values unique.
                  fp.union([cpu]),
                  fp.sortBy(fp.identity)
                )(validLeonardoMachineTypes)}
                onChange={
                  ({value}) => fp.flow(
                    fp.sortBy('memory'),
                    fp.find({cpu: value}),
                    maybeGetMachine,
                    onChange)(validLeonardoMachineTypes)
                }
                value={cpu}/>
    </div>
    <div>
      <label htmlFor='runtime-ram'
            style={{marginRight: '.25rem'}}>RAM (GB)</label>
      <Dropdown id='runtime-ram'
                // disabled={true}
                options={fp.flow(
                  // Show valid memory options as constrained by the currently selected CPU.
                  fp.filter(({cpu: availableCpu}) => availableCpu === cpu),
                  fp.map('memory'),
                  // See above comment on CPU union.
                  fp.union([memory]),
                  fp.sortBy(fp.identity)
                )(validLeonardoMachineTypes)}
                onChange={
                  ({value}) => fp.flow(
                    fp.find({cpu, memory: value}),
                    // If the selected machine is not different from the current machine return null
                    maybeGetMachine,
                    onChange
                    )(validLeonardoMachineTypes) }
                value={memory}
                />
    </div>
  </Fragment>;
};

const DiskSizeSelection = ({onChange, selectedDiskSize, currentRuntime}) => {
  const {dataprocConfig, gceConfig} = currentRuntime;
  const masterDiskSize = !!dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.bootDiskSize;

  return <div>
    <label htmlFor='runtime-disk'
          style={{marginRight: '.25rem'}}>Disk (GB)</label>
      <InputNumber id='runtime-disk'
                //  disabled={true}
                showButtons
                decrementButtonClassName='p-button-secondary'
                incrementButtonClassName='p-button-secondary'
                value={selectedDiskSize || masterDiskSize}
                inputStyle={{padding: '.75rem .5rem', width: '2rem'}}
                onChange={({value}) => onChange(value === masterDiskSize ? null : value)}
                min={50 /* Runtime API has a minimum 50GB requirement. */}/>
  </div>;
};

export const RuntimePanel = withCurrentWorkspace()(({workspace}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [selectedDiskSize, setSelectedDiskSize] = useState(null);
  const [selectedMachine, setselectedMachine] = useState(null);
  const runtimeOps = useStore(runtimeOpsStore);
  const [currentRuntime, setRequestedRuntime] = useCustomRuntime(workspace.namespace);

  const activeRuntimeOp: RuntimeOperation = runtimeOps.opsByWorkspaceNamespace[workspace.namespace];
  const {status = RuntimeStatus.Unknown, toolDockerImage = '', dataprocConfig = null, gceConfig = {}} = currentRuntime || {};
  const masterMachineType = !!dataprocConfig ? dataprocConfig.masterMachineType : gceConfig.machineType;
  const masterDiskSize = !!dataprocConfig ? dataprocConfig.masterDiskSize : gceConfig.bootDiskSize;
  const selectedMachineType = selectedMachine && selectedMachine.name;

  useEffect(() => {
    const aborter = new AbortController();
    const {namespace} = workspace;
    const loadRuntime = async() => {
        // TODO(RW-5420): Centralize a runtimeStore.
      try {
        const promise = runtimeApi().getRuntime(namespace, {signal: aborter.signal});
        updateRuntimeOpsStoreForWorkspaceNamespace(namespace, {
          promise: promise,
          operation: 'get',
          aborter: aborter
        });
        await promise;
      } catch (e) {
        // 404 is expected if the runtime doesn't exist, represent this as a null
        // runtime rather than an error mode.
        if (e.status !== 404) {
          setError(true);
        }
      }
      markRuntimeOperationCompleteForWorkspace(namespace);
    };

    loadRuntime();
    return () => aborter.abort();
  }, []);

  useEffect(() => {
    if (currentRuntime) {
      setLoading(false);
    }
  }, [currentRuntime]);

  if (loading) {
    return <Spinner style={{width: '100%', marginTop: '5rem'}}/>;
  } else if (error) {
    return <div>Error loading compute configuration</div>;
  } else if (!currentRuntime) {
    // TODO(RW-5591): Create runtime page goes here.
    return <React.Fragment>
      <div>No runtime exists yet</div>
      {activeRuntimeOp && <hr/>}
      {activeRuntimeOp && <div>
        <ActiveRuntimeOp operation={activeRuntimeOp.operation} workspaceNamespace={workspace.namespace}/>
      </div>}
    </React.Fragment>;
  }

  const isDataproc = (currentRuntime && !!currentRuntime.dataprocConfig);
  const runtimeChanged = selectedMachine || selectedDiskSize;

  return <div data-test-id='runtime-panel'>
    <h3 style={styles.sectionHeader}>Cloud analysis environment</h3>
    <div>
      Your analysis environment consists of an application and compute resources.
      Your cloud environment is unique to this workspace and not shared with other users.
    </div>
    {/* TODO(RW-5419): Cost estimates go here. */}
    <div style={styles.controlSection}>
      {/* Recommended runtime: pick from default templates or change the image. */}
      <PopupTrigger side='bottom'
                    closeOnClick
                    content={
                      <React.Fragment>
                        <MenuItem style={styles.presetMenuItem}>General purpose analysis</MenuItem>
                        <MenuItem style={styles.presetMenuItem}>Genomics analysis</MenuItem>
                      </React.Fragment>
                    }>
        <Clickable data-test-id='runtime-presets-menu'
                   disabled={true}>
          Recommended environments <ClrIcon shape='caret down'/>
        </Clickable>
      </PopupTrigger>
      <h3 style={styles.sectionHeader}>Application configuration</h3>
      {/* TODO(RW-5413): Populate the image list with server driven options. */}
      <Dropdown style={{width: '100%'}}
                data-test-id='runtime-image-dropdown'
                disabled={true}
                options={[toolDockerImage]}
                value={toolDockerImage}/>
      {/* Runtime customization: change detailed machine configuration options. */}
      <h3 style={styles.sectionHeader}>Cloud compute profile</h3>
      <FlexRow style={{justifyContent: 'space-between'}}>
        <MachineSelector selectedMachine={selectedMachine} onChange={setselectedMachine} currentRuntime={currentRuntime}/>
        <DiskSizeSelection selectedDiskSize={selectedDiskSize} onChange={setSelectedDiskSize} currentRuntime={currentRuntime}/>
      </FlexRow>
      <FlexColumn style={{marginTop: '1rem'}}>
        <label htmlFor='runtime-compute'>Compute type</label>
        <Dropdown id='runtime-compute'
                  style={{width: '10rem'}}
                  disabled={true}
                  options={['Dataproc cluster', 'Standard VM']}
                  value={isDataproc ? 'Dataproc cluster' : 'Standard VM'}/>
      </FlexColumn>
    </div>
    <FlexRow style={{justifyContent: 'flex-end', marginTop: '.75rem'}}>
      <Button
        disabled={status !== RuntimeStatus.Running || !runtimeChanged}
        onClick={() => setRequestedRuntime({dataprocConfig: {
          masterMachineType: selectedMachineType || masterMachineType,
          masterDiskSize: selectedDiskSize || masterDiskSize
        }
        })
        }
      >{currentRuntime ? 'Update' : 'Create'}</Button>
    </FlexRow>
    {activeRuntimeOp && <React.Fragment>
      <hr/>
      <ActiveRuntimeOp operation={activeRuntimeOp.operation} workspaceNamespace={workspace.namespace}/>
    </React.Fragment>}
  </div>;

});
