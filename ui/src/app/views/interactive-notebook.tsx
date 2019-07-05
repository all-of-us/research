import {Component, Input} from '@angular/core';
import * as Cookies from 'js-cookie';
import * as fp from 'lodash';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit';
import {PlaygroundModeIcon} from 'app/icons/playground-mode-icon';
import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {navigate, queryParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {ConfirmPlaygroundModeModal} from 'app/views/confirm-playground-mode-modal';
import {ClusterStatus} from 'generated/fetch';


const styles = reactStyles({
  navBar: {
    display: 'flex',
    height: 40,
    backgroundColor: colors.white,
    border: 'solid thin ' + colorWithWhiteness(colors.dark, .75),
    fontSize: '16px'
  },
  navBarItem: {
    display: 'flex',
    height: '100%',
    color: colors.primary,
    borderRight: '1px solid ' + colorWithWhiteness(colors.dark, .75),
    backgroundColor: 'rgba(38,34,98,0.05)',
    alignItems: 'center',
    padding: '0 20px'
  },
  active: {
    backgroundColor: 'rgba(38,34,98,0.2)'
  },
  clickable: {
    cursor: 'pointer'
  },
  disabled: {
    cursor: 'not-allowed',
    opacity: 0.7
  },
  navBarIcon: {
    height: '16px',
    width: '16px',
    marginLeft: 0,
    marginRight: '5px'
  },
  previewFrame: {
    width: '100%',
    height: 800,
    border: 0
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  }
});

interface Props {
  workspace: WorkspaceData;
  billingProjectId: string;
  workspaceName: string;
  notebookName: string;
}

interface State {
  clusterStatus: ClusterStatus;
  userRequestedExecutableNotebook: boolean;
  html: string;
  showPlaygroundModeModal: boolean;
}

export const Modes = {
  EDIT: 'edit',
  PLAYGROUND: 'playground'
};

export const InteractiveNotebook = fp.flow(withUrlParams(), withCurrentWorkspace())(
  class extends React.Component<Props, State> {

    constructor(props) {
      super(props);
      this.state = {
        userRequestedExecutableNotebook: false,
        clusterStatus: ClusterStatus.Unknown,
        html: '',
        showPlaygroundModeModal: false
      };
    }

    componentDidMount(): void {
      workspacesApi().readOnlyNotebook(this.props.billingProjectId,
        this.props.workspaceName, this.props.notebookName)
        .then(html => {
          this.setState({html: html.html});
        });

      const startMode = queryParamsStore.getValue().startMode;
      if (startMode === Modes.EDIT) {
        this.startEditMode();
      } else if (startMode === Modes.PLAYGROUND) {
        this.onPlaygroundModeClick();
      }
    }

    private runCluster(onClusterReady: Function): void {
      const retry = () => {
        setTimeout(() => this.runCluster(onClusterReady), 5000);
      };

      clusterApi().listClusters(this.props.billingProjectId)
        .then((body) => {
          const cluster = body.defaultCluster;
          this.setState({clusterStatus: cluster.status});

          if (cluster.status === ClusterStatus.Stopped) {
            notebooksClusterApi()
              .startCluster(cluster.clusterNamespace, cluster.clusterName);
          }

          if (cluster.status === ClusterStatus.Running) {
            onClusterReady();
          } else {
            retry();
          }
        })
        .catch(() => {
          retry();
        });
    }

    private startEditMode() {
      if (this.canWrite()) {
        this.setState({userRequestedExecutableNotebook: true});
        this.runCluster(() => { this.navigateEditMode(); });
      }
    }

    private startPlaygroundMode() {
      if (this.canWrite()) {
        this.setState({userRequestedExecutableNotebook: true});
        this.runCluster(() => { this.navigatePlaygroundMode(); });
      }
    }

    private onPlaygroundModeClick() {
      if (Cookies.get(ConfirmPlaygroundModeModal.DO_NOT_SHOW_AGAIN) === String(true)) {
        this.startPlaygroundMode();
      } else {
        this.setState({showPlaygroundModeModal: true});
      }
    }

    private navigateOldNotebooksPage(playgroundMode: boolean) {
      const queryParams = {
        playgroundMode: playgroundMode
      };

      navigate(['workspaces', this.props.billingProjectId, this.props.workspaceName,
        'notebooks', this.props.notebookName], {'queryParams': queryParams});
    }

    private navigatePlaygroundMode() {
      this.navigateOldNotebooksPage(true);
    }

    private navigateEditMode() {
      this.navigateOldNotebooksPage(false);
    }

    private canWrite() {
      return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
    }

    render() {
      return (
        <div>
          <div style={styles.navBar}>
            <div style={{...styles.navBarItem, ...styles.active}}>
              Preview (Read-Only)
            </div>
            {this.state.userRequestedExecutableNotebook ? (
              <div style={{...styles.navBarItem}}>
                <ClrIcon shape='sync' style={{...styles.navBarIcon, ...styles.rotate}}></ClrIcon>
                Preparing your Jupyter environment. This may take up to 10 minutes.
              </div>) : (
              <div style={{display: 'flex'}}>
                <div style={
                  Object.assign({}, styles.navBarItem,
                    this.canWrite() ? styles.clickable : styles.disabled)}
                     onClick={() => { this.startEditMode(); }}>
                  <EditComponentReact enableHoverEffect={false}
                                      disabled={!this.canWrite()}
                                      style={styles.navBarIcon}/>
                  Edit
                </div>
                <div style={Object.assign({}, styles.navBarItem,
                  this.canWrite() ? styles.clickable : styles.disabled)}
                     onClick={() => { this.onPlaygroundModeClick(); }}>
                  <div style={{...styles.navBarIcon, marginBottom: '5px'}}>
                    <PlaygroundModeIcon />
                  </div>
                  Run (Playground Mode)
                </div>
              </div>)
            }
          </div>
          <div style={styles.previewFrame}>
            {this.state.html ?
              (<iframe style={styles.previewFrame} srcDoc={this.state.html}></iframe>) :
              (<SpinnerOverlay/>)}
          </div>
          {this.state.showPlaygroundModeModal &&
            <ConfirmPlaygroundModeModal
              onCancel={() => {
                this.setState({showPlaygroundModeModal: false}); }}
              onContinue={() => {
                this.setState({showPlaygroundModeModal: false});
                this.startPlaygroundMode();
              }}/>}
        </div>
      );
    }
  });

@Component({
  template: '<div #root></div>'
})
export class InteractiveNotebookComponent extends ReactWrapperBase {
  constructor() {
    super(InteractiveNotebook, []);
  }
}
