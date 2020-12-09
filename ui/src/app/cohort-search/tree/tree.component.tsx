import * as fp from 'lodash/fp';
import * as React from 'react';

import {SearchBar} from 'app/cohort-search/search-bar/search-bar.component';
import {ppiSurveys} from 'app/cohort-search/search-state.service';
import {TreeNode} from 'app/cohort-search/tree-node/tree-node.component';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCdrVersions, withCurrentConcept, withCurrentWorkspace} from 'app/utils';
import {getCdrVersion} from 'app/utils/cdr-versions';
import {currentCohortCriteriaStore, currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  CdrVersionListResponse,
  Criteria,
  CriteriaSubType,
  CriteriaType,
  Domain
} from 'generated/fetch';

const styles = reactStyles({
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: `1px solid ${colorWithWhiteness(colors.danger, 0.5)}`,
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
  ingredients: {
    float: 'left',
    fontWeight: 'bold',
    padding: '0.5rem',
  },
  returnLink: {
    background: 'transparent',
    border: 0,
    color: colors.accent,
    cursor: 'pointer',
    float: 'left',
    fontSize: '12px',
    height: '1.5rem',
    margin: '0.25rem 0',
    padding: '0 0.5rem',
  },
  searchBarContainer: {
    width: '95%',
    marginTop: '-1px',
    display: 'flex',
    padding: '0.4rem 0',
    backgroundColor: colors.white,
    zIndex: 1,
  },
  treeHeader: {
    position: 'sticky',
    top: 0,
    overflow: 'auto',
    background: colorWithWhiteness(colors.black, 0.97),
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
  },
  node: {
    height: '16rem',
    overflow: 'auto',
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    borderTop: 'none'
  }
});

const scrollbarCSS = `
  .show-scrollbar::-webkit-scrollbar {
    -webkit-appearance: none;
    width: 7px;
  }
  .show-scrollbar::-webkit-scrollbar-thumb {
    border-radius: 4px;
    background-color: rgba(0, 0, 0, .5);
    box-shadow: 0 0 1px rgba(255, 255, 255, .5);
  }
`;

interface Props {
  autocompleteSelection: any;
  back: Function;
  groupSelections: Array<number>;
  node: Criteria;
  source?: string;
  scrollToMatch: Function;
  searchTerms: string;
  select: Function;
  selectedSurvey?: string;
  selectedIds: Array<string>;
  selectOption: Function;
  setAttributes?: Function;
  setSearchTerms: Function;
  concept: Array<any>;
  workspace: WorkspaceData;
  cdrVersionListResponse: CdrVersionListResponse;
}

interface State {
  autocompleteSelection: any;
  children: any;
  error: boolean;
  ingredients: any;
  loading: boolean;
}

export const CriteriaTree = fp.flow(withCurrentWorkspace(), withCurrentConcept(), withCdrVersions())
(class extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      autocompleteSelection: undefined,
      children: undefined,
      error: false,
      ingredients: undefined,
      loading: true,
    };
  }

  componentDidMount(): void {
    this.loadRootNodes();
  }

  componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any) {
    const {concept, node: {domainId}, source} = this.props;
    if (source === 'conceptSetDetails') {
      if (prevProps.concept !== concept) {
        const {cdrVersionId} = (currentWorkspaceStore.getValue());
        this.setState({children: concept, loading: false});
        if (domainId === Domain.SURVEY.toString()) {
          const rootSurveys = ppiSurveys.getValue();
          if (!rootSurveys[cdrVersionId]) {
            rootSurveys[cdrVersionId] = concept;
            ppiSurveys.next(rootSurveys);
          }
        }
      }
    }
  }

  async loadRootNodes() {
    try {
      const {node: {domainId, id, isStandard, type}, selectedSurvey, source} = this.props;
      this.setState({loading: true});
      const {cdrVersionId} = (currentWorkspaceStore.getValue());
      const criteriaType = domainId === Domain.DRUG.toString() ? CriteriaType.ATC.toString() : type;
      const parentId = domainId === Domain.PHYSICALMEASUREMENT.toString() ? null : id;
      const promises = [cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, parentId)];
      if (source === 'criteria' && currentCohortCriteriaStore.getValue().some(crit => !crit.id)) {
        const criteriaRequest = {
          sourceConceptIds: currentCohortCriteriaStore.getValue().filter(s => !s.isStandard).map(s => s.conceptId),
          standardConceptIds: currentCohortCriteriaStore.getValue().filter(s => s.isStandard).map(s => s.conceptId),
        };
        promises.push(cohortBuilderApi().findCriteriaForCohortEdit(+cdrVersionId, domainId, criteriaRequest));
      }
      const [rootNodes, criteriaLookup] = await Promise.all(promises);
      if (criteriaLookup) {
        const updatedSelections = currentCohortCriteriaStore.getValue().map(sel => {
          const criteriaMatch = criteriaLookup.items.find(item => item.conceptId === sel.conceptId
            && item.isStandard === sel.isStandard
            && (domainId !== Domain.SURVEY.toString() || item.subtype === sel.subtype)
            && (sel.subtype !== CriteriaSubType.ANSWER.toString() || (item.value === sel.code))
          );
          if (criteriaMatch) {
            sel.id = criteriaMatch.id;
          }
          return sel;
        });
        currentCohortCriteriaStore.next(updatedSelections);
      }
      if (domainId === Domain.PHYSICALMEASUREMENT.toString()) {
        let children = [];
        rootNodes.items.forEach(child => {
          child['children'] = [];
          if (child.parentId === 0) {
            children.push(child);
          } else {
            children = this.addChildToParent(child, children);
          }
        });
        this.setState({children});
      } else if (domainId === Domain.SURVEY.toString() && selectedSurvey) {
        // Temp: This should be handle in API
        this.updatePpiSurveys(rootNodes, rootNodes.items.filter(child => child.name === selectedSurvey));
      } else if (domainId === Domain.SURVEY.toString() && this.props.source === 'conceptSetDetails') {
        const selectedSurveyChild = rootNodes.items.filter(child => child.id === this.props.node.parentId);
        this.updatePpiSurveys(rootNodes, selectedSurveyChild);
      } else {
        this.setState({children: rootNodes.items});
        if (domainId === Domain.SURVEY.toString()) {
          const rootSurveys = ppiSurveys.getValue();
          if (!rootSurveys[cdrVersionId]) {
            rootSurveys[cdrVersionId] = rootNodes.items;
            ppiSurveys.next(rootSurveys);
          }
        }
      }
    } catch (error) {
      console.error(error);
      this.setState({error: true});
    } finally {
      this.setState({loading: false});
    }
  }

  updatePpiSurveys(resp, selectedSurveyChild) {
    const {node: {domainId, isStandard, type}} = this.props;
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === Domain.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    if (selectedSurveyChild && selectedSurveyChild.length > 0) {
      cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, selectedSurveyChild[0].id)
          .then(surveyResponse => {
            console.log(surveyResponse.items);
            this.setState({children: surveyResponse.items});
          });
    } else {
      this.setState({children: resp.items});
      if (domainId === Domain.SURVEY.toString()) {
        const rootSurveys = ppiSurveys.getValue();
        if (!rootSurveys[cdrVersionId]) {
          rootSurveys[cdrVersionId] = resp.items;
          ppiSurveys.next(rootSurveys);
        }
      }
    }
  }

  addChildToParent(child, nodeList) {
    for (const node of nodeList) {
      if (!node.group) {
        continue;
      }
      if (node.id === child.parentId) {
        node.children.push(child);
        return nodeList;
      }
      if (node.children.length) {
        const nodeChildren = this.addChildToParent(child, node.children);
        if (nodeChildren) {
          node.children = nodeChildren;
          return nodeList;
        }
      }
    }
  }

  get showHeader() {
    const {node: {domainId}, source} = this.props;
    return !(source === 'criteria' && domainId === Domain.SURVEY.toString())
      && domainId !== Domain.PHYSICALMEASUREMENT.toString()
      && domainId !== Domain.VISIT.toString();
  }

  // Hides the tree node for COPE survey if enableCOPESurvey config flag is set to false
  showNode(node: Criteria) {
    const {workspace, cdrVersionListResponse} = this.props;
    return node.subtype === CriteriaSubType.SURVEY.toString() && node.name.includes('COPE')
        ? getCdrVersion(workspace, cdrVersionListResponse).hasCopeSurveyData
        : true;
  }

  selectIconDisabled() {
    const {selectedIds, source} = this.props;
    return source !== 'criteria' && selectedIds && selectedIds.length >= 1000;
  }

  render() {
    const {
      autocompleteSelection, back, groupSelections, node, scrollToMatch, searchTerms,
      select, selectedIds, selectOption, setAttributes, setSearchTerms
    } = this.props;
    const {children, error, ingredients, loading} = this.state;
    return <React.Fragment>
      <style>{scrollbarCSS}</style>
      {this.selectIconDisabled() &&
      <div style={{color: colors.warning, fontWeight: 'bold', maxWidth: '1000px'}}>
        NOTE: Concept Set can have only 1000 concepts. Please delete some concepts before adding
        more.
      </div>}
      {node.domainId !== Domain.VISIT.toString() &&
        <div style={serverConfigStore.getValue().enableCohortBuilderV2
          ? {...styles.searchBarContainer, backgroundColor: 'transparent', width: '80%'}
          : styles.searchBarContainer}>
          <SearchBar node={node}
                     searchTerms={searchTerms}
                     selectOption={selectOption}
                     setIngredients={(i) => this.setState({ingredients: i})}
                     setInput={(v) => setSearchTerms(v)}/>
        </div>
      }
      {!loading && <div style={{paddingTop: this.showHeader ? '0.5rem' : 0, width: '99%'}}>
        {this.showHeader && <div style={{...styles.treeHeader, border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`}}>
          {!!ingredients && <div style={styles.ingredients}>
            Ingredients in this brand: {ingredients.join(', ')}
          </div>}
          <button style={styles.returnLink} onClick={() => back()}>Return to list</button>
        </div>}
        {error && <div style={styles.error}>
          <ClrIcon style={{color: colors.white}} className='is-solid' shape='exclamation-triangle'/>
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation
        </div>}
        <div style={this.showHeader ? styles.node : {...styles.node, border: 'none'}} className='show-scrollbar'>
          {!!children && children.map((child, c) => this.showNode(child) && <TreeNode key={c}
                                                                                      source={this.props.source}
                                                                                      autocompleteSelection={autocompleteSelection}
                                                                                      groupSelections={groupSelections}
                                                                                      node={child}
                                                                                      scrollToMatch={scrollToMatch}
                                                                                      searchTerms={searchTerms}
                                                                                      select={(s) => select(s)}
                                                                                      selectedIds={selectedIds}
                                                                                      setAttributes={setAttributes}/>)}
        </div>
      </div>}
      {loading && !this.showHeader && <SpinnerOverlay/>}
    </React.Fragment>;
  }
});
