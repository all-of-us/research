import * as React from 'react';

import {validate} from 'validate.js';

import {dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {AlertDanger} from 'app/components/alert';
import {Button, TabButton} from 'app/components/buttons';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {
  CheckBox,
  RadioButton,
  Select,
  TextArea,
  TextInput
} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import colors from 'app/styles/colors';
import {summarizeErrors} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {
  DataSet,
  DataSetRequest,
  DomainValuePair,
  FileDetail,
  KernelTypeEnum
} from 'generated/fetch';

interface Props {
  closeFunction: Function;
  dataSet: DataSet;
  includesAllParticipants: boolean;
  selectedConceptSetIds: number[];
  selectedCohortIds: number[];
  selectedValues: DomainValuePair[];
  workspaceNamespace: string;
  workspaceId: string;
}

interface State {
  conflictDataSetName: boolean;
  existingNotebooks: FileDetail[];
  exportToNotebook: boolean;
  kernelType: KernelTypeEnum;
  loading: boolean;
  missingDataSetInfo: boolean;
  name: string;
  newNotebook: boolean;
  notebookName: string;
  notebooksLoading: boolean;
  previewedKernelType: KernelTypeEnum;
  queries: Map<KernelTypeEnum, String>;
  seePreview: boolean;
}

const styles = {
  codePreviewSelector: {
    display: 'flex',
    marginTop: '1rem'
  },
  codePreviewSelectorTab: {
    width: '2.6rem'
  }
};


class NewDataSetModal extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      conflictDataSetName: false,
      existingNotebooks: [],
      exportToNotebook: true,
      kernelType: KernelTypeEnum.Python,
      loading: false,
      missingDataSetInfo: false,
      name: '',
      newNotebook: true,
      notebookName: '',
      notebooksLoading: false,
      previewedKernelType: KernelTypeEnum.Python,
      queries: new Map([[KernelTypeEnum.Python, undefined], [KernelTypeEnum.R, undefined]]),
      seePreview: false
    };
  }

  componentDidMount() {
    this.generateQuery();
    this.loadNotebooks();
  }

  private async loadNotebooks() {
    try {
      const {workspaceNamespace, workspaceId} = this.props;
      if (this.props.dataSet) {
        this.setState({name: this.props.dataSet.name});
      }
      this.setState({notebooksLoading: true});
      const existingNotebooks =
        await workspacesApi().getNoteBookList(workspaceNamespace, workspaceId);
      this.setState({existingNotebooks});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({notebooksLoading: false});
    }
  }

  async updateDataSet() {
    const {dataSet, workspaceNamespace, workspaceId} = this.props;
    const {name} = this.state;
    const request = {
      name: name,
      includesAllParticipants: dataSet.includesAllParticipants,
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      values: this.props.selectedValues,
      etag: dataSet.etag
    };
    await dataSetApi().updateDataSet(workspaceNamespace, workspaceId, dataSet.id, request);
  }

  async saveDataSet() {

    const {dataSet, workspaceNamespace, workspaceId} = this.props;
    if (!this.state.name) {
      return;
    }
    this.setState({conflictDataSetName: false, missingDataSetInfo: false, loading: true});
    const {name} = this.state;
    let request = {
      name: name,
      description: '',
      includesAllParticipants: this.props.includesAllParticipants,
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      values: this.props.selectedValues
    };
    try {
      // If data set exist it is an update
      if (this.props.dataSet) {
        const updateReq = {
          ...request,
          description: dataSet.description,
          etag: dataSet.etag
        };
        await dataSetApi().updateDataSet(workspaceNamespace, workspaceId, dataSet.id, request);
      } else {
        await dataSetApi().createDataSet(workspaceNamespace, workspaceId, request);
      }
      if (!this.state.exportToNotebook) {
        window.history.back();
      } else {
        await dataSetApi().exportToNotebook(
          workspaceNamespace, workspaceId,
          {
            dataSetRequest: request,
            kernelType: this.state.kernelType,
            notebookName: this.state.notebookName,
            newNotebook: this.state.newNotebook
          });
        navigate(['workspaces',
          workspaceNamespace,
          workspaceId, 'notebooks', this.state.notebookName + '.ipynb']);
      }
    } catch (e) {
      if (e.status === 409) {
        this.setState({conflictDataSetName: true, loading: false});
      } else if (e.status === 400) {
        this.setState({missingDataSetInfo: true, loading: false});
      }
    }
  }

  changeExportToNotebook() {
    this.setState({exportToNotebook: !this.state.exportToNotebook});
  }

  async generateQuery() {
    const {workspaceNamespace, workspaceId} = this.props;
    const dataSetRequest: DataSetRequest = {
      name: 'dataSet',
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      values: this.props.selectedValues,
      includesAllParticipants: this.props.includesAllParticipants,
    };
    dataSetApi().generateCode(
      workspaceNamespace,
      workspaceId,
      KernelTypeEnum.Python.toString(),
      dataSetRequest).then(pythonCode => {
        this.setState(({queries}) => ({
          queries: queries.set(KernelTypeEnum.Python, pythonCode.code)}));
      });
    dataSetApi().generateCode(
      workspaceNamespace,
      workspaceId,
      KernelTypeEnum.R.toString(),
      dataSetRequest).then(rCode => {
        this.setState(({queries}) => ({queries: queries.set(KernelTypeEnum.R, rCode.code)}));
      });
  }

  render() {
    const {
      conflictDataSetName,
      exportToNotebook,
      loading,
      missingDataSetInfo,
      name,
      newNotebook,
      notebookName,
      notebooksLoading,
      existingNotebooks,
      previewedKernelType,
      queries,
      seePreview
    } = this.state;

    const selectOptions = [{label: '(Create a new notebook)', value: ''}]
      .concat(existingNotebooks.map(notebook => ({
        value: notebook.name.slice(0, -6),
        label: notebook.name.slice(0, -6)
      })));

    const errors = validate({name, notebookName}, {
      name: {
        presence: {allowEmpty: false}
      },
      notebookName: {
        presence: {allowEmpty: !exportToNotebook},
        exclusion: {
          within: newNotebook ? existingNotebooks.map(fd => fd.name.slice(0, -6)) : [],
          message: 'already exists'
        }
      }
    });
    return <Modal loading={loading}>
      <ModalTitle>{this.props.dataSet ? 'Update' : 'Save'} Dataset</ModalTitle>
      <ModalBody>
        <div>
          {conflictDataSetName &&
          <AlertDanger>DataSet with same name exist</AlertDanger>
          }
          {missingDataSetInfo &&
          <AlertDanger> Data state cannot save as some information is missing</AlertDanger>
          }
          <TextInput type='text' autoFocus placeholder='Data Set Name'
                     value={name} data-test-id='data-set-name-input'
                     onChange={v => this.setState({
                       name: v, conflictDataSetName: false
                     })}/>
        </div>
        <div style={{display: 'flex', alignItems: 'center', marginTop: '1rem'}}>
          <CheckBox style={{height: 17, width: 17}}
                    data-test-id='export-to-notebook'
                    onChange={() => this.changeExportToNotebook()}
                    checked={this.state.exportToNotebook} />
          <div style={{marginLeft: '.5rem',
            color: colors.black[0]}}>Export to notebook</div>
        </div>
        {exportToNotebook && <React.Fragment>
          {notebooksLoading && <SpinnerOverlay />}
          <Button style={{marginTop: '1rem'}} data-test-id='code-preview-button'
                  onClick={() => this.setState({seePreview: !seePreview})}>
            {seePreview ? 'Hide Preview' : 'See Code Preview'}
          </Button>
          {seePreview && <React.Fragment>
            {Array.from(queries.values())
              .filter(query => query !== undefined).length === 0 && <SpinnerOverlay />}
            <div style={styles.codePreviewSelector}>
              {Object.keys(KernelTypeEnum)
                .map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
                .map((kernelTypeEnum, i) =>
                  <TabButton onClick={() => this.setState({previewedKernelType: kernelTypeEnum})}
                             key={i}
                             active={previewedKernelType === kernelTypeEnum}
                             style={styles.codePreviewSelectorTab}
                             disabled={queries.get(kernelTypeEnum) === undefined}>
                    {kernelTypeEnum}
                  </TabButton>)}
            </div>
            <TextArea disabled={true} onChange={() => {}}
                      data-test-id='code-text-box'
                      value={queries.get(previewedKernelType)} />
          </React.Fragment>}
          <div style={{marginTop: '1rem'}}>
            <Select value={this.state.notebookName}
                    options={selectOptions}
                    onChange={v => this.setState({notebookName: v, newNotebook: v === ''})}/>
          </div>
          {newNotebook && <React.Fragment>
            <SmallHeader style={{fontSize: 14, marginTop: '1rem'}}>Notebook Name</SmallHeader>
            <TextInput onChange={(v) => this.setState({notebookName: v})}
                       value={notebookName} data-test-id='notebook-name-input'/>
            <div style={headerStyles.formLabel}>
              Programming Language:
            </div>
            {Object.keys(KernelTypeEnum).map(kernelTypeEnumKey => KernelTypeEnum[kernelTypeEnumKey])
              .map((kernelTypeEnum, i) =>
                <label key={i} style={{display: 'block'}}>
                  <RadioButton
                    checked={this.state.kernelType === kernelTypeEnum}
                    onChange={() => this.setState({kernelType: kernelTypeEnum})}
                  />
                  &nbsp;{kernelTypeEnum}
                </label>
              )}
          </React.Fragment>}
        </React.Fragment>}
      </ModalBody>
      <ModalFooter>
        <Button type='secondary'
                onClick={this.props.closeFunction}
                style={{marginRight: '2rem'}}>
          Cancel
        </Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button type='primary' data-test-id='save-data-set'
                  disabled={errors} onClick={() => this.saveDataSet()}>
            {!this.props.dataSet ? 'Save' : 'Update' }{exportToNotebook && ' and Analyze'}
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

export {
  NewDataSetModal,
};
