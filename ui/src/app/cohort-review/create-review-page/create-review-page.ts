import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CreateReviewRequest,
} from 'generated';

@Component({
  templateUrl: './create-review-page.html',
})
export class CreateReviewPage implements OnInit {
  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  private creating = false;
  private maxParticipants: number;
  private matchedParticipantCount: number;
  private cohortName: string;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private router: Router,
    private route: ActivatedRoute,
  ) { }

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  ngOnInit() {
    this.state.review$
      .take(1)
      .pluck('matchedParticipantCount')
      .do((count: number) => this.matchedParticipantCount = count)
      .map((count: number) => Math.min(10000, count))
      .do((count: number) => this.maxParticipants = count)
      .map(count => Validators.compose([
        Validators.required,
        Validators.min(1),
        Validators.max(count)]))
      .subscribe(validators => this.numParticipants.setValidators(validators));

    this.state.cohort$
      .take(1)
      .pluck('name')
      .subscribe((name: string) => this.cohortName = name);
  }

  cancelReview() {
    const {ns, wsid} = this.route.snapshot.params;
    this.router.navigate(['workspace', ns, wsid]);
  }

  createReview() {
    this.creating = true;
    const {ns, wsid, cid} = this.route.snapshot.params;
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;

    Observable.of(<CreateReviewRequest>{size: this.numParticipants.value})
      .mergeMap(request => this.reviewAPI.createCohortReview(ns, wsid, cid, cdrid, request))
      .subscribe(review => {
        this.creating = false;
        this.state.review.next(review);
      });
  }
}
