import {Component, Input} from '@angular/core';
import * as React from 'react';

import {SearchGroupItem} from 'app/cohort-search/search-group-item/search-group-item.component';
import {criteriaMenuOptionsStore, initExisting, searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, mapGroup, typeToTitle} from 'app/cohort-search/utils';
import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {isAbortError} from 'app/utils/errors';
import {WorkspaceData} from 'app/utils/workspace-data';
import {DomainType, SearchRequest, TemporalMention, TemporalTime} from 'generated/fetch';
import {InputSwitch} from 'primereact/inputswitch';
import {Menu} from 'primereact/menu';
import {TieredMenu} from 'primereact/tieredmenu';

const styles = reactStyles({
  card: {
    background: colors.white,
    borderColor: 'rgba(215, 215, 215, 0.5)',
    borderRadius: '0.2rem',
    boxShadow: '0 0.125rem 0.125rem 0 #d7d7d7',
    margin: '0 0 0.6rem'
  },
  cardBlock: {
    borderBottom: '1px solid #eee',
    padding: '0.5rem 0.75rem'
  },
  cardHeader: {
    backgroundColor: 'rgb(226, 226, 233)',
    borderBottom: '1px solid #eee',
    color: 'rgb(38, 34, 98)',
    fontSize: '14px',
    fontWeight: 600,
    minWidth: '100%',
    padding: '0.5rem 0.75rem'
  },
  overlay: {
    background: 'rgba(255, 255, 255, 0.9)',
    display: 'table',
    position: 'relative',
    textAlign: 'center',
    verticalAlign: 'middle',
  },
  overlayInner: {
    color: colors.warning,
    display: 'table-cell',
    fontSize: '18px',
    verticalAlign: 'middle',
  },
  overlayButton: {
    background: 'transparent',
    border: 0,
    color: colors.accent,
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    margin: '0.25rem 0',
  },
  itemOr: {
    background: colors.white,
    color: 'rgb(195, 195, 195)',
    float: 'right',
    marginRight: '46%',
    padding: '0 10px'
  },
  menu: {
    maxWidth: '15rem',
    minWidth: '5rem',
    width: 'auto'
  },
  searchItem: {
    borderBottom: '1px solid rgb(195, 195, 195)',
    margin: '0 0.5rem',
    padding: '0.5rem 0.25rem'
  },
  menuButton: {
    border: '1px solid rgb(195, 195, 195)',
    borderRadius: '0.125rem',
    color: 'rgb(114, 114, 114)',
    fontSize: '12px',
    fontWeight: 100,
    height: '1.5rem',
    letterSpacing: '1px',
    lineHeight: '1.5rem',
    padding: '0 0.5rem',
    textTransform: 'uppercase',
    verticalAlign: 'middle'
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginLeft: '-0.5rem',
    marginRight: '-0.5rem',
  },
  col6: {
    flex: '0 0 50%',
    maxWidth: '50%',
    padding: '0 0.5rem',
  },
  temporalSubCardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '.5rem .75rem'
  },
  timeInput: {
    border: '1px solid #9a9a9a',
    borderRadius: '0.1rem',
    padding: '0.6rem',
    textAlign: 'center',
    width: '3.4rem'
  }
});

const temporalMentions = [
  TemporalMention.ANYMENTION,
  TemporalMention.FIRSTMENTION,
  TemporalMention.LASTMENTION
];

const temporalTimes = [
  TemporalTime.DURINGSAMEENCOUNTERAS,
  TemporalTime.XDAYSAFTER,
  TemporalTime.XDAYSBEFORE,
  TemporalTime.WITHINXDAYSOF
];

function formatOption(option) {
  switch (option) {
    case TemporalMention.ANYMENTION:
      return 'Any Mention';
    case TemporalMention.FIRSTMENTION:
      return 'First Mention';
    case TemporalMention.LASTMENTION:
      return 'Last Mention';
    case TemporalTime.DURINGSAMEENCOUNTERAS:
      return 'During same encounter as';
    case TemporalTime.XDAYSBEFORE:
      return 'X Days before';
    case TemporalTime.XDAYSAFTER:
      return 'X Days after';
    case TemporalTime.WITHINXDAYSOF:
      return 'Within X Days of';
  }
}

interface Props {
  group: any;
  index: number;
  role: keyof SearchRequest;
  updated: number;
  updateRequest: Function;
  workspace: WorkspaceData;
}

interface State {
  count: number;
  criteriaMenuOptions: any;
  demoOpen: boolean;
  demoMenuHover: boolean;
  error: boolean;
  loading: boolean;
  overlayStyle: any;
  position: string;
  preventInputCalculate: boolean;
  status: string;
}

export const SearchGroup = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private aborter = new AbortController();
    private criteriaMenu: any;
    private groupMenu: any;
    private mentionMenu: any;
    private temporalCriteriaMenu: any;
    private timeMenu: any;

    constructor(props: any) {
      super(props);
      this.state = {
        count: undefined,
        criteriaMenuOptions: {programTypes: [], domainTypes: []},
        demoOpen: false,
        demoMenuHover: false,
        error: false,
        loading: false,
        overlayStyle: {},
        position: 'bottom-left',
        preventInputCalculate: false,
        status: undefined
      };
    }

    componentDidMount(): void {
      const {group: {id}, workspace: {cdrVersionId}} = this.props;
      criteriaMenuOptionsStore.subscribe(options => {
        if (!!options[cdrVersionId]) {
          this.setState({criteriaMenuOptions: options[cdrVersionId]});
        }
      });
      if (typeof ResizeObserver === 'function') {
        const groupDiv = document.getElementById(id);
        // check that groupDiv is of type Element
        if (groupDiv && groupDiv.tagName) {
          // create observer to reposition overlays on div resize
          const ro = new ResizeObserver(() => {
            const {status} = this.props.group;
            if (status === 'hidden' || status === 'pending') {
              this.setOverlayPosition();
            }
          });
          ro.observe(groupDiv);
        }
      }
    }

    componentWillUnmount(): void {
      this.aborter.abort();
    }

    getGroupCount() {
      try {
        const {group, role, workspace: {cdrVersionId}} = this.props;
        const mappedGroup = mapGroup(group);
        const request = {
          includes: [],
          excludes: [],
          [role]: [mappedGroup]
        };
        cohortBuilderApi().countParticipants(+cdrVersionId, request, {signal: this.aborter.signal})
          .then(count => this.setState({count, loading: false}));
      } catch (error) {
        if (!isAbortError(error)) {
          console.error(error);
          this.setState({error: true, loading: false});
        }
      }
    }

    checkPendingCalls() {
      if (this.state.loading) {
        this.aborter.abort();
        this.aborter = new AbortController();
      }
    }

    update(recalculate: boolean) {
      const {index, group: {temporal}, updateRequest} = this.props;
      // timeout prevents Angular 'value changed after checked' error
      setTimeout(() => {
        // prevent multiple total count calls when initializing multiple groups simultaneously
        // (on cohort edit or clone)
        const init = initExisting.getValue();
        if (!init || (init && index === 0)) {
          updateRequest(recalculate);
          if (init) {
            this.setState({preventInputCalculate: true});
            initExisting.next(false);
          }
        }
        if (recalculate && this.hasActiveItems && (!temporal || !this.temporalError)) {
          this.checkPendingCalls();
          this.setState({error: false, loading: true});
          this.getGroupCount();
        }
      });
    }

    get hasActiveItems() {
      return this.props.group.items.some(it => it.status === 'active');
    }

    get items() {
      const {group: {items, temporal}} = this.props;
      return !temporal ? items : items.filter(it => it.temporalGroup === 0);
    }

    get temporalItems() {
      const {group: {items, temporal}} = this.props;
      return !temporal ? [] : items.filter(it => it.temporalGroup === 1);
    }

    get disableTemporal() {
      return this.items.some(it => [DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(it.type));
    }

    remove() {
      triggerEvent('Delete', 'Click', 'Snowman - Delete Group - Cohort Builder');
      this.hide('pending');
      const timeoutId = setTimeout(() => {
        this.removeGroup();
      }, 10000);
      this.setGroupProperty('timeout', timeoutId);
    }

    hide(status: string) {
      triggerEvent('Suppress', 'Click', 'Snowman - Suppress Group - Cohort Builder');
      this.setGroupProperty('status', status);
      setTimeout(() => this.setOverlayPosition());
    }

    enable() {
      triggerEvent('Enable', 'Click', 'Enable - Suppress Group - Cohort Builder');
      this.setGroupProperty('status', 'active');
    }

    undo() {
      triggerEvent('Undo', 'Click', 'Undo - Delete Group - Cohort Builder');
      clearTimeout(this.props.group.timeout);
      this.enable();
    }

    removeGroup() {
      const {group, role} = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role] = searchRequest[role].filter(grp => grp.id !== group.id);
      searchRequestStore.next(searchRequest);
    }

    setOverlayPosition() {
      const {group} = this.props;
      const groupCard = document.getElementById(group.id);
      if (groupCard) {
        const {marginBottom, width, height} = window.getComputedStyle(groupCard);
        const margin = `-${(parseFloat(height) + parseFloat(marginBottom))}px 0 ${marginBottom}`;
        this.setState({overlayStyle: {height, margin, width}});
      }
    }

    launchWizard(criteria: any, tempGroup?: number) {
      const {group, role} = this.props;
      const {domain, type, standard} = criteria;
      if (tempGroup !== undefined) {
        triggerEvent('Temporal', 'Click', `${domainToTitle(domain)} - Temporal - Cohort Builder`);
      } else {
        const category = `${role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
        // If domain is PERSON, list the type as well as the domain in the label
        const label = `${domainToTitle(domain)} ${(domain === DomainType.PERSON ? `- ${typeToTitle(type)}` : '')} - Cohort Builder`;
        triggerEvent(category, 'Click', `${category} - ${label}`);
      }
      const itemId = generateId('items');
      tempGroup = tempGroup || 0;
      const item = this.initItem(itemId, domain, tempGroup);
      const fullTree = criteria.fullTree || false;
      const groupId = group.id;
      const context = {item, domain, type, standard, role, groupId, itemId, fullTree, tempGroup};
      wizardStore.next(context);
    }

    initItem(id: string, type: string, tempGroup: number) {
      return {
        id,
        type,
        searchParameters: [],
        modifiers: [],
        count: null,
        temporalGroup: tempGroup,
        status: 'active'
      };
    }

    setGroupProperty(property: string, value: any) {
      const {group, role, updateRequest} = this.props;
      const searchRequest = searchRequestStore.getValue();
      const groupIndex = searchRequest[role].findIndex(grp => grp.id === group.id);
      if (groupIndex > -1) {
        searchRequest[role][groupIndex][property] = value;
        searchRequestStore.next(searchRequest);
        updateRequest(true);
      }
    }

    handleTemporalChange(e: any) {
      const {value} = e.target;
      triggerEvent('Temporal', 'Click', 'Turn On Off - Temporal - Cohort Builder');
      this.setGroupProperty('temporal', value);
      if ((!value && this.hasActiveItems) || (value && !this.temporalError)) {
        this.checkPendingCalls();
        this.setState({error: false, loading: true});
        this.getGroupCount();
      }
    }

    setMention(mention: TemporalMention) {
      if (mention !== this.props.group.mention) {
        triggerEvent('Temporal', 'Click', `${formatOption(mention)} - Temporal - Cohort Builder`);
        this.setGroupProperty('mention', mention);
        this.calculateTemporal();
      }
    }

    setTime(time: TemporalTime) {
      const {group} = this.props;
      if (time !== this.props.group.time) {
        triggerEvent('Temporal', 'Click', `${formatOption(time)} - Temporal - Cohort Builder`);
        // prevents duplicate group count calls if switching from TemporalTime.DURINGSAMEENCOUNTERAS
        this.setState({preventInputCalculate: group.time === TemporalTime.DURINGSAMEENCOUNTERAS});
        this.setGroupProperty('time', time);
        this.calculateTemporal();
      }
    }

    setTimeValue(timeValue: number) {
      // prevents duplicate group count calls if changes is triggered by rendering of input
      if (!this.state.preventInputCalculate) {
        this.setGroupProperty('timeValue', timeValue);
        this.calculateTemporal();
      } else {
        this.setState({preventInputCalculate: false});
      }
    }

    calculateTemporal() {
      if (!this.temporalError) {
        this.checkPendingCalls();
        this.setState({error: false, loading: true});
        this.getGroupCount();
      }
    }

    get temporalError() {
      const {group: {items, time, timeValue}} = this.props;
      const counts = items.reduce((acc, it) => {
        if (it.status === 'active') {
          acc[it.temporalGroup]++;
        }
        return acc;
      }, [0, 0]);
      const inputError = time !== TemporalTime.DURINGSAMEENCOUNTERAS && (timeValue === null || timeValue < 0);
      return counts.includes(0) || inputError;
    }

    get validateInput() {
      const {group: {temporal, time}} = this.props;
      return temporal && time !== TemporalTime.DURINGSAMEENCOUNTERAS;
    }

    setMenuPosition() {
      const {index, role} = this.props;
      const id = role + index + '-button';
      const dropdown = document.getElementById(id).getBoundingClientRect();
      const position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
      this.setState({position});
    }

    render() {
      const {group: {id, items, mention, status, temporal, time, timeValue}, index, role} = this.props;
      const {count, criteriaMenuOptions: {domainTypes, programTypes}, error, loading, overlayStyle} = this.state;
      const domainMap = (domain) => {
        if (!!domain.children) {
          return {label: domain.name, items: domain.children.map(child => ({label: child.name, command: () => this.launchWizard(child)}))};
        } else {
          return {label: domain.name, command: () => this.launchWizard(domain)};
        }
      };
      const criteriaMenuItems = temporal
        ? domainTypes.map(domainMap)
        : [
          {label: 'Program Data', className: 'menuitem-header'},
          ...programTypes.map(domainMap),
          {separator: true},
          {label: 'Domains', className: 'menuitem-header'},
          ...domainTypes.map(domainMap)
        ];
      const groupMenuItems = [
        {label: 'Suppress group from total count', command: () => this.hide('hidden')},
        {label: 'Delete group', command: () => this.remove()},
      ];
      const mentionMenuItems = temporalMentions.map(tm => ({label: formatOption(tm), command: () => this.setMention(tm)}));
      const timeMenuItems = temporalTimes.map(tt => ({label: formatOption(tt), command: () => this.setTime(tt)}));
      return <React.Fragment>
        <style>
          {`
          .p-inputswitch.p-disabled > .p-inputswitch-slider {
            cursor: not-allowed;
          }
          body .p-menuitem > .p-menuitem-link {
            height: 1.25rem;
            line-height: 1.25rem;
            padding: 0 1rem;
          }
          body .p-menuitem.menuitem-header > .p-menuitem-link {
            font-size: 12px;
            font-weight: 600;
            height: auto;
            line-height: 0.75rem;
            padding-left: 0.5rem;
          }
          body .p-tieredmenu .p-menu-separator {
            margin: 0.25rem 0;
          }
          body .p-tieredmenu .p-submenu-list {
            width: 10rem;
          }
        `}
        </style>
        <div id={id} style={styles.card}>
          <div style={styles.cardHeader}>
            <Menu style={styles.menu} appendTo={document.body} model={groupMenuItems} popup ref={el => this.groupMenu = el} />
            <Clickable style={{display: 'inline-block', paddingRight: '0.5rem'}} onClick={(e) => this.groupMenu.toggle(e)}>
              <ClrIcon style={{color: colors.accent}} shape='ellipsis-vertical'/>
            </Clickable>
            Group {index + 1}
          </div>
          {temporal && <div style={styles.cardBlock}>
            <Menu style={styles.menu} appendTo={document.body} model={mentionMenuItems} popup ref={el => this.mentionMenu = el} />
            <button style={styles.menuButton} onClick={(e) => this.mentionMenu.toggle(e)}>
              {formatOption(mention)} <ClrIcon shape='caret down' size={12}/>
            </button>
            <span style={{fontSize: '14px', padding: '0.2rem 0.25rem 0'}}> of </span>
          </div>}
          {this.items.map((item, i) => <div key={i} style={styles.searchItem} data-test-id='item-list'>
            <SearchGroupItem role={role} groupId={id} item={item} index={i} updateGroup={(recalculate) => this.update(recalculate)}/>
            {status === 'active' && <div style={styles.itemOr}>OR</div>}
          </div>)}
          <div style={styles.cardBlock}>
            <TieredMenu style={{...styles.menu, padding: '0.5rem 0'}} appendTo={document.body}
              model={criteriaMenuItems} popup ref={el => this.criteriaMenu = el} />
            <button style={styles.menuButton} onClick={(e) => this.criteriaMenu.toggle(e)}>
              Add Criteria <ClrIcon shape='caret down' size={12}/>
            </button>
          </div>
          {temporal && <React.Fragment>
            <div style={styles.temporalSubCardHeader}>
              <Menu style={styles.menu} appendTo={document.body} model={timeMenuItems} popup ref={el => this.timeMenu = el} />
              <button style={styles.menuButton} onClick={(e) => this.timeMenu.toggle(e)}>
                {formatOption(time)} <ClrIcon shape='caret down' size={12}/>
              </button>
              {time !== TemporalTime.DURINGSAMEENCOUNTERAS &&
                <input style={styles.timeInput} type='number' value={timeValue}
                  onChange={(v) => this.setTimeValue(parseInt(v.target.value, 10))}/>
              }
            </div>
            {this.temporalItems.map((item, i) => <div key={i} style={styles.searchItem} data-test-id='temporal-item-list'>
              <SearchGroupItem role={role} groupId={id} item={item} index={i} updateGroup={(recalculate) => this.update(recalculate)}/>
              {status === 'active' && <div style={styles.itemOr}>OR</div>}
            </div>)}
            <div style={styles.temporalSubCardHeader}>
              <Menu style={styles.menu} appendTo={document.body} model={criteriaMenuItems}
                popup ref={el => this.temporalCriteriaMenu = el} />
              <button style={styles.menuButton} onClick={(e) => this.temporalCriteriaMenu.toggle(e)}>
                Add Criteria <ClrIcon shape='caret down' size={12}/>
              </button>
            </div>
          </React.Fragment>}
          {!!items.length && <div style={styles.cardHeader}>
            <div style={this.disableTemporal ? {...styles.row, cursor: 'not-allowed'} : styles.row}>
              <div style={{...styles.col6, display: 'flex'}}>
                <InputSwitch checked={temporal} disabled={this.disableTemporal} onChange={(e) => this.handleTemporalChange(e)}/>
                <div style={{paddingLeft: '0.5rem'}}>Temporal</div>
              </div>
              <div style={{...styles.col6, textAlign: 'right'}}>
                <div>
                  Group Count:&nbsp;
                  {loading && (!temporal || !this.temporalError) && <Spinner size={16}/>}
                  {!loading && <span>
                    {(!temporal || !this.temporalError) && this.hasActiveItems && !!count && count.toLocaleString()}
                    {(count === null || !this.hasActiveItems) && !temporal && <span>
                      -- <ClrIcon style={{color: colors.warning}} shape='warning-standard' size={21}/>
                    </span>}
                  </span>}
                  {!temporal && error &&
                    <ClrIcon className='is-solid' style={{color: colors.white}} shape='exclamation-triangle' size={22}/>
                  }
                  {temporal && this.temporalError && <span>
                    -- <ClrIcon style={{color: colors.warning}} shape='warning-standard' size={18}/>
                  </span>}
                </div>
              </div>
            </div>
          </div>}
        </div>
        {status !== 'active' && <div style={{...styles.overlay, ...overlayStyle}} data-test-id='disabled-overlay'>
          <div style={styles.overlayInner}>
            {status === 'pending' && <React.Fragment>
              <ClrIcon className='is-solid' shape='exclamation-triangle' size={56}/>
              <span>
                This group has been deleted
                <button style={styles.overlayButton} onClick={() => this.undo()}>UNDO</button>
              </span>
            </React.Fragment>}
            {status === 'hidden' && <React.Fragment>
              <ClrIcon className='is-solid' shape='eye-hide' size={56}/>
              <span>
                This group has been suppressed
                <button style={styles.overlayButton} onClick={() => this.enable()}>ENABLE</button>
              </span>
            </React.Fragment>}
          </div>
        </div>}
      </React.Fragment>;
    }
  }
);

@Component({
  selector: 'app-list-search-group',
  template: '<div #root></div>'
})
export class SearchGroupComponent extends ReactWrapperBase {
  @Input('group') group: Props['group'];
  @Input('index') index: Props['index'];
  @Input('role') role: Props['role'];
  @Input('updated') updated: Props['updated'];
  @Input('updateRequest') updateRequest: Props['updateRequest'];

  constructor() {
    super(SearchGroup, ['group', 'index', 'role', 'updated', 'updateRequest']);
  }
}
