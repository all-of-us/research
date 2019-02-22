import {Location} from '@angular/common';
import {Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {hasRegisteredAccess} from 'app/utils';
import {routeConfigDataStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {Authority, BillingProjectStatus} from 'generated';

@Component({
  selector: 'app-signed-in',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/errors.css'],
  templateUrl: './component.html'
})
export class SignedInComponent implements OnInit, OnDestroy {
  hasDataAccess = true;
  hasReviewResearchPurpose = false;
  hasReviewIdVerification = false;
  showBugReportItem = !environment.useZendeskForSupport;
  showHelpDeskItem = environment.useZendeskForSupport;
  showHubForumItem = environment.useZendeskForSupport;
  headerImg = '/assets/images/all-of-us-logo.svg';
  givenName = '';
  familyName = '';
  // The email address of the AoU researcher's Google Account. Note that this
  // address should *not* be used for contact purposes, since the AoU GSuite
  // doesn't provide Gmail!
  aouAccountEmailAddress = '';
  // The researcher's preferred contact email address.
  contactEmailAddress = '';
  profileImage = '';
  sidenavToggle = false;
  publicUiUrl = environment.publicUiUrl;
  minimizeChrome = false;
  // True if the user tried to open the Zendesk support widget and an error
  // occurred.
  zendeskLoadError = false;
  billingProjectInitialized = false;
  billingProjectQuery: NodeJS.Timer;
  private profileLoadingSub: Subscription;
  private profileBlockingSub: Subscription;
  private subscriptions = [];

  bugReportOpen: boolean;

  @ViewChild('sidenavToggleElement') sidenavToggleElement: ElementRef;

  @ViewChild('sidenav') sidenav: ElementRef;

  @HostListener('document:click', ['$event'])
  onClickOutsideSideNav(event: MouseEvent) {
    const inSidenav = this.sidenav.nativeElement.contains(event.target);
    const inSidenavToggle = this.sidenavToggleElement.nativeElement.contains(event.target);
    if (this.sidenavToggle && !(inSidenav || inSidenavToggle)) {
      this.sidenavToggle = false;
    }
  }

  constructor(
    /* Ours */
    public errorHandlingService: ErrorHandlingService,
    private signInService: SignInService,
    private serverConfigService: ServerConfigService,
    private profileStorageService: ProfileStorageService,
    /* Angular's */
    private locationService: Location,
  ) {}

  ngOnInit(): void {
    this.serverConfigService.getConfig().subscribe((config) => {
      this.profileLoadingSub = this.profileStorageService.profile$.subscribe((profile) => {
        this.hasDataAccess =
          !config.enforceRegistered || hasRegisteredAccess(profile.dataAccessLevel);

        this.hasReviewResearchPurpose =
          profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
        this.hasReviewIdVerification =
          profile.authorities.includes(Authority.REVIEWIDVERIFICATION);
        this.givenName = profile.givenName;
        this.familyName = profile.familyName;
        this.aouAccountEmailAddress = profile.username;
        this.contactEmailAddress = profile.contactEmail;
      });
    });

    // TODO: Remove or move into CSS.
    document.body.style.backgroundColor = '#f1f2f2';
    this.signInService.isSignedIn$.subscribe(signedIn => {
      if (signedIn) {
        this.profileImage = this.signInService.profileImage;
      } else {
        this.navigateSignOut();
      }
    });

    this.profileBlockingSub = this.profileStorageService.profile$.subscribe((profile) => {
      // This will block workspace creation until the billing project is initialized
      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.billingProjectInitialized = true;
      } else {
        this.billingProjectQuery = setTimeout(() => {
          this.profileStorageService.reload();
        }, 10000);
      }
    });

    this.subscriptions.push(routeConfigDataStore.subscribe(({minimizeChrome}) => {
      this.minimizeChrome = minimizeChrome;
    }));
  }

  ngOnDestroy() {
    if (this.profileLoadingSub) {
      this.profileLoadingSub.unsubscribe();
    }
    if (this.profileBlockingSub) {
      this.profileBlockingSub.unsubscribe();
    }
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  signOut(): void {
    this.signInService.signOut();
    this.navigateSignOut();
  }

  private navigateSignOut(): void {
    // Force a hard browser reload here. We want to ensure that no local state
    // is persisting across user sessions, as this can lead to subtle bugs.
    window.location.assign('/');
  }


  get reviewWorkspaceActive(): boolean {
    return this.locationService.path() === '/admin/review-workspace';
  }

  get userAdminActive(): boolean {
    return this.locationService.path() === '/admin/user';
  }

  get homeActive(): boolean {
    return this.locationService.path() === '';
  }

  get workspacesActive(): boolean {
    return this.locationService.path() === '/workspaces';
  }

  get createWorkspaceActive(): boolean {
    return this.locationService.path() === '/workspaces/build';
  }

  openDataBrowser(): void {
    window.open(this.publicUiUrl, '_blank');
  }

  openZendeskWidget(): void {
    // Note: we're string-protecting our access of the 'zE' property, since
    // this property is dynamically loaded by the Zendesk web widget snippet,
    // and can't properly be typed. If for some reason the support widget is
    // unavailable, we'll show a notice to the user.
    if (window['zE'] == null) {
      // Show an error message to the user, asking them to reload or contact
      // support via email.
      this.zendeskLoadError = true;
      console.error('Error loading Zendesk widget');
      return;
    }

    // In theory, this webWidget call should identify the user who is filing
    // this support request.
    //
    // In practice, the values provided via 'identify' don't seem to do much.
    // Zendesk uses values from the 'prefill' action to assign the user email
    // in the created ticket, which means that for AoU these tickets won't be
    // correctly associated with the researcher's AoU Google Account. See
    // the Zendesk integration doc for more discussion.
    window['zE']('webWidget', 'identify', {
      name: `${this.givenName} ${this.familyName}`,
      email: this.aouAccountEmailAddress,
    });

    window['zE']('webWidget', 'prefill', {
      name: {
        value: `${this.givenName} ${this.familyName}`,
        readOnly: true,
      },
      // For the contact email we use the user's *contact* email address,
      // since the AoU email address is not a valid email inbox.
      email: {
        value: this.contactEmailAddress,
        readOnly: true,
      },
    });

    // Trigger the widget to open the full contact form (instead of the
    // help icon, which is the starting state).
    window['zE']('webWidget', 'open');
  }

  openHubForum(): void {
    window.open(environment.zendeskHelpCenterUrl, '_blank');
  }

}
