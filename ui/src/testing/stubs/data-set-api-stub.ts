import {
  DataSet,
  DataSetApi,
  DataSetExportRequest,
  DataSetPreviewResponse,
  DataSetQueryList,
  DataSetRequest,
  EmptyResponse
} from 'generated/fetch';

export class DataSetApiStub extends DataSetApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  generateQuery(workspaceNamespace: string,
    workspaceId: string,
    dataSet: DataSetRequest): Promise<DataSetQueryList> {
    return new Promise<DataSetQueryList>(resolve => {
      resolve({queryList: []});
    });
  }

  createDataSet(workspaceNamespace: string,
    workspaceId: string,
    dataSet: DataSetRequest): Promise<DataSet> {
    return new Promise<DataSet>(resolve => {
      resolve({});
    });
  }

  exportToNotebook(workspaceNamespace: string,
    workspaceId: string,
    dataSetExportRequest: DataSetExportRequest): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      resolve({});
    });
  }

  previewQuery(workspaceNamespace: string,
    workspaceId: string, dataSet: DataSetRequest): Promise<DataSetPreviewResponse> {
    return Promise.resolve({
      domainValue: [
          {domain: 'CONDITION', values: [{value: 'Value1'}, {value: 'Value2'}]}
      ]
    });
  }
}
