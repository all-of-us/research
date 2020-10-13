import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertDanger} from 'app/components/alert';
import {Button, Clickable, SlidingFabReact} from 'app/components/buttons';
import {DomainCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, TextInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {ConceptAddModal} from 'app/pages/data/concept/concept-add-modal';
import {ConceptSurveyAddModal} from 'app/pages/data/concept/concept-survey-add-modal';
import {ConceptTable} from 'app/pages/data/concept/concept-table';
import {CriteriaSearch} from 'app/pages/data/criteria-search';
import {cohortBuilderApi, conceptsApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  validateInputForMySQL,
  withCurrentConcept,
  withCurrentWorkspace
} from 'app/utils';
import {
  currentConceptStore,
  NavStore,
  queryParamsStore,
  serverConfigStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {environment} from 'environments/environment';
import {Concept, ConceptSet, Domain, DomainCount, DomainInfo, StandardConceptFilter, SurveyModule, SurveyQuestions} from 'generated/fetch';
import {Key} from 'ts-key-enum';
import {SurveyDetails} from './survey-details';

const styles = reactStyles({
  arrowIcon: {
    height: '21px',
    marginTop: '-0.2rem',
    width: '18px'
  },
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    display: 'inline-block',
    height: '1.5rem',
    lineHeight: '1.6rem',
    textAlign: 'center',
    width: '1.5rem'
  },
  searchBar: {
    boxShadow: '0 4px 12px 0 rgba(0,0,0,0.15)', height: '3rem', width: '64.3%', lineHeight: '19px', paddingLeft: '2rem',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.85), fontSize: '16px'
  },
  domainBoxHeader: {
    color: colors.accent, fontSize: '18px', lineHeight: '22px'
  },
  domainBoxLink: {
    color: colors.accent, lineHeight: '18px', fontWeight: 400, letterSpacing: '0.05rem'
  },
  conceptText: {
    marginTop: '0.3rem', fontSize: '14px', fontWeight: 400, color: colors.primary,
    display: 'flex', flexDirection: 'column', marginBottom: '0.3rem'
  },
  domainHeaderLink: {
    justifyContent: 'center', padding: '0.1rem 1rem', color: colors.accent,
    lineHeight: '18px'
  },
  domainHeaderSelected: {
    height: '4px', width: '100%', backgroundColor: colors.accent, border: 'none'
  },
  conceptCounts: {
    backgroundColor: colors.white, height: '2rem', border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`, borderBottom: 0,
    borderTopLeftRadius: '3px', borderTopRightRadius: '3px', marginTop: '-1px', paddingLeft: '0.5rem', display: 'flex',
    justifyContent: 'flex-start', lineHeight: '15px', fontWeight: 600, fontSize: '14px',
    color: colors.primary, alignItems: 'center'
  },
  selectedConceptsCount: {
    backgroundColor: colors.accent, color: colors.white, borderRadius: '5px',
    padding: '0 5px', fontSize: 12
  },
  clearSearchIcon: {
    fill: colors.accent, transform: 'translate(-1.5rem)', height: '1rem', width: '1rem'
  },
  sectionHeader: {
    height: 24,
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px',
    marginBottom: '1rem',
    marginTop: '2.5rem'
  },
  cardList: {
    display: 'flex',
    flexDirection: 'row',
    width: '94.3%',
    flexWrap: 'wrap'
  },
  backBtn: {
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer'
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.2rem',
    width: '64.3%',
  }
});

interface ConceptCacheItem {
  domain: Domain;
  items: Array<Concept | SurveyQuestions>;
}

const DomainCard: React.FunctionComponent<{conceptDomainInfo: DomainInfo,
  standardConceptsOnly: boolean, browseInDomain: Function, updating: boolean}> =
    ({conceptDomainInfo, standardConceptsOnly, browseInDomain, updating}) => {
      const conceptCount = standardConceptsOnly ?
          conceptDomainInfo.standardConceptCount.toLocaleString() : conceptDomainInfo.allConceptCount.toLocaleString();
      return <DomainCardBase style={{width: 'calc(25% - 1rem)'}} data-test-id='domain-box'>
        <Clickable style={styles.domainBoxHeader}
             onClick={browseInDomain}
             data-test-id='domain-box-name'>{conceptDomainInfo.name}</Clickable>
        <div style={styles.conceptText}>
          {updating ? <Spinner size={42}/> : <React.Fragment>
            <span style={{fontSize: 30}}>{conceptCount.toLocaleString()}</span> concepts in this domain. <p/>
          </React.Fragment>}
          <div><b>{conceptDomainInfo.participantCount.toLocaleString()}</b> participants in domain.</div>
        </div>
        <Clickable style={styles.domainBoxLink}
                   onClick={browseInDomain}>Select Concepts</Clickable>
      </DomainCardBase>;
    };

const SurveyCard: React.FunctionComponent<{survey: SurveyModule, browseSurvey: Function, updating: boolean}> =
    ({survey, browseSurvey, updating}) => {
      return <DomainCardBase style={{maxHeight: 'auto', width: 'calc(25% - 1rem)'}}>
        <Clickable style={styles.domainBoxHeader}
          onClick={browseSurvey}
          data-test-id='survey-box-name'>{survey.name}</Clickable>
        <div style={styles.conceptText}>
          {updating ? <Spinner size={42}/> : <React.Fragment>
            <span style={{fontSize: 30}}>{survey.questionCount.toLocaleString()}</span> survey questions with
          </React.Fragment>}
          <div><b>{survey.participantCount.toLocaleString()}</b> participants</div>
        </div>
        <div style={{...styles.conceptText, height: '3.5rem'}}>
          {survey.description}
        </div>
        <Clickable style={{...styles.domainBoxLink}} onClick={browseSurvey}>Select Concepts</Clickable>
      </DomainCardBase>;
    };

const PhysicalMeasurementsCard: React.FunctionComponent<{physicalMeasurement: DomainInfo, browsePhysicalMeasurements: Function}> =
    ({physicalMeasurement, browsePhysicalMeasurements}) => {
      return <DomainCardBase style={{maxHeight: 'auto', width: '11.5rem'}}>
        <Clickable style={styles.domainBoxHeader}
          onClick={browsePhysicalMeasurements}
          data-test-id='pm-box-name'>{physicalMeasurement.name}</Clickable>
        <div style={styles.conceptText}>
          <span style={{fontSize: 30}}>{physicalMeasurement.allConceptCount.toLocaleString()}</span> physical measurements.
          <div><b>{physicalMeasurement.participantCount.toLocaleString()}</b> participants in this domain</div>
        </div>
        <div style={{...styles.conceptText, height: 'auto'}}>
          {physicalMeasurement.description}
        </div>
        <Clickable style={styles.domainBoxLink} onClick={browsePhysicalMeasurements}>Select Concepts</Clickable>
      </DomainCardBase>;
    };

interface Props {
  workspace: WorkspaceData;
  concept?: Array<Concept>;
}

interface State {
  // Domain tab being viewed when all tabs are visible
  activeDomainTab: DomainCount;
  // Browse survey
  browsingSurvey: boolean;
  // Array of domains that have finished being searched for concepts with search string
  completedDomainSearches: Array<Domain>;
  // Array of concepts found in the search
  concepts: Array<any>;
  // If modal to add concepts to set is open
  conceptAddModalOpen: boolean;
  // Cache for storing selected concepts, their domain, and vocabulary
  conceptsCache: Array<ConceptCacheItem>;
  // Array of domains and the number of concepts found in the search for each
  conceptDomainCounts: Array<DomainCount>;
  // Array of domains and their metadata
  conceptDomainList: Array<DomainInfo>;
  conceptsSavedText: string;
  // Array of surveys
  conceptSurveysList: Array<SurveyModule>;
  // True if the domainCounts call fails
  countsError: boolean;
  // Current string in search box
  currentInputString: string;
  // Last string that was searched
  currentSearchString: string;
  // List of domains where the search api call failed
  domainErrors: Domain[];
  // True if the getDomainInfo call fails
  domainInfoError: boolean;
  // List of domains loading updated counts for domain cards
  domainsLoading: Array<Domain>;
  // List of error messages to display if the search input is invalid
  inputErrors: Array<string>;
  // If concept metadata is still being gathered for any domain
  loadingDomains: boolean;
  // If we are still searching concepts and should show a spinner on the table
  countsLoading: boolean;
  // If we are in 'search mode' and should show the table
  searching: boolean;
  // Map of domain to selected concepts in domain
  selectedConceptDomainMap: Map<String, any[]>;
  // Domain being viewed. Will be the domain that the add button uses.
  selectedDomain: Domain;
  // Name of the survey selected
  selectedSurvey: string;
  // Array of survey questions selected to be added to concept set
  selectedSurveyQuestions: Array<SurveyQuestions>;
  // Show if a search error occurred
  showSearchError: boolean;
  // Only search on standard concepts
  standardConceptsOnly: boolean;
  // Open modal to add survey questions to concept set
  surveyAddModalOpen: boolean;
  // True if the getSurveyInfo call fails
  surveyInfoError: boolean;
  // List of surveys loading updated counts for survey cards
  surveysLoading: Array<string>;
  workspacePermissions: WorkspacePermissions;
}

export const ConceptHomepage = fp.flow(withCurrentWorkspace(), withCurrentConcept())(
  class extends React.Component<Props, State> {

    private MAX_CONCEPT_FETCH = 1000;
    constructor(props) {
      super(props);
      this.state = {
        activeDomainTab: {name: '', domain: undefined, conceptCount: 0},
        browsingSurvey: false,
        completedDomainSearches: [],
        conceptAddModalOpen: false,
        conceptDomainCounts: [],
        conceptDomainList: [],
        concepts: [],
        conceptsCache: [],
        conceptsSavedText: '',
        conceptSurveysList: [],
        countsError: false,
        currentInputString: '',
        currentSearchString: '',
        domainErrors: [],
        domainInfoError: false,
        domainsLoading: [],
        inputErrors: [],
        loadingDomains: true,
        countsLoading: false,
        searching: false,
        selectedConceptDomainMap: new Map<string, Concept[]>(),
        selectedDomain: undefined,
        selectedSurvey: '',
        selectedSurveyQuestions: [],
        showSearchError: false,
        standardConceptsOnly: !this.isConceptSetFlagEnable(),
        surveyAddModalOpen: false,
        surveyInfoError: false,
        surveysLoading: [],
        workspacePermissions: new WorkspacePermissions(props.workspace),
      };
    }

    componentDidMount() {
      this.loadDomainsAndSurveys();
    }

    componentWillUnmount() {
      currentConceptStore.next(null);
    }

    isConceptSetFlagEnable() {
      return serverConfigStore.getValue().enableConceptSetSearchV2;
    }

    async loadDomainsAndSurveys() {
      const {cdrVersionId} = this.props.workspace;
      const getDomainInfo = cohortBuilderApi().findDomainInfos(+cdrVersionId)
        .then(conceptDomainInfo => {
          let conceptsCache: ConceptCacheItem[] = conceptDomainInfo.items.map((domain) => ({
            domain: domain.domain,
            items: []
          }));
          // Add ConceptCacheItem for Surveys tab
          conceptsCache.push({domain: Domain.SURVEY, items: []});
          let conceptDomainCounts: DomainCount[] = conceptDomainInfo.items.map((domain) => ({
            domain: domain.domain,
            name: domain.name,
            conceptCount: 0
          }));
          // Add DomainCount for Surveys tab
          conceptDomainCounts.push({domain: Domain.SURVEY, name: 'Surveys', conceptCount: 0});
          if (!environment.enableNewConceptTabs) {
            // Don't show Physical Measurements tile or tab if feature flag disabled
            conceptsCache = conceptsCache.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
            conceptDomainCounts = conceptDomainCounts.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
          }
          this.setState({
            conceptsCache: conceptsCache,
            conceptDomainList: conceptDomainInfo.items,
            conceptDomainCounts: conceptDomainCounts,
            activeDomainTab: conceptDomainCounts[0],
          });
        })
        .catch((e) => {
          this.setState({domainInfoError: true});
          console.error(e);
        });
      const getSurveyInfo = cohortBuilderApi().findSurveyModules(+cdrVersionId)
        .then(surveysInfo => this.setState({conceptSurveysList: surveysInfo.items}))
        .catch((e) => {
          this.setState({surveyInfoError: true});
          console.error(e);
        });
      await Promise.all([getDomainInfo, getSurveyInfo]);
      this.browseDomainFromQueryParams();
      this.setState({loadingDomains: false});
    }

    async updateCardCounts() {
      const {cdrVersionId} = this.props.workspace;
      const {conceptDomainList, conceptSurveysList, currentInputString} = this.state;
      this.setState({
        domainsLoading: conceptDomainList.map(domain => domain.domain),
        surveysLoading: conceptSurveysList.map(survey => survey.name),
      });
      const promises = [];
      conceptDomainList.forEach(conceptDomain => {
        promises.push(cohortBuilderApi().findDomainCount(+cdrVersionId, conceptDomain.domain.toString(), currentInputString)
          .then(domainCount => {
            conceptDomain.allConceptCount = domainCount.conceptCount;
            this.setState({domainsLoading: this.state.domainsLoading.filter(domain => domain !== conceptDomain.domain)});
          })
        );
      });
      conceptSurveysList.forEach(conceptSurvey => {
        promises.push(cohortBuilderApi().findSurveyCount(+cdrVersionId, conceptSurvey.name, currentInputString)
          .then(surveyCount => {
            conceptSurvey.questionCount = surveyCount.conceptCount;
            this.setState({surveysLoading: this.state.surveysLoading.filter(survey => survey !== conceptSurvey.name)});
          }));
      });
      await Promise.all(promises);
      this.setState({conceptDomainList, conceptSurveysList});
    }

    browseDomainFromQueryParams() {
      const queryParams = queryParamsStore.getValue();
      if (queryParams.survey) {
        this.browseSurvey(queryParams.survey);
      }
      if (queryParams.domain) {
        if (queryParams.domain === Domain.SURVEY) {
          this.browseSurvey('');
        } else {
          this.browseDomain(this.state.conceptDomainList.find(dc => dc.domain === queryParams.domain));
        }
      }
    }

    handleSearchKeyPress(e) {
      const {currentInputString, selectedDomain, selectedSurvey} = this.state;
      // search on enter key if no forbidden characters are present
      if (e.key === Key.Enter) {
        if (currentInputString.trim().length < 3) {
          this.setState({inputErrors: [], showSearchError: true});
        } else {
          const inputErrors = validateInputForMySQL(currentInputString);
          this.setState({inputErrors, showSearchError: false});
          if (inputErrors.length === 0) {
            this.setState({currentSearchString: currentInputString}, () => {
              if (this.isConceptSetFlagEnable() && !(selectedDomain || selectedSurvey)) {
                this.updateCardCounts();
              } else {
                this.searchConcepts();
              }
            });
          }
        }
      }
    }

    handleCheckboxChange() {
      const {currentInputString, currentSearchString, searching, standardConceptsOnly} = this.state;
      this.setState({standardConceptsOnly: !standardConceptsOnly}, () => {
        // Check that we're in search mode and that the input hasn't changed since the last search before searching again
        if (searching && currentInputString === currentSearchString) {
          this.searchConcepts().then();
        }
      });
    }

    selectDomain(domainCount: DomainCount) {
      this.setState({activeDomainTab: domainCount}, this.setConceptsAndVocabularies);
    }

    setConceptsAndVocabularies() {
      const cacheItem = this.state.conceptsCache.find(c => c.domain === this.state.activeDomainTab.domain);
      this.setState({concepts: cacheItem.items});
    }

    async searchConcepts() {
      const {standardConceptsOnly, currentSearchString, conceptsCache, activeDomainTab,
        selectedConceptDomainMap, selectedDomain, selectedSurvey} = this.state;
      const {namespace, id} = this.props.workspace;
      this.setState({completedDomainSearches: [], concepts: [], countsError: false,
        domainErrors: [], countsLoading: true, searching: true});
      const standardConceptFilter = standardConceptsOnly ? StandardConceptFilter.STANDARDCONCEPTS : StandardConceptFilter.ALLCONCEPTS;
      const completedDomainSearches = [];
      const request = {query: currentSearchString, standardConceptFilter: standardConceptFilter, maxResults: this.MAX_CONCEPT_FETCH};
      if (!!selectedSurvey) {
        request['surveyName'] = selectedSurvey;
      }
      conceptsApi().domainCounts(namespace, id, request).then(counts => {
        // Filter Physical Measurements if feature flag disabled
        const conceptDomainCounts = !environment.enableNewConceptTabs
          ? counts.domainCounts.filter(dc => dc.domain !== Domain.PHYSICALMEASUREMENT)
          : counts.domainCounts;
        // update activeDomainTab with new conceptCount
        activeDomainTab.conceptCount = conceptDomainCounts.find(c => c.domain === activeDomainTab.domain).conceptCount;
        this.setState({conceptDomainCounts: conceptDomainCounts, countsLoading: false, activeDomainTab: activeDomainTab});
      }).catch(error => {
        console.error(error);
        this.setState({countsError: true});
      });
      conceptsCache.filter(domain => !selectedDomain || selectedDomain === domain.domain).forEach(async(cacheItem) => {
        selectedConceptDomainMap[cacheItem.domain] = [];
        const activeTabSearch = cacheItem.domain === activeDomainTab.domain;
        if (cacheItem.domain === Domain.SURVEY) {
          await conceptsApi().searchSurveys(namespace, id, request)
            .then(resp => cacheItem.items = resp)
            .catch(error => {
              console.error(error);
              const {domainErrors} = this.state;
              this.setState({domainErrors: [...domainErrors, Domain.SURVEY]});
            });
        } else {
          await conceptsApi().searchConcepts(namespace, id, {...request, domain: cacheItem.domain})
            .then(resp => cacheItem.items = resp.items)
            .catch(error => {
              console.error(error);
              const {domainErrors} = this.state;
              this.setState({domainErrors: [...domainErrors, cacheItem.domain]});
            });
        }
        completedDomainSearches.push(cacheItem.domain);
        this.setState({completedDomainSearches: completedDomainSearches});
        if (activeTabSearch) {
          this.setConceptsAndVocabularies();
        }
      });
      this.setState({selectedConceptDomainMap: selectedConceptDomainMap});
    }

    selectConcepts(concepts: any[]) {
      const {activeDomainTab: {domain}, selectedConceptDomainMap} = this.state;
      if (domain === Domain.SURVEY) {
        selectedConceptDomainMap[domain] = concepts.filter(concept => !!concept.question);
      } else {
        selectedConceptDomainMap[domain] = concepts.filter(
          concept => concept.domainId.replace(' ', '')
            .toLowerCase() === Domain[domain].toLowerCase());
      }
      this.setState({selectedConceptDomainMap: selectedConceptDomainMap});
    }

    clearSearch() {
      const {conceptDomainCounts} = this.state;
      this.setState({
        activeDomainTab: conceptDomainCounts[0],
        currentInputString: '',
        currentSearchString: '',
        inputErrors: [],
        selectedDomain: undefined,
        selectedSurvey: '',
        showSearchError: false,
        searching: false // reset the search result table to show browse/domain cards instead
      });
      if (this.isConceptSetFlagEnable()) {
        currentConceptStore.next(null);
      }
    }

    browseDomain(domain: DomainInfo) {
      const {conceptDomainCounts} = this.state;
      const activeDomainTab = conceptDomainCounts.find(domainCount => domainCount.domain === domain.domain);
      if (this.isConceptSetFlagEnable()) {
        currentConceptStore.next([]);
        this.setState({activeDomainTab: activeDomainTab, searching: true, selectedDomain: domain.domain});
      } else {
        this.setState({
          activeDomainTab: activeDomainTab,
          currentInputString: '',
          currentSearchString: '',
          inputErrors: [],
          selectedDomain: domain.domain,
          selectedSurvey: ''
        }, () => this.searchConcepts());
      }
    }

    browseSurvey(surveyName) {
      this.setState({
        activeDomainTab: {domain: Domain.SURVEY, name: 'Surveys', conceptCount: 0},
        currentInputString: '',
        currentSearchString: '',
        inputErrors: [],
        selectedDomain: Domain.SURVEY,
        selectedSurvey: surveyName,
        standardConceptsOnly: false
      }, () => this.searchConcepts());
    }

    domainLoading(domain) {
      return this.state.countsLoading || !this.state.completedDomainSearches.includes(domain.domain);
    }

    get noConceptsConstant() {
      return 'No concepts found for domain \'' + this.state.activeDomainTab.name + '\' this search.';
    }

    get activeSelectedConceptCount(): number {
      const {activeDomainTab, selectedConceptDomainMap} = this.state;
      if (!this.isConceptSetFlagEnable() || !this.props.concept) {
        if (!activeDomainTab || !activeDomainTab.domain || !selectedConceptDomainMap[activeDomainTab.domain]) {
          return 0;
        }
        return selectedConceptDomainMap[activeDomainTab.domain].length;
      } else {
        const selectedConcept = this.props.concept;
        if (!activeDomainTab && selectedConcept && selectedConcept.length === 0) {
          return 0;
        }
        return selectedConcept.length;
      }
    }

    get addToSetText(): string {
      const count = this.activeSelectedConceptCount;
      return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
    }

    get addSurveyToSetText(): string {
      const count = this.state.selectedSurveyQuestions.length;
      return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
    }

    afterConceptsSaved(conceptSet: ConceptSet) {
      const {namespace, id} = this.props.workspace;
      NavStore.navigate(['workspaces', namespace, id, 'data',
        'concepts', 'sets', conceptSet.id, 'actions']);
    }

    errorMessage() {
      return <div style={styles.error}>
        <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid' shape='exclamation-triangle' size='22'/>
        Sorry, the request cannot be completed. Please try refreshing the page or contact Support in the left hand navigation.
      </div>;
    }

    renderConcepts() {
      const {activeDomainTab, conceptDomainCounts, concepts, countsError, currentSearchString, domainErrors, selectedDomain,
        selectedConceptDomainMap} = this.state;
      const domainError = domainErrors.includes(activeDomainTab.domain);
      return <React.Fragment>
        {!this.isConceptSetFlagEnable() && <FadeBox>
          <FlexRow style={{justifyContent: 'flex-start'}}>
            {conceptDomainCounts.filter(domain => !selectedDomain || selectedDomain === domain.domain).map((domain) => {
              const tabError = countsError || domainErrors.includes(domain.domain);
              return <FlexColumn key={domain.name}>
                <Clickable style={styles.domainHeaderLink}
                           onClick={() => this.selectDomain(domain)}
                           disabled={this.domainLoading(domain)}
                           data-test-id={'domain-header-' + domain.name}>
                  <div style={{fontSize: '16px'}}>
                    {domain.name}
                    {tabError && <ClrIcon style={{color: colors.warning}} className='is-solid' shape='exclamation-triangle' size='22'/>}
                  </div>
                  {this.domainLoading(domain) ?
                      <Spinner style={{height: '15px', width: '15px'}}/> :
                      <FlexRow style={{justifyContent: 'space-between'}}>
                        {!tabError && <div>{domain.conceptCount.toLocaleString()}</div>}
                        {(selectedConceptDomainMap && selectedConceptDomainMap[domain.domain].length > 0) &&
                        <div style={styles.selectedConceptsCount} data-test-id='selectedConcepts'>
                          {selectedConceptDomainMap[domain.domain].length}
                        </div>}
                      </FlexRow>
                  }
                </Clickable>
                {domain.domain === activeDomainTab.domain && <hr data-test-id='active-domain'
                                                  key={activeDomainTab.domain}
                                                  style={styles.domainHeaderSelected}/>}
              </FlexColumn>;
            })}
          </FlexRow>
          {!this.domainLoading(activeDomainTab) && activeDomainTab.conceptCount > 1000 && !domainError &&
            <div style={styles.conceptCounts}>Showing top {concepts.length.toLocaleString()} {activeDomainTab.name}</div>
          }
          <ConceptTable concepts={concepts}
                        domain={activeDomainTab.domain}
                        loading={this.domainLoading(activeDomainTab)}
                        onSelectConcepts={this.selectConcepts.bind(this)}
                        placeholderValue={this.noConceptsConstant}
                        searchTerm={this.state.currentSearchString}
                        selectedConcepts={selectedConceptDomainMap[activeDomainTab.domain]}
                        reactKey={activeDomainTab.name}
                        error={domainError}/>
          <SlidingFabReact submitFunction={() => this.setState({conceptAddModalOpen: true})}
                           iconShape='plus'
                           tooltip={!this.state.workspacePermissions.canWrite}
                           tooltipContent={<div>Requires Owner or Writer permission</div>}
                           expanded={this.addToSetText}
                           disable={this.activeSelectedConceptCount === 0 ||
                           !this.state.workspacePermissions.canWrite}/>
        </FadeBox>}
        {this.isConceptSetFlagEnable() && <React.Fragment>
          <CriteriaSearch
            cohortContext={{domain: activeDomainTab.domain, type: 'PPI', standard: this.state.standardConceptsOnly}}
            conceptSearchTerms={currentSearchString}
            source='concept' selectedSurvey={this.state.selectedSurvey}/>
          <Button style={{float: 'right', marginBottom: '2rem'}}
                disabled={this.activeSelectedConceptCount === 0 ||
                !this.state.workspacePermissions.canWrite}
                onClick={() => setSidebarActiveIconStore.next('concept')}>Finish & Review</Button>
        </React.Fragment>}
      </React.Fragment>;
    }

    render() {
      const {activeDomainTab, browsingSurvey, conceptAddModalOpen, conceptDomainList, conceptsSavedText, conceptSurveysList,
        currentInputString, currentSearchString, domainInfoError, domainsLoading, inputErrors, loadingDomains, surveyInfoError,
        standardConceptsOnly, showSearchError, searching, selectedDomain, selectedSurvey, selectedConceptDomainMap, selectedSurveyQuestions,
        surveyAddModalOpen, surveysLoading} = this.state;
      return <React.Fragment>
        <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
          <FlexRow>
            {(selectedSurvey || selectedDomain) &&
            <Clickable style={styles.backArrow} onClick={() => this.clearSearch()}>
              <img src='/assets/icons/arrow-left-regular.svg' style={styles.arrowIcon}
                   alt='Go back'/>
            </Clickable>}
            <Header style={{fontSize: '24px', marginTop: 0, fontWeight: 600, paddingLeft: '0.4rem', paddingTop: '0.2rem'}}>
              Search {selectedDomain ? (selectedSurvey ? selectedSurvey : activeDomainTab.name) : 'Concepts'}
            </Header>
          </FlexRow>
          <div style={{margin: '1rem 0'}}>
            {!(this.isConceptSetFlagEnable() && searching) && <div style={{display: 'flex', alignItems: 'center'}}>
              <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
                fill: colors.accent, left: 'calc(1rem + 3.5%)'}}/>
              <TextInput style={styles.searchBar} data-test-id='concept-search-input'
                         placeholder='Search concepts in domain'
                         value={currentInputString}
                         onChange={(e) => this.setState({currentInputString: e})}
                         onKeyPress={(e) => this.handleSearchKeyPress(e)}/>
              {currentSearchString !== '' && <Clickable onClick={() => this.clearSearch()}
                                                        data-test-id='clear-search'>
                  <ClrIcon shape='times-circle' style={styles.clearSearchIcon}/>
              </Clickable>}
              {!this.isConceptSetFlagEnable() && <CheckBox checked={standardConceptsOnly}
                        label='Standard concepts only'
                        labelStyle={{marginLeft: '0.2rem'}}
                        data-test-id='standardConceptsCheckBox'
                        style={{marginLeft: '0.5rem', height: '16px', width: '16px'}}
                        manageOwnState={false}
                        onChange={() => this.handleCheckboxChange()}/>}
            </div>}
            {inputErrors.map((error, e) => <AlertDanger key={e} style={styles.inputAlert}>
              <span data-test-id='input-error-alert'>{error}</span>
            </AlertDanger>)}
            {showSearchError && <AlertDanger style={styles.inputAlert}>
                Minimum concept search length is three characters.
                <AlertClose style={{width: 'unset'}}
                            onClick={() => this.setState({showSearchError: false})}/>
            </AlertDanger>}
            <div style={{marginTop: '0.5rem'}}>{conceptsSavedText}</div>
          </div>
          {browsingSurvey && <div><SurveyDetails surveyName={selectedSurvey}
                                                 surveySelected={(selectedQuestion) =>
                                                   this.setState(
                                                     {selectedSurveyQuestions: selectedQuestion})}/>
            <SlidingFabReact submitFunction={() => this.setState({surveyAddModalOpen: true})}
                             iconShape='plus'
                             tooltip={!this.state.workspacePermissions.canWrite}
                             tooltipContent={<div>Requires Owner or Writer permission</div>}
                             expanded={this.addSurveyToSetText}
                             disable={selectedSurveyQuestions.length === 0}/>
          </div>}
          {!browsingSurvey && loadingDomains ? <div style={{position: 'relative', minHeight: '10rem'}}><SpinnerOverlay/></div> :
            searching ? this.renderConcepts() :  !browsingSurvey && <div>
              <div style={styles.sectionHeader}>
                Domains
              </div>
              <div style={styles.cardList}>
                {domainInfoError
                  ? this.errorMessage()
                  : conceptDomainList
                    .filter(item => item.domain !== Domain.PHYSICALMEASUREMENT && item.allConceptCount !== 0)
                    .map((domain, i) => <DomainCard conceptDomainInfo={domain}
                                                    standardConceptsOnly={standardConceptsOnly}
                                                    browseInDomain={() => this.browseDomain(domain)}
                                                    key={i} data-test-id='domain-box'
                                                    updating={domainsLoading.includes(domain.domain)}/>)
                }
              </div>
              <div style={styles.sectionHeader}>
                Survey Questions
              </div>
              <div style={styles.cardList}>
                {surveyInfoError
                  ? this.errorMessage()
                  : conceptSurveysList
                    .filter(survey => survey.questionCount > 0)
                    .map((survey) => <SurveyCard survey={survey}
                                                  key={survey.orderNumber}
                                                  browseSurvey={() => this.browseSurvey(survey.name)}
                                                  updating={surveysLoading.includes(survey.name)}/>)
                }
               </div>
              {environment.enableNewConceptTabs && <React.Fragment>
                <div style={styles.sectionHeader}>
                  Program Physical Measurements
                </div>
                <div style={styles.cardList}>
                  {domainInfoError
                    ? this.errorMessage()
                    : conceptDomainList.filter(item => item.domain === Domain.PHYSICALMEASUREMENT).map((physicalMeasurement, p) => {
                      return <PhysicalMeasurementsCard physicalMeasurement={physicalMeasurement} key={p}
                                         browsePhysicalMeasurements={() => this.browseDomain(physicalMeasurement)}/>;
                    })
                  }
                </div>
              </React.Fragment>}
            </div>
          }
          {conceptAddModalOpen &&
            <ConceptAddModal activeDomainTab={activeDomainTab}
                             selectedConcepts={selectedConceptDomainMap[activeDomainTab.domain]}
                             onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
                             onClose={() => this.setState({conceptAddModalOpen: false})}/>}
          {surveyAddModalOpen &&
          <ConceptSurveyAddModal selectedSurvey={selectedSurveyQuestions}
                                 onClose={() => this.setState({surveyAddModalOpen: false})}
                                 onSave={() => this.setState({surveyAddModalOpen: false})}
                                 surveyName={selectedSurvey}/>}
        </FadeBox>
      </React.Fragment>;
    }
  }
);

@Component({
  template: '<div #root></div>'
})
export class ConceptHomepageComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptHomepage, []);
  }
}

