import {Component} from "@angular/core";
import * as fp from 'lodash/fp';
import * as React from 'react';

import {SpinnerOverlay} from "app/components/spinners";
import {tabs, WorkspaceNavBar} from "app/pages/workspace/workspace-nav-bar";
import {ReactWrapperBase, withUrlParams, withUserProfile} from 'app/utils';
import {currentWorkspaceStore, navigate, routeConfigDataStore} from 'app/utils/navigation';
import {Profile, ResourceType, UserRole, Workspace, WorkspaceAccessLevel} from "generated/fetch";
import {ConfirmDeleteModal} from "app/components/confirm-delete-modal";
import {workspacesApi} from "app/services/swagger-fetch-clients";
import {WorkspaceDeletionErrorModal} from "./workspace-deletion-error-modal";
import {BugReportModal} from "app/components/bug-report";
import {WorkspaceShare} from "app/pages/workspace/workspace-share";
import {HelpSidebar} from "app/components/help-sidebar";
import {WorkspaceData} from "app/utils/workspace-data";
import {DataPage, DataPageComponent} from "../data/data-page";
import {NotebookList} from "../analysis/notebook-list";
import {WorkspaceAbout} from "./workspace-about";

const LOCAL_STORAGE_KEY_SIDEBAR_STATE = 'WORKSPACE_SIDEBAR_STATE';

interface Props {
  profileState: {
    profile: Profile,
    reload: Function
  }
  urlParams: {
    ns: string,
    wsid: string
  }
}

interface State {
  bugReportOpen: boolean;
  deleting: boolean;
  displayNavBar: boolean;
  helpContent: string;
  loading: boolean;
  sharing: boolean;
  sidebarOpen: boolean;
  tabPath: string;
  userRoles: Array<UserRole>;
  workspace: WorkspaceData;
  workspaceDeletionError: boolean;
}

export const WorkspaceWrapper = fp.flow(withUrlParams(), withUserProfile())
(class extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      bugReportOpen: false,
      deleting: false,
      displayNavBar: false,
      helpContent: '',
      loading: false,
      sharing: false,
      sidebarOpen: false,
      tabPath: '',
      userRoles: [],
      workspace: undefined,
      workspaceDeletionError: false
    };
    this.onCloseShare = this.onCloseShare.bind(this);
    this.onWorkspaceDeletionErrorModalClose = this.onWorkspaceDeletionErrorModalClose.bind(this);
    this.setSidebarState = this.setSidebarState.bind(this);
    this.setTabPath = this.setTabPath.bind(this);
    this.submitDeletionErrorBugReport = this.submitDeletionErrorBugReport.bind(this);
  }

  componentDidMount() {
    const {ns, wsid} = this.props.urlParams;
    workspacesApi().getWorkspace(ns, wsid)
    .then(response => {
      // All this stuff has to be nested because nothing below it works if we don't have the ws.
      const workspace = {...response.workspace, accessLevel: response.accessLevel};
      this.setState({
        workspace: workspace
      });
      currentWorkspaceStore.next(workspace);

      const sidebarState = localStorage.getItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE);
      if (!!sidebarState) {
        this.setState({sidebarOpen: sidebarState === 'open'});
      } else {
        // Default the sidebar to open if no localStorage value is set
        this.setSidebarState(true);
      }

      this.setState({
        displayNavBar: !routeConfigDataStore.getValue().minimizeChrome,
        tabPath: this.getTabPath()
      });

      this.setHelpContent();
    })
  }

  delete(workspace: Workspace): void {
    this.setState({deleting: true});
    workspacesApi().deleteWorkspace(
        workspace.namespace, workspace.id).then(() => {
      navigate(['/workspaces']);
    }).catch(() => {
      this.setState({workspaceDeletionError: true});
    });
  }

  getTabPath() {
    const pathComponents = window.location.pathname.split('/');
    const lastPath = pathComponents[pathComponents.length - 1];
    const tabLinks = fp.map(tab => tab.link, tabs);
    if (tabLinks.includes(lastPath)) {
      return lastPath;
    } else {
      return '';
    }
  }

  onCloseShare() {
    this.setState({sharing: false});
  }

  async onShare() {
    const {workspace} = this.state;

    this.setState({loading: true});

    await workspacesApi().getFirecloudWorkspaceUserRoles(workspace.namespace, workspace.id)
      .then(response => this.setState({
        userRoles: response.items
      }))
      .catch(() => this.setState({loading: false}));
  }

  onWorkspaceDeletionErrorModalClose() {
    this.setState({workspaceDeletionError: false});
  }

  // FIXME: This DEFINITELY shouldn't use magic everythings, and it won't need to when we write a
  // React router.
  setHelpContent() {
    debugger;
    const pathComponents = window.location.pathname.split('/');
    // The first four elements of this array will always be empty string (window.location.pathname
    // always starts with a slash), 'workspaces', wsid, ws name.
    pathComponents.splice(0, 4);
    // in order to repent for this i promise i will research a react router. i am very sorry.
    switch (pathComponents.shift()) {
      case "about":
        this.setState({helpContent: "about"});
        break;
      case "edit":
        this.setState({helpContent: "edit"});
        break;
      case "duplicate":
        this.setState({helpContent: "duplicate"});
        break;
      case "notebooks": {
        if (pathComponents.length > 0 || pathComponents.shift() === "preview") {
          this.setState({helpContent: "notebooks"});
        }
        break;
      }
      case "data": {
        if (pathComponents.length === 0) {
          this.setState({helpContent: "data"});
          break;
        }
        // i did say i was sorry.
        else {
          switch (pathComponents.shift()) {
            case "data-sets":
              this.setState({helpContent: "datasetBuilder"});
              break;
            case "cohorts": {
              // If we're looking at a specific cohort
              if (pathComponents.length > 0) {
                // Get rid of cohort id
                pathComponents.shift();
                // ‾\_(ツ)_/‾
                switch (pathComponents.shift()) {
                  case "actions":
                    this.setState({helpContent: "cohortBuilder"});
                    break;
                  case "review": {
                    if (pathComponents.length === 0 || pathComponents[1] === "participants") {
                      this.setState({helpContent: "reviewParticipants"});
                      break;
                    } else {
                      this.setState({helpContent: "cohortDescription"});
                      break;
                    }
                  }
                  case "default":
                    this.setState({helpContent: ""});
                    break;
                }
              }
            }
            case "concepts":
              this.setState({helpContent: "conceptSets"});
              break;
            case "default":
              this.setState({helpContent: ""});
              break;
          }
        }
      }
      case "default":
        this.setState({helpContent: ""});
        break;
    }
  }

  setSidebarState(sidebarOpen: boolean) {
    this.setState({sidebarOpen: sidebarOpen});
    const sidebarState = sidebarOpen ? 'open' : 'closed';
    localStorage.setItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE, sidebarState);
  };

  setTabPath(tabPath) {
    this.setState({tabPath: tabPath})
  }

  submitDeletionErrorBugReport() {
    this.setState({
      bugReportOpen: true,
      workspaceDeletionError: false});
  }

  // FIXME: This probably shouldn't use magic numbers and strings, and it won't need to when we
  // write a React router
  useNotebookSidebarStyles() {
    const pathComponents = window.location.pathname.split('/');
    return pathComponents[4] === "notebooks";
  }

  render() {
    const {
      bugReportOpen,
      deleting,
      displayNavBar,
      helpContent,
      loading,
      sidebarOpen,
      sharing,
      tabPath,
      userRoles,
      workspace,
      workspaceDeletionError
    } = this.state;
    return <React.Fragment>
      {displayNavBar && <WorkspaceNavBar
          tabPath={tabPath}
          setTabPath={this.setTabPath}
      />}
      {loading && <SpinnerOverlay/>}
      {workspace && <React.Fragment>
        {deleting && <ConfirmDeleteModal
            closeFunction={() => this.setState({deleting: false})}
            resourceType={ResourceType.WORKSPACE}
            receiveDelete={() => this.delete(workspace)}
            resourceName={workspace.name}

        />}

        {workspaceDeletionError && <WorkspaceDeletionErrorModal
            onClose={this.onWorkspaceDeletionErrorModalClose}
            onSubmitBugReport={this.submitDeletionErrorBugReport}
            workspace={workspace}
        />}
        {bugReportOpen && <BugReportModal
            bugReportDescription={"Could not delete workspace."}
            onClose={() => this.setState({bugReportOpen: false})}
        />}

        {sharing && <WorkspaceShare
            onClose={this.onCloseShare}
            workspace={workspace}
            accessLevel={workspace.accessLevel}
            userRoles={userRoles}
            userEmail={this.props.profileState.profile.username}
        />}

        {helpContent && <HelpSidebar
          deleteFunction={() => this.setState({deleting: true})}
          helpContent={helpContent}
          profileState={this.props.profileState}
          setSidebarState={this.setSidebarState}
          shareFunction={this.onShare}
          sidebarOpen={sidebarOpen}
          notebookStyles={this.useNotebookSidebarStyles()}
          workspace={workspace}
        />}

        {/* FIXME: Once we have a React router, this should probably change - but I don't know how
            it will yet. Angular router used to fill this space with workspace components. */}
        {tabPath === 'data' && <DataPage
            workspace={workspace}
        />}
        {tabPath === 'notebooks' && <NotebookList
            workspace={workspace}
        />}
        {tabPath === 'about' && <WorkspaceAbout/>}

      </React.Fragment>}
    </React.Fragment>
  }

});

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class WorkspaceWrapperComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceWrapper, []);
  }
}