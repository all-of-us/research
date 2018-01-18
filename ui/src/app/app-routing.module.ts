import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {HomeComponent} from './data-browser/home/home.component';
import {SearchComponent} from './data-browser/search/search.component';

import {CohortEditComponent} from './views/cohort-edit/component';
import {HomePageComponent} from './views/home-page/component';
import {IdVerificationPageComponent} from './views/id-verification-page/component';
import {ProfileEditComponent} from './views/profile-edit/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {AdminReviewIdVerificationComponent} from './views/admin-review-id-verification/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

declare let gtag: Function;
declare let ga_tracking_id: string;

const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    data: {title: 'View Workspaces'}
  }, {
    path: 'admin-review-workspace',
    component: AdminReviewWorkspaceComponent,
    data: {title: 'Review Workspaces'}
  }, {
    path: 'admin-review-id-verification',
    component: AdminReviewIdVerificationComponent,
    data: {title: 'Review ID Verifications'}
  }, {
    path: 'data-browser/home',
    component: HomeComponent,
    data: {title: 'Data Browser'}
  }, {
    path: 'data-browser/browse',
    component: SearchComponent,
    data: {title: 'Browse'}
  }, {
    path: 'profile/id-verification',
    component: IdVerificationPageComponent,
    data: {title: 'ID Verification'}
  }, {
    path: 'profile',
    component: ProfilePageComponent,
    data: {title: 'Profile'}
  }, {
    path: 'profile/edit',
    component: ProfileEditComponent,
    data: {title: 'Profile'}
  }, {
    path: 'workspace/:ns/:wsid',
    component: WorkspaceComponent,
    data: {title: 'View Workspace Details'}
  }, {
    path: 'workspace/:ns/:wsid/cohorts/:cid/edit',
    component: CohortEditComponent,
    data: {title: 'Edit Cohort'},
  }, {
    path: 'workspace/build',
    component: WorkspaceEditComponent,
    data: {title: 'Create Workspace', adding: true}
  }, {
    path: 'workspace/:ns/:wsid/edit',
    component: WorkspaceEditComponent,
    data: {title: 'Edit Workspace', adding: false}
  }, {
    path: 'workspace/:ns/:wsid/share',
    component: WorkspaceShareComponent,
    data: {title: 'Share Workspace'}
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {

 constructor(public router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', ga_tracking_id, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
