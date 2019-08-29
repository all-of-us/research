import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {CheckBox} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {CircleWithText} from 'app/icons/circleWithText';
import {NewDataSetModal} from 'app/pages/data/data-set/new-dataset-modal';
import {
  cohortsApi,
  conceptsApi,
  conceptSetsApi,
  dataSetApi
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  toggleIncludes,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActions';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {
  Cohort,
  ConceptSet,
  DataSet,
  DataSetPreviewList,
  Domain,
  DomainValue,
  DomainValuePair,
  DomainValuesResponse,
  ErrorResponse,
  PrePackagedConceptSetEnum,
  Surveys,
  ValueSet,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';

export const styles = reactStyles({
  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingRight: '0.55rem',
    color: colors.primary,
    borderBottom: `1px solid ${colors.light}`,
    display: 'flex',
    justifyContent: 'space-between',
    flexDirection: 'row'
  },

  listItem: {
    border: `0.5px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    margin: '.4rem .4rem .4rem .55rem',
    height: '1.5rem',
    display: 'flex'
  },

  listItemCheckbox: {
    height: 17,
    width: 17,
    marginLeft: 10,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.success
  },

  valueListItemCheckboxStyling: {
    height: 17,
    width: 17,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.success
  },

  subheader: {
    fontWeight: 400,
    fontSize: '0.6rem',
    marginTop: '0.5rem',
    paddingLeft: '0.55rem',
    color: colors.primary
  },

  previewButtonBox: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginTop: '2.675rem',
    marginBottom: '2rem'
  },

  previewDataHeaderBox: {
    display: 'flex',
    flexDirection: 'row',
    position: 'relative',
    lineHeight: 'auto',
    paddingTop: '0.5rem',
    paddingBottom: '0.5rem',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
    borderBottom: `1px solid ${colors.light}`,
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 'auto'
  },

  previewDataHeader: {
    height: '19px',
    width: 'auto',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '1rem',
    paddingRight: '1.5rem',
    justifyContent: 'space-between',
    display: 'flex'
  },

  warningMessage: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: '10rem'
  },

  selectAllContainer: {
    marginLeft: 'auto',
    width: '5rem',
    display: 'flex',
    alignItems: 'center'
  },
  previewLink: {
    marginTop: '0.5rem',
    height: '1.8rem',
    width: '6.5rem',
    color: colors.secondary
  },
  footer: {
    display: 'block',
    padding: '20px',
    height: '60px',
    width: '100%'
  },
  stickyFooter: {
    backgroundColor: colors.white,
    borderTop: `1px solid ${colors.light}`,
    textAlign: 'right',
    padding: '3px 10px 50px 20px',
    position: 'fixed',
    left: '0',
    bottom: '0',
    height: '60px',
    width: '100%'
  }
});

const stylesFunction = {
  plusIconColor: (disabled) => {
    return {
      fill: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent
    };
  }
};

const ImmutableListItem: React.FunctionComponent <{
  name: string, onChange: Function, checked: boolean}> = ({name, onChange, checked}) => {
    return <div style={styles.listItem}>
      <input type='checkbox' value={name} onChange={() => onChange()}
             style={styles.listItemCheckbox} checked={checked}/>
      <div style={{lineHeight: '1.5rem', color: colors.primary}}>{name}</div>
    </div>;
  };

const Subheader = (props) => {
  return <div style={{...styles.subheader, ...props.style}}>{props.children}</div>;
};

export const ValueListItem: React.FunctionComponent <
  {domainValue: DomainValue, onChange: Function, checked: boolean}> =
  ({domainValue, onChange, checked}) => {
    return <div style={{display: 'flex', height: '1.2rem', marginLeft: '0.55rem'}}>
      <input type='checkbox' value={domainValue.value} onChange={() => onChange()}
             style={styles.valueListItemCheckboxStyling} checked={checked}/>
      <div style={{lineHeight: '1.5rem', wordWrap: 'break-word', color: colors.primary}}>
        {domainValue.value}</div>
    </div>;
  };

const plusLink = (dataTestId: string, path: string, disable?: boolean) => {
  return <TooltipTrigger data-test-id='plus-icon-tooltip' disabled={!disable}
                         content='Requires Owner or Writer permission'>
    <Clickable disabled={disable} data-test-id={dataTestId} href={path}
            onClick={e => {navigateAndPreventDefaultIfNoKeysPressed(e, path); }}>
    <ClrIcon shape='plus-circle' class='is-solid' size={16}
             style={stylesFunction.plusIconColor(disable)}/>
  </Clickable></TooltipTrigger>;
};

const BoxHeader = ({text= '', header =  '', subHeader = '', style= {}, ...props}) => {
  return  <div style={styles.selectBoxHeader}>
    <div style={{display: 'flex', marginLeft: '0.2rem'}}>
      <CircleWithText text={text} width='23.78px' height='23.78px'
                      style={{fill: colorWithWhiteness(colors.primary, 0.5), marginTop: '0.5rem'}}/>
      <label style={{marginLeft: '0.5rem', color: colors.primary, display: 'flex', ...style}}>
        <div style={{fontWeight: 600, marginRight: '0.3rem'}}>{header}</div>
        ({subHeader})
      </label>
    </div>
    {props.children}
  </div>;
};

interface Props {
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  cohortList: Cohort[];
  conceptSetList: ConceptSet[];
  creatingConceptSet: boolean;
  dataSet: DataSet;
  dataSetTouched: boolean;
  includesAllParticipants: boolean;
  prePackagedDemographics: boolean;
  prePackagedSurvey: boolean;
  loadingResources: boolean;
  openSaveModal: boolean;
  previewError: boolean;
  previewErrorText: string;
  previewList: Array<DataSetPreviewList>;
  previewDataLoading: boolean;
  selectedCohortIds: number[];
  selectedConceptSetIds: number[];
  selectedPreviewDomain: string;
  selectedValues: DomainValuePair[];
  valueSets: ValueSet[];
  valuesLoading: boolean;
}

const DataSetPage = fp.flow(withCurrentWorkspace(), withUrlParams())(
  class extends React.Component<Props, State> {
    dt: any;
    constructor(props) {
      super(props);
      this.state = {
        cohortList: [],
        conceptSetList: [],
        creatingConceptSet: false,
        dataSet: undefined,
        dataSetTouched: false,
        includesAllParticipants: false,
        loadingResources: true,
        openSaveModal: false,
        prePackagedDemographics: false,
        prePackagedSurvey: false,
        previewError: false,
        previewErrorText: '',
        previewList: [],
        previewDataLoading: false,
        selectedCohortIds: [],
        selectedConceptSetIds: [],
        selectedPreviewDomain: '',
        selectedValues: [],
        valueSets: [],
        valuesLoading: false,
      };
    }

    get editing() {
      return this.props.urlParams.dataSetId !== undefined;
    }

    async componentDidMount() {
      const {namespace, id} = this.props.workspace;
      const allPromises = [];
      allPromises.push(this.loadResources());
      if (this.editing) {
        allPromises.push(dataSetApi().getDataSet(
          namespace, id, this.props.urlParams.dataSetId).then((response) => {
            this.setState({
              dataSet: response,
              includesAllParticipants: response.includesAllParticipants,
              selectedConceptSetIds: response.conceptSets.map(cs => cs.id),
              selectedCohortIds: response.cohorts.map(c => c.id),
              selectedValues: response.values,
              valuesLoading: true,
            });
            if (response.prePackagedConceptSet === PrePackagedConceptSetEnum.BOTH) {
              this.setState({prePackagedSurvey: true, prePackagedDemographics: true});
            } else if (response.prePackagedConceptSet === PrePackagedConceptSetEnum.DEMOGRAPHICS) {
              this.setState({prePackagedDemographics: true});
            } else if (response.prePackagedConceptSet === PrePackagedConceptSetEnum.SURVEY) {
              this.setState({prePackagedSurvey: true});
            }
            return response;
          }));
        const [, dataSet] = await Promise.all(allPromises);
        // We can only run this command once both the data set fetch and the
        // load resources have concluded. However, we want those to happen in
        // parallel, and one is conditional, so we add them to an array to await
        // and only run once both have finished.
        const domainList = this.getDomainsFromConceptIds(dataSet.conceptSets.map(cs => cs.id));
        if (dataSet.prePackagedConceptSet === PrePackagedConceptSetEnum.BOTH) {
          domainList.push(Domain.PERSON);
          domainList.push(Domain.SURVEY);
        } else if (dataSet.prePackagedConceptSet === PrePackagedConceptSetEnum.SURVEY) {
          domainList.push(Domain.SURVEY);
        } else if (dataSet.prePackagedConceptSet === PrePackagedConceptSetEnum.DEMOGRAPHICS) {
          domainList.push(Domain.PERSON);
        }
        this.getValuesList(fp.uniq(domainList))
          .then(valueSets => this.setState({valueSets: valueSets, valuesLoading: false}));
      }
    }

    async loadResources(): Promise<void> {
      try {
        const {namespace, id} = this.props.workspace;
        const [conceptSets, cohorts] = await Promise.all([
          conceptSetsApi().getConceptSetsInWorkspace(namespace, id),
          cohortsApi().getCohortsInWorkspace(namespace, id)]);
        this.setState({conceptSetList: conceptSets.items, cohortList: cohorts.items,
          loadingResources: false});
        return Promise.resolve();
      } catch (error) {
        console.error(error);
        return Promise.resolve();
      }
    }

    getDomainsFromConceptIds(selectedConceptSetIds: number[]): Domain[] {
      const {conceptSetList} = this.state;
      const domains = fp.uniq(conceptSetList.filter((conceptSet: ConceptSet) =>
        selectedConceptSetIds.includes(conceptSet.id))
        .map((conceptSet: ConceptSet) => conceptSet.domain));
      if (this.state.prePackagedSurvey) {
        domains.push(Domain.SURVEY);
      }
      if (this.state.prePackagedDemographics) {
        domains.push(Domain.PERSON);
      }
      return domains;
    }

    async getValuesList(domains: Domain[], survey?: Surveys): Promise<ValueSet[]> {
      const {namespace, id} = this.props.workspace;
      const valueSets = fp.zipWith((domain: Domain, valueSet: DomainValuesResponse) =>
          ({domain: domain, values: valueSet, survey: survey}),
        domains,
        await Promise.all(domains.map((domain) =>
          conceptsApi().getValuesFromDomain(namespace, id, domain.toString()))));
      return valueSets;
    }

    handlePrePackagedConceptSets(domain, selected) {
      const {valueSets, selectedValues} = this.state;
      if (!selected) {
        const updatedValueSets =
            valueSets.filter(valueSet => !(fp.contains(valueSet.domain, domain)));
        const updatedSelectedValues =
            selectedValues.filter(selectedValue =>
                !fp.contains(selectedValue.domain, domain));
        this.setState({valueSets: updatedValueSets, selectedValues: updatedSelectedValues});
        return;
      }
      const currentDomains = [];
      if (this.state.prePackagedDemographics) {
        currentDomains.push(Domain.PERSON);
      }
      if (this.state.prePackagedSurvey) {
        currentDomains.push(Domain.SURVEY);
      }
      const origDomains = valueSets.map(valueSet => valueSet.domain);
      const newDomains = fp.without(origDomains, currentDomains) as unknown as Domain[];

      this.setState({valuesLoading: true});
      this.getValuesList(newDomains)
        .then(newValueSets => this.setState({
          valueSets: valueSets.concat(newValueSets),
          valuesLoading: false
        }));
    }

    select(resource: ConceptSet | Cohort, rtype: ResourceType): void {
      this.setState({dataSetTouched: true});
      if (rtype === ResourceType.CONCEPT_SET) {
        const {valueSets, selectedValues} = this.state;
        const origSelected = this.state.selectedConceptSetIds;
        const newSelectedConceptSets =
          toggleIncludes(resource.id, origSelected)as unknown as number[];
        const currentDomains = this.getDomainsFromConceptIds(newSelectedConceptSets);
        const origDomains = valueSets.map(valueSet => valueSet.domain);
        const newDomains = fp.without(origDomains, currentDomains) as unknown as Domain[];
        const removedDomains = fp.without(currentDomains, origDomains);
        const updatedValueSets =
          valueSets.filter(valueSet => !(fp.contains(valueSet.domain, removedDomains)));
        const updatedSelectedValues =
          selectedValues.filter(selectedValue =>
            !fp.contains(selectedValue.domain, removedDomains));
        this.setState({
          selectedConceptSetIds: newSelectedConceptSets,
          selectedValues: updatedSelectedValues});
        if (newDomains.length > 0) {
          this.setState({valuesLoading: true});
          const cSet = resource as ConceptSet;
          if (cSet.survey != null ) {
            this.getValuesList(newDomains, cSet.survey)
                .then(newValueSets => this.setState({
                  valueSets: updatedValueSets.concat(newValueSets),
                  valuesLoading: false
                }));
          } else {
            this.getValuesList(newDomains)
                .then(newValueSets => this.setState({
                  valueSets: updatedValueSets.concat(newValueSets),
                  valuesLoading: false
                }));
          }
        } else {
          this.setState({valueSets: updatedValueSets});
        }
      } else {
        this.setState({selectedCohortIds: toggleIncludes(resource.id,
          this.state.selectedCohortIds) as unknown as number[]});
      }
    }

    selectDomainValue(domain: Domain, domainValue: DomainValue): void {
      const valueSets = this.state.valueSets
          .filter(value => value.domain === domain)
          .map(valueSet => valueSet.values.items)[0];
      const origSelected = this.state.selectedValues;
      const selectObj = {domain: domain, value: domainValue.value};
      let valuesSelected = [];
      if (fp.some(selectObj, origSelected)) {
        valuesSelected = fp.remove((dv) => dv.domain === selectObj.domain
            && dv.value === selectObj.value, origSelected);

      } else {
        valuesSelected = (origSelected).concat(selectObj);
      }
      // Sort the values selected as per the order display rather than appending top end
      valuesSelected = valuesSelected.sort((a, b) =>
          valueSets.findIndex(({value}) => a.value === value) -
          valueSets.findIndex(({value}) => b.value === value));
      this.setState({selectedValues: valuesSelected, dataSetTouched: true});
    }

    get selectAll() {
      return fp.isEmpty(this.state.selectedValues);
    }

    selectAllValues() {
      if (!this.selectAll) {
        this.setState({selectedValues: []});
        return;
      } else {
        const allValuesSelected = [];
        this.state.valueSets.map(valueSet => {
          valueSet.values.items.map(value => {
            allValuesSelected.push({domain: valueSet.domain, value: value.value});
          });
        });
        this.setState({selectedValues: allValuesSelected});
      }
    }

    get canWrite() {
      return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
    }

    disableSave() {
      return !this.state.selectedConceptSetIds || (this.state.selectedConceptSetIds.length === 0
          && !this.state.prePackagedDemographics && !this.state.prePackagedSurvey) ||
          ((!this.state.selectedCohortIds || this.state.selectedCohortIds.length === 0) &&
              !this.state.includesAllParticipants) || !this.state.selectedValues ||
          this.state.selectedValues.length === 0;
    }

    getDataTableValue(data) {
      // convert data model from api :
      // [{value[0]: '', queryValue: []}, {value[1]: '', queryValue: []}]
      // to compatible with DataTable
      // {value[0]: queryValue[0], value[1]: queryValue[1]}

      const tableData = fp.flow(
        fp.map(({value, queryValue}) => fp.map(v => [value, v], queryValue)),
        fp.unzip,
        fp.map(fp.fromPairs)
      )(data);
      return tableData;
    }

    getPrePackagedConceptSet() {
      let prePackagedConceptState = PrePackagedConceptSetEnum.NONE;
      if (this.state.prePackagedDemographics && this.state.prePackagedSurvey) {
        prePackagedConceptState = PrePackagedConceptSetEnum.BOTH;
      } else if (this.state.prePackagedSurvey) {
        prePackagedConceptState = PrePackagedConceptSetEnum.SURVEY;
      } else if (this.state.prePackagedDemographics) {
        prePackagedConceptState = PrePackagedConceptSetEnum.DEMOGRAPHICS;
      }
      return prePackagedConceptState;
    }

    async getPreviewList() {
      this.setState({previewList: [], previewDataLoading: true});
      const {namespace, id} = this.props.workspace;
      const request = {
        name: '',
        description: '',
        conceptSetIds: this.state.selectedConceptSetIds,
        includesAllParticipants: this.state.includesAllParticipants,
        cohortIds: this.state.selectedCohortIds,
        prePackagedConceptSet: this.getPrePackagedConceptSet(),
        values: this.state.selectedValues
      };
      try {
        const dataSetPreviewResp = await dataSetApi().previewQuery(namespace, id, request);
        this.setState({
          previewList: dataSetPreviewResp.domainValue,
          selectedPreviewDomain: dataSetPreviewResp.domainValue[0].domain
        });
      } catch (ex) {
        const exceptionResponse = await ex.json() as unknown as ErrorResponse;
        let errorText: string;
        switch (exceptionResponse.statusCode) {
          case 400:
            if (exceptionResponse.message ===
              'Data Sets must include at least one cohort and concept.') {
              errorText = exceptionResponse.message;
            } else if (exceptionResponse.message ===
              'Concept Sets must contain at least one concept') {
              errorText = 'One or more of your concept sets has no concepts. ' +
                'Please check your concept sets to ensure all concept sets have concepts.';
            }
            break;
          case 404:
            if (exceptionResponse.message.startsWith(
              'Not Found: No Cohort definition matching cohortId')) {
              errorText = 'Error with one or more cohorts in the data set. ' +
                'Please submit a bug using the contact support button';
            }
            break;
          case 504:
            if (exceptionResponse.message ===
              'Timeout while querying the CDR to pull preview information.') {
              errorText = 'Query to load data from the All of Us Database timed out. ' +
                'Please either try again or export data set to a notebook to try there';
            }
            break;
          default:
            errorText = 'An unexpected error has occurred. ' +
              'Please submit a bug using the contact support button';
        }
        this.setState({previewError: true, previewErrorText: errorText});
        console.error(ex);
      } finally {
        this.setState({previewDataLoading: false});
      }
    }

    isEllipsisActive(text) {
      if (this.dt) {
        const columnIndex = this.dt.props.children.findIndex(child => child.key === text);
        const columnTitlesDOM = document.getElementsByClassName('p-column-title');
        if (columnTitlesDOM && columnTitlesDOM.item(columnIndex)) {
          const element = columnTitlesDOM.item(columnIndex).children[0] as HTMLElement;
          if (element.offsetWidth < element.scrollWidth) {
            return false;
          }
        }
      }
      return true;
    }

    getHeaderValue(value) {
      const text = value.value;
      const dataTestId = 'data-test-id-' + text;
      return <TooltipTrigger data-test-id={dataTestId} side='top' content={text}
                             disabled={this.isEllipsisActive(text)}>
        <div style={{overflow: 'hidden', textOverflow: 'ellipsis'}}>
          {text}
        </div>
      </TooltipTrigger>;
    }


    renderPreviewDataTable() {
      const filteredPreviewData =
          this.state.previewList.filter(
            preview => fp.contains(preview.domain, this.state.selectedPreviewDomain))[0];
      return <DataTable ref={el => this.dt = el} key={this.state.selectedPreviewDomain}
                        scrollable={true} style={{width: '100%'}}
                        value={this.getDataTableValue(filteredPreviewData.values)}>
        {filteredPreviewData.values.map(value =>
          <Column key={value.value} header={this.getHeaderValue(value)}
                  headerStyle={{textAlign: 'left', width: '5rem'}} style={{width: '5rem'}}
                  field={value.value}/>
        )}
      </DataTable>;
    }

    render() {
      const {namespace, id} = this.props.workspace;
      const pathPrefix = 'workspaces/' + namespace + '/' + id + '/data';
      const cohortsPath = pathPrefix + '/cohorts/build';
      const conceptSetsPath = pathPrefix + '/concepts';
      const {
        dataSet,
        dataSetTouched,
        includesAllParticipants,
        loadingResources,
        openSaveModal,
        prePackagedDemographics,
        prePackagedSurvey,
        previewDataLoading,
        previewError,
        previewErrorText,
        previewList,
        selectedCohortIds,
        selectedConceptSetIds,
        selectedPreviewDomain,
        selectedValues,
        valuesLoading,
        valueSets
      } = this.state;
      return <React.Fragment>
        <FadeBox style={{marginTop: '1rem'}}>
          <h2 style={{paddingTop: 0, marginTop: 0}}>Data Sets{this.editing &&
            dataSet !== undefined && ' - ' + dataSet.name}</h2>
          <div style={{color: colors.primary, fontSize: '14px'}}>Build a data set by selecting the
            variables and values for one or more of your cohorts. Then export the completed Data Set
            to Notebooks where you can perform your analysis</div>
          <div style={{display: 'flex', paddingTop: '1rem'}}>
            <div style={{width: '33%', height: '80%'}}>
              <div style={{backgroundColor: 'white', border: `1px solid ${colors.light}`}}>
                <BoxHeader text='1' header='Select Cohorts' subHeader='Participants'>
                  {plusLink('cohorts-link', cohortsPath, !this.canWrite)}
                </BoxHeader>
                <div style={{height: '9rem', overflowY: 'auto'}}>
                  <Subheader>Prepackaged Cohorts</Subheader>
                  <ImmutableListItem name='All Participants' checked={includesAllParticipants}
                                     onChange={
                                       () => this.setState({
                                         includesAllParticipants: !includesAllParticipants,
                                         dataSetTouched: true
                                       })}/>
                  <Subheader>Workspace Cohorts</Subheader>
                  {!loadingResources && this.state.cohortList.map(cohort =>
                    <ImmutableListItem key={cohort.id} name={cohort.name}
                                      data-test-id='cohort-list-item'
                                      checked={selectedCohortIds.includes(cohort.id)}
                                      onChange={
                                        () => this.select(cohort, ResourceType.COHORT)}/>
                    )
                  }
                  {loadingResources && <Spinner style={{position: 'relative', top: '0.5rem',
                    left: '7rem'}}/>}
                </div>
              </div>
            </div>
            <div style={{marginLeft: '1.5rem', width: '70%'}}>
              <div style={{display: 'flex', backgroundColor: colors.white,
                border: `1px solid ${colors.light}`}}>
                <div style={{width: '60%', borderRight: `1px solid ${colors.light}`}}>
                    <BoxHeader text='2' header='Select Concept Sets' subHeader='Rows'
                               style={{paddingRight: '1rem'}}>
                      {plusLink('concept-sets-link', conceptSetsPath, !this.canWrite)}
                    </BoxHeader>
                  <div style={{height: '9rem', overflowY: 'auto'}}>
                    <Subheader>Prepackaged Concept Sets</Subheader>
                    <ImmutableListItem name='Demographics' checked={prePackagedDemographics}
                                       onChange={
                                         () => {
                                           this.setState({
                                             prePackagedDemographics: !prePackagedDemographics,
                                             dataSetTouched: true
                                           }, () => {this.handlePrePackagedConceptSets(
                                             Domain.PERSON, !prePackagedDemographics);})}}/>
                    <ImmutableListItem name='All Surveys' checked={prePackagedSurvey}
                                       onChange={
                                         () => {
                                           this.setState({
                                             prePackagedSurvey: !prePackagedSurvey,
                                             dataSetTouched: true
                                           }, () => {this.handlePrePackagedConceptSets(
                                             Domain.SURVEY, !prePackagedSurvey);});

                                         }}/>
                    <Subheader>Workspace Concept Set</Subheader>
                    {!loadingResources && this.state.conceptSetList.map(conceptSet =>
                        <ImmutableListItem key={conceptSet.id} name={conceptSet.name}
                                          data-test-id='concept-set-list-item'
                                          checked={selectedConceptSetIds.includes(conceptSet.id)}
                                          onChange={
                                            () => this.select(conceptSet, ResourceType.CONCEPT_SET)
                                          }/>)
                    }
                    {loadingResources && <Spinner style={{position: 'relative', top: '2rem',
                      left: '10rem'}}/>}
                  </div>
                </div>
                <div style={{width: '55%'}}>
                    <BoxHeader text='3' header='Select Values' subHeader='Columns'>
                    <div style={styles.selectAllContainer}>
                      <CheckBox style={{height: 17, width: 17}}
                                disabled={fp.isEmpty(valueSets)}
                                data-test-id='select-all'
                                onChange={() => this.selectAllValues()}
                                checked={!this.selectAll} />
                      <div style={{marginLeft: '0.25rem', fontSize: '13px', lineHeight: '17px'}}>
                        {this.selectAll ? 'Select All' : 'Deselect All'}
                      </div>
                    </div>
                  </BoxHeader>
                  <div style={{height: '9rem', overflowY: 'auto'}}>
                    {valuesLoading && <Spinner style={{position: 'relative',
                      top: '2rem', left: 'calc(50% - 36px)'}}/>}
                    {valueSets.map(valueSet =>
                      <div key={valueSet.domain}>
                        <Subheader>
                          {valueSet.survey ? 'Survey' : fp.capitalize(valueSet.domain.toString())}
                        </Subheader>
                        {valueSet.values.items.map(domainValue =>
                          <ValueListItem data-test-id='value-list-items'
                            key={domainValue.value} domainValue={domainValue}
                            onChange={() => this.selectDomainValue(valueSet.domain, domainValue)}
                            checked={fp.some({domain: valueSet.domain, value: domainValue.value},
                              selectedValues)}/>
                        )}
                      </div>)
                    }
                  </div>
                </div>
              </div>
            </div>
          </div>
        </FadeBox>
        <FadeBox style={{marginTop: '1rem'}}>
          <div style={{backgroundColor: 'white', border: `1px solid ${colors.light}`}}>
            <div style={styles.previewDataHeaderBox}>
              <div style={{display: 'flex', flexDirection: 'column'}}>
              <div style={{display: 'flex', alignItems: 'flex-end'}}>
                <div style={styles.previewDataHeader}>
                  <div>
                    <CircleWithText text={4} width='23.78px' height='23.78px'
                      style={{marignTop: '0.3rem', fill: colorWithWhiteness(colors.primary, 0.5)}}/>
                  </div>
                  <label style={{marginLeft: '0.5rem', color: colors.primary}}>
                    Preview Data Set
                  </label>
                </div>
                <div style={{color: colors.primary, fontSize: '14px', width: '60%'}}>
                  A visualization of your data table based on concept sets
                  and values you selected above. Once complete, export for analysis
                </div>
              </div>
              </div>
              <Clickable data-test-id='preview-button' style={{
                marginTop: '0.5rem',
                cursor: this.disableSave() ? 'not-allowed' : 'pointer', height: '1.8rem',
                width: '6.5rem', color: this.disableSave() ? colorWithWhiteness(colors.dark, 0.6) :
                  colors.accent}} disabled={this.disableSave()}
                onClick={() => this.getPreviewList()}>
                  View Preview Table
              </Clickable>
            </div>
            {previewDataLoading && <div style={styles.warningMessage}>
              <Spinner style={{position: 'relative', top: '2rem'}} />
              <div style={{top: '3rem', position: 'relative'}}>
                It may take up to few minutes to load the data
              </div>
            </div>}
            {previewList.length > 0 &&
              <div style={{display: 'flex', flexDirection: 'column'}}>
                <div style={{display: 'flex', flexDirection: 'row', paddingTop: '0.5rem'}}>
                  {previewList.map(previewRow =>
                     <Clickable key={previewRow.domain}
                          onClick={() =>
                            this.setState({selectedPreviewDomain: previewRow.domain})}
                            style={{
                              marginLeft: '0.2rem', color: colors.accent,
                              marginRight: '0.25rem', paddingBottom: '0.25rem',
                              width: '7rem',
                              borderBottom:
                               (selectedPreviewDomain === previewRow.domain) ?
                                 '4px solid ' + colors.accent : '',
                              fontWeight: (selectedPreviewDomain === previewRow.domain)
                               ? 600 : 400,
                              fontSize: '18px',
                              display: 'flex',
                              justifyContent: 'center',
                              lineHeight: '32px',
                            }}>
                       {previewRow.domain === 'OBSERVATION' ? 'SURVEY' : previewRow.domain}
                     </Clickable>
                  )}
                </div>
                {this.renderPreviewDataTable()}
              </div>
            }
            {previewList.length === 0 && !previewDataLoading &&
              <div style={styles.previewButtonBox}>
                <div style={{color: colorWithWhiteness(colors.dark, 0.6),
                  fontSize: '20px', fontWeight: 400}}>
                  Select cohorts, concept sets, and values above to generate a preview table
                </div>
              </div>
            }
          </div>
        </FadeBox>
        <div>
          <div style={styles.footer} />
          <div style={styles.stickyFooter}>
            <TooltipTrigger data-test-id='save-tooltip'
              content='Requires Owner or Writer permission' disabled={this.canWrite}>
            <Button style={{marginBottom: '2rem'}} data-test-id='save-button'
              onClick ={() => this.setState({openSaveModal: true})}
              disabled={this.disableSave() || !this.canWrite}>
                {this.editing ? !(dataSetTouched && this.canWrite) ? 'Analyze' :
                  'Update and Analyze' : 'Save and Analyze'}
            </Button>
            </TooltipTrigger>
          </div>
        </div>
        {openSaveModal && <NewDataSetModal includesAllParticipants={includesAllParticipants}
                                           selectedConceptSetIds={selectedConceptSetIds}
                                           selectedCohortIds={selectedCohortIds}
                                           selectedValues={selectedValues}
                                           workspaceNamespace={namespace}
                                           workspaceId={id}
                                           prePackagedConceptSet={this.getPrePackagedConceptSet()}
                                           dataSet={dataSet ? dataSet : undefined}
                                           closeFunction={() => {
                                             this.setState({openSaveModal: false});
                                           }}
        />}
        {previewError && <Modal>
          <ModalTitle>Error Loading Data Set Preview</ModalTitle>
          <ModalBody>{previewErrorText}</ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick={() => {this.setState({previewError: false}); }}>
              Close
            </Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>;
    }
  });

export {
  DataSetPage,
  Props as DataSetPageProps
};

@Component({
  template: '<div #root></div>'
})
export class DataSetPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataSetPage, []);
  }
}
