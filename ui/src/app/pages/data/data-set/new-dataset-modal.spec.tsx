import {mount} from 'enzyme';
import * as React from 'react';

import {dataSetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {
  DataSetApi,
  DataSetRequest,
  KernelTypeEnum,
  PrePackagedConceptSetEnum,
  WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {NewDataSetModal} from './new-dataset-modal';

const prePackagedConceptSet = Array.of(PrePackagedConceptSetEnum.NONE);
const workspaceNamespace = 'workspaceNamespace';
const workspaceId = 'workspaceId';

let dataSet;

const createNewDataSetModal = () => {
  return <NewDataSetModal
    closeFunction={() => {}}
    includesAllParticipants={false}
    selectedConceptSetIds={[]}
    selectedCohortIds={[]}
    selectedDomainValuePairs={[]}
    workspaceNamespace={workspaceNamespace}
    workspaceId={workspaceId}
    dataSet={dataSet}
    prePackagedConceptSet={prePackagedConceptSet}
    billingLocked={false}
  />;
};

describe('NewDataSetModal', () => {
  beforeEach(() => {
    window.open = jest.fn();
    dataSet = undefined;
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mount(createNewDataSetModal());
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should only display code when button pressed', async() => {
    const wrapper = mount(createNewDataSetModal());
    expect(wrapper.find('[data-test-id="code-text-box"]').exists()).toBeFalsy();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
      .first().text()).toEqual('See Code Preview');
    wrapper.find('[data-test-id="code-preview-button"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
      .first().text()).toEqual('Hide Preview');
  });

  it('should not allow submission if no name specified', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).not.toHaveBeenCalled();
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should not export if export is not checked', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="export-to-notebook"]').first().simulate('change', {target: {checked: false}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      domainValuePairs: [],
      prePackagedConceptSet: Array.of(PrePackagedConceptSetEnum.NONE)
    });
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should not submit if export checked but no name or selected notebook', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).not.toHaveBeenCalled();
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should submit if export is unchecked and name specified', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';
    const notebookNameStub = 'Notebook Name';
    const dataSetRequestStub: DataSetRequest = {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      domainValuePairs: [],
      prePackagedConceptSet: Array.of(PrePackagedConceptSetEnum.NONE)
    };

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="notebook-name-input"]')
      .first().simulate('change', {target: {value: notebookNameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).toHaveBeenCalled();
    expect(exportSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      dataSetRequest: dataSetRequestStub,
      newNotebook: true,
      notebookName: notebookNameStub,
      kernelType: KernelTypeEnum.Python
    });
  });

  it ('should have default dataSet name if dataset is passed as props', () => {
    const name = 'Update Dataset';
    dataSet = {...dataSet, name: name, description: 'dataset'};
    const wrapper = mount(createNewDataSetModal());
    const dataSetName  =
      wrapper.find('[data-test-id="data-set-name-input"]').first().prop('value');
    expect(dataSetName).toBe(name);
  });
});
