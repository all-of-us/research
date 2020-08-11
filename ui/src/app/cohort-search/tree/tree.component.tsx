import * as React from 'react';

import {SearchBar} from 'app/cohort-search/search-bar/search-bar.component';
import {TreeNode} from 'app/cohort-search/tree-node/tree-node.component';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {Criteria, CriteriaType, DomainType} from 'generated/fetch';

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
    margin: '3rem 0 1rem',
    width: '99%',
  },
  treeHeader: {
    overflow: 'auto',
    background: colorWithWhiteness(colors.black, 0.97),
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
  }
});

interface Props {
  autocompleteSelection: any;
  back: Function;
  groupSelections: Array<number>;
  node: Criteria;
  scrollToMatch: Function;
  searchTerms: string;
  select: Function;
  selectedIds: Array<string>;
  selectOption: Function;
  setAttributes?: Function;
  setSearchTerms: Function;
}

interface State {
  autocompleteSelection: any;
  children: any;
  error: boolean;
  ingredients: any;
  loading: boolean;
}

export const CriteriaTree = withCurrentWorkspace()(class extends React.Component<Props, State> {
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

  loadRootNodes() {
    const {node: {domainId, id, isStandard, type}} = this.props;
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
      } else {
        this.setState({children: resp.items});
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

  render() {
    const {autocompleteSelection, back, groupSelections, node, scrollToMatch, searchTerms, select, selectedIds, selectOption, setAttributes,
      setSearchTerms} = this.props;
    const {children, error, ingredients, loading} = this.state;
    return <React.Fragment>
      {node.domainId !== DomainType.VISIT.toString() && <div style={styles.searchBarContainer}>
        <SearchBar node={node}
                   searchTerms={searchTerms}
                   selectOption={selectOption}
                   setIngredients={(i) => this.setState({ingredients: i})}
                   setInput={(v) => setSearchTerms(v)}/>
      </div>}
      {!loading && <div style={this.showHeader
        ? {...styles.treeContainer, border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`}
        : styles.treeContainer}>
        {this.showHeader && <div style={styles.treeHeader}>
          {!!ingredients && <div style={styles.ingredients}>
            Ingredients in this brand: {ingredients.join(', ')}
          </div>}
          <button style={styles.returnLink} onClick={() => back()}>Return to list</button>
        </div>}
        {error && <div style={styles.error}>
          <ClrIcon style={{color: colors.white}} className='is-solid' shape='exclamation-triangle' />
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
        </div>}
        {!!children && children.map((child, c) => <TreeNode key={c}
                                                            autocompleteSelection={autocompleteSelection}
                                                            groupSelections={groupSelections}
                                                            node={child}
                                                            scrollToMatch={scrollToMatch}
                                                            searchTerms={searchTerms}
                                                            select={(s) => select(s)}
                                                            selectedIds={selectedIds}
                                                            setAttributes={setAttributes}/>)}
      </div>}
      {loading && !this.showHeader && <SpinnerOverlay/>}
    </React.Fragment>;
  }
});
