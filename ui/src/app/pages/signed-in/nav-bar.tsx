import {Component, Input} from '@angular/core';
import {AccessRenewalNotificationMaybe} from 'app/components/access-renewal-notification';
import {Breadcrumb} from 'app/components/breadcrumb';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {SideNav} from 'app/components/side-nav';
import {StatusAlertBanner} from 'app/components/status-alert-banner';
import {statusAlertApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {cookiesEnabled} from 'app/utils/cookies';
import {ProfileStore} from 'app/utils/stores';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {environment} from "../../../environments/environment";

const styles = reactStyles({
  headerContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    boxShadow: '3px 0px 10px',
    paddingTop: '1rem',
    paddingBottom: '0.5rem',
    paddingRight: '30px',
    backgroundColor: colors.white,
    /*
     * NOTE: if you ever need to change this number, you need to ALSO change the
     * min-height calc in .content-container in signed-in/component.css or we'll
     * wind up with a container that is either too short or so tall it creates a
     * scrollbar
     */
    height: '4rem',
  },
  sidenavToggle: {
    transform: 'rotate(0deg)',
    display: 'inline-block',
    marginLeft: '1rem',
    transition: 'transform 0.5s',
  },
  sidenavIcon: {
    width: '1.5rem',
    height: '1.5rem',
    fill: colors.accent,
  },
  sidenavIconHovering: {
    cursor: 'pointer',
  },
  headerImage: {
    height: '57px',
    width: '155px',
    marginLeft: '1rem',
  },
  displayTag: {
    marginLeft: '1rem',
    height: '12px',
    width: '155px',
    borderRadius: '2px',
    backgroundColor: colors.primary,
    color: colors.white,
    fontFamily: 'Montserrat',
    fontSize: '8px',
    lineHeight: '12px',
    textAlign: 'center',
  }
});

export interface Props {
  profileState: ProfileStore;
  bannerAdminActive: boolean;
  workspaceAdminActive: boolean;
  profileImage: string;
  sidenavToggle: boolean;
  homeActive: boolean;
  workspacesActive: boolean;
  libraryActive: boolean;
  profileActive: boolean;
  userAdminActive: boolean;
  userAuditActive: boolean;
  workspaceAuditActive: boolean;
}

export interface State {
  sideNavVisible: boolean;
  statusAlertVisible: boolean;
  statusAlertDetails: {
    statusAlertId: number;
    title: string;
    message: string;
    link: string;
  };
  barsTransform: string;
  hovering: boolean;
  wrapperRef: React.RefObject<HTMLDivElement>;
}

const barsTransformNotRotated = 'rotate(0deg)';
const barsTransformRotated = 'rotate(90deg)';

const cookieKey = 'status-alert-banner-dismissed';

export const NavBar = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      // Bind the this context - this will be passed down into the actual
      // sidenav / alert banner so clicks on it can close the modal
      this.onToggleSideNav = this.onToggleSideNav.bind(this);
      this.handleClickOutside = this.handleClickOutside.bind(this);
      this.handleStatusAlertBannerUnmount = this.handleStatusAlertBannerUnmount.bind(this);
      this.state = {
        sideNavVisible: false,
        statusAlertVisible: false,
        statusAlertDetails: {
          statusAlertId: 0,
          title: '',
          message: '',
          link: '',
        },
        barsTransform: barsTransformNotRotated,
        hovering: false,
        wrapperRef: React.createRef(),
      };
    }

    async componentDidMount() {
      document.addEventListener('click', this.handleClickOutside);
      const statusAlert = await statusAlertApi().getStatusAlert();
      const statusAlertVisible = this.statusAlertVisible(statusAlert.statusAlertId, statusAlert.message);
      if (!!statusAlert) {
        this.setState({
          statusAlertVisible: statusAlertVisible,
          statusAlertDetails: {
            statusAlertId: statusAlert.statusAlertId,
            title: statusAlert.title,
            message: statusAlert.message,
            link: statusAlert.link
          }
        });
      }
    }

    componentWillUnmount() {
      document.removeEventListener('click', this.handleClickOutside);
    }

    onToggleSideNav() {
      this.setState(previousState => ({sideNavVisible: !previousState.sideNavVisible}));
      this.setState(previousState => ({
        barsTransform: previousState.barsTransform === barsTransformNotRotated
            ? barsTransformRotated
            : barsTransformNotRotated
      }));
    }

    handleClickOutside(event) {
      if (
          this.state.wrapperRef
          && !this.state.wrapperRef.current.contains(event.target)
          && this.state.sideNavVisible
      ) {
        this.onToggleSideNav();
      }
    }

    statusAlertVisible(statusAlertId, statusAlertMessage) {
      if (cookiesEnabled()) {
        const cookie = localStorage.getItem(cookieKey);
        return (!cookie || (cookie && cookie !== `${statusAlertId}`)) && !!statusAlertMessage;
      } else {
        return !!statusAlertMessage;
      }
    }

    navigateToLink(link) {
      window.open(link, '_blank');
    }

    handleStatusAlertBannerUnmount() {
      if (cookiesEnabled()) {
        localStorage.setItem(cookieKey, `${this.state.statusAlertDetails.statusAlertId}`);
      }
      this.setState({statusAlertVisible: false});
    }

    render() {
      return <div
          style={styles.headerContainer}
          ref={this.state.wrapperRef}
      >
        <div style={{
          transform: this.state.barsTransform,
          display: 'inline-block',
          marginLeft: '1rem',
          transition: 'transform 0.5s',
        }}>
          <ClrIcon
              shape='bars'
              onClick={() => this.onToggleSideNav()}
              onMouseEnter={() => this.setState({hovering: true})}
              onMouseLeave={() => this.setState({hovering: false})}
              style={this.state.hovering
                  ? {...styles.sidenavIcon, ...styles.sidenavIconHovering}
                  : {...styles.sidenavIcon}}
          >
          </ClrIcon>
        </div>
        <div>
          <a href={'/'}>
            <img
                src='/assets/images/all-of-us-logo.svg'
                style={styles.headerImage}
            />
          </a>
          {
            environment.shouldShowDisplayTag
            && <div style={styles.displayTag}>
              {environment.displayTag}
            </div>
          }
        </div>
        <Breadcrumb/>
        {window.location.pathname !== '/access-renewal' && <AccessRenewalNotificationMaybe/>}
        {
          this.state.statusAlertVisible && <StatusAlertBanner
              title={this.state.statusAlertDetails.title}
              message={this.state.statusAlertDetails.message}
              footer={
                this.state.statusAlertDetails.link &&
                <Button data-test-id='status-banner-read-more-button'
                        onClick={() => this.navigateToLink(this.state.statusAlertDetails.link)}>
                  READ MORE
                </Button>
              }
              onClose={this.handleStatusAlertBannerUnmount}
          />
        }
        {
          this.state.sideNavVisible
          && <SideNav
              profile={this.props.profileState.profile}
              bannerAdminActive={this.props.bannerAdminActive}
              homeActive={this.props.homeActive}
              libraryActive={this.props.libraryActive}
              // Passing the function itself deliberately, we want to be able to
              // toggle the nav whenever we click anything in it
              onToggleSideNav={this.onToggleSideNav}
              profileActive={this.props.profileActive}
              userAdminActive={this.props.userAdminActive}
              userAuditActive={this.props.userAuditActive}
              workspaceAuditActive={this.props.workspaceAuditActive}
              workspaceAdminActive={this.props.workspaceAdminActive}
              workspacesActive={this.props.workspacesActive}
          />
        }
      </div>;
    }
  }
);

@Component({
  selector: 'app-nav-bar',
  template: '<div #root></div>'
})
export class NavBarComponent extends ReactWrapperBase {
  @Input('bannerAdminActive') bannerAdminActive: Props['bannerAdminActive'];
  @Input('workspaceAdminActive') workspaceAdminActive: Props['workspaceAdminActive'];
  @Input('homeActive') homeActive: Props['homeActive'];
  @Input('workspacesActive') workspacesActive: Props['workspacesActive'];
  @Input('libraryActive') libraryActive: Props['libraryActive'];
  @Input('profileActive') profileActive: Props['profileActive'];
  @Input('userAdminActive') userAdminActive: Props['userAdminActive'];
  constructor() {
    super(NavBar, [
      'bannerAdminActive',
      'workspaceAdminActive',
      'homeActive',
      'workspacesActive',
      'libraryActive',
      'profileActive',
      'userAdminActive',
      'userAuditActive',
      'workspaceAuditActive'
    ]);
  }
}
