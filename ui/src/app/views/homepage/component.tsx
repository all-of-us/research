import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {environment} from 'environments/environment';
import {navigate, navigateByUrl} from 'app/utils/navigation';

import * as React from 'react';

import {
  BillingProjectStatus,
  IdVerificationStatus,
  PageVisit,
  Profile,
  ProfileService
} from 'generated';
import {reactStyles, ReactWrapperBase} from "app/utils";
import {
  Clickable,
  styles as buttonStyles
} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';


const styles = reactStyles({
  mainHeader: {
    color: '#FFFFFF',
    fontSize: 28,
    fontWeight: 400,
    width: '25.86%',
    display: 'flex',
    minWidth: '18.2rem',
    marginLeft: '4%',
    marginTop: '4%',
    letterSpacing: 'normal'
  },
  minorHeader: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 600,
    display: 'flex',
    marginTop: '1rem',
    lineHeight: '24px'
  },
  text: {
    color: '#FFFFFF',
    fontSize: 16,
    lineHeight: '22px',
    fontWeight: 150,
    marginTop: '3%'
  },
  infoBox: {
    padding: '1rem',
    backgroundColor: '#FFFFFF',
    borderRadius: '5px',
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  infoBoxHeader: {
    fontSize: 16,
    color: '#262262',
    fontWeight: 600
  },
  infoBoxBody: {
    color: '#000',
    lineHeight: '18px'
  },
  infoBoxButton: {
    color: '#FFFFFF',
    height: '49px',
    borderRadius: '5px',
    marginLeft: '1rem',
    maxWidth: '20rem'
  }
});

const AccountLinkingButton: React.FunctionComponent<{
  failed: boolean, completed: boolean, failedText: string,
  completedText: string, defaultText: string, onClick: Function
}> = ({failed, completed, defaultText, completedText, failedText, onClick}) => {
  if (failed) {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#f27376'}} disabled={true}>
      <ClrIcon shape='exclamation-triangle'/>{failedText}
    </Clickable>;
  } else if (completed) {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#8BC990'}} disabled={true}>
      <ClrIcon shape='check'/>{completedText}
    </Clickable>;
  } else {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#2691D0'}}
                      onClick={onClick}>
      {defaultText}
    </Clickable>;
  }
};

export interface AccountLinkingState {
  eraCommonsLinked: boolean;
  trainingCompleted: boolean;
}

export class AccountLinking extends React.Component<{}, AccountLinkingState> {

  constructor(props: {}) {
    super(props);
    this.state = {
      eraCommonsLinked: false,
      trainingCompleted: false
    }
  }

  static redirectToNiH(): void {
    const url = environment.shibbolethUrl + '/link-nih-account?redirect-url=' +
        encodeURIComponent(window.location.href + 'nih-callback?token={token}');
    window.location.assign(url);
  }

  render() {
    return <React.Fragment>
      <div style={{display: 'flex', flexDirection: 'row'}}>
        <div style={{display: 'flex', flexDirection: 'column', width: '50%'}}>
          <div style={styles.mainHeader}>Researcher Workbench</div>
          <div style={{marginLeft: '2rem', flexDirection: 'column'}}>
            {/*<ClrIcon shape='exclamation-triangle' class='is-solid'/>*/}
            <div style={styles.minorHeader}>In order to get access to data and tools
              please complete the following:</div>
            <div style={styles.text}>Please login to your ERA Commons account and complete
              the online training courses in order to gain full access to the Researcher
              Workbench data and tools.</div>
          </div>
        </div>
        <div style={{flexDirection: 'column', width: '50%', padding: '1rem'}}>
          <div style={styles.infoBox}>
            <div style={{flexDirection: 'column', width: '70%'}}>
              <div style={styles.infoBoxHeader}>Login to ERA Commons</div>
              <div style={styles.infoBoxBody}>Vel illum dolore eu feugiat nulla facilisis at vero
                eros et accumsan et iusto odio dignissim.
                Ullamcorper suscipit lortis nisl ex.</div>
            </div>
            <AccountLinkingButton failed={false}
                                  completed={this.state.eraCommonsLinked}
                                  defaultText='Login'
                                  completedText='Linked'
                                  failedText='Error Linking Accounts'
                                  onClick={AccountLinking.redirectToNiH}/>
          </div>
          <div style={{...styles.infoBox, marginTop: '0.7rem'}}>
            <div style={{flexDirection: 'column', width: '70%'}}>
              <div style={styles.infoBoxHeader}>Complete Online Training</div>
              <div style={styles.infoBoxBody}>Wisi enim ad minim veniam quis nod
                exerci tation ullamcorper suscipit lortis nisl ex. Vel illum dolore
                eu feugiat nulla facilisis at vero eros et accumsan et.
                <br/><br/>If you have completed the training,
                click here to update the page.</div>
            </div>
            <AccountLinkingButton failed={false}
                                  completed={this.state.trainingCompleted}
                                  defaultText='Complete Training'
                                  completedText='Completed'
                                  failedText=''
                                  onClick={() => {}}/>
          </div>
        </div>
      </div>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-account-linking',
  template: '<div #root></div>',
})
export class AccountLinkingComponent extends ReactWrapperBase {
  constructor() {
    super(AccountLinking, []);
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class HomepageComponent implements OnInit, OnDestroy {
  private static pageId = 'homepage';
  @ViewChild('myVideo') myVideo: any;
  profile: Profile;
  view: any[] = [180, 180];
  numberOfTotalTasks = 4;
  completedTasksName = 'Completed';
  unfinishedTasksName = 'Unfinished';
  open = false;
  src = '';
  spinnerValues = [
    {
      'name': this.completedTasksName,
      'value': this.completedTasks
    },
    {
      'name': this.unfinishedTasksName,
      'value': this.numberOfTotalTasks - this.completedTasks
    }
  ];
  billingProjectInitialized = false;
  billingProjectQuery: NodeJS.Timer;
  firstSignIn: Date;
  firstVisit = true;
  newPageVisit: PageVisit = { page: HomepageComponent.pageId};
  footerLinks = [
    {
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
  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;
  quickTour: boolean;
  accountsLinked = false;

  constructor(
    private profileService: ProfileService,
    private profileStorageService: ProfileStorageService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    // create bound methods to use as callbacks
    this.closeQuickTour = this.closeQuickTour.bind(this);
  }

  ngOnInit(): void {
    this.profileService.getMe().subscribe(profile => {
      if (profile.pageVisits) {
        this.firstVisit = !profile.pageVisits.some(v =>
        v.page === HomepageComponent.pageId);
      }
      if (profile.linkedNihUsername) {
        if (profile.linkExpireTime > Date.now()) {
          this.accountsLinked = true;
        }
      }
    },
      e => {},
      () => {
        if (this.firstVisit) {
          this.quickTour = true;
        }
        this.profileService.updatePageVisits(this.newPageVisit).subscribe();
      });

    this.profileStorageService.profile$.subscribe((profile) => {
      // This will block workspace creation until the billing project is initialized
      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.billingProjectInitialized = true;
      } else {
        this.billingProjectQuery = setTimeout(() => {
          this.profileStorageService.reload();
        }, 10000);
      }
      this.profile = profile;
      this.reloadSpinner();
    });
    this.profileStorageService.reload();

  }

  public closeQuickTour(): void {
    this.quickTour = false;
  }

  play(type): void {
    this.src = '/assets/videos/Workbench Tutorial - Cohorts.mp4';
    if (type === 'notebook') {
      this.src = '/assets/videos/Workbench Tutorial - Notebooks.mp4';
    }
    this.open = true;
  }

  public get completedTasks() {
    let completedTasks = 0;
    if (this.profile === undefined) {
      return completedTasks;
    }
    if (this.profile.idVerificationStatus === IdVerificationStatus.VERIFIED) {
      completedTasks += 1;
    }
    if (this.profile.demographicSurveyCompletionTime !== null) {
      completedTasks += 1;
    }
    if (this.profile.ethicsTrainingCompletionTime !== null) {
      completedTasks += 1;
    }
    if (this.profile.termsOfServiceCompletionTime !== null) {
      completedTasks += 1;
    }
    return completedTasks;
  }

  public get completedTasksAsPercentage() {
    return this.completedTasks / this.numberOfTotalTasks * 100;
  }

  reloadSpinner(): void {
    this.spinnerValues = [
      {
        'name': this.completedTasksName,
        'value': this.completedTasks
      },
      {
        'name': this.unfinishedTasksName,
        'value': this.numberOfTotalTasks - this.completedTasks
      }
    ];
  }

  ngOnDestroy(): void {
    clearTimeout(this.billingProjectQuery);
  }

  addWorkspace(): void {
    this.router.navigate(['workspaces/build'], {relativeTo : this.route});
  }

  navigateToProfile(): void {
    this.router.navigate(['profile']);
  }

  listWorkspaces(): void {
    this.router.navigate(['workspaces']);
  }

  get twoFactorBannerEnabled() {
    if (this.firstSignIn == null) {
      return false;
    }
    // Don't show the banner after 1 week as their account would
    // have been disabled had they not enabled 2-factor auth.
    return !(new Date().getTime() - this.firstSignIn.getTime() > 7 * 24 * 60 * 60 * 1000);
  }
}
