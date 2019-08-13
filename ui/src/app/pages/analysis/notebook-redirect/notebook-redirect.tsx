import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {serverConfigStore, urlParamsStore} from 'app/utils/navigation';

import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {NotebookIcon} from 'app/icons/notebook-icon';
import {ReminderIconComponentReact} from 'app/icons/reminder';
import {jupyterApi, notebooksApi, notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withQueryParams, withUserProfile} from 'app/utils';
import {Kernels} from 'app/utils/notebook-kernels';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cluster, ClusterStatus, Profile} from 'generated/fetch';


enum Progress {
  Unknown,
  Initializing,
  Resuming,
  Authenticating,
  Copying,
  Creating,
  Redirecting,
  Loaded
}

const styles = reactStyles({
  main: {
    display: 'flex', flexDirection: 'column', marginLeft: '3rem', paddingTop: '1rem', width: '780px'
  },
  progressCard: {
    height: '180px', width: '195px', borderRadius: '5px', backgroundColor: colors.white,
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)', display: 'flex',
    flexDirection: 'column', alignItems: 'center', padding: '1rem'
  },
  progressIcon: {
    height: '46px', width: '46px', marginBottom: '5px',
    fill: colorWithWhiteness(colors.primary, 0.9)
  },
  progressIconDone: {
    fill: colors.success
  },
  progressText: {
    textAlign: 'center', color: colors.black, fontSize: 14, lineHeight: '22px', marginTop: '10px'
  },
  reminderText: {
    display: 'flex', flexDirection: 'row', marginTop: '1rem', fontSize: 14, color: colors.primary
  }
});


const commonNotebookFormat = {
  'cells': [
    {
      'cell_type': 'code',
      'execution_count': null,
      'metadata': {},
      'outputs': [],
      'source': []
    }
  ],
  metadata: {},
  'nbformat': 4,
  'nbformat_minor': 2
};

const rNotebookMetadata = {
  'kernelspec': {
    'display_name': 'R',
    'language': 'R',
    'name': 'ir'
  },
  'language_info': {
    'codemirror_mode': 'r',
    'file_extension': '.r',
    'mimetype': 'text/x-r-source',
    'name': 'R',
    'pygments_lexer': 'r',
    'version': '3.4.4'
  }
};

const pyNotebookMetadata = {
  'kernelspec': {
    'display_name': 'Python 3',
    'language': 'python',
    'name': 'python3'
  },
  'language_info': {
    'codemirror_mode': {
      'name': 'ipython',
      'version': 3
    },
    'file_extension': '.py',
    'mimetype': 'text/x-python',
    'name': 'python',
    'nbconvert_exporter': 'python',
    'pygments_lexer': 'ipython3',
    'version': '3.4.2'
  }
};

const progressCardStates = [
  {includes: [Progress.Unknown, Progress.Initializing, Progress.Resuming], icon: 'notebook'},
  {includes: [Progress.Authenticating], icon: 'success-standard'},
  {includes: [Progress.Creating, Progress.Copying], icon: 'copy'},
  {includes: [Progress.Redirecting], icon: 'circle-arrow'}
];

const ProgressCard: React.FunctionComponent<{progressState: Progress, index: number,
  progressComplete: Map<Progress, boolean>, creating: boolean}> =
  ({index, progressState, progressComplete, creating}) => {
    const includesStates = progressCardStates[index].includes;
    const icon = progressCardStates[index].icon;
    const isCurrent = includesStates.includes(progressState);
    const isComplete = progressState.valueOf() > includesStates.slice(-1).pop().valueOf();

    // Conditionally render card text
    const renderText = () => {
      switch (index) {
        case 0:
          if (progressState === Progress.Unknown || progressComplete[Progress.Unknown]) {
            return 'Connecting to the notebook server';
          } else if (progressState === Progress.Initializing ||
            progressComplete[Progress.Initializing]) {
            return 'Initializing notebook server, may take up to 10 minutes';
          } else {
            return 'Resuming notebook server, may take up to 1 minute';
          }
        case 1:
          return 'Authenticating with the notebook server';
        case 2:
          if (creating) {
            return 'Creating the new notebook';
          } else {
            return 'Copying the notebook onto the server';
          }
        case 3:
          return 'Redirecting to the notebook server';
      }
    };
    const rotateIcon = () => {
      return icon === 'circle-arrow' ? 'rotate(90deg)' : undefined;
    };

    return <div style={isCurrent ? {...styles.progressCard, backgroundColor: '#F2FBE9'} :
      styles.progressCard}>
      {isCurrent ? <Spinner style={{width: '46px', height: '46px'}}/> :
        <React.Fragment>
          {icon === 'notebook' ? <NotebookIcon style={styles.progressIcon}/> :
          <ClrIcon shape={icon} style={isComplete ?
          {...styles.progressIcon, ...styles.progressIconDone,
            transform: rotateIcon()} :
            {...styles.progressIcon, transform: rotateIcon()}}/>}
        </React.Fragment>}
        <div style={styles.progressText}>
          {renderText()}
        </div>
    </div>;
  };

interface State {
  progress: Progress;
  progressComplete: Map<Progress, boolean>;
  creating: boolean;
  cluster: Cluster;
  playgroundMode: boolean;
  jupyterLabMode: boolean;
  notebookName: string;
  fullNotebookName: string;
  useBillingProjectBuffer: boolean;
  freeTierBillingProjectName: string;
  initialized: boolean;
}

interface Props {
  workspace: WorkspaceData;
  queryParams: any;
  profileState: {profile: Profile, reload: Function, updateCache: Function};
}

export const NotebookRedirect = fp.flow(withUserProfile(), withCurrentWorkspace(),
  withQueryParams())(class extends React.Component<Props, State> {

    private pollClusterTimer: NodeJS.Timer;

    constructor(props) {
      super(props);
      this.state = {
        progress: Progress.Unknown,
        progressComplete: new Map<Progress, boolean>(),
        creating: !!props.queryParams.creating,
        playgroundMode: props.queryParams.playgroundMode === true,
        jupyterLabMode: props.queryParams.jupyterLabMode === true,
        notebookName: undefined,
        fullNotebookName: undefined,
        useBillingProjectBuffer: undefined,
        freeTierBillingProjectName: undefined,
        cluster: undefined,
        initialized: false
      };
    }

    componentDidMount() {
      this.setNotebookNames();

      const {profileState: {profile}} = this.props;
      this.setState({
        useBillingProjectBuffer: serverConfigStore.getValue().useBillingProjectBuffer,
        freeTierBillingProjectName: profile.freeTierBillingProjectName
      }, () => {
        if (this.state.useBillingProjectBuffer) {
          this.pollCluster(this.props.workspace.namespace);
        } else {
          this.pollCluster(this.state.freeTierBillingProjectName);
        }
      });

    }

    isClusterInProgress(cluster: Cluster): boolean {
      return cluster.status === ClusterStatus.Starting ||
        cluster.status === ClusterStatus.Stopping ||
        cluster.status === ClusterStatus.Stopped;
    }

    async pollCluster(billingProjectId) {
      const repoll = () => {
        this.pollClusterTimer = setTimeout(() => this.pollCluster(billingProjectId), 15000);
      };

      try {
        const resp = await clusterApi().listClusters(billingProjectId);
        const cluster = resp.defaultCluster;
        if (!this.state.initialized) {
          if (cluster.status === ClusterStatus.Running) {
            this.incrementProgress(Progress.Unknown);
          } else if (this.isClusterInProgress(cluster)) {
            this.incrementProgress(Progress.Resuming);
          } else {
            this.incrementProgress(Progress.Initializing);
          }
          this.setState({initialized: true});
        }

        if (cluster.status === ClusterStatus.Running) {
          this.setState({cluster: cluster});
          this.incrementProgress(Progress.Authenticating);
          await this.initializeNotebookCookies(cluster);
          // TODO: add retries
          const localizeRetry = 0;
          const notebookLocation = await this.loadNotebook();

          // console.log(notebookLocation);
          // this.incrementProgress(Progress.Redirecting);
          //
          //
          //
          // setTimeout(() => {
          //   this.incrementProgress(Progress.Loaded);
          // }, 1000);

        } else {
          // If cluster is not running, keep re-polling until it is.
          if (cluster.status === ClusterStatus.Stopped) {
            await notebooksClusterApi().startCluster(cluster.clusterNamespace, cluster.clusterName);
          }
          await this.timeout(10000);
          repoll();
        }
      } catch (e) {
        repoll();
      }
    }

    timeout(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }

    // this maybe overkill, but should handle all situations
    setNotebookNames(): void {
      const {nbName} = urlParamsStore.getValue();
      this.setState({notebookName: decodeURIComponent(nbName)});
      if (nbName.endsWith('.ipynb')) {
        this.setState({fullNotebookName: decodeURIComponent(nbName)}, () => {
          this.setState({notebookName: this.state.fullNotebookName.replace('.ipynb$', '')});
        });
      } else {
        this.setState({notebookName: decodeURIComponent(nbName)}, () => {
          this.setState({fullNotebookName: this.state.notebookName + '.ipynb'});
        });
      }
    }

    async initializeNotebookCookies(c: Cluster) {
      return notebooksApi().setCookie(c.clusterNamespace, c.clusterName, {withCredentials: true});
    }

    async loadNotebook() {
      const {fullNotebookName, playgroundMode} = this.state;
      if (!this.state.creating) {
        this.incrementProgress(Progress.Copying);
        const localizedNotebookDir =
          await this.localizeNotebooks([fullNotebookName], playgroundMode);
        return `${localizedNotebookDir}/${fullNotebookName}`;
      } else {
        this.incrementProgress(Progress.Creating);
        return this.newNotebook();
      }
    }

    async localizeNotebooks(notebookNames: Array<string>, playgroundMode: boolean) {
      const cluster = this.state.cluster;
      const {workspace} = this.props;
      const resp = await clusterApi().localize(cluster.clusterNamespace, cluster.clusterName,
        {workspaceNamespace: workspace.namespace, workspaceId: workspace.id,
          notebookNames: notebookNames, playgroundMode: playgroundMode});
      return resp.clusterLocalDirectory;
    }

    incrementProgress(p: Progress): void {
      this.setState({
        progress: p,
        progressComplete: this.state.progressComplete.set(p, true)
      });
    }

    async newNotebook() {
      const {cluster, notebookName} = this.state;
      const fileContent = commonNotebookFormat;
      const {kernelType} = this.props.queryParams.kernelspec;
      if (kernelType === Kernels.R.toString()) {
        fileContent.metadata = rNotebookMetadata;
      } else {
        fileContent.metadata = pyNotebookMetadata;
      }
      const localizedDir = await this.localizeNotebooks([], false);
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking 'new notebook' in the Jupyter UI.
      const workspaceDir = localizedDir.replace(/^workspaces\//, '');
      const jupyterResp = await jupyterApi().putContents(
        cluster.clusterNamespace, cluster.clusterName, workspaceDir, notebookName + '.ipynb', {
          'type': 'file',
          'format': 'text',
          'content': JSON.stringify(fileContent)
        }
      );
      return `${localizedDir}/${jupyterResp.name}`;
    }

    render() {
      const {creating, progress, progressComplete} = this.state;
      return <React.Fragment>
        <div style={styles.main}>
          <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
            <h2 style={{lineHeight: 0}}>
              Creating New Notebook: {this.state.notebookName}
            </h2>
            <Button type='secondary' onClick={() => window.history.back()}>Cancel</Button>
          </div>
          <div style={{display: 'flex', flexDirection: 'row', marginTop: '1rem'}}>
            {progressCardStates.map((_, i) => {
              return <ProgressCard progressState={progress} index={i}
                                   creating={creating} progressComplete={progressComplete}/>;
            })}
          </div>
          <div style={styles.reminderText}>
            <ReminderIconComponentReact
              style={{height: '80px', width: '80px', marginRight: '0.5rem'}}/>
            It is All of Us data use policy that researchers should not make copies of
            or download individual-level data (including taking screenshots or other means
            of viewing individual-level data) outside of the All of Us research environment
            without approval from All of Us Resource Access Board (RAB).
          </div>
        </div>
      </React.Fragment>;
    }
  });

@Component({
  template: '<div #root></div>'
})
export class NotebookRedirectComponent extends ReactWrapperBase {
  constructor() {
    super(NotebookRedirect, []);
  }
}

