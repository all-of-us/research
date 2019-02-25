import {Component, Input} from '@angular/core';

import {Clickable, MenuItem} from 'app/components/buttons';
import {PopupTrigger} from 'app/components/popups';
import {CardMenuIconComponentReact} from 'app/icons/card-menu-icon/component';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {withCurrentWorkspace} from 'app/utils/index';
import {NavStore} from 'app/utils/navigation';
import {WorkspaceAccessLevel} from 'generated';
import {environment} from 'environments/environment';

import * as fp from 'lodash/fp';
import * as React from 'react';


const styles = reactStyles({
  container: {
    display: 'flex', alignItems: 'center', backgroundColor: colors.blue[1],
    fontWeight: 500, color: 'white', textTransform: 'uppercase',
    height: 60, paddingRight: 16,
    boxShadow: 'inset rgba(0, 0, 0, 0.12) 0px 3px 2px 0px',
    width: 'calc(100% + 1.2rem)',
    marginLeft: '-0.6rem',
    paddingLeft: 80, borderBottom: `5px solid ${colors.blue[0]}`, flex: 'none'
  },
  tab: {
    minWidth: 140, flexGrow: 0, padding: '0 20px',
    color: colors.gray[5],
    alignSelf: 'stretch', display: 'flex', justifyContent: 'center', alignItems: 'center'
  },
  active: {
    backgroundColor: 'rgba(255,255,255,0.15)', color: 'unset',
    borderBottom: `4px solid ${colors.blue[0]}`, fontWeight: 'bold'
  },
  separator: {
    background: 'rgba(255,255,255,0.15)', width: 1, height: 48, flexShrink: 0
  },
  dropdownHeader: {
    fontSize: 12,
    lineHeight: '30px',
    color: '#262262',
    fontWeight: 600,
    paddingLeft: 12,
    width: 160
  },
  menuButtonIcon: {
    width: 27, height: 27,
    opacity: 0.65, marginRight: 16
  }
});

const tabs = [
  {name: 'About', link: ''},
  ...(environment.enableDatasetBuilder ? [{name: 'Data', link: 'data'}] : []),
  {name: 'Cohorts', link: 'cohorts'},
  {name: 'Concepts', link: 'concepts'},
  {name: 'Notebooks', link: 'notebooks'},
];

const navSeparator = <div style={styles.separator}/>;

export const WorkspaceNavBarReact = withCurrentWorkspace()(props => {
  const {shareFunction, deleteFunction, workspace, tabPath} = props;
  const {namespace, id, accessLevel} = workspace;
  const isNotOwner = accessLevel !== WorkspaceAccessLevel.OWNER;
  const activeTabIndex = fp.findIndex(['link', tabPath], tabs);


  const navTab = currentTab => {
    const {name, link} = currentTab;
    const selected = tabPath === link;
    const hideSeparator = selected || (activeTabIndex === tabs.indexOf(currentTab) + 1);

    return <React.Fragment key={name}>
      <Clickable
        data-test-id={name}
        aria-selected={selected}
        style={{...styles.tab, ...(selected ? styles.active : {})}}
        hover={{color: styles.active.color}}
        onClick={() => NavStore.navigate(fp.compact(['/workspaces', namespace, id, link]))}
      >
        {name}
      </Clickable>
      {!hideSeparator && navSeparator}
    </React.Fragment>;
  };

  return <div id='workspace-top-nav-bar' className='do-not-print' style={styles.container}>
    {activeTabIndex > 0 && navSeparator}
    {fp.map(tab => navTab(tab), tabs)}
    <div style={{flexGrow: 1}}/>
    <PopupTrigger
      side='bottom'
      closeOnClick={true}
      content={
        <React.Fragment>
          <div style={styles.dropdownHeader}>Workspace Actions</div>
          <MenuItem
            icon='copy'
            onClick={() => NavStore.navigate(['/workspaces', namespace, id, 'clone'])}>
            Duplicate
          </MenuItem>
          <MenuItem
            icon='pencil'
            tooltip={isNotOwner && 'Requires owner permission'}
            disabled={isNotOwner}
            onClick={() => NavStore.navigate(['/workspaces', namespace, id, 'edit'])}
          >
            Edit
          </MenuItem>
          <MenuItem
            icon='share'
            tooltip={isNotOwner && 'Requires owner permission'}
            disabled={isNotOwner}
            onClick={() => shareFunction()}>
            Share
          </MenuItem>
          <MenuItem
            icon='trash'
            tooltip={isNotOwner && 'Requires owner permission'}
            disabled={isNotOwner}
            onClick={() => deleteFunction()}>
            Delete
          </MenuItem>
        </React.Fragment>
      }>
      <Clickable
        data-test-id='workspace-menu-button'
        style={styles.menuButtonIcon}
        hover={{opacity: 1}}
      >
        <CardMenuIconComponentReact/>
      </Clickable>
    </PopupTrigger>
  </div>;
});

@Component({
  selector: 'app-workspace-nav-bar',
  styleUrls: ['./component.css'],
  template: '<div #root></div>',
})
export class WorkspaceNavBarComponent extends ReactWrapperBase {
  @Input() shareFunction;
  @Input() deleteFunction;
  @Input() tabPath;

  constructor() {
    super(WorkspaceNavBarReact, ['shareFunction', 'deleteFunction', 'tabPath']);
  }
}
