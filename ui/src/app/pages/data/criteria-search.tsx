import * as fp from 'lodash/fp';
import {Growl} from 'primereact/growl';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {ListSearchV2} from 'app/cohort-search/list-search-v2/list-search-v2.component';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {CriteriaTree} from 'app/cohort-search/tree/tree.component';
import {domainToTitle, typeToTitle} from 'app/cohort-search/utils';
import {Clickable, StyledAnchorTag} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentConceptStore
} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {Criteria, Domain} from 'generated/fetch';

const styles = reactStyles({
  arrowIcon: {
    height: '21px',
    marginTop: '-0.2rem',
    width: '18px'
  },
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    height: '1.5rem',
    lineHeight: '1.6rem',
    textAlign: 'center',
    width: '1.5rem',
  },
  externalLinks: {
    flex: '0 0 calc(55% - 1.25rem)',
    maxWidth: 'calc(55% - 1.25rem)',
    lineHeight: '0.75rem',
    textAlign: 'right',
    verticalAlign: 'middle'
  },
  growl: {
    position: 'absolute',
    right: '0',
    top: 0
  },
  loadingSubTree: {
    height: '100%',
    minHeight: '15rem',
    pointerEvents: 'none',
    opacity: 0.3
  },
  titleBar: {
    alignItems: 'center',
    color: colors.primary,
    margin: '0 0.25rem',
    width: '80%',
    height: '2rem',
  },
  titleHeader: {
    flex: '0 0 calc(45% - 1rem)',
    maxWidth: 'calc(45% - 1rem)',
    lineHeight: '1rem',
    margin: '0 0 0 0.75rem'
  }
});
const css = `
  .p-growl {
    position: sticky;
  }
  .p-growl.p-growl-topright {
    height: 1rem;
    width: 6.4rem;
    line-height: 0.7rem;
  }
  .p-growl .p-growl-item-container .p-growl-item .p-growl-image {
    font-size: 1rem !important;
    margin-top: 0.19rem
  }
  .p-growl-item-container:after {
    content:"";
    position: absolute;
    left: 97.5%;
    top: 0.1rem;
    width: 0px;
    height: 0px;
    border-top: 0.5rem solid transparent;
    border-left: 0.5rem solid ` + colorWithWhiteness(colors.success, 0.6) + `;
    border-bottom: 0.5rem solid transparent;
  }
  .p-growl-item-container {
    background-color: ` + colorWithWhiteness(colors.success, 0.6) + `!important;
  }
  .p-growl-item {
    padding: 0rem !important;
    background-color: ` + colorWithWhiteness(colors.success, 0.6) + `!important;
    margin-left: 0.3rem;
  }
  .p-growl-message {
    margin-left: 0.5em
  }
  .p-growl-details {
    margin-top: 0.1rem;
  }
 `;

const arrowIcon = '/assets/icons/arrow-left-regular.svg';

interface Props {
  backFn?: () => void;
  cohortContext: any;
  conceptSearchTerms?: string;
  selectedSurvey?: string;
  source: string;
  urlParams: any;
}

interface State {
  backMode: string;
  autocompleteSelection: Criteria;
  growlVisible: boolean;
  groupSelections: Array<number>;
  hierarchyNode: Criteria;
  mode: string;
  selections: Array<Selection>;
  selectedCriteriaList: Array<any>;
  selectedIds: Array<string>;
  treeSearchTerms: string;
  loadingSubtree: boolean;

}
export const CriteriaSearch = fp.flow(withUrlParams(), withCurrentWorkspace())(class extends React.Component<Props, State>  {
  growl: any;
  growlTimer: NodeJS.Timer;
  subscription: Subscription;

  constructor(props: Props) {
    super(props);
    this.state = {
      autocompleteSelection: undefined,
      backMode: 'list',
      growlVisible: false,
      hierarchyNode: undefined,
      groupSelections: [],
      mode: 'list',
      selectedIds: [],
      selections: [],
      selectedCriteriaList: [],
      treeSearchTerms: props.source !== 'criteria' ? props.conceptSearchTerms : '',
      loadingSubtree: false
    };
  }

  componentDidMount(): void {
    const {cohortContext: {domain, standard, type}, source} = this.props;
    let {backMode, mode} = this.state;
    let hierarchyNode;
    if (this.initTree) {
      hierarchyNode = {
        domainId: domain,
        type: type,
        isStandard: standard,
        id: 0,
      };
      backMode = 'tree';
      mode = 'tree';
    }
    this.setState({backMode, hierarchyNode, mode});
    if (source === 'criteria') {
      this.subscription = currentCohortCriteriaStore.subscribe(currentCohortCriteria => {
        this.setState({selectedCriteriaList: currentCohortCriteria});
      });
    } else {
      this.subscription = currentConceptStore.subscribe(currentConcepts => {
        const value = fp.map(selected => selected.conceptId + '', currentConcepts);
        this.setState({selectedCriteriaList: currentConcepts, selectedIds: value});
      });
    }
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
  }

  get initTree() {
    const {cohortContext: {domain}, source} = this.props;
    return domain === Domain.VISIT
      || (source === 'criteria' && domain === Domain.PHYSICALMEASUREMENT)
      || (source === 'criteria' && domain === Domain.SURVEY);
  }

  get isConcept() {
    return this.props.source === 'concept' || this.props.source === 'conceptSetDetails';
  }

  getGrowlStyle() {
    return !this.isConcept ? styles.growl : {...styles.growl, marginRight: '2.5rem', paddingTop: '2.75rem' };
  }

  searchContentStyle(mode: string) {
    let style = {
      display: 'none',
      flex: 1,
      minWidth: '14rem',
      overflowY: 'auto',
      overflowX: 'hidden',
      width: '100%',
      height: '100%',
    } as React.CSSProperties;
    if (this.state.mode === mode) {
      style = {...style, display: 'block', animation: 'fadeEffect 1s'};
    }
    return style;
  }

  showHierarchy = (criterion: Criteria) => {
    this.setState({
      autocompleteSelection: criterion,
      backMode: 'tree',
      hierarchyNode: {...criterion, id: 0},
      mode: 'tree',
      loadingSubtree: true,
      treeSearchTerms: criterion.name
    });
  }

  addSelection = (selectCriteria)  => {
    let criteriaList = this.state.selectedCriteriaList;
    if (criteriaList && criteriaList.length > 0) {
      criteriaList.push(selectCriteria);
    } else {
      criteriaList =  [selectCriteria];
    }
    this.setState({selectedCriteriaList: criteriaList});
    this.isConcept ?  currentConceptStore.next(criteriaList) : currentCohortCriteriaStore.next(criteriaList);
    const growlMessage = this.isConcept ? 'Concept Added' : 'Criteria Added';
    this.growl.show({severity: 'success', detail: growlMessage, closable: false, life: 2000});
    if (!!this.growlTimer) {
      clearTimeout(this.growlTimer);
    }
    // This is to set style display: 'none' on the growl so it doesn't block the nav icons in the sidebar
    this.growlTimer = setTimeout(() => this.setState({growlVisible: false}), 2500);
    this.setState({growlVisible: true});
  }

  getListSearchSelectedIds() {
    const {selectedCriteriaList} = this.state;
    const value = fp.map(selected => ('param' + selected.conceptId + selected.code + selected.isStandard), selectedCriteriaList);
    return value;
  }

  setScroll = (id: string) => {
    const nodeId = `node${id}`;
    const node = document.getElementById(nodeId);
    if (node) {
      setTimeout(() => node.scrollIntoView({behavior: 'smooth', block: 'center'}), 200);
    }
    this.setState({loadingSubtree: false});
  }

  back = () => {
    if (this.state.mode === 'tree') {
      this.setState({autocompleteSelection: undefined, backMode: 'list', hierarchyNode: undefined, mode: 'list'});
    } else {
      attributesSelectionStore.next(undefined);
      this.setState({mode: this.state.backMode});
    }
  }


  setTreeSearchTerms = (input: string) => {
    this.setState({treeSearchTerms: input});
  }

  setAutocompleteSelection = (selection: any) => {
    this.setState({loadingSubtree: true, autocompleteSelection: selection});
  }

  get showDataBrowserLink() {
    return [Domain.CONDITION, Domain.PROCEDURE, Domain.MEASUREMENT, Domain.DRUG].includes(this.props.cohortContext.domain);
  }

  get domainTitle() {
    const {cohortContext: {domain, type}, selectedSurvey} = this.props;
    if (!!selectedSurvey) {
      return selectedSurvey;
    } else {
      return domain === Domain.PERSON ? typeToTitle(type) : domainToTitle(domain);
    }
  }

  render() {
    const {backFn, cohortContext, conceptSearchTerms, selectedSurvey, source} = this.props;
    const {autocompleteSelection, groupSelections, hierarchyNode, loadingSubtree,
      treeSearchTerms, growlVisible} = this.state;
    return <div id='criteria-search-container'>
      {loadingSubtree && <SpinnerOverlay/>}
      <Growl ref={(el) => this.growl = el} style={!growlVisible ? {...styles.growl, display: 'none'} : styles.growl}/>
      <FlexRowWrap style={{...styles.titleBar, marginTop: source === 'criteria' ? '1rem' : 0}}>
        <Clickable style={styles.backArrow} onClick={() => backFn()}>
          <img src={arrowIcon} style={styles.arrowIcon} alt='Go back' />
        </Clickable>
        <h2 style={styles.titleHeader}>{this.domainTitle}</h2>
        <div style={styles.externalLinks}>
          {cohortContext.domain === Domain.DRUG && <div>
            <StyledAnchorTag
                href='https://mor.nlm.nih.gov/RxNav/'
                target='_blank'
                rel='noopener noreferrer'>
              Explore
            </StyledAnchorTag>
            &nbsp;drugs by brand names outside of <AoU/>
          </div>}
          {cohortContext.domain === Domain.SURVEY && <div>
            Find more information about each survey in the&nbsp;
            <StyledAnchorTag
                href='https://www.researchallofus.org/survey-explorer/'
                target='_blank'
                rel='noopener noreferrer'>
              Survey Explorer
            </StyledAnchorTag>
          </div>}
          {this.showDataBrowserLink && <div>
            Explore Source information on the&nbsp;
            <StyledAnchorTag
                href={environment.publicUiUrl}
                target='_blank'
                rel='noopener noreferrer'>
              Data Browser
            </StyledAnchorTag>
          </div>}
        </div>
      </FlexRowWrap>
      <div style={loadingSubtree ? styles.loadingSubTree : {height: '100%', minHeight: '15rem'}}>
        <style>{css}</style>
        <Growl ref={(el) => this.growl = el}
               style={!growlVisible ? {...this.getGrowlStyle(), display: 'none'} : this.getGrowlStyle()}/>
        {hierarchyNode && <CriteriaTree
            source={source}
            selectedSurvey={selectedSurvey}
            autocompleteSelection={autocompleteSelection}
            back={this.back}
            groupSelections={groupSelections}
            node={hierarchyNode}
            scrollToMatch={this.setScroll}
            searchTerms={treeSearchTerms}
            select={this.addSelection}
            selectedIds={this.getListSearchSelectedIds()}
            selectOption={this.setAutocompleteSelection}
            setSearchTerms={this.setTreeSearchTerms}/>}
         {/*List View (using duplicated version of ListSearch) */}
        <div style={this.searchContentStyle('list')}>
          <ListSearchV2 source={source}
                        hierarchy={this.showHierarchy}
                        searchContext={cohortContext}
                        searchTerms={conceptSearchTerms}
                        select={this.addSelection}
                        selectedSurvey={selectedSurvey}
                        selectedIds={this.getListSearchSelectedIds()}/>
        </div>
      </div>
     </div>;
  }
});
