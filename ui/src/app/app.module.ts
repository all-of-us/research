import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Http, HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {environment} from 'environments/environment';

import {ErrorHandlingService} from './services/error-handling.service';
import {GoogleAnalyticsEventsService} from './services/google-analytics-events.service';
import {SignInService} from './services/sign-in.service';

import {AccountCreationComponent} from './views/account-creation/component';
import {AppComponent, overriddenUrlKey} from './views/app/component';
import {BugReportComponent} from './views/bug-report/component';
import {CohortEditComponent} from './views/cohort-edit/component';
import {ErrorHandlerComponent} from './views/error-handler/component';
import {HomePageComponent} from './views/home-page/component';

import {IdVerificationPageComponent} from './views/id-verification-page/component';
import {InvitationKeyComponent} from './views/invitation-key/component';
import {ProfileEditComponent} from './views/profile-edit/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {ReviewComponent} from './views/review/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {DataBrowserModule} from './data-browser/data-browser.module';
import {IconsModule} from './icons/icons.module';

import {
  ApiModule,
  ConfigService,
  Configuration,
} from 'generated';

function getBasePath() {
  return localStorage.getItem(overriddenUrlKey) || environment.allOfUsApiUrl;
}

export function getConfigService(http: Http) {
  return new ConfigService(http, getBasePath(), null);
}

// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService): Configuration {
    return new Configuration({
      basePath: getBasePath(),
      accessToken: () => signInService.currentAccessToken
    });
}

@NgModule({
  imports: [
    ApiModule,
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    IconsModule,
    ClarityModule,
    DataBrowserModule,
  ],
  declarations: [
    AppComponent,
    AccountCreationComponent,
    BugReportComponent,
    CohortEditComponent,
    ErrorHandlerComponent,
    HomePageComponent,
    IdVerificationPageComponent,
    ProfileEditComponent,
    ProfilePageComponent,
    ReviewComponent,
    WorkspaceComponent,
    WorkspaceEditComponent,
    WorkspaceShareComponent,
    InvitationKeyComponent
  ],
  providers: [
    {
      provide: ConfigService,
      deps: [Http],
      useFactory: getConfigService
    },
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    ErrorHandlingService,
    SignInService,
    GoogleAnalyticsEventsService,
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, BugReportComponent, ErrorHandlerComponent]
})
export class AppModule {}
