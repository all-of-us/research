import {Component} from '@angular/core';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {navigate} from 'app/utils/navigation';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';

import {AlertDanger, AlertWarning} from 'app/components/alert';
import {CardButton, Clickable, MenuItem} from 'app/components/buttons';
import {Card} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {ListPageHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {displayDate, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils/index';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal/component';
import {
  BillingProjectStatus,
  ErrorResponse,
  Profile,
} from 'generated/fetch';
import * as React from 'react';
import {WorkspaceShare} from "../workspace-share/component";

const styles = reactStyles({
  fadeBox: {
    margin: '1rem auto 0 auto', width: '97.5%', padding: '0 1rem'
  },
  cardArea: {
    display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'
  },
  addCard: {
    display: 'flex', fontSize: '20px', lineHeight: '28px', marginTop: '0',
    fontWeight: 600, color: '#216FB4'
  },
  workspaceName: {
    color: '#216FB4', marginBottom: '0.5rem', fontWeight: 600,
    fontSize: 18, wordBreak: 'break-all', cursor: 'pointer',
  },
  workspaceDescription: {
    textOverflow: 'ellipsis', overflow: 'hidden', height: '2rem', display: '-webkit-box',
    WebkitLineClamp: 2, WebkitBoxOrient: 'vertical'
  },
  workspaceCard: {
    display: 'flex', flexDirection: 'column', justifyContent: 'space-between', height: '100%'
  },
  workspaceCardFooter: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'
  },
  permissionBox: {
    color: '#FFFFFF', height: '1rem', width: '3rem', fontSize: 10, textAlign: 'center',
    borderRadius: '0.2rem', padding: 0
  }
});


const WorkspaceCardMenu: React.FunctionComponent<{
  disabled: boolean, wp: WorkspacePermissions, onShare: Function, onDelete: Function
}> = ({
        disabled, wp, onShare, onDelete
      }) => {
  const wsPathPrefix = 'workspaces/' + wp.workspace.namespace + '/' + wp.workspace.id;

  return <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={ <React.Fragment>
        <MenuItem icon='copy'
                  onClick={() => {navigate([wsPathPrefix, 'clone']); }}>
          Duplicate
        </MenuItem>
        <TooltipTrigger content={<div>Requires Write Permission</div>}
                        disabled={wp.canWrite}>
          <MenuItem icon='pencil'
                    onClick={() => {navigate([wsPathPrefix, 'edit']); }}
                    disabled={!wp.canWrite}>
            Edit
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={wp.isOwner}>
          <MenuItem icon='pencil' onClick={onShare} disabled={!wp.isOwner}>
            Share
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={wp.isOwner}>
          <MenuItem icon='trash' onClick={onDelete} disabled={!wp.isOwner}>
            Delete
          </MenuItem>
        </TooltipTrigger>
      </React.Fragment>}
  >
    <Clickable disabled={disabled} data-test-id='resource-menu'>
      <ClrIcon shape='ellipsis-vertical' size={21}
               style={{color: disabled ? '#9B9B9B' : '#216FB4', marginLeft: -9,
                 cursor: disabled ? 'auto' : 'pointer'}}/>
    </Clickable>
  </PopupTrigger>;
};

export class WorkspaceCard extends React.Component<
    {wp: WorkspacePermissions, userEmail: string, reload: Function},
    {sharing: boolean, confirmDeleting: boolean}> {

  constructor(props) {
    super(props);
    this.state = {
      sharing: false,
      confirmDeleting: false
    };
  }

  // todo: deletion error
  async deleteWorkspace() {
    const {wp} = this.props;
    // this.setState({deleting: true});
    workspacesApi().deleteWorkspace(wp.workspace.namespace, wp.workspace.id).then(() => {
      this.setState({confirmDeleting: false});
      this.props.reload();
    });
  }

  shareWorkspace(): void {
    this.setState({sharing: false});
    this.props.reload();
  }

  render() {
    const {wp, userEmail} = this.props;
    const {confirmDeleting, sharing} = this.state;
    const permissionBoxColors = {'OWNER': '#4996A2', 'READER': '#8F8E8F', 'WRITER': '#92B572'};

    return <React.Fragment>
      <Card>
        <div style={styles.workspaceCard}>
          <div style={{display: 'flex', flexDirection: 'column'}}>
            <div style={{ display: 'flex', alignItems: 'flex-start', flexDirection: 'row'}}>
              <WorkspaceCardMenu wp={wp}
                                 onDelete={() => {this.setState({confirmDeleting: true}); }}
                                 onShare={() => {this.setState({sharing: true})}}
                                 disabled={false}/>
              <Clickable>
                <div style={styles.workspaceName}
                     onClick={() => navigate(
                         ['workspaces', wp.workspace.namespace, wp.workspace.id])}>
                  {wp.workspace.name}</div>
              </Clickable>
            </div>
            <div style={styles.workspaceDescription}>{wp.workspace.description}</div>
            {wp.isPending && <div style={{color: '#f8c954'}}>
              <ClrIcon shape='exclamation-triangle' className='is-solid' style={{fill: '#f8c954'}}/>
              Pending Approval
            </div>}
            {wp.isRejected && <div style={{color: '#f58771'}}>
              <ClrIcon shape='exclamation-triangle' className='is-solid' style={{fill: '#f58771'}}/>
              Rejected
            </div> }
          </div>
          <div style={styles.workspaceCardFooter}>
            <div style={{fontSize: 12, lineHeight: '17px'}}>Last Changed: <br/>
              {displayDate(wp.workspace.lastModifiedTime)}</div>
            <div style={{
              ...styles.permissionBox,
              backgroundColor: permissionBoxColors[wp.accessLevel]}}>{wp.accessLevel}</div>
          </div>
        </div>
      </Card>
      {confirmDeleting &&
        <ConfirmDeleteModal resourceType='workspace'
                            resourceName={wp.workspace.name}
                            receiveDelete={() => {this.deleteWorkspace()}}
                            closeFunction={() => {this.setState({confirmDeleting: false})}}/>}
      {sharing && <WorkspaceShare workspace={wp.workspace}
                                  accessLevel={wp.accessLevel}
                                  userEmail={userEmail}
                                  sharing={sharing}
                                  onClose={() => {this.shareWorkspace()}} />}
    </React.Fragment>;

  }
}


export const WorkspaceList = withUserProfile()
(class extends React.Component<
  { profileState: { profile: Profile, reload: Function } },
  { workspacesLoading: boolean, billingProjectInitialized: boolean,
    workspaceList: WorkspacePermissions[], errorText: string,
    firstSignIn: Date, twoFactorEnabled: boolean
  }> {
  private timer: NodeJS.Timer;

  constructor(props) {
    super(props);
    this.state = {
      workspacesLoading: true,
      billingProjectInitialized: false,
      workspaceList: [],
      errorText: '',
      twoFactorEnabled: false,
      firstSignIn: undefined,
    };
  }

  componentDidMount() {
    this.checkBillingProjectStatus();
    this.checkTwoFactorAuth();
    this.reloadWorkspaces();
  }

  componentWillUnmount() {
    clearTimeout(this.timer);
  }

  async reloadWorkspaces() {
    this.setState({workspacesLoading: true});
    try {
      const workspacesReceived = await workspacesApi().getWorkspaces();
      workspacesReceived.items.sort(
        (a, b) => a.workspace.name.localeCompare(b.workspace.name));
      this.setState({workspaceList: workspacesReceived.items
          .map(w => new WorkspacePermissions(w))});
      this.setState({workspacesLoading: false});
    } catch (e) {
      const response = ErrorHandlingService.convertAPIError(e) as unknown as ErrorResponse;
      this.setState({errorText: response.message});
    }
  }

  checkBillingProjectStatus() {
    const {profileState: {profile, reload}} = this.props;
    if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
      this.setState({billingProjectInitialized: true});
    } else {
      this.timer = setTimeout(() => {
        reload();
      }, 10000);
    }
  }

  checkTwoFactorAuth() {
    const {profileState: {profile}} = this.props;
    this.setState({
      twoFactorEnabled: profile.twoFactorEnabled,
      firstSignIn: new Date(profile.firstSignInTime)
    });
  }

  get twoFactorBannerEnabled() {
    if (this.state.firstSignIn === undefined) {
      return false;
    }
    if (this.state.twoFactorEnabled === true) {
      return false;
    }
    // Don't show the banner after 1 week as their account would
    // have been disabled had they not enabled 2-factor auth.
    if (new Date().getTime() - this.state.firstSignIn.getTime() > 7 * 24 * 60 * 60 * 1000) {
      return false;
    }
    return true;
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {billingProjectInitialized, errorText,
      workspaceList, workspacesLoading} = this.state;

    return <React.Fragment>
      <FadeBox style={styles.fadeBox}>
        <div style={{padding: '0 1rem'}}>
          <ListPageHeader>Workspaces</ListPageHeader>
          {this.twoFactorBannerEnabled && <AlertWarning>
            <div>Please add a second layer of protection with 2-Step Verification.
            After three days your account will be suspended.
            If you already completed setup, please ignore this message.  <a
                  style={{color: '#2691D0'}} href='https://myaccount.google.com/security'>
              Set up 2-Step Verification here</a>.</div>
          </AlertWarning>}
          {errorText && <AlertDanger>
            <ClrIcon shape='exclamation-circle'/>
            {errorText}
          </AlertDanger>}

          <div style={styles.cardArea}>
            {workspacesLoading ?
              (<Spinner style={{width: '100%', marginTop: '1.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '1.5rem', flexWrap: 'wrap'}}>
                <CardButton disabled={!billingProjectInitialized}
                            onClick={() => navigate(['workspaces/build'])}
                            style={styles.addCard}>
                  Create a <br/> New Workspace
                  <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                </CardButton>
                {workspaceList.map(wp => {
                  return <WorkspaceCard wp={wp}
                                        userEmail={profile.username}
                                        reload={() => {this.reloadWorkspaces(); }}/>;

                })}
              </div>)}
          </div>
        </div>
      </FadeBox>
    </React.Fragment>;
  }


});

@Component({
  template: '<div #root></div>'
})
export class WorkspaceListComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceList, []);
  }
}
