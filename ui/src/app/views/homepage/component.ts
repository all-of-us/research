import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {BugReportComponent} from 'app/views/bug-report/component';

import {
  BillingProjectStatus,
  DataAccessLevel,
  IdVerificationStatus,
  Profile,
  ProfileService
} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class HomepageComponent implements OnInit, OnDestroy {
  firstTimeUser = true;
  profile: Profile;
  view: any[] = [180, 180];
  numberOfTotalTasks = 4;
  completedTasksName = 'Completed';
  unfinishedTasksName = 'Unfinished';
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
  cardDetails = [
    {
      position: 'left',
      title: 'Browse All of Us Data',
      text: 'Dolor sit amet consectetuer adipiscing sed diam euismod tincidunt ut laoreet ' +
      'dolore. Mirum est notare, quam littera gothica quam nunc.',
      icon: '/assets/icons/browse-data.svg'
    },
    {
      position: 'right',
      title: 'Explore Public Work',
      text: 'Dolor sit amet consectetuer adipiscing sed diam euismod tincidunt ut laoreet ' +
      'dolore. Mirum est notare, quam littera gothica quam nunc.',
      icon: '/assets/icons/explore.svg'
    }];
  cards: any[] = [];
  private enforceRegistered: boolean;
  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  constructor(private serverConfigService: ServerConfigService,
              private profileService: ProfileService,
              private profileStorageService: ProfileStorageService,
              private route: ActivatedRoute,
              private router: Router,) {
    /*this.cards = [
      {
        title: 'Notebook1',
        description: 'This is a dummy notebook',
        type: 'notebook',
        createdOn: '01/01/2018'
      },
      {
        title: 'Cohort', description: 'This is a dummy cohort', type: 'cohort',
        updatedOn: '07/01/2018'
      },
      {
        title: 'Notebook2',
        description: 'This is a dummy notebook',
        type: 'notebook',
        createdOn: '01/01/2018',
        updatedOn: '04/03/2018'
      },
      {
        title: 'Cohort2', description: 'This is a dummy cohort', type: 'cohort',
        updatedOn: '07/01/2018'
      }];*/
  }

  ngOnInit(): void {
    const currentDate = new Date();
    this.serverConfigService.getConfig().subscribe((config) => {
      this.enforceRegistered = config.enforceRegistered;
    });
    this.profileStorageService.profile$.subscribe((profile) => {
      if (this.firstSignIn === undefined) {
        this.firstSignIn = new Date(profile.firstSignInTime);
      }
      this.firstTimeUser = this.firstSignIn && currentDate - this.firstSignIn < 2000;

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
    this.router.navigate(['workspaces/build'], {relativeTo: this.route});
  }

  navigateToProfile(): void {
    this.router.navigate(['profile']);
  }

  listWorkspaces(): void {
    this.router.navigate(['workspaces']);
  }

  // The user is FC initialized and has access to the CDR, if enforced in this
  // environment.
  hasCdrAccess(): boolean {
    if (!this.profile) {
      return false;
    }
    if (!this.enforceRegistered) {
      return true;
    }
    return [
      DataAccessLevel.Registered,
      DataAccessLevel.Protected
    ].includes(this.profile.dataAccessLevel);
  }

  get twoFactorBannerEnabled() {
    if (this.firstSignIn == null) {
      return false;
    }
    // Don't show the banner after 1 week as their account would
    // have been disabled had they not enabled 2-factor auth.
    if (new Date().getTime() - this.firstSignIn.getTime() > 1 * 7 * 24 * 60 * 60 * 1000) {
      return false;
    }
    return true;
  }
}
