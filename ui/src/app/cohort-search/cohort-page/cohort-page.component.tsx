import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {CohortSearch} from 'app/cohort-search/cohort-search/cohort-search.component';
import {CBModal} from 'app/cohort-search/modal/modal.component';
import {ListOverview} from 'app/cohort-search/overview/overview.component';
import {SearchGroupList} from 'app/cohort-search/search-group-list/search-group-list.component';
import {idsInUse, searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest, parseCohortDefinition} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  withCurrentCohortSearchContext,
  withCurrentWorkspace
} from 'app/utils';
import {
  currentCohortSearchContextStore,
  currentCohortStore,
  queryParamsStore,
  serverConfigStore
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, SearchRequest} from 'generated/fetch';

const styles = reactStyles({
  cohortError: {
    background: colors.warning,
    color: colors.white,
    padding: '0.25rem 0.5rem',
    borderRadius: '5px',
    marginBottom: '0.5rem'
  }
});

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
  cohortContext: any;
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
  cohortError: boolean;
  minHeight: string;
  modalPromise: Promise<boolean> | null;
  modalOpen: boolean;
  updateGroupListsCount: number;
  cohortChanged: boolean;
  searchContext: any;
}

export const CohortPage = fp.flow(withCurrentWorkspace(), withCurrentCohortSearchContext()) (
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
        cohortError: false,
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
        /* If a cohort id is given in the route, we initialize state with it */
        const {cohortId} = params;
        if (cohortId) {
          this.setState({loading: true});
          cohortsApi().getCohort(namespace, id, cohortId)
          .then(cohort => {
            this.setState({cohort, loading: false});
            currentCohortStore.next(cohort);
            if (cohort.criteria) {
              searchRequestStore.next(parseCohortDefinition(cohort.criteria));
            }
          })
          .catch(error => {
            console.error(error);
            this.setState({cohortError: true, loading: false});
          });
        } else {
          this.setState({cohort: {criteria: `{'includes':[],'excludes':[],'dataFilters':[]}`, name: '', type: ''}});
        }
      });

      this.subscription.add(searchRequestStore.subscribe(searchRequest => {
        const cohortChanged = !!this.state.cohort && this.state.cohort.criteria !== JSON.stringify(mapRequest(searchRequest));
        this.props.setCohortChanged(cohortChanged);
        this.setState({
          criteria: searchRequest,
          overview: searchRequest.includes.length > 0 || searchRequest.excludes.length > 0,
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

    setSearchContext(context) {
      currentCohortSearchContextStore.next(context);
      this.setState({searchContext: context});
    }

    render() {
      const {cohortContext} = this.props;
      const {cohort, cohortChanged, cohortError, criteria, loading, modalOpen, overview, searchContext, updateCount, updateGroupListsCount}
        = this.state;
      return <React.Fragment>
        <div ref={el => this.searchWrapper = el} style={{padding: '0 0.5rem'}}>
          {cohortError
            ? <div style={styles.cohortError}>
              <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
              Sorry, the cohort could not be loaded. Please try again or contact Support in the left hand navigation.
            </div>
            : serverConfigStore.getValue().enableCohortBuilderV2
              /* Cohort Builder V2 UI - behind enableCohortBuilderV2 feature flag */
              ? <React.Fragment>
                <FlexRowWrap style={{margin: '1rem 0 2rem', ...(!!cohortContext ? {display: 'none'} : {})}}>
                  <div style={colStyle('66.66667')}>
                    <FlexRowWrap style={{margin: '0 -0.5rem'}}>
                      {!!cohort && <div style={{height: '1.5rem', padding: '0 0.5rem', width: '100%'}}>
                        <h3 style={{marginTop: 0}}>{cohort.name}</h3>
                      </div>}
                      <div id='list-include-groups-v2' style={colStyle('50')}>
                        <SearchGroupList groups={criteria.includes}
                                         setSearchContext={(c) => this.setSearchContext(c)}
                                         role='includes'
                                         updated={updateGroupListsCount}
                                         updateRequest={() => this.updateRequest()}/>
                      </div>
                      <div id='list-exclude-groups-v2' style={colStyle('50')}>
                        {overview && <SearchGroupList groups={criteria.excludes}
                                                      setSearchContext={(c) => this.setSearchContext(c)}
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
                {!!cohortContext && <CohortSearch/>}
              </React.Fragment>
              /* Current Cohort Builder UI - not behind enableCohortBuilderV2 feature flag */
              : <FlexRowWrap style={{margin: '0 -0.5rem'}}>
                <div style={colStyle('66.66667')}>
                  <FlexRowWrap style={{margin: '0 -0.5rem'}}>
                    {!!cohort && <div style={{height: '1.5rem', padding: '0 0.5rem', width: '100%'}}>
                      <h3 style={{marginTop: 0}}>{cohort.name}</h3>
                    </div>}
                    <div id='list-include-groups' style={colStyle('50')}>
                      <SearchGroupList groups={criteria.includes}
                                       setSearchContext={(c) => this.setSearchContext(c)}
                                       role='includes'
                                       updated={updateGroupListsCount}
                                       updateRequest={() => this.updateRequest()}/>
                    </div>
                    <div id='list-exclude-groups' style={colStyle('50')}>
                      {overview && <SearchGroupList groups={criteria.excludes}
                                                    setSearchContext={(c) => this.setSearchContext(c)}
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
                {searchContext && <CBModal
                    closeSearch={() => this.setSearchContext(undefined)}
                    searchContext={searchContext}
                    setSearchContext={(c) => this.setSearchContext(c)}/>}
              </FlexRowWrap>
          }
        </div>
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
  selector: 'app-cohort-page',
  template: '<div #root></div>'
})
export class CohortPageComponent extends ReactWrapperBase {
  // The functions and variables here are a temporary workaround to keep the unsaved changes warning until we can move this route to the
  // new React router (RW-5256)
  cohortChanged: boolean;
  updatingCohort: boolean;
  showWarningModal: () => Promise<boolean>;
  constructor() {
    super(CohortPage, ['setCohortChanged', 'setUpdatingCohort', 'setShowWarningModal']);
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
