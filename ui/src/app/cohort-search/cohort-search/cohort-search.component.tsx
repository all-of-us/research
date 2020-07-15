import {Component, Input} from '@angular/core';
import * as React from 'react';

import {AttributesPage} from 'app/cohort-search/attributes-page/attributes-page.component';
import {Demographics} from 'app/cohort-search/demographics/demographics.component';
import {ListSearchV2} from 'app/cohort-search/list-search-v2/list-search-v2.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {CriteriaTree} from 'app/cohort-search/tree/tree.component';
import {domainToTitle, generateId, typeToTitle} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentCohortCriteriaStore} from 'app/utils/navigation';
import {Criteria, CriteriaType, DomainType, TemporalMention, TemporalTime} from 'generated/fetch';
import {Messages} from 'primereact/messages';

const styles = reactStyles({
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    height: '1.5rem',
    lineHeight: '1.4rem',
    textAlign: 'center',
    width: '1.5rem',
  },
  footer: {
    marginTop: '0.5rem',
    padding: '0.45rem 0rem',
    display: 'flex',
    justifyContent: 'flex-end',
  },
  footerButton: {
    height: '1.5rem',
    margin: '0.25rem 0.5rem'
  },
  panelLeft: {
    display: 'none',
    flex: 1,
    minWidth: '14rem',
    overflowY: 'auto',
    overflowX: 'hidden',
    width: '100%',
    height: '100%',
    padding: '0 0.4rem 0 1rem',
  },
  searchContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    height: '70vh',
    width: '100%',
  },
  titleBar: {
    marginBottom: '0.5rem',
    padding: '0rem 1rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    height: '2.5rem',
    marginTop: '0.5rem',
  }
});

const arrowIcon = '/assets/icons/arrow-left-regular.svg';

function initGroup(role: string, item: any) {
  return {
    id: generateId(role),
    items: [item],
    count: null,
    temporal: false,
    mention: TemporalMention.ANYMENTION,
    time: TemporalTime.DURINGSAMEENCOUNTERAS,
    timeValue: '',
    timeFrame: '',
    isRequesting: false,
    status: 'active'
  };
}

interface Props {
  closeSearch: () => void;
  searchContext: any;
  selections?: Array<Selection>;
}

interface State {
  attributesNode: Criteria;
  autocompleteSelection: Criteria;
  backMode: string;
  count: number;
  disableFinish: boolean;
  groupSelections: Array<number>;
  hierarchyNode: Criteria;
  loadingSubtree: boolean;
  mode: string;
  selectedIds: Array<string>;
  selections: Array<Selection>;
  title: string;
  treeSearchTerms: string;
}

const css = `
  .p-messages {
     position: relative;
     height: 29px;
     width: 7rem;
     background-color: ` + colorWithWhiteness(colors.success, 0.6) + `;
     line-height:1.2rem;
   }

  .p-messages::before {
    content:"";
    position: absolute;
    right: 100%;
    top:0px;
    width:0px;
    height:0px;
    border-top:0.6rem solid transparent;
    border-right:0.6rem solid transparent;
    border-bottom:0.6rem solid transparent;
  }
  .p-messages:after {
    content:"";
    position: absolute;
    left: 100%;
    top:0px;
    width:0px;
    height:0px;
    border-top:0.6rem solid transparent;
    border-left:0.8rem solid ` + colorWithWhiteness(colors.success, 0.6) + `;
    border-bottom:0.6rem solid transparent;
   }
   .p-messages.p-messages-success {
     background-color: ` + colorWithWhiteness(colors.success, 0.6) + `!important;
   }
   .p-messages-wrapper {
     padding: 0rem !important;
     background-color: ` + colorWithWhiteness(colors.success, 0.6) + `!important;
     margin-left: 0.3rem;
   }
 `;

export class CohortSearch extends React.Component<Props, State> {

  message: any;
  constructor(props: Props) {
    super(props);
    this.state = {
      attributesNode: undefined,
      autocompleteSelection: undefined,
      backMode: 'list',
      count: 0,
      disableFinish: false,
      groupSelections: [],
      hierarchyNode: undefined,
      loadingSubtree: false,
      mode: 'list',
      selectedIds: [],
      selections: [],
      title: '',
      treeSearchTerms: '',
    };
  }

  componentWillUnmount() {
    currentCohortCriteriaStore.next(undefined);
  }

  componentDidMount(): void {
    const {searchContext: {domain, item, standard, type}} = this.props;
    const selections = item.searchParameters;
    const selectedIds = selections.map(s => s.parameterId);
    if (type === CriteriaType.DECEASED) {
      this.selectDeceased();
    } else {
      const title = domain === DomainType.PERSON ? typeToTitle(type) : domainToTitle(domain);
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
      this.setState({backMode, hierarchyNode, mode, selectedIds, selections, title});
    }
    currentCohortCriteriaStore.next([]);
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
      this.setState({attributesNode: undefined, mode: this.state.backMode});
    }
  }

  finish = () => {
    const {searchContext: {domain, groupId, item, role, type}} = this.props;
    const {selections} = this.state;
    if (domain === DomainType.PERSON) {
      triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(type)} - Finish`);
    }
    const searchRequest = searchRequestStore.getValue();
    item.searchParameters = selections;
    if (groupId) {
      const groupIndex = searchRequest[role].findIndex(grp => grp.id === groupId);
      if (groupIndex > -1) {
        const itemIndex = searchRequest[role][groupIndex].items.findIndex(it => it.id === item.id);
        if (itemIndex > -1) {
          searchRequest[role][groupIndex].items[itemIndex] = item;
        } else {
          searchRequest[role][groupIndex].items.push(item);
        }
      }
    } else {
      searchRequest[role].push(initGroup(role, item));
    }
    searchRequestStore.next(searchRequest);
    this.props.closeSearch();
  }

  get initTree() {
    const {searchContext: {domain}} = this.props;
    return domain === DomainType.PHYSICALMEASUREMENT
      || domain === DomainType.SURVEY
      || domain === DomainType.VISIT;
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
      padding: '0 0.4rem 0 1rem',
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

  modifiersFlag = (disabled: boolean) => {
    this.setState({disableFinish: disabled});
  }

  setTreeSearchTerms = (input: string) => {
    this.setState({treeSearchTerms: input});
  }

  setAutocompleteSelection = (selection: any) => {
    this.setState({loadingSubtree: true, autocompleteSelection: selection});
  }

  setAttributes = (criterion: Criteria) => {
    this.setState({attributesNode: criterion, backMode: this.state.mode, mode: 'attributes'});
  }

  addSelection = (param: any) => {
    let {groupSelections, selectedIds, selections} = this.state;
    if (selectedIds.includes(param.parameterId)) {
      selections = selections.filter(p => p.parameterId !== param.parameterId);
    } else {
      selectedIds = [...selectedIds, param.parameterId];
      if (param.group) {
        groupSelections = [...groupSelections, param.id];
      }
    }
    selections = [...selections, param];
    this.message.show({ severity: 'success', detail: 'Criteria Added', closable: false, life: 2000});
    currentCohortCriteriaStore.next(selections);
    this.setState({groupSelections, selections, selectedIds});
  }

  selectDeceased() {
    const param = {
      id: null,
      parentId: null,
      parameterId: '',
      type: CriteriaType.DECEASED.toString(),
      name: 'Deceased',
      group: false,
      domainId: DomainType.PERSON.toString(),
      hasAttributes: false,
      selectable: true,
      attributes: []
    } as Selection;
    // wrapping in a timeout here prevents 'ExpressionChangedAfterItHasBeenCheckedError' in the parent component
    // TODO remove timeout once cohort-search component is converted to React
    setTimeout(() => this.setState({selections: [param]}, () => this.finish()));
  }

  render() {
    const {closeSearch, searchContext, searchContext: {domain, type}} = this.props;
    const {attributesNode, autocompleteSelection, count, groupSelections, hierarchyNode, loadingSubtree, mode, selectedIds, selections,
      title, treeSearchTerms} = this.state;
    return !!searchContext && <FlexRowWrap style={styles.searchContainer}>
      <div style={{position: 'absolute', paddingLeft: '83%', marginTop: '-1rem'}}>
        <style>
          {css}
        </style>
        <Messages ref={(el) => this.message = el}></Messages>
      </div>
      <div style={{height: '100%', width: '100%'}}>
        <div style={styles.titleBar}>
          <div style={{display: 'inline-flex', marginRight: '0.5rem'}}>
            <Clickable style={styles.backArrow} onClick={() => closeSearch()}>
              <img src={arrowIcon} style={{height: '21px', width: '18px'}} alt='Go back' />
            </Clickable>
            <h2 style={{color: colors.primary, lineHeight: '1.5rem', margin: '0 0 0 0.75rem'}}>
              {title}
            </h2>
          </div>
          {mode === 'attributes' && <Button type='link' onClick={this.back}>
            <ClrIcon size='24' shape='close'/>
          </Button>}
        </div>
        <div style={
          (domain === DomainType.PERSON && type !== CriteriaType.AGE)
            ? {marginBottom: '3.5rem'}
            : {height: 'calc(100% - 3.5rem)'}
        }>
          {domain === DomainType.PERSON ? <div style={{flex: 1, overflow: 'auto'}}>
              <Demographics
                count={count}
                criteriaType={type}
                select={this.addSelection}
                selectedIds={selectedIds}
                selections={selections}/>
            </div>
            : <React.Fragment>
              {loadingSubtree && <SpinnerOverlay/>}
              <div style={loadingSubtree ? {height: '100%', pointerEvents: 'none', opacity: 0.3} : {height: '100%'}}>
                {/* Tree View */}
                <div style={this.searchContentStyle('tree')}>
                  {hierarchyNode && <CriteriaTree
                      autocompleteSelection={autocompleteSelection}
                      back={this.back}
                      groupSelections={groupSelections}
                      node={hierarchyNode}
                      scrollToMatch={this.setScroll}
                      searchTerms={treeSearchTerms}
                      select={this.addSelection}
                      selectedIds={selectedIds}
                      selectOption={this.setAutocompleteSelection}
                      setAttributes={this.setAttributes}
                      setSearchTerms={this.setTreeSearchTerms}/>}
                </div>
                {/* List View (using duplicated version of ListSearch) */}
                <div style={this.searchContentStyle('list')}>
                  <ListSearchV2 hierarchy={this.showHierarchy}
                              searchContext={searchContext}
                              select={this.addSelection}
                              selectedIds={selectedIds}
                              setAttributes={this.setAttributes}/>
                </div>
                {/**
                 Attributes Page - This will no longer be rendered here in the future, leaving temporarily for reference
                 TODO Remove once AttributesPage is moved to the sidebar with RW-4595
                 **/}
                <div style={this.searchContentStyle('attributes')}>
                  {!!attributesNode && <AttributesPage
                      close={this.back}
                      node={attributesNode}
                      select={this.addSelection}/>}
                </div>
              </div>
            </React.Fragment>}
          {type === CriteriaType.AGE && <div style={styles.footer}>
            <Button style={styles.footerButton}
                    type='link'
                    onClick={closeSearch}>
              Cancel
            </Button>
            <Button style={styles.footerButton}
                    type='primary'
                    onClick={this.finish}>
              Finish
            </Button>
          </div>}
        </div>
      </div>
    </FlexRowWrap>;
  }
}

@Component({
  selector: 'app-cohort-search',
  template: '<div #root></div>'
})
export class CohortSearchComponent extends ReactWrapperBase {
  @Input('closeSearch') closeSearch: Props['closeSearch'];
  @Input('searchContext') searchContext: Props['searchContext'];
  constructor() {
    super(CohortSearch, ['closeSearch', 'searchContext']);
  }
}
