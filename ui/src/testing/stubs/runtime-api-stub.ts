import {
  DataprocConfig, GceConfig,
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeLocalizeRequest,
  RuntimeLocalizeResponse,
  RuntimeStatus
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export const mockGceConfig: GceConfig = {
  diskSize: 80,
  machineType: 'n1-standard-4'
};

export const mockDataprocConfig: DataprocConfig = {
  masterMachineType: 'n1-standard-4',
  masterDiskSize: 80,
  workerDiskSize: 40,
  workerMachineType: 'n1-standard-4',
  numberOfWorkers: 1,
  numberOfPreemptibleWorkers: 2,
  numberOfWorkerLocalSSDs: 0
};

export class RuntimeApiStub extends RuntimeApi {
  public runtime: Runtime;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
    // this.runtime = {
    //   runtimeName: 'Runtime Name',
    //   googleProject: 'Namespace',
    //   status: RuntimeStatus.Running,
    //   createdDate: '08/08/2018',
    //   toolDockerImage: 'broadinstitute/terra-jupyter-aou:1.0.999',
    //   configurationType: RuntimeConfigurationType.GeneralAnalysis,
    //   // TODO eric: what kind of non nullability guraantees do we have around this response?
    //   dataprocConfig: {
    //     masterMachineType: 'n1-standard-4',
    //     masterDiskSize: 80,
    //     numberOfWorkers: 0,
    //     numberOfPreemptibleWorkers: 0,
    //     workerDiskSize: 40,
    //     workerMachineType: 'n1-standard-4',
    //     numberOfWorkerLocalSSDs: 0
    //   }
    // };
    this.runtime = {
      runtimeName: 'Runtime Name',
      googleProject: 'Namespace',
      status: RuntimeStatus.Running,
      createdDate: '08/08/2018',
      toolDockerImage: 'broadinstitute/terra-jupyter-aou:1.0.999',
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      // TODO eric: what kind of non nullability guraantees do we have around this response?
      gceConfig: mockGceConfig
    };
  }

  getRuntime(workspaceNamespace: string, options?: any): Promise<Runtime> {
    return new Promise<Runtime>(resolve => {
      resolve(this.runtime);
    });
  }

  createRuntime(workspaceNamespace: string, runtime: Runtime): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.runtime = {...runtime, status: RuntimeStatus.Creating};
      resolve({});
    });
  }

  deleteRuntime(workspaceNamespace: string, options?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.runtime.status = RuntimeStatus.Deleting;
      resolve({});
    });
  }

  localize(projectName: string, req: RuntimeLocalizeRequest,
    extraHttpRequestParams?: any): Promise<RuntimeLocalizeResponse> {
    return new Promise<RuntimeLocalizeResponse>(resolve => {
      resolve({runtimeLocalDirectory: 'workspaces/${req.workspaceId}'});
    });
  }
}
