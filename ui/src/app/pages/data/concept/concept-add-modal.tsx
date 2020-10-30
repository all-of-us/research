import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {RadioButton, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, summarizeErrors, withCurrentWorkspace} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  ConceptSet,
  CreateConceptSetRequest,
  Criteria,
  Domain,
  DomainCount,
  UpdateConceptSetRequest
} from 'generated/fetch';
import {validate} from 'validate.js';

const styles = reactStyles({
  label: {
    color: colors.primary,
    paddingLeft: '0.5rem',
    lineHeight: '19px',
    fontSize: '14px',
    fontWeight: 400
  }
});

const filterConcepts = (concepts: any[], domain: Domain) => {
  if (domain === Domain.SURVEY) {
    if (serverConfigStore.getValue().enableConceptSetSearchV2) {
      return concepts.filter(concept => concept.subtype === 'QUESTION');
    } else {
      return concepts.filter(concept => !!concept.question);
    }
  } else {
    return concepts.filter(concept => concept.domainId.replace(' ', '').toLowerCase() === Domain[domain].toLowerCase());
  }
};

const getConceptIdsToAddOrRemove = (conceptsToFilter: Array<Criteria>, conceptsToCompare: Array<Criteria>) => {
  return conceptsToFilter.reduce((conceptIds, concept) => {
    if (!conceptsToCompare.find(con => con.conceptId === concept.conceptId)) {
      conceptIds.push(concept.conceptId);
    }
    return conceptIds;
  }, []);
};

export const ConceptAddModal = withCurrentWorkspace()
(class extends React.Component<{
  workspace: WorkspaceData,
  activeDomainTab: DomainCount,
  selectedConcepts: Criteria[],
  onSave: Function,
  onClose: Function,
}, {
  conceptSets: ConceptSet[],
  errorMessage: string,
  errorSaving: boolean,
  addingToExistingSet: boolean,
  loading: boolean,
  nameTouched: boolean,
  newSetDescription: string,
  name: string,
  saving: boolean,
  selectedSet: ConceptSet,
  selectedConceptsInDomain: Criteria[]
}> {

  constructor(props) {
    super(props);
    this.state = {
      conceptSets: [],
      errorMessage: null,
      errorSaving: false,
      addingToExistingSet: true,
      loading: true,
      nameTouched: false,
      newSetDescription: '',
      name: '',
      saving: false,
      selectedSet: null,
      selectedConceptsInDomain: filterConcepts(props.selectedConcepts, props.activeDomainTab.domain)
    };
  }

  componentDidMount() {
    this.getExistingConceptSets();
  }

  async getExistingConceptSets() {
    try {
      const {workspace: {namespace, id}} = this.props;
      const conceptSets = await conceptSetsApi().getConceptSetsInWorkspace(namespace, id);
      const conceptSetsInDomain = conceptSets.items
          .filter((conceptset) => conceptset.domain === this.props.activeDomainTab.domain);

      this.setState({
        conceptSets: conceptSetsInDomain,
        addingToExistingSet: (conceptSetsInDomain.length > 0),
        selectedConceptsInDomain: filterConcepts(this.props.selectedConcepts, this.props.activeDomainTab.domain),
        loading: false,
      });
      if (conceptSetsInDomain) {
        this.setState({selectedSet: conceptSetsInDomain[0]});
      }
    } catch (error) {
      console.error(error);
    }
  }

  async saveConcepts() {
    const {workspace: {namespace, id}} = this.props;
    const {onSave, activeDomainTab} = this.props;
    const {selectedSet, addingToExistingSet, newSetDescription, name, selectedConceptsInDomain} = this.state;
    this.setState({saving: true});
    const conceptIds = fp.map(selected => selected.conceptId, selectedConceptsInDomain);

    // This is added temporary until users can create concept sets of Domain PERSON,
    // in the meantime there will be default Demogrpahics Concept Set on DATASET PAGE

    if (name === 'Demographics') {
      this.setState({
        errorMessage: 'Name Demographics cannot be used for creating a concept set',
        saving: false});
      return;
    }
    if (addingToExistingSet) {
      // Selections that don't exist on the existing concept set are added
      const addedIds = getConceptIdsToAddOrRemove(selectedConceptsInDomain, selectedSet.criteriums);
      // Concept ids on the existing concept set that don't exist on the selections get removed
      const removedIds = getConceptIdsToAddOrRemove(selectedSet.criteriums, selectedConceptsInDomain);
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: selectedSet.etag,
        addedIds,
        removedIds
      };
      try {
        const conceptSet = await conceptSetsApi().updateConceptSetConcepts(namespace, id, selectedSet.id, updateConceptSetReq);
        this.setState({saving: false});
        onSave(conceptSet);
      } catch (error) {
        console.error(error);
      }
    } else {
      const conceptSet: ConceptSet = {
        name: name,
        description: newSetDescription,
        domain: activeDomainTab.domain
      };
      const request: CreateConceptSetRequest = {
        conceptSet: conceptSet,
        addedIds: conceptIds
      };
      try {
        const createdConceptSet = await conceptSetsApi().createConceptSet(namespace, id, request);
        this.setState({saving: false});
        onSave(createdConceptSet);
      } catch (error) {
        console.error(error);
        this.setState({errorSaving: true});
      }
    }
  }


  render() {
    const {activeDomainTab, onClose} = this.props;
    const {conceptSets, loading, nameTouched, saving, addingToExistingSet,
      newSetDescription, name, errorMessage, errorSaving, selectedConceptsInDomain} = this.state;
    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: conceptSets.map((concept: ConceptSet) => concept.name),
          message: 'already exists'
        }
      }
    });

    return <Modal>
      <ModalTitle data-test-id='add-concept-title'>
        Add {selectedConceptsInDomain.length} Concepts to
        {' '}{activeDomainTab.name} Concept Set</ModalTitle>
      {loading ?
          <div style={{display: 'flex', justifyContent: 'center'}}>
            <Spinner style={{alignContent: 'center'}}/>
          </div> :
      <ModalBody>
        <ModalBody>
          <FlexRow>
            <TooltipTrigger content={
              <div>No concept sets in domain '{activeDomainTab.name}'</div>}
                            disabled={conceptSets.length > 0}>
              <div>
                <RadioButton value={addingToExistingSet}
                            checked={addingToExistingSet}
                            disabled={conceptSets.length === 0}
                            data-test-id='toggle-existing-set'
                            onChange={() => {this.setState({
                              addingToExistingSet: true,
                              nameTouched: false}); }}/>
                <label style={styles.label}>Choose existing set</label>
              </div>
            </TooltipTrigger>
            <div>
              <RadioButton value={!addingToExistingSet}
                         checked={!addingToExistingSet}
                         style={{marginLeft: '0.7rem'}}
                         data-test-id='toggle-new-set'
                         onChange={() => {this.setState({addingToExistingSet: false}); }}/>
              <label style={styles.label}>Create new set</label>
            </div>
          </FlexRow>
        </ModalBody>
        {addingToExistingSet ? (
            <ModalBody data-test-id='add-to-existing'>
              <select style={{marginTop: '1rem', height: '1.5rem', width: '100%'}}
                      onChange={(e) => this.setState({selectedSet: conceptSets[e.target.value]})}>
                {conceptSets.map((set: ConceptSet, i) =>
                    <option data-test-id='existing-set' key={i} value={i}>
                      {set.name}
                    </option>)}
              </select>
            </ModalBody>
            ) :
          (<ModalBody data-test-id='create-new-set'>
            <TextInput placeholder='Name' value={name}
                       data-test-id='create-new-set-name'
                       onChange={(v) => {
                         this.setState({name: v, nameTouched: true});
                       }}/>
            <ValidationError>
              {summarizeErrors(nameTouched && errors && errors.name)}
            </ValidationError>
            <textarea style={{marginTop: '1rem'}} placeholder='Add a Description'
                      value={newSetDescription}
                      onChange={(v) => {
                        this.setState({newSetDescription: v.target.value});
                      }}/>
          </ModalBody>)}
        {errorSaving &&
          <AlertDanger>Error saving concepts to set; please try again!</AlertDanger>}
        {errorMessage && <AlertDanger>{errorMessage}</AlertDanger>}
        <ModalFooter>
          <Button type='secondary' onClick={onClose}>Cancel</Button>
          <Button style={{marginLeft: '0.5rem'}}
                  disabled={(!addingToExistingSet && !!errors) || saving}
                  data-test-id='save-concept-set'
                  onClick={() => this.saveConcepts()}>Save</Button>
        </ModalFooter>
      </ModalBody>}
      {saving && <SpinnerOverlay/>}
    </Modal>;
  }

});

