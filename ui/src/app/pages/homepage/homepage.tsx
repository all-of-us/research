import {Component} from '@angular/core';
import {navigate, queryParamsStore, serverConfigStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';

import {
  CardButton,
  Clickable,
} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {Header, SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Modal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {QuickTourReact} from 'app/pages/homepage/quick-tour-modal';
import {RecentWork} from 'app/pages/homepage/recent-work';
import {getRegistrationTasksMap, RegistrationDashboard} from 'app/pages/homepage/registration-dashboard';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {hasRegisteredAccessFetch, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {
  BillingProjectStatus,
  Profile,
} from 'generated/fetch';

export const styles = reactStyles({
  mainHeader: {
    color: colors.primary, fontSize: 28, fontWeight: 400,
    display: 'flex', letterSpacing: 'normal'
  },
  pageWrapper: {
    marginLeft: '-1rem', marginRight: '-0.6rem', display: 'flex',
    flexDirection: 'column', justifyContent: 'space-between'
  },
  welcomeMessage: {
    marginLeft: '3%', display: 'flex', flexDirection: 'row'
  },
  welcomeMessageIconRow: {
    display: 'flex', flexDirection: 'row', alignItems: 'flex-end', marginLeft: '1rem'
  },
  welcomeMessageIcon: {
    height: '2.25rem', width: '2.75rem'
  },
  fadeBox: {
    margin: '1rem 0 0 3%', width: '97.5%', padding: '0 0.1rem'
  },
  singleCard: {
    width: '87.34%', minHeight: '18rem', maxHeight: '26rem',
    display: 'flex', flexDirection: 'column', borderRadius: '5px',
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    border: 'none', marginTop: '1rem'
  },
  contentWrapperLeft: {
    display: 'flex', flexDirection: 'column', paddingLeft: '3%', width: '40%'
  },
  contentWrapperRight: {
    display: 'flex', flexDirection: 'column', justifyContent: 'space-between', width: '60%'
  },
  quickTour: {
    display: 'flex', flexDirection: 'column', marginLeft: '3%'
  },
  quickTourCardsRow: {
    display: 'flex', justifyContent: 'flex-start', maxHeight: '26rem',
    flexDirection: 'row', marginTop: '0.5rem'
  },
  quickTourLabel: {
    fontSize: 18, lineHeight: '34px', color: colors.primary, paddingRight: '2.3rem',
    fontWeight: 600, marginTop: '2rem', width: '33%'
  },
  footer: {
    width: '100%', backgroundColor: colors.light, marginTop: '2%'
  },
  footerInner: {
    display: 'flex', flexDirection: 'column', marginLeft: '3%', marginRight: '2%',
  },
  footerTitle: {
    height: '34px', opacity: 0.87, color: colors.primary, fontSize: '0.75rem',
    fontWeight: 600, lineHeight: '34px', marginTop: '1rem', marginBottom: '0.5rem'
  },
  footerText: {
    height: '176px', opacity: 0.87, color: colors.secondary, fontSize: '0.6rem',
    fontWeight: 500, lineHeight: '30px', display: 'flex', width: '100%',
    flexDirection: 'column', flexWrap: 'nowrap', overflowY: 'auto'
  },
  footerTextTitle: {
    color: colors.primary,
    fontSize: '0.67rem',
    fontWeight: 500
  },
  linksBlock: {
    display: 'flex', marginBottom: '1rem',
    flexDirection: 'column', flexShrink: 1, minWidth: '13rem'
  },
  bottomBanner: {
    width: '100%', display: 'flex', backgroundColor: colors.primary,
    paddingLeft: '3.5rem', alignItems: 'center'
  },
  logo: {
    height: '3.5rem', width: '7rem', lineHeight: '85px'
  },
  bottomLinks: {
    color: colors.white, fontSize: '.5rem', height: '1rem',
    marginLeft: '2.5rem', fontWeight: 400
  }
});

export const Homepage = withUserProfile()(class extends React.Component<
  { profileState: { profile: Profile, reload: Function } },
  { accessTasksLoaded: boolean,
    accessTasksRemaining: boolean,
    betaAccessGranted: boolean,
    billingProjectInitialized: boolean,
    dataUseAgreementCompleted: boolean,
    eraCommonsError: string,
    eraCommonsLinked: boolean,
    firstVisit: boolean,
    firstVisitTraining: boolean,
    quickTour: boolean,
    trainingCompleted: boolean,
    twoFactorAuthCompleted: boolean,
    videoOpen: boolean,
    videoLink: string
  }> {
  private pageId = 'homepage';
  private timer: NodeJS.Timer;

  constructor(props: any) {
    super(props);
    this.state = {
      accessTasksLoaded: false,
      accessTasksRemaining: undefined,
      betaAccessGranted: undefined,
      billingProjectInitialized: false,
      dataUseAgreementCompleted: undefined,
      eraCommonsError: '',
      eraCommonsLinked: undefined,
      firstVisit: undefined,
      firstVisitTraining: true,
      quickTour: false,
      trainingCompleted: undefined,
      twoFactorAuthCompleted: undefined,
      videoOpen: false,
      videoLink: '',
    };
  }

  componentDidMount() {
    this.validateNihToken();
    this.callProfile();
    this.checkBillingProjectStatus();
  }

  componentDidUpdate(prevProps) {
    const {profileState: {profile}} = this.props;
    if (!this.state.billingProjectInitialized) {
      this.checkBillingProjectStatus();
    }
    if (!fp.isEqual(prevProps.profileState.profile, profile)) {
      this.callProfile();
    }
  }

  componentWillUnmount() {
    clearTimeout(this.timer);
  }

  async validateNihToken() {
    const token = (new URL(window.location.href)).searchParams.get('token');
    if (token) {
      try {
        await profileApi().updateNihToken({ jwt: token });
      } catch (e) {
        this.setState({eraCommonsError: 'Error saving NIH Authentication status.'});
      }
    }
  }

  setFirstVisit() {
    this.setState({firstVisit: true});
    profileApi().updatePageVisits({ page: this.pageId});
  }

  async syncCompliance() {
    const complianceStatus = profileApi().syncComplianceTrainingStatus().then(result => {
      this.setState({
        trainingCompleted: getRegistrationTasksMap()['complianceTraining'].isComplete(result)
      });
    }).catch(err => {
      this.setState({trainingCompleted: false});
      console.error('error fetching moodle training status:', err);
    });
    const twoFactorAuthStatus = profileApi().syncTwoFactorAuthStatus().then(result => {
      this.setState({
        twoFactorAuthCompleted: getRegistrationTasksMap()['twoFactorAuth'].isComplete(result)
      });
    }).catch(err => {
      this.setState({twoFactorAuthCompleted: false});
      console.error('error fetching two factor auth status:', err);
    });
    return Promise.all([complianceStatus, twoFactorAuthStatus]);
  }

  async callProfile() {
    const {profileState: {profile, reload}} = this.props;

    if (fp.isEmpty(profile)) {
      setTimeout(() => {
        reload();
      }, 10000);
    } else {
      if (!profile.betaAccessRequestTime) {
        profileApi().requestBetaAccess();
      }
      if (profile.pageVisits) {
        if (!profile.pageVisits.some(v => v.page === this.pageId)) {
          this.setFirstVisit();
        }
        if (profile.pageVisits.some(v => v.page === 'moodle')) {
          this.setState({firstVisitTraining: false});
        }
      } else {
        // page visits is null; is first visit
        this.setFirstVisit();
      }

      this.setState({
        eraCommonsLinked: getRegistrationTasksMap()['eraCommons'].isComplete(profile),
        dataUseAgreementCompleted: (serverConfigStore.getValue().enableDataUseAgreement ?
          (() => getRegistrationTasksMap()['dataUseAgreement'].isComplete(profile))() : true)
      });
      this.setState({betaAccessGranted: !!profile.betaAccessBypassTime});

      const {workbenchAccessTasks} = queryParamsStore.getValue();
      const hasRegisteredAccess = hasRegisteredAccessFetch(profile.dataAccessLevel);
      if (!hasRegisteredAccess || workbenchAccessTasks) {
        await this.syncCompliance();
      }
      if (workbenchAccessTasks) {
        this.setState({accessTasksRemaining: true, accessTasksLoaded: true});
      } else {
        try {
          if (serverConfigStore.getValue().enforceRegistered) {
            this.setState({
              accessTasksRemaining: !hasRegisteredAccess,
              accessTasksLoaded: true
            });
          } else {
            this.setState({accessTasksRemaining: false, accessTasksLoaded: true});
          }
        } catch (ex) {
          console.error('error fetching config: ' + ex.toString());
        }
      }
    }
    this.setState((state, props) => ({
      quickTour: state.firstVisit && state.accessTasksRemaining === false
    }));
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

  openVideo(videoLink: string): void {
    this.setState({videoOpen: true, videoLink: videoLink});
  }


  render() {
    const {billingProjectInitialized, betaAccessGranted,
      videoOpen, accessTasksLoaded, accessTasksRemaining, eraCommonsLinked,
      eraCommonsError, firstVisitTraining, trainingCompleted, quickTour, videoLink,
      twoFactorAuthCompleted, dataUseAgreementCompleted
    } = this.state;

    const canCreateWorkspaces = billingProjectInitialized ||
      serverConfigStore.getValue().useBillingProjectBuffer;

    const quickTourResources = [
      {
        src: '/assets/images/QT-thumbnail.svg',
        onClick: () => this.setState({quickTour: true})
      }, {
        src: '/assets/images/cohorts-thumbnail.png',
        onClick: () => this.openVideo('/assets/videos/Workbench Tutorial - Cohorts.mp4')
      }, {
        src: '/assets/images/notebooks-thumbnail.png',
        onClick: () => this.openVideo('/assets/videos/Workbench Tutorial - Notebooks.mp4')
      }
    ];
    const footerLinks = [{
      title: 'Working Within Researcher Workbench',
      links: ['Researcher Workbench Mission',
        'User interface components',
        'What to do when things go wrong',
        'Contributing to the Workbench']
    },
      {
        title: 'Workspace',
        links: ['Workspace interface components',
          'User interface components',
          'Collaborating with other researchers',
          'Sharing and Publishing Workspaces']
      },
      {
        title: 'Working with Notebooks',
        links: ['Notebook interface components',
          'Notebooks and data',
          'Collaborating with other researchers',
          'Sharing and Publishing Notebooks']
      }];

    return <React.Fragment>
      <div style={styles.pageWrapper}>
        <div style={styles.welcomeMessage}>
          <div style={{display: 'flex', flexDirection: 'column', width: '50%'}}>
            <div style={{display: 'flex', flexDirection: 'row'}}>
              <div style={{display: 'flex', flexDirection: 'column'}}>
                <Header style={{fontWeight: 500, color: colors.secondary, fontSize: '0.92rem'}}>
                  Welcome to</Header>
                <Header style={{textTransform: 'uppercase', marginTop: '0.2rem'}}>
                  Researcher Workbench</Header>
              </div>
              <div style={styles.welcomeMessageIconRow}>
                <img style={styles.welcomeMessageIcon} src='/assets/images/workspace-icon.svg'/>
                <img style={styles.welcomeMessageIcon} src='/assets/images/cohort-icon.svg'/>
                <img style={styles.welcomeMessageIcon} src='/assets/images/analysis-icon.svg'/>
              </div>
            </div>
            <SmallHeader style={{color: colors.primary, marginTop: '0.25rem'}}>
              the secure analysis platform for All of Us data</SmallHeader>
          </div>
          <div></div>
        </div>
        <FadeBox style={styles.fadeBox}>
          <div style={{display: 'flex', justifyContent: 'center'}}>
            <div style={styles.singleCard}>
              {accessTasksLoaded ?
                (accessTasksRemaining ?
                    (<RegistrationDashboard eraCommonsLinked={eraCommonsLinked}
                                            eraCommonsError={eraCommonsError}
                                            trainingCompleted={trainingCompleted}
                                            firstVisitTraining={firstVisitTraining}
                                            betaAccessGranted={betaAccessGranted}
                                            twoFactorAuthCompleted={twoFactorAuthCompleted}
                                            dataUseAgreementCompleted={dataUseAgreementCompleted}/>
                    ) : (
                      <div style={{display: 'flex', flexDirection: 'row', paddingTop: '2rem'}}>
                        <div style={styles.contentWrapperLeft}>
                          <div style={styles.mainHeader}>Researcher Workbench</div>
                          <TooltipTrigger disabled={canCreateWorkspaces} content={<div>
                            Your Firecloud billing project is still being initialized.
                            Workspace creation will be available in a few minutes.</div>}>
                            <CardButton disabled={!canCreateWorkspaces}
                                        onClick={() => navigate(['workspaces/build'])}
                                        style={{margin: '1.9rem 106px 0 3%'}}>
                              Create a <br/> New Workspace
                              <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                            </CardButton>
                          </TooltipTrigger>
                        </div>
                        <div style={styles.contentWrapperRight}>
                          <a onClick={() => navigate(['workspaces'])}
                             style={{fontSize: '14px', color: colors.primary}}>
                            See All Workspaces</a>
                          <div style={{marginRight: '3%', display: 'flex',
                            flexDirection: 'column'}}>
                            <div style={{color: colors.primary, height: '1.9rem'}}>
                              <div style={{marginTop: '.5rem'}}>Your Last Accessed Items</div>
                            </div>
                            <RecentWork dark={true}/>
                          </div>
                        </div>
                      </div>)
                ) :
                <Spinner dark={true} style={{width: '100%', marginTop: '5rem'}}/>}
            </div>
          </div>
        </FadeBox>
        <div>
          <div style={styles.quickTour}>
            <div style={styles.quickTourLabel}>Quick Tour and Videos</div>
            <div style={styles.quickTourCardsRow}>
              {quickTourResources.map((thumbnail, i) => {
                return <React.Fragment key={i}>
                  <Clickable onClick={thumbnail.onClick}
                             data-test-id={'quick-tour-resource-' + i}>
                    <img style={{maxHeight: '7rem', width: '9rem', marginRight: '0.5rem'}}
                         src={thumbnail.src}/>
                  </Clickable>
                </React.Fragment>;
              })}
            </div>
          </div>
          <div>
            <div style={styles.footer}>
              <div style={styles.footerInner}>
                <div style={styles.footerTitle}>
                  How to Use the All of Us Researcher Workbench</div>
                <div style={{display: 'flex', flexDirection: 'row',
                  justifyContent: 'space-between'}}>
                  {footerLinks.map((col, i) => {
                    return <React.Fragment key={i}>
                      <div style={styles.linksBlock}>
                        <div style={styles.footerText}>
                          <div style={styles.footerTextTitle}>{col.title}</div>
                          <ul style={{color: colors.secondary, marginLeft: '2%'}}>
                            {col.links.map((link, ii) => {
                              return <li key={ii}>
                                <a href='#' style={{color: colors.secondary}}>{link}</a>
                              </li>;
                            } )}
                          </ul>
                        </div>
                      </div>
                    </React.Fragment>;
                  })}
                </div>
              </div>
            </div>
          </div>
          <div style={styles.bottomBanner}>
            <div style={styles.logo}>
              <img src='/assets/images/all-of-us-logo-footer.svg'/>
            </div>
            <div style={styles.bottomLinks}>Copyright ©2018</div>
            <div style={styles.bottomLinks}>Privacy Policy</div>
            <div style={styles.bottomLinks}>Terms of Service</div>
          </div>
        </div>
      </div>
      {quickTour &&
      <QuickTourReact closeFunction={() => this.setState({quickTour: false})} />}
      {videoOpen && <Modal width={900}>
        <div style={{display: 'flex'}}>
          <div style={{flexGrow: 1}}></div>
          <Clickable onClick={() => this.setState({videoOpen: false})}>
            <ClrIcon
              shape='times'
              size='24'
              style={{color: colors.accent, marginBottom: 17}}
            />
          </Clickable>
        </div>
        <video width='100%' controls autoPlay>
          <source src={videoLink} type='video/mp4'/>
        </video>
      </Modal>}
    </React.Fragment>;
  }

});

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class HomepageComponent extends ReactWrapperBase {
  constructor() {
    super(Homepage, []);
  }
}
