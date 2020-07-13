import {Component} from '@angular/core';
import * as React from 'react';

import {CBModal} from 'app/cohort-search/modal/modal.component';
import {ListOverview} from 'app/cohort-search/overview/overview.component';
import {SearchGroupList} from 'app/cohort-search/search-group-list/search-group-list.component';
import {idsInUse, searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest, parseCohortDefinition} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, queryParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, SearchRequest} from 'generated/fetch';

function colStyle(percentage: string) {
  return {
    flex: `0 0 ${percentage}%`,
    maxWidth: `${percentage}%`,
    minHeight: '1px',
    padding: '0 0.5rem',
    position: 'relative',
    width: '100%'
  } as React.CSSProperties;
}

interface Props {
  setCohortChanged: (cohortChanged: boolean) => void;
  setUpdatingCohort: (updatingCohort: boolean) => void;
  setShowWarningModal: (showWarningModal: () => Promise<boolean>) => void;
  workspace: WorkspaceData;
}

interface State {
  loading: boolean;
  overview: boolean;
  criteria: SearchRequest;
  updateCount: number;
  cohort: Cohort;
  minHeight: string;
  modalPromise: Promise<boolean> | null;
  modalOpen: boolean;
  updateGroupListsCount: number;
  cohortChanged: boolean;
  searchContext: any;
}

export const CohortSearch = withCurrentWorkspace() (
  class extends React.Component<Props, State> {
    private subscription;
    resolve: Function;
    searchWrapper: HTMLDivElement;

    constructor(props: any) {
      super(props);
      this.state = {
        loading: false,
        overview: false,
        criteria: {includes: [], excludes: [], dataFilters: []},
        updateCount: 0,
        cohort: undefined,
        minHeight: '10rem',
        modalPromise:  null,
        modalOpen: false,
        updateGroupListsCount: 0,
        cohortChanged: false,
        searchContext: undefined
      };
      this.showWarningModal = this.showWarningModal.bind(this);
    }

    componentDidMount() {
      const {workspace: {id, namespace}} = this.props;
      this.subscription = queryParamsStore.subscribe(params => {
        /* If a cohort id is given in the route, we initialize state with
         * it */
        const cohortId = params.cohortId;
        if (cohortId) {
          this.setState({loading: true});
          cohortsApi().getCohort(namespace, id, cohortId).then(cohort => {
            this.setState({cohort, loading: false});
            currentCohortStore.next(cohort);
            if (cohort.criteria) {
              searchRequestStore.next(parseCohortDefinition(cohort.criteria));
            }
          });
        } else {
          this.setState({cohort: {criteria: `{'includes':[],'excludes':[],'dataFilters':[]}`, name: '', type: ''}});
        }
      });

      this.subscription.add(searchRequestStore.subscribe(sr => {
        const cohortChanged = !!this.state.cohort && this.state.cohort.criteria !== JSON.stringify(mapRequest(sr));
        this.props.setCohortChanged(cohortChanged);
        this.setState({
          criteria: sr,
          overview: sr.includes.length > 0 || sr.excludes.length > 0,
          cohortChanged,
          updateGroupListsCount: this.state.updateGroupListsCount + 1
        });
      }));
      this.updateWrapperDimensions();
      this.props.setShowWarningModal(this.showWarningModal);
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
      idsInUse.next(new Set());
      currentCohortStore.next(undefined);
      searchRequestStore.next({includes: [], excludes: [], dataFilters: []} as SearchRequest);
    }

    async showWarningModal() {
      this.setState({modalOpen: true});
      return await new Promise<boolean>((resolve => this.resolve = resolve));
    }

    getModalResponse(res: boolean) {
      this.setState({modalOpen: false});
      this.resolve(res);
    }

    updateWrapperDimensions() {
      const {top} = this.searchWrapper.getBoundingClientRect();
      this.searchWrapper.style.minHeight = `${window.innerHeight - top - 24}px`;
    }

    updateRequest = () => {
      // timeout prevents Angular 'Expression changed after checked' error
      setTimeout(() => this.setState({updateCount: this.state.updateCount + 1}));
    }

    render() {
      const {cohort, cohortChanged, criteria, loading, modalOpen, overview, searchContext, updateCount, updateGroupListsCount} = this.state;
      return <React.Fragment>
        <div ref={el => this.searchWrapper = el} style={{padding: '1rem 1rem 2rem'}}>
          <FlexRowWrap style={{margin: '0 -0.5rem'}}>
            <div style={colStyle('66.66667')}>
              <FlexRowWrap style={{margin: '0 -0.5rem'}}>
                <div style={{height: '1.5rem', padding: '0 0.5rem', width: '100%'}}>
                  {!!cohort && <h3 style={{marginTop: 0}}>{cohort.name}</h3>}
                </div>
                <div id='list-include-groups' style={colStyle('50')}>
                  <SearchGroupList groups={criteria.includes}
                                 setSearchContext={(c) => this.setState({searchContext: c})}
                                 role='includes'
                                 updated={updateGroupListsCount}
                                 updateRequest={() => this.updateRequest()}/>
                </div>
                <div id='list-exclude-groups' style={colStyle('50')}>
                  {overview && <SearchGroupList groups={criteria.excludes}
                                   setSearchContext={(c) => this.setState({searchContext: c})}
                                   role='excludes'
                                   updated={updateGroupListsCount}
                                   updateRequest={() => this.updateRequest()}/>}
                </div>
              </FlexRowWrap>
            </div>
            <div style={colStyle('33.33333')}>
              {overview && <ListOverview
                cohort={cohort}
                cohortChanged={cohortChanged}
                searchRequest={criteria}
                updateCount={updateCount}
                updating={() => this.props.setUpdatingCohort(true)}/>}
            </div>
            {loading && <SpinnerOverlay/>}
          </FlexRowWrap>
        </div>
        {searchContext && <CBModal
          closeSearch={() => this.setState({searchContext: undefined})}
          searchContext={searchContext}
          setSearchContext={(c) => this.setState({searchContext: c})}/>}
        {modalOpen && <Modal>
          <ModalTitle>Warning! </ModalTitle>
          <ModalBody>
            Your cohort has not been saved. If you’d like to save your cohort criteria, please click CANCEL
            and {cohort && cohort.id ? 'use Save or Save As' : 'click CREATE COHORT'} to save your criteria.
          </ModalBody>
          <ModalFooter>
            <Button type='link' onClick={() => this.getModalResponse(false)}>Cancel</Button>
            <Button type='primary' onClick={() => this.getModalResponse(true)}>Discard Changes</Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>;
    }
  }
);

@Component({
  selector: 'app-cohort-search',
  template: '<div #root></div>'
})
export class CohortSearchComponent extends ReactWrapperBase {
  // The functions and variables here are a temporary workaround to keep the unsaved changes warning until we can move this route to the
  // new React router (RW-5256)
  cohortChanged: boolean;
  updatingCohort: boolean;
  showWarningModal: () => Promise<boolean>;
  constructor() {
    super(CohortSearch, ['setCohortChanged', 'setUpdatingCohort', 'setShowWarningModal']);
    this.setCohortChanged = this.setCohortChanged.bind(this);
    this.setUpdatingCohort = this.setUpdatingCohort.bind(this);
    this.setShowWarningModal = this.setShowWarningModal.bind(this);
  }

  setCohortChanged(cohortChanged: boolean): void {
    this.cohortChanged = cohortChanged;
  }

  setUpdatingCohort(updatingCohort: boolean): void {
    this.updatingCohort = updatingCohort;
  }

  setShowWarningModal(showWarningModal: () => Promise<boolean>): void {
    this.showWarningModal = showWarningModal;
  }

  canDeactivate(): Promise<boolean> | boolean {
    return !this.cohortChanged || this.updatingCohort || this.showWarningModal();
  }
}
