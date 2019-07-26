import {DataSet, DataSetApi, WorkspacesApi} from "../../../../generated/fetch/api";
import {WorkspacesApiStub} from "../../../../testing/stubs/workspaces-api-stub";
import {DataSetApiStub} from "../../../../testing/stubs/data-set-api-stub";
import {registerApiClient} from "../../../services/swagger-fetch-clients";
import {mount} from "enzyme";
import * as React from "react";
import {ExportDataSetModal} from "./export-data-set-modal";

const workspaceNamespace = 'workspaceNamespace';
const workspaceFirecloudName = 'workspaceId';
const dataSet: DataSet = {
  name: 'hello world',
  description: 'hi',
  conceptSets: [],
  cohorts: [],
  values: [],
  includesAllParticipants: false,
};

const createExportDataSetModal = () => {
  return <ExportDataSetModal
    closeFunction={() => {}}
    workspaceNamespace={workspaceNamespace}
    workspaceFirecloudName={workspaceFirecloudName}
    dataSet={dataSet}
  />;
}


describe('ExportDataSetModal', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
  });


  it('should render', () => {
    const wrapper = mount(createExportDataSetModal());
    expect(wrapper.exists()).toBeTruthy();
  });
})