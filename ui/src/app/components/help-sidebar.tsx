import {Component, Input} from '@angular/core';
import {faEdit} from '@fortawesome/free-regular-svg-icons';
import {
  faBook,
  faEllipsisV,
  faFolderOpen,
  faInbox,
  faInfoCircle
} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {faCircle} from '@fortawesome/free-solid-svg-icons/faCircle';
import {faSyncAlt} from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import {SelectionList} from 'app/cohort-search/selection-list/selection-list.component';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {PopupTrigger} from 'app/components/popups';
import {RuntimePanel} from 'app/pages/analysis/runtime-panel';
import {SidebarContent} from 'app/pages/data/cohort-review/sidebar-content.component';
import {ConceptListPage} from 'app/pages/data/concept/concept-list';
import {participantStore} from 'app/services/review-state.service';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  highlightSearchTerm,
  reactStyles,
  ReactWrapperBase,
  withCurrentCohortCriteria,
  withCurrentConcept,
  withCurrentWorkspace,
  withUserProfile
} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {
  currentCohortSearchContextStore,
  currentConceptStore,
  NavStore,
  serverConfigStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {RuntimeStore, runtimeStore, withStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';

import {Criteria, ParticipantCohortStatus, RuntimeStatus, WorkspaceAccessLevel} from 'generated/fetch';
import {Clickable, MenuItem, StyledAnchorTag} from './buttons';

const proIcons = {
  arrowLeft: '/assets/icons/arrow-left-regular.svg',
  runtime: '/assets/icons/thunderstorm-solid.svg',
  times: '/assets/icons/times-light.svg'
};
const sidebarContent = require('assets/json/help-sidebar.json');

const styles = reactStyles({
  sidebarContainer: {
    position: 'absolute',
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
    transition: 'width 0.5s ease-out',
    overflow: 'hidden',
    color: colors.primary,
    zIndex: -1,
  },
  notebookOverrides: {
    top: '0px',
    height: '100%'
  },
  sidebarContainerActive: {
    zIndex: 100,
  },
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '45px',
    height: '100%',
    overflow: 'auto',
    background: colorWithWhiteness(colors.primary, .87),
    transition: 'margin-right 0.5s ease-out, width 0.5s ease-out',
    boxShadow: `-10px 0px 10px -8px ${colorWithWhiteness(colors.dark, .5)}`,
  },
  sidebarOpen: {
    marginRight: 0,
  },
  iconContainer: {
    position: 'absolute',
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
    minHeight: 'calc(100vh - 156px)',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 101
  },
  icon: {
    background: colorWithWhiteness(colors.primary, 0.48),
    color: colors.white,
    display: 'table-cell',
    height: '46px',
    width: '45px',
    borderBottom: `1px solid ${colorWithWhiteness(colors.primary, 0.4)}`,
    cursor: 'pointer',
    textAlign: 'center',
    transition: 'background 0.2s linear',
    verticalAlign: 'middle'
  },
  runtimeStatusIcon: {
    width: '.5rem',
    height: '.5rem'
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.25rem',
  },
  runtimeStatusIconContainer: {
    alignSelf: 'flex-end',
    margin: '0 .1rem .1rem auto'
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  },
  navIcons: {
    position: 'absolute',
    right: '0',
    top: '0.75rem',
  },
  sectionTitle: {
    marginTop: '0.5rem',
    fontWeight: 600,
    color: colors.primary
  },
  contentTitle: {
    marginTop: '0.25rem',
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary
  },
  contentItem: {
    marginTop: 0,
    color: colors.primary
  },
  textSearch: {
    width: '100%',
    borderRadius: '4px',
    backgroundColor: colorWithWhiteness(colors.primary, .95),
    marginTop: '5px',
    color: colors.primary,
  },
  textInput: {
    width: '85%',
    height: '1.5rem',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    padding: '1.25rem 0.75rem',
    background: colorWithWhiteness(colors.primary, .8),
  },
  link: {
    color: colors.accent,
    cursor: 'pointer',
    textDecoration: 'none'
  },
  dropdownHeader: {
    fontSize: 12,
    lineHeight: '30px',
    color: colors.primary,
    fontWeight: 600,
    paddingLeft: 12,
    width: 160
  },
  menuButtonIcon: {
    width: 27,
    height: 27,
    opacity: 0.65,
    marginRight: 16
  },
  criteriaCount: {
    position: 'absolute',
    height: '0.8rem',
    width: '0.8rem',
    top: '1rem',
    left: '0.55rem',
    textAlign: 'center',
    backgroundColor: colors.danger,
    borderRadius: '50%',
    display: 'inline-block',
    fontSize: '0.4rem'
  },
  buttons: {
    paddingTop: '1rem',
    paddingLeft: '9rem',
    position: 'absolute'
  },
  backButton: {
    border: '0px',
    backgroundColor: 'none',
    color: colors.accent,
    marginRight: '1rem'
  }
});

const iconStyles = {
  active: {
    ...styles.icon,
    background: colorWithWhiteness(colors.primary, 0.55)
  },
  disabled: {
    ...styles.icon,
    cursor: 'not-allowed'
  },
  menu: {
    ...styles.icon,
    margin: '0.5rem auto 1.5rem',
    height: 27,
    width: 27
  }
};

export const NOTEBOOK_HELP_CONTENT = 'notebookStorage';

const iconConfigs = {
  'criteria': {
    disabled: false,
    faIcon: faInbox,
    label: 'Selected Criteria',
    page: 'criteria',
    style: {fontSize: '21px'},
    tooltip: 'Selected Criteria',
    contentPadding: '0.5rem 0.5rem 0',
    contentWidthRem: '20',
    hideSidebarFooter: true
  },
  'concept': {
    disabled: false,
    faIcon: faInbox,
    label: 'Selected Concepts',
    page: 'concept',
    style: {fontSize: '21px'},
    tooltip: 'Selected Concepts',
    contentPadding: '0.5rem 0.5rem 0',
    contentWidthRem: '20',
    hideSidebarFooter: true
  },
  'help': {
    disabled: false,
    faIcon: faInfoCircle,
    label: 'Help Icon',
    page: null,
    style: {fontSize: '21px'},
    tooltip: 'Help Tips',
  },
  'notebooksHelp': {
    disabled: false,
    faIcon: faFolderOpen,
    label: 'Storage Icon',
    page: null,
    style: {fontSize: '21px'},
    tooltip: 'Workspace Storage',
  },
  'dataDictionary': {
    disabled: false,
    faIcon: faBook,
    label: 'Data Dictionary Icon',
    page: null,
    style: {color: colors.white, fontSize: '20px', marginTop: '5px'},
    tooltip: 'Data Dictionary',
  },
  'annotations': {
    disabled: false,
    faIcon: faEdit,
    label: 'Annotations Icon',
    page: 'reviewParticipantDetail',
    style: {fontSize: '20px', marginLeft: '3px'},
    tooltip: 'Annotations',
  },
  'runtime': {
    disabled: false,
    faIcon: null,
    label: 'Cloud Icon',
    page: null,
    style: {height: '22px', width: '22px', marginTop: '0.25rem'},
    tooltip: 'Compute Configuration',
    contentPadding: '1.25rem',
    contentWidthRem: '30',
    hideSidebarFooter: true
  }
};

const helpIconName = (helpContentKey: string) => {
  return helpContentKey === NOTEBOOK_HELP_CONTENT ? 'notebooksHelp' : 'help';
};

const icons = (
  helpContentKey: string,
  enableCustomRuntimes: boolean,
  workspaceAccessLevel: WorkspaceAccessLevel
) => {
  const keys = [
    'criteria',
    'concept',
    helpIconName(helpContentKey),
    'dataDictionary',
    'annotations'
  ];
  if (enableCustomRuntimes && WorkspacePermissionsUtil.canWrite(workspaceAccessLevel)) {
    keys.push('runtime');
  }
  return keys.map(k => ({...iconConfigs[k], id: k}));
};

const analyticsLabels = {
  about: 'About Page',
  cohortBuilder: 'Cohort Builder',
  conceptSets: 'Concept Set',
  data: 'Data Landing Page',
  datasetBuilder: 'Dataset Builder',
  notebooks: 'Analysis Tab Landing Page',
  reviewParticipants: 'Review Participant List',
  reviewParticipantDetail: 'Review Individual',
};

interface Props {
  deleteFunction: Function;
  helpContentKey: string;
  profileState: any;
  setSidebarState: Function;
  shareFunction: Function;
  sidebarOpen: boolean;
  notebookStyles: boolean;
  workspace: WorkspaceData;
  criteria: Array<Selection>;
  concept?: Array<Criteria>;
  currentRuntimeStore: RuntimeStore;
}

interface State {
  activeIcon: string;
  filteredContent: Array<any>;
  participant: ParticipantCohortStatus;
  searchTerm: string;
  showCriteria: boolean;
  tooltipId: number;
}

export const HelpSidebar = fp.flow(
  withCurrentCohortCriteria(),
  withCurrentConcept(),
  withCurrentWorkspace(),
  withStore(runtimeStore, 'currentRuntimeStore'),
  withUserProfile()
)(
  class extends React.Component<Props, State> {
    subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        // TODO(RW-5607): Remember which icon was active.
        activeIcon: props.sidebarOpen ? helpIconName(props.helpContentKey) : undefined,
        filteredContent: undefined,
        participant: undefined,
        searchTerm: '',
        showCriteria: false,
        tooltipId: undefined
      };
    }

    debounceInput = fp.debounce(300, (input: string) => {
      if (input.length < 3) {
        this.setState({filteredContent: undefined});
      } else {
        this.searchHelpTips(input.trim().toLowerCase());
      }
    });

    async componentDidMount() {
      this.subscription = participantStore.subscribe(participant => this.setState({participant}));
      this.subscription.add(setSidebarActiveIconStore.subscribe(activeIcon => {
        if (activeIcon !== null) {
          this.setState({activeIcon});
          this.props.setSidebarState(!!activeIcon);
        }
      }));
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      // close the sidebar on each navigation excluding navigating between participants in cohort review
      if (!this.props.sidebarOpen && prevProps.sidebarOpen) {
        setTimeout(() => {
          // check if the sidebar has been opened again before resetting activeIcon
          if (!this.props.sidebarOpen) {
            this.setState({activeIcon: undefined});
          }
        }, 300);
      }
      if ((!this.props.criteria && !!prevProps.criteria ) || (!this.props.concept && !!prevProps.concept)) {
        this.props.setSidebarState(false);
      }
    }

    componentWillUnmount(): void {
      this.subscription.unsubscribe();
    }

    searchHelpTips(input: string) {
      this.analyticsEvent('Search');
      // For each object, we check the title first. If it matches, we return the entire content array.
      // If the title doesn't match, we check each element of the content array for matches
      const filteredContent = fp.values(JSON.parse(JSON.stringify(sidebarContent))).reduce((acc, section) => {
        const inputMatch = (text: string) => text.toLowerCase().includes(input);
        const content = section.reduce((ac, item) => {
          if (!inputMatch(item.title)) {
            item.content = item.content.reduce((a, ic) => {
              if (typeof ic === 'string') {
                if (inputMatch(ic)) {
                  a.push(ic);
                }
              } else if (inputMatch(ic.title)) {
                a.push(ic);
              } else {
                ic.content = ic.content.filter(inputMatch);
                if (ic.content.length) {
                  a.push(ic);
                }
              }
              return a;
            }, []);
          }
          return item.content.length ? [...ac, item] : ac;
        }, []);
        return [...acc, ...content];
      }, []);
      this.setState({filteredContent});
    }

    onIconClick(icon: any) {
      const {setSidebarState, sidebarOpen} = this.props;
      const {activeIcon} = this.state;
      const {id, label} = icon;
      const newSidebarOpen = !(id === activeIcon && sidebarOpen);
      if (newSidebarOpen) {
        this.setState({activeIcon: id});
        setSidebarState(true);
        this.analyticsEvent('OpenSidebar', `Sidebar - ${label}`);
      } else {
        setSidebarState(false);
      }
    }

    onInputChange(value: string) {
      this.setState({searchTerm: value});
      this.debounceInput(value);
    }

    openContactWidget() {
      const {profileState: {profile: {contactEmail, familyName, givenName, username}}} = this.props;
      this.analyticsEvent('ContactUs');
      openZendeskWidget(givenName, familyName, username, contactEmail);
    }

    highlightMatches(content: string) {
      const {searchTerm} = this.state;
      return highlightSearchTerm(searchTerm, content, colors.success);
    }

    analyticsEvent(type: string, label?: string) {
      const {helpContentKey} = this.props;
      const analyticsLabel = analyticsLabels[helpContentKey];
      if (analyticsLabel) {
        const eventLabel = label ? `${label} - ${analyticsLabel}` : analyticsLabel;
        AnalyticsTracker.Sidebar[type](eventLabel);
      }
    }

    sidebarContainerStyles(activeIcon, notebookStyles) {
      const sidebarContainerStyle = {
        ...styles.sidebarContainer,
        width: `calc(${this.sidebarWidth}rem + 55px)`
      };
      if (notebookStyles) {
        if (activeIcon) {
          return {...sidebarContainerStyle, ...styles.notebookOverrides, ...styles.sidebarContainerActive};
        } else {
          return {...sidebarContainerStyle, ...styles.notebookOverrides};
        }
      } else {
        if (activeIcon) {
          return {...sidebarContainerStyle, ...styles.sidebarContainerActive};
        } else {
          return sidebarContainerStyle;
        }
      }
    }

    renderWorkspaceMenu() {
      const {deleteFunction, shareFunction, workspace, workspace: {accessLevel, id, namespace}} = this.props;
      const isNotOwner = !workspace || accessLevel !== WorkspaceAccessLevel.OWNER;
      const tooltip = isNotOwner && 'Requires owner permission';
      return <PopupTrigger
        side='bottom'
        closeOnClick
        content={
          <React.Fragment>
            <div style={styles.dropdownHeader}>Workspace Actions</div>
            <MenuItem
              icon='copy'
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenDuplicatePage();
                NavStore.navigate(['/workspaces', namespace, id, 'duplicate']);
              }}>
              Duplicate
            </MenuItem>
            <MenuItem
              icon='pencil'
              tooltip={tooltip}
              disabled={isNotOwner}
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenEditPage();
                NavStore.navigate(['/workspaces', namespace, id, 'edit']);
              }}>
              Edit
            </MenuItem>
            <MenuItem
              icon='share'
              tooltip={tooltip}
              disabled={isNotOwner}
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenShareModal();
                shareFunction();
              }}>
              Share
            </MenuItem>
            <MenuItem
              icon='trash'
              tooltip={tooltip}
              disabled={isNotOwner}
              onClick={() => {
                AnalyticsTracker.Workspaces.OpenDeleteModal();
                deleteFunction();
              }}>
              Delete
            </MenuItem>
          </React.Fragment>
        }>
        <div data-test-id='workspace-menu-button'>
          <TooltipTrigger content={<div>Menu</div>} side='left'>
            <div style={styles.icon} onClick={() => this.analyticsEvent('OpenSidebar', 'Sidebar - Menu Icon')}>
              <FontAwesomeIcon icon={faEllipsisV} style={{fontSize: '21px'}}/>
            </div>
          </TooltipTrigger>
        </div>
      </PopupTrigger>;
    }

    showIcon(icon) {
      const {concept, criteria, helpContentKey} = this.props;
      return !icon.page || icon.page === helpContentKey || (criteria && icon.page === 'criteria') || (concept && icon.page === 'concept');
    }

    displayFontAwesomeIcon(icon) {
      const {concept, criteria} = this.props;

      return <React.Fragment>
        {(criteria && icon.page === 'criteria' && criteria.length > 0) && <span data-test-id='criteria-count'
                                                         style={styles.criteriaCount}>
          {criteria.length}</span>}
        {(concept && icon.page === 'concept' && concept.length > 0) && <span data-test-id='concept-count'
                                                         style={styles.criteriaCount}>
          {concept.length}</span>}
            <FontAwesomeIcon data-test-id={'help-sidebar-icon-' + icon.id} icon={icon.faIcon} style={icon.style} />
          </React.Fragment> ;
    }

    displayRuntimeIcon(icon) {
      const {currentRuntimeStore} = this.props;
      const status = currentRuntimeStore && currentRuntimeStore.runtime && currentRuntimeStore.runtime.status;

      // We always want to show the thunderstorm icon.
      // For most runtime statuses (Deleting and Unknown currently excepted), we will show a small
      // overlay icon in the bottom right of the tab showing the runtime status.
      return <FlexRow style={{height: '100%', alignItems: 'center', justifyContent: 'space-around'}}>
        <img data-test-id={'help-sidebar-icon-' + icon.id} src={proIcons[icon.id]} style={{...icon.style, position: 'absolute'}} />
        <FlexRow style={styles.runtimeStatusIconContainer}>
          {(status === RuntimeStatus.Creating
          || status === RuntimeStatus.Starting
          || status === RuntimeStatus.Updating)
            && <FontAwesomeIcon icon={faSyncAlt} style={{
              ...styles.runtimeStatusIcon,
              ...styles.rotate,
              color: colors.runtimeStatus.starting,
            }}/>
          }
          {status === RuntimeStatus.Stopped
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.runtimeStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.runtimeStatus.stopped,
            }}/>
          }
          {status === RuntimeStatus.Running
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.runtimeStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.runtimeStatus.running,
            }}/>
          }
          {(status === RuntimeStatus.Stopping
          || status === RuntimeStatus.Deleting)
            && <FontAwesomeIcon icon={faSyncAlt} style={{
              ...styles.runtimeStatusIcon,
              ...styles.rotate,
              color: colors.runtimeStatus.stopping,
            }}/>
          }
          {status === RuntimeStatus.Error
            && <FontAwesomeIcon icon={faCircle} style={{
              ...styles.runtimeStatusIcon,
              ...styles.runtimeStatusIconOutline,
              color: colors.runtimeStatus.error,
            }}/>
          }
        </FlexRow>
      </FlexRow>;
    }

    get sidebarStyle() {
      const sidebarStyle = {
        ...styles.sidebar,
        marginRight: `calc(-${this.sidebarWidth}rem - 40px)`,
        width: `${this.sidebarWidth}.5rem`,
      };
      return this.props.sidebarOpen ? {...sidebarStyle, ...styles.sidebarOpen} : sidebarStyle;
    }

    get sidebarWidth() {
      if (this.state.activeIcon && iconConfigs[this.state.activeIcon].contentWidthRem) {
        return iconConfigs[this.state.activeIcon].contentWidthRem;
      }
      return '14';
    }

    render() {
      const {concept, criteria, helpContentKey, notebookStyles, setSidebarState, workspace} = this.props;
      const {activeIcon, filteredContent, participant, searchTerm, tooltipId} = this.state;
      const displayContent = filteredContent !== undefined ? filteredContent : sidebarContent[helpContentKey];

      const contentStyle = (tab: string) => ({
        height: 'calc(100% - 1rem)',
        overflow: 'auto',
        padding: iconConfigs[tab].contentPadding || '0.5rem 0.5rem 5.5rem'
      });
      return <div id='help-sidebar'>
        <div style={notebookStyles ? {...styles.iconContainer, ...styles.notebookOverrides} : {...styles.iconContainer}}>
          {!(criteria || concept)  && this.renderWorkspaceMenu()}
          {icons(helpContentKey, serverConfigStore.getValue().enableCustomRuntimes, workspace.accessLevel).map((icon, i) =>
          this.showIcon(icon) && <div key={i} style={{display: 'table'}}>
                <TooltipTrigger content={<div>{tooltipId === i && icon.tooltip}</div>} side='left'>
                  <div style={activeIcon === icon.id ? iconStyles.active : icon.disabled ? iconStyles.disabled : styles.icon}
                       onClick={() => {
                         if (icon.id !== 'dataDictionary' && !icon.disabled) {
                           this.onIconClick(icon);
                         }
                       }}
                       onMouseOver={() => this.setState({tooltipId: i})}
                       onMouseOut={() => this.setState({tooltipId: undefined})}>
                    {icon.id === 'dataDictionary'
                      ? <a href={supportUrls.dataDictionary} target='_blank'>
                          <FontAwesomeIcon data-test-id={'help-sidebar-icon-' + icon.id} icon={icon.faIcon} style={icon.style} />
                        </a>
                      : icon.id === 'runtime'
                        ? this.displayRuntimeIcon(icon)
                        : icon.faIcon === null
                          ? <img data-test-id={'help-sidebar-icon-' + icon.id} src={proIcons[icon.id]} style={icon.style} />
                          : this.displayFontAwesomeIcon(icon)
                    }
                  </div>
                </TooltipTrigger>
              </div>
            )
          }
        </div>
        <div style={this.sidebarContainerStyles(activeIcon, notebookStyles)}>
          <div style={this.sidebarStyle} data-test-id='sidebar-content'>
            {!(activeIcon === 'criteria' || activeIcon === 'concept') && <FlexRow style={styles.navIcons}>
              <Clickable style={{marginRight: '1rem'}}
                         onClick={() => setSidebarState(false)}>
                <img src={proIcons.times}
                     style={{height: '27px', width: '17px'}}
                     alt='Close'/>
              </Clickable>
            </FlexRow>}
            {activeIcon === helpIconName(helpContentKey) && <div style={contentStyle(helpIconName(helpContentKey))}>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>{helpContentKey === NOTEBOOK_HELP_CONTENT ? 'Workspace storage' : 'Help Tips'}</h3>
              {helpContentKey !== NOTEBOOK_HELP_CONTENT &&
                <div style={styles.textSearch}>
                  <ClrIcon style={{color: colors.primary, margin: '0 0.25rem'}} shape='search' size={16} />
                  <input
                    type='text'
                    style={styles.textInput}
                    value={searchTerm}
                    onChange={(e) => this.onInputChange(e.target.value)}
                    placeholder={'Search'} />
                </div>
              }
              {!!displayContent && displayContent.length > 0
                ? displayContent.map((section, s) => <div key={s}>
                    <h3 style={styles.sectionTitle} data-test-id={`section-title-${s}`}>{this.highlightMatches(section.title)}</h3>
                    {section.content.map((content, c) => {
                      return typeof content === 'string'
                          ? <p key={c} style={styles.contentItem}>{this.highlightMatches(content)}</p>
                          : <div key={c}>
                            <h4 style={styles.contentTitle}>{this.highlightMatches(content.title)}</h4>
                            {content.content.map((item, i) =>
                                <p key={i} style={styles.contentItem}>{this.highlightMatches(item)}</p>
                            )}
                          </div>;
                    })}
                  </div>)
                : <div style={{marginTop: '0.5rem'}}><em>No results found</em></div>
              }
            </div>}
            {activeIcon === 'runtime' && <div style={contentStyle('runtime')}>
              {<RuntimePanel onUpdate={() => this.props.setSidebarState(false)}/>}
            </div>}
            {activeIcon === 'annotations' && <div style={contentStyle('annotations')}>
              {participant && <SidebarContent />}
            </div>}
            {activeIcon === 'criteria' && <div style={contentStyle('criteria')}>
              <div style={{height: '100%', padding: '0.25rem 0.25rem 0rem'}}>
                {!!currentCohortSearchContextStore.getValue() && <SelectionList back={() => setSidebarState(false)} selections={[]}/>}
              </div>
            </div>}
            {activeIcon === 'concept' && <div style={contentStyle('concept')}>
              <div style={{padding: '0.25rem 0.25rem 0rem'}}>
                {!!currentConceptStore.getValue() && <ConceptListPage/>}
              </div>
            </div>}
            {(iconConfigs[activeIcon] || {}).hideSidebarFooter || <div style={styles.footer}>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>Not finding what you're looking for?</h3>
              <p style={styles.contentItem}>
                Visit our <StyledAnchorTag href={supportUrls.helpCenter}
                                           target='_blank' onClick={() => this.analyticsEvent('UserSupport')}> User Support
                </StyledAnchorTag> page or <span style={styles.link} onClick={() => this.openContactWidget()}> contact us</span>.
              </p>
            </div>}
          </div>
        </div>
      </div>;
    }
  }
);

@Component({
  selector: 'app-help-sidebar',
  template: '<div #root></div>',
})
export class HelpSidebarComponent extends ReactWrapperBase {
  @Input('deleteFunction') deleteFunction: Props['deleteFunction'];
  @Input('helpContentKey') helpContentKey: Props['helpContentKey'];
  @Input('setSidebarState') setSidebarState: Props['setSidebarState'];
  @Input('shareFunction') shareFunction: Props['shareFunction'];
  @Input('sidebarOpen') sidebarOpen: Props['sidebarOpen'];
  @Input('notebookStyles') notebookStyles: Props['notebookStyles'];
  constructor() {
    super(HelpSidebar, ['deleteFunction', 'helpContentKey', 'setSidebarState', 'shareFunction', 'sidebarOpen', 'notebookStyles']);
  }
}
