import * as fp from 'lodash/fp';
import * as React from 'react';


import {SearchBar} from 'app/cohort-search/search-bar/search-bar.component';
import {ppiSurveys} from 'app/cohort-search/search-state.service';
import {TreeNode} from 'app/cohort-search/tree-node/tree-node.component';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentConcept, withCurrentWorkspace} from 'app/utils';
import {currentConceptStore, currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {Criteria, CriteriaSubType, CriteriaType, DomainType} from 'generated/fetch';

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
    float: 'right',
    fontSize: '12px',
    height: '1.5rem',
    margin: '0.25rem 0',
    padding: '0 0.5rem',
  },
  searchBarContainer: {
    position: 'absolute',
    width: '95%',
    marginTop: '-1px',
    display: 'flex',
    padding: '0.4rem 0',
    backgroundColor: colors.white,
    zIndex: 1,
  },
  treeContainer: {
    width: '99%',
    paddingTop: '2.5rem'
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
}

interface State {
  autocompleteSelection: any;
  children: any;
  error: boolean;
  ingredients: any;
  loading: boolean;
}

export const CriteriaTree = fp.flow(withCurrentWorkspace(), withCurrentConcept())(class extends React.Component<Props, State> {
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
    const {source} = this.props;
    if (source === 'conceptSetDetails') {
      this.setState({children: currentConceptStore.getValue()});
    } else {
      this.loadRootNodes();
    }
  }

  componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any) {
    const {concept, node: {domainId}, source} = this.props;
    if (source === 'conceptSetDetails') {
      if (prevProps.concept !== concept) {
        const {cdrVersionId} = (currentWorkspaceStore.getValue());
        this.setState({children: concept, loading: false});
        if (domainId === DomainType.SURVEY.toString()) {
          const rootSurveys = ppiSurveys.getValue();
          if (!rootSurveys[cdrVersionId]) {
            rootSurveys[cdrVersionId] = concept;
            ppiSurveys.next(rootSurveys);
          }
          const surveyParentList = concept.filter((survey) => {
            return survey.parentCount !== 0;
          });
          if (surveyParentList.length !== concept.length) {
            currentConceptStore.next(surveyParentList);
          }
        }
      }
    }
  }

  loadRootNodes() {
    const {node: {domainId, id, isStandard, type}, selectedSurvey} = this.props;
    this.setState({loading: true});
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === DomainType.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    const parentId = domainId === DomainType.PHYSICALMEASUREMENT.toString() ? null : id;
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, parentId)
    .then(resp => {
      if (domainId === DomainType.PHYSICALMEASUREMENT.toString()) {
        let children = [];
        resp.items.forEach(child => {
          child['children'] = [];
          if (child.parentId === 0) {
            children.push(child);
          } else {
            children = this.addChildToParent(child, children);
          }
        });
        this.setState({children});
      } else if (domainId === DomainType.SURVEY.toString() &&  selectedSurvey) {
        // Temp: This should be handle in API
        const selectedSurveyChild = resp.items.filter(child => child.name === selectedSurvey);
        if (selectedSurveyChild && selectedSurveyChild.length > 0) {
          cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, selectedSurveyChild[0].id)
              .then(surveyResponse => {
                this.setState({children: surveyResponse.items});
              });
        } else {
          this.setState({children: resp.items});
          if (domainId === DomainType.SURVEY.toString()) {
            const rootSurveys = ppiSurveys.getValue();
            if (!rootSurveys[cdrVersionId]) {
              rootSurveys[cdrVersionId] = resp.items;
              ppiSurveys.next(rootSurveys);
            }
          }
        }
      } else {
        this.setState({children: resp.items});
        if (domainId === DomainType.SURVEY.toString()) {
          const rootSurveys = ppiSurveys.getValue();
          if (!rootSurveys[cdrVersionId]) {
            rootSurveys[cdrVersionId] = resp.items;
            ppiSurveys.next(rootSurveys);
          }
        }
      }
    })
    .catch(error => {
      console.error(error);
      this.setState({error: true});
    })
    .finally(() => this.setState({loading: false}));
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
    const {node: {domainId}} = this.props;
    return domainId !== DomainType.PHYSICALMEASUREMENT.toString()
      && domainId !== DomainType.SURVEY.toString()
      && domainId !== DomainType.VISIT.toString();
  }

  // Hides the tree node for COPE survey if enableCOPESurvey config flag is set to false
  showNode(node: Criteria) {
    return node.subtype === CriteriaSubType.SURVEY.toString() && node.name.includes('COPE')
      ? serverConfigStore.getValue().enableCOPESurvey
      : true;
  }

  render() {
    const {autocompleteSelection, back, groupSelections, node, scrollToMatch, searchTerms, select, selectedIds, selectOption, setAttributes,
      setSearchTerms} = this.props;
    const {children, error, ingredients, loading} = this.state;
    return <React.Fragment>
      {node.domainId !== DomainType.VISIT.toString() &&
        <div style={serverConfigStore.getValue().enableCohortBuilderV2
          ? {...styles.searchBarContainer, backgroundColor: 'transparent', width: '65%'}
          : styles.searchBarContainer}>
          <SearchBar node={node}
                     searchTerms={searchTerms}
                     selectOption={selectOption}
                     setIngredients={(i) => this.setState({ingredients: i})}
                     setInput={(v) => setSearchTerms(v)}/>
        </div>
      }
      {!loading && <div style={this.showHeader
        ? {...styles.treeContainer, paddingTop: '3rem', marginTop: '0rem'}
        : styles.treeContainer}>
        {this.showHeader && <div style={{...styles.treeHeader, border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`}}>
          {!!ingredients && <div style={styles.ingredients}>
            Ingredients in this brand: {ingredients.join(', ')}
          </div>}
          <button style={styles.returnLink} onClick={() => back()}>Return to list</button>
        </div>}
        {error && <div style={styles.error}>
          <ClrIcon style={{color: colors.white}} className='is-solid' shape='exclamation-triangle' />
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
        </div>}
        <div style={this.showHeader ? styles.node : {...styles.node, border: 'none'}}>
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
