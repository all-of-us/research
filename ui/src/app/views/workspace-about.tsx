import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {cdrVersionStore, currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';

import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {Button, Link} from 'app/components/buttons';
import {InfoIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUrlParams, withUserProfile} from 'app/utils';
import {ResearchPurpose} from 'app/views/research-purpose';
import {ResetClusterButton} from 'app/views/reset-cluster-button';
import {CdrVersion, Cohort, Profile, UserRole, WorkspaceAccessLevel} from 'generated/fetch';
import {WorkspaceShare} from './workspace-share';


interface WorkspaceState {
  sharing: boolean;
  cdrVersion: CdrVersion;
  useBillingProjectBuffer: boolean;
  freeTierBillingProject: string;
  cohortsLoading: boolean;
  cohortsError: boolean;
  cohortList: Cohort[];
  workspace: WorkspaceData;
  workspaceUserRoles: UserRole[];
  googleBucketModalOpen: boolean;
}

const styles = reactStyles({
  mainPage: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'stretch',
    height: 'calc(100% - 60px)'
  },
  rightSidebar: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85), marginRight: '-0.6rem',
    paddingLeft: '0.5rem', paddingTop: '1rem', width: '22%', display: 'flex',
    flexDirection: 'column'
  },
  shareHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', paddingRight: '0.5rem',
    paddingBottom: '0.5rem'
  },
  infoBox: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75), height: '2rem', width: '6rem',
    borderRadius: '5px', padding: '0.4rem', marginRight: '0.5rem', marginBottom: '0.5rem',
    color: colors.black, lineHeight: '14px'
  },
  infoBoxHeader: {
    textTransform: 'uppercase', fontSize: '0.4rem'
  }
});

const pageId = 'workspace';

const ShareTooltipText = () => {
  return <div>
    Here you can add and see collaborators with whom you share your workspace.
    <ul>
      <li>A <u>Reader</u> can view your notebooks, but not make edits,
        deletes or share contents of the Workspace.</li>
      <li>A <u>Writer</u> can view, edit and delete content in the Workspace
        but not share the Workspace with others.</li>
      <li>An <u>Owner</u> can view, edit, delete and share contents in the Workspace.</li>
    </ul>
  </div>;
};

const WorkspaceInfoTooltipText = () => {
  return <div>
    <u>Dataset</u>
    <br/>The version of the dataset used by this workspace is displayed here<br/>
    <u>Creation date</u>
    <br/>The date you created your workspace<br/>
    <u>Last updated</u>
    <br/>The date this workspace was last updated<br/>
    <u>Access level</u>
    <br/>To make sure data is accessed only by authorized users, users can request
      and be granted access to data access tiers within the All of Us Research Program.
      Currently there are 3 tiers  - “Public”, “Registered” and “Controlled”.<br/>
  </div>;
};

export const WorkspaceAbout = fp.flow(withUserProfile(), withUrlParams())
(class extends React.Component<
  {profileState: {profile: Profile, reload: Function, updateCache: Function}}, WorkspaceState> {

  constructor(props) {
    super(props);
    this.state = {
      sharing: false,
      cdrVersion: undefined,
      useBillingProjectBuffer: undefined,
      freeTierBillingProject: undefined,
      cohortsLoading: true,
      cohortsError: false,
      cohortList: [],
      workspace: undefined,
      workspaceUserRoles: [],
      googleBucketModalOpen: false
    };
  }

  async componentDidMount() {
    const {profileState: {profile}} = this.props;
    this.setState({
      useBillingProjectBuffer: serverConfigStore.getValue().useBillingProjectBuffer,
      freeTierBillingProject: profile.freeTierBillingProjectName
    });
    this.setVisits();
    await this.reloadWorkspace(currentWorkspaceStore.getValue());
    this.loadUserRoles();
    this.setCdrVersion();
  }

  async setVisits() {
    const {profileState: {profile}} = this.props;
    if (!profile.pageVisits.some(v => v.page === pageId)) {
      await profileApi().updatePageVisits({ page: pageId});
    }
  }

  setCdrVersion() {
    this.setState({cdrVersion: cdrVersionStore.getValue().
      find(v => v.cdrVersionId === this.state.workspace.cdrVersionId)});
  }

  async reloadWorkspace(workspace: WorkspaceData) {
    this.setState({workspace: workspace});
  }

  async loadUserRoles() {
    const {workspace} = this.state;
    workspacesApi().getFirecloudWorkspaceUserRoles(workspace.namespace, workspace.id).then(
      resp => {
        this.setState({workspaceUserRoles: fp.sortBy('familyName', resp.items)});
      }
    ).catch(error => {
      console.error(error);
    });
  }

  get workspaceCreationTime(): string {
    if (this.state.workspace) {
      const asDate = new Date(this.state.workspace.creationTime);
      return asDate.toDateString();
    } else {
      return 'Loading...';
    }
  }

  get workspaceLastModifiedTime(): string {
    if (this.state.workspace) {
      const asDate = new Date(this.state.workspace.lastModifiedTime);
      return asDate.toDateString();
    } else {
      return 'Loading...';
    }
  }

  openGoogleBucket() {
    const googleBucketUrl = 'https://console.cloud.google.com/storage/browser/' +
      this.state.workspace.googleBucketName + '?authuser=' +
      this.props.profileState.profile.username;
    window.open(googleBucketUrl, '_blank');
  }

  workspaceClusterBillingProjectId(): string {
    const {useBillingProjectBuffer, freeTierBillingProject, workspace} = this.state;
    if (useBillingProjectBuffer === undefined) {
      // The server config hasn't loaded yet, we don't yet know which billing
      // project should be used for clusters.
      return null;
    }
    if (!useBillingProjectBuffer) {
      return freeTierBillingProject;
    }

    if ([WorkspaceAccessLevel.WRITER, WorkspaceAccessLevel.OWNER].includes(workspace.accessLevel)) {
      return workspace.namespace;
    }
    return null;
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {cdrVersion, workspace, workspaceUserRoles, googleBucketModalOpen, sharing} = this.state;
    return <div style={styles.mainPage}>
      <ResearchPurpose data-test-id='researchPurpose'/>
      <div style={styles.rightSidebar}>
        <div style={styles.shareHeader}>
          <h3 style={{marginTop: 0}}>Collaborators:</h3>
          <TooltipTrigger content={ShareTooltipText()}>
            <InfoIcon style={{margin: '0 0.3rem'}}/>
          </TooltipTrigger>
          <Button style={{height: '22px', fontSize: 12, marginRight: '0.5rem',
            maxWidth: '13px'}} disabled={workspaceUserRoles.length === 0}
                  data-test-id='workspaceShareButton'
                  onClick={() => this.setState({sharing: true})}>Share</Button>
        </div>
        {workspaceUserRoles.length > 0 ?
          <React.Fragment>
            {workspaceUserRoles.map((user, i) =>
              <div key={i} data-test-id={'workspaceUser-' + i}>
                {user.role + ' : ' + user.email}
              </div>)}
          </React.Fragment> :
          <Spinner size={50} style={{display: 'flex', alignSelf: 'center'}}/>}
        <div>
          <h3 style={{marginBottom: '0.5rem'}}>Workspace Information:
            <TooltipTrigger content={WorkspaceInfoTooltipText()}>
              <InfoIcon style={{margin: '0 0.3rem'}}/>
            </TooltipTrigger>
          </h3>
          <div style={styles.infoBox} data-test-id='cdrVersion'>
            <div style={styles.infoBoxHeader}>Dataset</div>
            <div style={{fontSize: '0.5rem'}}>{cdrVersion ? cdrVersion.name : 'Loading...'}</div>
          </div>
          <div style={styles.infoBox} data-test-id='creationDate'>
            <div style={styles.infoBoxHeader}>Creation Date</div>
            <div style={{fontSize: '0.5rem'}}>{this.workspaceCreationTime}</div>
          </div>
          <div style={styles.infoBox} data-test-id='lastUpdated'>
            <div style={styles.infoBoxHeader}>Last Updated</div>
            <div style={{fontSize: '0.5rem'}}>{this.workspaceLastModifiedTime}</div>
          </div>
          <div style={styles.infoBox} data-test-id='dataAccessLevel'>
            <div style={styles.infoBoxHeader}>Data Access Level</div>
            <div style={{fontSize: '0.5rem'}}>{workspace ?
              fp.capitalize(workspace.dataAccessLevel.toString()) : 'Loading...'}</div>
          </div>
          <Link disabled={!workspace}
                onClick={() => this.setState({googleBucketModalOpen: true})}>Google Bucket</Link>
          {!!this.workspaceClusterBillingProjectId() &&
            <ResetClusterButton billingProjectId={this.workspaceClusterBillingProjectId()}/>}
        </div>
      </div>
      {googleBucketModalOpen && <Modal>
        <ModalTitle>Note</ModalTitle>
        <ModalBody>
            It is All of Us data use policy that researchers should not make copies of or download
            individual-level data (including taking screenshots or other means of viewing
            individual-level data) outside of the <i>All of Us</i> research environment without
            approval from All of Us Resource Access Board (RAB).<br/>
            Notebooks should rarely be downloaded directly from Google Cloud Console, as output
            cells in Notebooks may contain sensitive individual level data.
        </ModalBody>
        <ModalFooter>
          <Button type='secondary'
                  onClick={() => this.setState({googleBucketModalOpen: false})}>Cancel</Button>
          <Button onClick={() => this.openGoogleBucket()}
                  style={{marginLeft: '0.5rem'}}>Continue</Button>
        </ModalFooter>
      </Modal>}
      {sharing && <WorkspaceShare workspace={workspace}
                                  accessLevel={workspace.accessLevel}
                                  userEmail={profile.username}
                                  onClose={() => this.setState({sharing: false})}
                                  userRoles={workspaceUserRoles}
                                  data-test-id='workspaceShareModal'/>}
    </div>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class WorkspaceAboutComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceAbout, []);
  }
}
