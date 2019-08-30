import {Location} from '@angular/common';
import {AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';
import {debouncer, hasRegisteredAccess} from 'app/utils';
import {cdrVersionStore, routeConfigDataStore} from 'app/utils/navigation';
import {initializeZendeskWidget, openZendeskWidget} from 'app/utils/zendesk';
import {environment} from 'environments/environment';
import {Authority} from 'generated';

@Component({
  selector: 'app-signed-in',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/errors.css'],
  templateUrl: './component.html'
})
export class SignedInComponent implements OnInit, OnDestroy, AfterViewInit {
  hasDataAccess = true;
  hasReviewResearchPurpose = false;
  hasAccessModuleAdmin = false;
  headerImg = '/assets/images/all-of-us-logo.svg';
  displayTag = environment.displayTag;
  shouldShowDisplayTag = environment.shouldShowDisplayTag;
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
  showInactivityModal = true;
  private profileLoadingSub: Subscription;
  private subscriptions = [];

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
    private elementRef: ElementRef
  ) {
    this.closeInactivityModal = this.closeInactivityModal.bind(this);
    console.log('bounded');
  }

  ngOnInit(): void {
    this.serverConfigService.getConfig().subscribe((config) => {
      this.profileLoadingSub = this.profileStorageService.profile$.subscribe((profile) => {
        this.hasDataAccess =
          !config.enforceRegistered || hasRegisteredAccess(profile.dataAccessLevel);

        this.hasReviewResearchPurpose =
          profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
        this.hasAccessModuleAdmin =
          profile.authorities.includes(Authority.ACCESSCONTROLADMIN);
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

    this.subscriptions.push(routeConfigDataStore.subscribe(({minimizeChrome}) => {
      this.minimizeChrome = minimizeChrome;
    }));

    cdrVersionsApi().getCdrVersions().then(resp => {
      cdrVersionStore.next(resp.items);
    });

    const signalUserActivity = debouncer(() => {
      window.postMessage("Frame is active", '*');
    }, 1000);
    window.addEventListener('mousemove', () => signalUserActivity(), false);
    window.addEventListener('mousedown', () => signalUserActivity(), false);
    window.addEventListener('keypress', () => signalUserActivity(), false);
    window.addEventListener('scroll', () => signalUserActivity(), false);
    window.addEventListener('click', () => signalUserActivity(), false);

    const resetLogoutTimeout = this.resettableTimeout(() => {
      console.log("logging out the user!");
    }, environment.inactivityTimeoutInSeconds * 1000);
    const resetInactivityModalTimeout = this.resettableTimeout(() => {
      this.showInactivityModal = true;
    }, (environment.inactivityTimeoutInSeconds - 5) * 1000);

    window.addEventListener('message', (e) => {
      window.localStorage.setItem('LAST_ACTIVE_TIMESTAMP_EPOCH_MS', Date.now().toString());
      resetLogoutTimeout();
      resetInactivityModalTimeout();
    }, false);
  }

  resettableTimeout(f, timeoutInSeconds) {
    let timeout;
    return () => {
      clearTimeout(timeout);
      timeout = setTimeout(f, timeoutInSeconds);
    };
  }

  ngAfterViewInit() {
    // If we ever get a test Zendesk account,
    // the key should be templated in using an environment variable.
    initializeZendeskWidget(this.elementRef, '5a7d70b9-37f9-443b-8d0e-c3bd3c2a55e3');
  }

  ngOnDestroy() {
    if (this.profileLoadingSub) {
      this.profileLoadingSub.unsubscribe();
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
    window.location.assign('https://accounts.google.com/logout');
  }

  closeInactivityModal(): void {
    this.showInactivityModal = false;
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

  openZendesk(): void {
    openZendeskWidget(this.givenName, this.familyName, this.aouAccountEmailAddress,
      this.contactEmailAddress);
  }

  openHubForum(): void {
    window.open(environment.zendeskHelpCenterUrl, '_blank');
  }

}
