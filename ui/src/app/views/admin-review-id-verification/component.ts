import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  // IdVerificationRequest,
  Profile,
  ProfileService,
} from 'generated';


/**
 * Review ID Verifications. Users with the REVIEW_RESEARCH_PURPOSE permission use this
 * to view other users' workspaces for which a review has been requested, and approve/reject them.
 */
// TODO(RW-85) Design this UI. Current implementation is a rough sketch.
@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class AdminReviewIdVerificationComponent implements OnInit {
  profiles: Profile[] = [];
  contentLoaded = false;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(this.profileService.getIdVerificationsForReview())
        .subscribe(
            profilesResp => {
              for (const ws of profilesResp.profiles) {
                this.profiles.push(ws);
              }
              this.contentLoaded = true;
            });
  }

  // approve(workspace: Workspace, approved: boolean): void {
  //   const request = <ResearchPurposeReviewRequest>{
  //     approved: approved,
  //   };
  //   this.errorHandlingService.retryApi(this.workspacesService.reviewWorkspace(
  //       workspace.namespace, workspace.id, request))
  //       .subscribe(
  //           resp => {
  //             const i = this.workspaces.indexOf(workspace, 0);
  //             if (i >= 0) {
  //               this.workspaces.splice(i, 1);
  //             }
  //           });
  // }
}
