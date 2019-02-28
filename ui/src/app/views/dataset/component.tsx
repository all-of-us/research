import {Component} from '@angular/core';
import * as React from 'react';
import * as fp from 'lodash/fp';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortsApi, conceptsApi, conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {CreateConceptSetModal} from 'app/views/conceptset-create-modal/component';
import {
  Cohort,
  ConceptSet,
  DomainInfo,
  RecentResource,
  WorkspaceAccessLevel,
} from 'generated/fetch';
import {SpinnerOverlay} from "../../components/spinners";
import {ResourceCardMenu} from "../resource-card/component";
import {convertToResource, ResourceType} from "../../utils/resourceActionsReact";
import {ConfirmDeleteModal} from "../confirm-delete-modal/component";
import {EditModal} from "../edit-modal/component";

export const styles = {
  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingLeft: '13px',
    color: '#2F2E7E',
    borderBottom: '1px solid #E5E5E5'
  },

  addIcon: {
    marginLeft: 19,
    fill: '#2691D0',
    verticalAlign: '-6%'
  }
};

const ResourceListItem: React.FunctionComponent <{conceptSet: ConceptSet, openConfirmDelete: Function, edit: Function}> = ({conceptSet, openConfirmDelete, edit}) => {
  return<div style={{border: '0.5px solid #C3C3C3', margin: '.4rem', height: '1.5rem', display: 'flex'}}>
    <div style={{width: '.75rem', paddingTop: 5, paddingLeft: 10}}>
      <ResourceCardMenu disabled={false}
                        resourceType={ResourceType.CONCEPT_SET}
                        onDeleteResource={openConfirmDelete}
                        onEditConceptSet={edit}/>
    </div>
    <input type='checkbox' value={conceptSet.name} style={{height: 17, width: 17, marginLeft: 10, marginTop: 10, marginRight: 10, backgroundColor: '#7CC79B'}}/>
    <div style={{lineHeight: '1.5rem'}}>{conceptSet.name}</div>
  </div>;
};

export const DataSet = withCurrentWorkspace()(class extends React.Component<
  {workspace: WorkspaceData},
  {creatingConceptSet: boolean, conceptDomainList: DomainInfo[],
    conceptSetList: ConceptSet[], loadingConceptSets: boolean,
    confirmDeleting: boolean, editing: boolean, resource: RecentResource,
    rType: ResourceType
  }> {

  constructor(props) {
    super(props);
    this.state = {
      creatingConceptSet: false,
      conceptDomainList: undefined,
      conceptSetList: [],
      loadingConceptSets: true,
      confirmDeleting: false,
      editing: false,
      resource: undefined,
      rType: undefined
    };
  }

  componentDidMount() {
    const {namespace, id} = this.props.workspace;
    this.loadResources();
    conceptsApi().getDomainInfo(namespace, id).then((response) => {
      this.setState({conceptDomainList: response.items});
    });
  }

  async loadResources() {
    try {
      const {namespace, id} = this.props.workspace;
      const conceptSets = await Promise.resolve(conceptSetsApi()
        .getConceptSetsInWorkspace(namespace, id));
      this.setState({conceptSetList: conceptSets.items, loadingConceptSets: false});
    } catch (error) {
      console.log(error);
    }
  }

  convertResource(r: ConceptSet | Cohort, rType: ResourceType): RecentResource {
    const {workspace} = this.props;
    this.setState({rType: rType});
    console.log(rType);
    return convertToResource(r, workspace.namespace, workspace.id,
      workspace.accessLevel as unknown as WorkspaceAccessLevel, rType);
  }

  openConfirmDelete(r: ConceptSet, rType: ResourceType): void {
    const rc = this.convertResource(r, rType);
    this.setState({confirmDeleting: true, resource: rc});
  }

  closeConfirmDelete(): void {
    this.setState({confirmDeleting: false});
    if (this.state.rType === ResourceType.CONCEPT_SET) {
      const deleted = this.state.resource.conceptSet;
      const updatedList = fp.filter(conceptSet => conceptSet.id !== deleted.id,
        this.state.conceptSetList);
      this.setState({conceptSetList: updatedList});
    }
    this.setState({resource: undefined, rType: undefined});
  }

  edit(r: Cohort | ConceptSet, rType: ResourceType): void {
    const rc = this.convertResource(r, rType);
    console.log(rType);
    this.setState({editing: true, resource: rc});
  }

  receiveDelete() {
    const {resource, rType} = this.state;
    let call;
    const id = rType === ResourceType.COHORT ? resource.cohort.id : resource.conceptSet.id;
    const argList = [resource.workspaceNamespace,
      resource.workspaceFirecloudName, id];
    if (rType === ResourceType.CONCEPT_SET) {
      // these also throw errors in the logs
      // functions correctly, and I think is so pretty, but
      // I don't like all the errors in the logs, so torn.
      call = conceptSetsApi().deleteConceptSet(...argList);
    } else {
      call = cohortsApi().deleteCohort(...argList);
    }

    call.then(() => this.closeConfirmDelete());
  }

  receiveEdit(resource: RecentResource): void {
    const {rType} = this.state;
    const updatedResource = rType === ResourceType.COHORT ? resource.cohort : resource.conceptSet;
    const argList = [resource.workspaceNamespace,
    resource.workspaceFirecloudName, updatedResource.id,
    updatedResource];
    let call;
    if (resource.cohort) {
      call = cohortsApi().updateCohort(...argList);
    } else if (resource.conceptSet) {
      this.setState({loadingConceptSets: true});
      call = conceptSetsApi().updateConceptSet(...argList);
    }
    call.then(() => this.closeEditModal());
  }

  closeEditModal(): void {
    const edited = this.state.resource.conceptSet;
    const updatedList = this.state.conceptSetList.map((conceptSet) => {
      return edited.id === conceptSet.id ? edited : conceptSet
    });
    this.setState({editing: false, conceptSetList: updatedList,
      resource: undefined, rType: undefined});
  }

  getCurrentResource(): Cohort | ConceptSet {
    if (this.state.resource) {
      return fp.compact([this.state.resource.cohort, this.state.resource.conceptSet])[0]
    }
  }

  render() {
    const {namespace, id} = this.props.workspace;
    const {
      creatingConceptSet,
      conceptDomainList,
      conceptSetList,
      loadingConceptSets,
      resource,
      rType
    } = this.state;
    const currentResource = this.getCurrentResource();
    return <React.Fragment>
      <FadeBox style={{marginTop: '1rem'}}>
        <h2 style={{marginTop: 0}}>Datasets</h2>
        <div style={{color: '#000000', fontSize: '14px'}}>Build a dataset by selecting the
          variables and values for one or more of your cohorts. Then export the completed dataset
          to Notebooks where you can perform your analysis</div>
        <div style={{display: 'flex'}}>
          <div style={{marginLeft: '1.5rem', marginRight: '1.5rem', width: '33%'}}>
            <h2>Select Cohorts</h2>
            <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
              <div style={styles.selectBoxHeader}>
                Cohorts
                <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}
                  onClick={() => navigate(['workspaces', namespace, id,  'cohorts', 'build'])}/>
              </div>
              {/*TODO: load cohorts and display here*/}
              <div style={{height: '8rem'}}/>
            </div>
          </div>
          <div style={{width: '58%'}}>
            <h2>Select Concept Sets</h2>
            <div style={{display: 'flex', backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
              <div style={{flexGrow: 1, borderRight: '1px solid #E5E5E5'}}>
                <div style={{...styles.selectBoxHeader}}>
                  Concept Sets
                  <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}
                           onClick={() => this.setState({creatingConceptSet: true})}/>
                </div>
                {this.state.conceptSetList.length > 0 && !loadingConceptSets &&
                  this.state.conceptSetList.map(conceptSet =>
                    <ResourceListItem key={conceptSet.id} conceptSet={conceptSet}
                                      openConfirmDelete={
                                        () => this.openConfirmDelete(conceptSet, ResourceType.CONCEPT_SET)
                                      }
                                      edit={() => this.edit(conceptSet, ResourceType.CONCEPT_SET)}/>)
                }
                {loadingConceptSets && <SpinnerOverlay/>}
              </div>
              <div style={{flexGrow: 1}}>
                <div style={styles.selectBoxHeader}>
                  Values
                </div>
                {/*TODO: load values and display here*/}
                <div style={{height: '8rem'}}/>
              </div>
            </div>
          </div>
        </div>
      </FadeBox>
      <FadeBox style={{marginTop: '1rem'}}>
        <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
          <div style={{...styles.selectBoxHeader, display: 'flex', position: 'relative'}}>
            <div>Preview Dataset</div>
            <div style={{marginLeft: '1rem', color: '#000000', fontSize: '14px'}}>A visualization
              of your data table based on the variable and value you selected above</div>
            {/* Button disabled until this functionality added*/}
            <Button style={{position: 'absolute', right: '1rem', top: '.25rem'}} disabled={true}>
              SAVE DATASET
            </Button>
          </div>
          {/*TODO: Display dataset preview*/}
          <div style={{height: '8rem'}}/>
        </div>
      </FadeBox>
      {creatingConceptSet &&
      <CreateConceptSetModal onCreate={() => {
        this.loadResources().then(() => {
          this.setState({creatingConceptSet: false});
        });
      }}
      onClose={() => {this.setState({ creatingConceptSet: false}); }}
      conceptDomainList={conceptDomainList}
      existingConceptSets={conceptSetList}/>}
      {console.log(resource)}
      {this.state.confirmDeleting &&
      // This throws a type error in the logs, figure out how to fix that
      //  Functions as it's supposed to
      <ConfirmDeleteModal resourceName={currentResource.name}
                          resourceType={rType}
                          receiveDelete={() => this.receiveDelete()}
                          closeFunction={() => this.closeConfirmDelete()}/>}
      {this.state.editing && resource &&
      <EditModal resource={resource}
                 onEdit={e => this.receiveEdit(e)}
                 onCancel={() => this.closeEditModal()}/>}
    </React.Fragment>;
  }

});

@Component({
  template: '<div #root></div>'
})
export class DataSetComponent extends ReactWrapperBase {
  constructor() {
    super(DataSet, []);
  }
}
