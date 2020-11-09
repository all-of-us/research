import {AlertDanger} from 'app/components/alert';
import {Button, TabButton} from 'app/components/buttons';
import {SmallHeader, styles as headerStyles} from 'app/components/headers';
import {CheckBox, RadioButton, Select, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {appendNotebookFileSuffix} from 'app/pages/analysis/util';

import {datasetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {summarizeErrors} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {encodeURIComponentStrict, navigateByUrl} from 'app/utils/navigation';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {
  Dataset,
  DatasetExportRequest,
  DatasetRequest,
  DomainValuePair,
  FileDetail,
  KernelTypeEnum,
  PrePackagedConceptSetEnum
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {validate} from 'validate.js';
import GenomicsAnalysisToolEnum = DatasetExportRequest.GenomicsAnalysisToolEnum;
import GenomicsDataTypeEnum = DatasetExportRequest.GenomicsDataTypeEnum;

interface Props {
  closeFunction: Function;
  dataset: Dataset;
  includesAllParticipants: boolean;
  prePackagedConceptSet: PrePackagedConceptSetEnum;
  selectedConceptSetIds: number[];
  selectedCohortIds: number[];
  selectedDomainValuePairs: DomainValuePair[];
  workspaceNamespace: string;
  workspaceId: string;
  billingLocked: boolean;
  displayMicroarrayOptions: boolean;
}

interface State {
  conflictDatasetName: boolean;
  existingNotebooks: FileDetail[];
  exportToNotebook: boolean;
  kernelType: KernelTypeEnum;
  loading: boolean;
  missingDatasetInfo: boolean;
  name: string;
  newNotebook: boolean;
  notebookName: string;
  notebooksLoading: boolean;
  previewedKernelType: KernelTypeEnum;
  queries: Map<KernelTypeEnum, String>;
  seePreview: boolean;
  includeRawMicroarrayData: boolean;
  genomicsAnalysisTool: GenomicsAnalysisToolEnum;
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

class NewDatasetModal extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      conflictDatasetName: false,
      existingNotebooks: [],
      exportToNotebook: !props.billingLocked,
      kernelType: KernelTypeEnum.Python,
      loading: false,
      missingDatasetInfo: false,
      name: '',
      newNotebook: true,
      notebookName: '',
      notebooksLoading: false,
      previewedKernelType: KernelTypeEnum.Python,
      queries: new Map([[KernelTypeEnum.Python, undefined], [KernelTypeEnum.R, undefined]]),
      seePreview: false,
      includeRawMicroarrayData: false,
      genomicsAnalysisTool: GenomicsAnalysisToolEnum.NONE
    };
  }

  componentDidMount() {
    this.generateQuery();
    this.loadNotebooks();
  }

  private async loadNotebooks() {
    try {
      const {workspaceNamespace, workspaceId} = this.props;
      if (this.props.dataset) {
        this.setState({name: this.props.dataset.name});
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

  async updateDataset() {
    const {dataset, workspaceNamespace, workspaceId} = this.props;
    const {name} = this.state;
    const request: DatasetRequest = {
      name: name,
      includesAllParticipants: dataset.includesAllParticipants,
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      domainValuePairs: this.props.selectedDomainValuePairs,
      etag: dataset.etag
    };
    await datasetApi().updateDataset(workspaceNamespace, workspaceId, dataset.id, request);
  }

  async saveDataset() {

    const {dataset, workspaceNamespace, workspaceId} = this.props;
    if (!this.state.name) {
      return;
    }
    this.setState({conflictDatasetName: false, missingDatasetInfo: false, loading: true});
    const {name} = this.state;
    const request: DatasetRequest = {
      name: name,
      description: '',
      includesAllParticipants: this.props.includesAllParticipants,
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      domainValuePairs: this.props.selectedDomainValuePairs,
      prePackagedConceptSet: this.props.prePackagedConceptSet
    };
    try {
      // If dataset exist it is an update
      if (this.props.dataset) {
        const updateReq = {
          ...request,
          description: dataset.description,
          etag: dataset.etag
        };
        await datasetApi().updateDataset(workspaceNamespace, workspaceId, dataset.id, updateReq);
      } else {
        await datasetApi().createDataset(workspaceNamespace, workspaceId, request);
      }
      if (this.state.exportToNotebook) {
        await datasetApi().exportToNotebook(
          workspaceNamespace, workspaceId,
          {
            datasetRequest: request,
            kernelType: this.state.kernelType,
            notebookName: this.state.notebookName,
            newNotebook: this.state.newNotebook,
            genomicsDataType: this.state.includeRawMicroarrayData ? GenomicsDataTypeEnum.MICROARRAY : GenomicsDataTypeEnum.NONE,
            genomicsAnalysisTool: this.state.genomicsAnalysisTool
          });
        // Open notebook in a new tab and return back to the Data tab
        const notebookUrl = `/workspaces/${workspaceNamespace}/${workspaceId}/notebooks/preview/` +
          appendNotebookFileSuffix(encodeURIComponentStrict(this.state.notebookName));
        navigateByUrl(notebookUrl);
      } else {
        window.history.back();
      }
    } catch (e) {
      if (e.status === 409) {
        this.setState({conflictDatasetName: true, loading: false});
      } else if (e.status === 400) {
        this.setState({missingDatasetInfo: true, loading: false});
      }
    }
  }

  changeExportToNotebook(checked: boolean) {
    this.setState({exportToNotebook: checked});
  }

  onSaveClick() {
    if (this.props.dataset) {
      if (this.state.exportToNotebook) {
        AnalyticsTracker.DatasetBuilder.UpdateAndAnalyze(this.state.kernelType);
      } else {
        AnalyticsTracker.DatasetBuilder.Update();
      }
    } else {
      if (this.state.exportToNotebook) {
        AnalyticsTracker.DatasetBuilder.SaveAndAnalyze(this.state.kernelType);
      } else {
        AnalyticsTracker.DatasetBuilder.Save();
      }
    }

    this.saveDataset();
  }

  async generateQuery() {
    const {workspaceNamespace, workspaceId} = this.props;
    const datasetRequest: DatasetRequest = {
      name: 'dataset',
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      domainValuePairs: this.props.selectedDomainValuePairs,
      includesAllParticipants: this.props.includesAllParticipants,
      prePackagedConceptSet: this.props.prePackagedConceptSet
    };
    datasetApi().generateCode(
      workspaceNamespace,
      workspaceId,
      KernelTypeEnum.Python.toString(),
      datasetRequest).then(pythonCode => {
        this.setState(({queries}) => ({
          queries: queries.set(KernelTypeEnum.Python, pythonCode.code)}));
      });
    datasetApi().generateCode(
      workspaceNamespace,
      workspaceId,
      KernelTypeEnum.R.toString(),
      datasetRequest).then(rCode => {
        this.setState(({queries}) => ({queries: queries.set(KernelTypeEnum.R, rCode.code)}));
      });
  }

  render() {
    const {
      conflictDatasetName,
      exportToNotebook,
      loading,
      missingDatasetInfo,
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
      <ModalTitle>{this.props.dataset ? 'Update' : 'Save'} Dataset</ModalTitle>
      <ModalBody>
        <div>
          {conflictDatasetName &&
          <AlertDanger>Dataset with same name exist</AlertDanger>
          }
          {missingDatasetInfo &&
          <AlertDanger> Data state cannot save as some information is missing</AlertDanger>
          }
          <TextInput type='text' autoFocus placeholder='Dataset Name'
                     value={name} data-test-id='data-set-name-input'
                     onChange={v => this.setState({
                       name: v, conflictDatasetName: false
                     })}/>
        </div>
        <TooltipTrigger content={this.props.billingLocked && ACTION_DISABLED_INVALID_BILLING}>
          <div style={{display: 'flex', alignItems: 'center', marginTop: '1rem', ...(this.props.billingLocked && {opacity: 0.5})}}>
            <CheckBox style={{height: 17, width: 17}}
                      disabled={this.props.billingLocked}
                      data-test-id='export-to-notebook'
                      onChange={(checked) => this.changeExportToNotebook(checked)}
                      checked={this.state.exportToNotebook} />
            <div style={{marginLeft: '.5rem',
              color: colors.primary}}>Export to notebook</div>
          </div>
        </TooltipTrigger>
        {exportToNotebook && <React.Fragment>
          {notebooksLoading && <SpinnerOverlay />}
          <Button style={{marginTop: '1rem'}} data-test-id='code-preview-button'
                  onClick={() => {
                    if (!seePreview) {
                      AnalyticsTracker.DatasetBuilder.SeeCodePreview();
                    }
                    this.setState({seePreview: !seePreview});
                  }}>
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
                    data-test-id={'kernel-type-' + kernelTypeEnum.toLowerCase()}
                    checked={this.state.kernelType === kernelTypeEnum}
                    onChange={() => {
                      this.setState({
                        kernelType: kernelTypeEnum,
                        includeRawMicroarrayData: kernelTypeEnum === KernelTypeEnum.R ? false : this.state.includeRawMicroarrayData
                      });
                    }}
                  />
                  &nbsp;{kernelTypeEnum}
                </label>
              )}
          </React.Fragment>}
          {this.props.displayMicroarrayOptions && this.state.kernelType === KernelTypeEnum.Python &&
          <div style={{border: '1px solid grey', padding: '.5rem', paddingTop: 0, marginTop: '.5rem'}}>
            <TextColumn>
              <div style={headerStyles.formLabel}>Genomics Pre-alpha</div>
              <div>(non-production only; synthetic data)</div>
            </TextColumn>

            <div style={{display: 'flex', alignItems: 'center'}}>
              <CheckBox style={{height: 17, width: 17}}
                        data-test-id='include-raw-microarray-data'
                        onChange={(checked) => this.setState({includeRawMicroarrayData: checked})}
                        checked={this.state.includeRawMicroarrayData} />
              <div style={{marginLeft: '.5rem', color: colors.primary}}>
                Include raw microarray data
              </div>
            </div>

            {this.state.includeRawMicroarrayData && <div style={{marginTop: '.3rem'}}>
              <p style={{color: colors.primary}}>
                Extract genomics data for analysis using:
              </p>

              {Object.keys(GenomicsAnalysisToolEnum).map((enumKey, i) => {
                return <React.Fragment>
                  <label key={i} style={{display: 'block'}}>
                    <RadioButton
                      data-test-id={'genomics-analysis-tool-' + enumKey.toLowerCase()}
                      checked={this.state.genomicsAnalysisTool === GenomicsAnalysisToolEnum[enumKey]}
                      onChange={() => this.setState({genomicsAnalysisTool: GenomicsAnalysisToolEnum[enumKey]})}
                    />
                    &nbsp; {fp.startCase(enumKey.toLowerCase())}
                  </label>
                </React.Fragment>;
              })}
            </div>}
          </div> }
        </React.Fragment>}
      </ModalBody>
      <ModalFooter>
        <Button type='secondary'
                onClick={this.props.closeFunction}
                style={{marginRight: '2rem'}}>
          Cancel
        </Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button type='primary'
                  data-test-id='save-data-set'
                  disabled={errors}
                  onClick={() => this.onSaveClick()}>
            {!this.props.dataset ? 'Save' : 'Update' }{exportToNotebook && ' and Analyze'}
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

export {
  NewDatasetModal
};
