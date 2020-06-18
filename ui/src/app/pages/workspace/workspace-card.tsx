import * as React from 'react';

import {ResourceType, UserRole, Workspace, WorkspaceAccessLevel} from 'generated/fetch';

import {BugReportModal} from 'app/components/bug-report';
import {Button, Clickable, Link, MenuItem} from 'app/components/buttons';
import {WorkspaceCardBase} from 'app/components/card';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon, SnowmanIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AouTitle} from 'app/components/text-wrappers';
import {WorkspaceShare} from 'app/pages/workspace/workspace-share';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {displayDate, reactStyles} from 'app/utils';
import {AnalyticsTracker, triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore, navigate} from 'app/utils/navigation';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';

const EVENT_CATEGORY = 'Workspace list';

const styles = reactStyles({
  workspaceCard: {
    justifyContent: 'flex-end',
    height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative'
  },
  workspaceMenuWrapper: {
    paddingTop: '.5rem',
    borderRight: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, .6),
    flex: '0 0 1rem',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  workspaceName: {
    color: colors.accent,
    marginBottom: '0.5rem',
    fontSize: 18,
    wordBreak: 'break-all',
    cursor: 'pointer',
  },
  permissionBox: {
    color: colors.white,
    height: '1rem',
    width: '3rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '0.2rem',
    padding: 0
  }
});

interface WorkspaceCardMenuProps {
  disabled: boolean;
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  onShare: Function;
  onDelete: Function;
}

const WorkspaceCardMenu: React.FunctionComponent<WorkspaceCardMenuProps> = ({
  disabled,
  workspace,
  accessLevel,
  onShare,
  onDelete
}) => {
  const wsPathPrefix = 'workspaces/' + workspace.namespace + '/' + workspace.id;

  return <PopupTrigger
    side='bottom'
    closeOnClick
    content={
      <React.Fragment>
        <MenuItem icon='copy'
                  onClick={() => {
                    // Using workspace.published here to identify Featured Workspaces. At some point, we will need a separate property for
                    // this on the workspace object once users are able to publish their own workspaces
                    workspace.published ?
                      AnalyticsTracker.Workspaces.DuplicateFeatured(workspace.name) :
                      AnalyticsTracker.Workspaces.OpenDuplicatePage('Card');
                    navigate([wsPathPrefix, 'duplicate']); }
                  }>
          Duplicate
        </MenuItem>
        <TooltipTrigger content={<div>Requires Write Permission</div>}
                        disabled={WorkspacePermissionsUtil.canWrite(accessLevel)}>
          <MenuItem icon='pencil'
                    onClick={() => {
                      AnalyticsTracker.Workspaces.OpenEditPage('Card');
                      navigate([wsPathPrefix, 'edit']); }
                    }
                    disabled={!WorkspacePermissionsUtil.canWrite(accessLevel)}>
            Edit
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div data-test-id='workspace-share-disabled-tooltip'>Requires Owner Permission</div>}
                        disabled={WorkspacePermissionsUtil.isOwner(accessLevel)}>
          <MenuItem icon='pencil'
                    onClick={() => {
                      AnalyticsTracker.Workspaces.OpenShareModal('Card');
                      onShare();
                    }}
                    disabled={!WorkspacePermissionsUtil.isOwner(accessLevel)}>
            Share
          </MenuItem>
        </TooltipTrigger>
        <TooltipTrigger content={<div>Requires Owner Permission</div>}
                        disabled={WorkspacePermissionsUtil.isOwner(accessLevel)}>
          <MenuItem icon='trash'
                    onClick={() => {
                      AnalyticsTracker.Workspaces.OpenDeleteModal('Card');
                      onDelete();
                    }}
                    disabled={!WorkspacePermissionsUtil.isOwner(accessLevel)}>
            Delete
          </MenuItem>
        </TooltipTrigger>
      </React.Fragment>
    }
  >
    <Clickable disabled={disabled} data-test-id='workspace-card-menu'>
      <SnowmanIcon
        style={{marginLeft: '0px'}}
        disabled={disabled}
      />
    </Clickable>
  </PopupTrigger>;
};

interface WorkspaceCardState {
  bugReportError: string;
  bugReportOpen: boolean;
  confirmDeleting: boolean;
  // Whether this card is busy loading data specific to the workspace.
  loadingData: boolean;
  sharing: boolean;
  showResearchPurposeReviewModal: boolean;
  // The list of user roles associated with this workspace. Lazily populated
  // only when the workspace share dialog is opened.
  userRoles?: UserRole[];
  workspaceDeletionError: boolean;
}

interface WorkspaceCardProps {
  userEmail: string;
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  // A reload function that can be called by this component to reqeust a refresh
  // of the workspace contained in this card.
  reload(): Promise<void>;
}

export class WorkspaceCard extends React.Component<WorkspaceCardProps, WorkspaceCardState> {
  constructor(props) {
    super(props);
    this.state = {
      bugReportError: '',
      bugReportOpen: false,
      confirmDeleting: false,
      loadingData: false,
      sharing: false,
      showResearchPurposeReviewModal: false,
      userRoles: null,
      workspaceDeletionError: false
    };
  }

  async deleteWorkspace() {
    const {workspace} = this.props;
    this.setState({
      confirmDeleting: false,
      loadingData: true});
    try {
      await workspacesApi().deleteWorkspace(workspace.namespace, workspace.id);
      this.setState({loadingData: false});
      await this.props.reload();
    } catch (e) {
      this.setState({workspaceDeletionError: true, loadingData: false});
    }
  }

  // The function called when the 'share' action is called on a workspace card
  // within the recentWorkspaces list.
  async handleShareAction() {
    this.setState({
      loadingData: true
    });
    const userRolesResponse = await workspacesApi().getFirecloudWorkspaceUserRoles(
      this.props.workspace.namespace,
      this.props.workspace.id);
    // Trigger the sharing dialog to be shown.
    this.setState({
      loadingData: false,
      sharing: true,
      userRoles: userRolesResponse.items});
  }

  async handleShareDialogClose() {
    // Share workspace publishes to current workspace,
    // but here we aren't in the context of a workspace
    // so we need to clear it.
    currentWorkspaceStore.next(undefined);
    this.setState({
      sharing: false,
      userRoles: null});
    this.reloadData();
  }

  submitWorkspaceDeletionError(): void {
    this.setState({
      bugReportOpen: true,
      bugReportError: 'Could not delete workspace.',
      workspaceDeletionError: false});
  }

  // Reloads data by calling the callback from the owning component. This
  // currently causes the workspace-list to reload the entire list of recentWorkspaces.
  async reloadData() {
    await this.props.reload();
  }

  handleReviewResearchPurpose() {
    const {workspace} = this.props;
    navigate(['workspaces', workspace.namespace, workspace.id, 'about']);

  }

  onClick() {
    const {workspace} = this.props;
    if (!workspace.researchPurpose.researchPurposeReviewed) {
      this.setState({showResearchPurposeReviewModal: true});
    } else {
      workspace.published ?
          AnalyticsTracker.Workspaces.NavigateToFeatured(workspace.name) :
          triggerEvent(EVENT_CATEGORY, 'navigate', 'Click on workspace name');
      navigate(['workspaces', workspace.namespace, workspace.id, 'data']);
    }
  }

  render() {
    const {userEmail, workspace, accessLevel} = this.props;
    const {bugReportError, bugReportOpen, confirmDeleting, loadingData,
      sharing, showResearchPurposeReviewModal, userRoles, workspaceDeletionError} = this.state;

    return <React.Fragment>
      <WorkspaceCardBase>
        <FlexRow style={{height: '100%'}}>
          <FlexColumn style={styles.workspaceMenuWrapper}>
            <WorkspaceCardMenu
              workspace={workspace}
              accessLevel={accessLevel}
              onDelete={() => {
                triggerEvent(
                  EVENT_CATEGORY, 'delete', 'Card menu - click delete');
                this.setState({confirmDeleting: true});
              }}
              onShare={() => {
                triggerEvent(EVENT_CATEGORY, 'share', 'Card menu - click share');
                this.handleShareAction();
              }}
              disabled={false}
            />
          </FlexColumn>
          <FlexColumn
            style={{
              ...styles.workspaceCard,
              padding: '.5rem',
            }}
            data-test-id='workspace-card'
          >
            {loadingData && <SpinnerOverlay/>}
            <FlexColumn style={{marginBottom: 'auto'}}>
              <FlexRow style={{ alignItems: 'flex-start' }}>
                <Clickable>
                  <div style={styles.workspaceName}
                       data-test-id='workspace-card-name'
                       onClick={() => this.onClick()}>
                    {workspace.name}
                  </div>
                </Clickable>
              </FlexRow>
              {
                workspace.researchPurpose.reviewRequested === true &&
                workspace.researchPurpose.approved === false &&
                <div style={{color: colors.danger}}>
                  <ClrIcon shape='exclamation-triangle' className='is-solid'
                           style={{fill: colors.danger}}/>
                  Rejected
                </div>
              }
            </FlexColumn>
            <div
              style={{
                ...styles.permissionBox,
                backgroundColor: colors.workspacePermissionsHighlights[accessLevel]
              }}
             data-test-id='workspace-access-level'
            >
              {accessLevel}
            </div>
            <div style={{fontSize: 12}}>
              Last Changed: {displayDate(workspace.lastModifiedTime)}
            </div>
          </FlexColumn>
        </FlexRow>
      </WorkspaceCardBase>
      {workspaceDeletionError && <Modal>
        <ModalTitle>Error: Could not delete workspace '{workspace.name}'</ModalTitle>
        <ModalBody style={{display: 'flex', flexDirection: 'row'}}>
          Please{' '}
          <Link onClick={() => this.submitWorkspaceDeletionError()}>submit a bug report.</Link>
        </ModalBody>
        <ModalFooter>
          <Button type='secondary'
                  onClick={() => this.setState({workspaceDeletionError: false})}>Close</Button>
        </ModalFooter>
      </Modal>}
      {confirmDeleting &&
      <ConfirmDeleteModal data-test-id='confirm-delete-modal'
                          resourceType={ResourceType.WORKSPACE}
                          resourceName={workspace.name}
                          receiveDelete={() => {
                            AnalyticsTracker.Workspaces.Delete();
                            this.deleteWorkspace();
                          }}
                          closeFunction={() => {this.setState({confirmDeleting: false}); }}/>}
      {sharing && <WorkspaceShare data-test-id='workspace-share-modal'
                                  workspace={workspace}
                                  accessLevel={accessLevel}
                                  userEmail={userEmail}
                                  sharing={sharing}
                                  userRoles={userRoles}
                                  onClose={() => this.handleShareDialogClose()} />}
      {bugReportOpen && <BugReportModal bugReportDescription={bugReportError}
                                        onClose={() => this.setState({bugReportOpen: false})}/>}
      {showResearchPurposeReviewModal && <Modal>
        <ModalTitle>Please review Research Purpose for Workspace '{workspace.name}'</ModalTitle>
        <ModalBody style={{display: 'flex', flexDirection: 'column'}}>
          <div>
            Please review the workspace description to make sure it is current. To provide
            transparency to <AouTitle/> participants, your answers below
            will be made available publicly in the <a href='https://www.researchallofus.org/research-projects-directory/'
            target='_blank'>Research Hub Directory</a> on our public website.
          </div>
        </ModalBody>
        <ModalFooter>
          <Button type='primary' style={{marginLeft: '1rem', marginRight: '1rem'}}
                  onClick={() => this.handleReviewResearchPurpose()}>REVIEW NOW</Button>
          <Button type='secondary'
                  onClick={() => this.setState({showResearchPurposeReviewModal: false})}>REVIEW LATER</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;

  }
}
