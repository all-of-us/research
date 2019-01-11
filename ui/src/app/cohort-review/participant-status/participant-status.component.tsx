import {Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import * as React from 'react';
import * as ReactDOM from 'react-dom'
import * as ld from 'lodash/fp'

import {Participant} from '../participant.model';

import {
  CohortReviewService,
  CohortStatus,
  ModifyCohortStatusRequest,
  ParticipantCohortStatus,
} from 'generated';

const validStatuses = [
  CohortStatus.INCLUDED,
  CohortStatus.EXCLUDED,
  CohortStatus.NEEDSFURTHERREVIEW,
];

export interface ParticipantStatusComponentProps {
  participant: Participant;
}

export class ParticipantStatusComponentReact extends React.Component<ParticipantStatusComponentProps> {


  render() {
    return <div className="form-group">
      <label className="status" htmlFor="status-selector">
        Choose a Review Status for Participant { this.props.participant.id }
      </label>
      <div>
        <select>
          { (this.props.participant.status)?  <option>Select a Status</option> : null}
          {ld.map((x) => <option key={x}>{Participant.formatStatusForText(x)}</option>, validStatuses)}
        </select>
      </div>
    </div>;
  }
}

@Component({
  selector: 'app-participant-status',
  templateUrl: './participant-status.component.html',
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit, OnDestroy, OnChanges {

  participantOption: any;
  defaultOption = false;
  test: any;


  readonly cohortStatusList = validStatuses.map(status => ({
    value: status,
    display: Participant.formatStatusForText(status),
  }));

  private _participant: Participant;

  @Input() set participant(p: Participant) {
    this._participant = p;
    const status = p.status === CohortStatus.NOTREVIEWED ? null : p.status;
    this.statusControl.setValue(status, {emitEvent: false});
  }

  get participant() {
    return this._participant;
  }

  statusControl = new FormControl();
  subscription: Subscription;

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {
  }

  renderReactComponent() {
    ReactDOM.render(<ParticipantStatusComponentReact participant={this._participant}/>,
      document.getElementById('participant-status-react'));
  }

  ngOnChanges() {
    this.renderReactComponent();
    // if (this.statusControl.value) {
    //   this.defaultOption = true;
    //   if (this.statusControl.value === 'NEEDS_FURTHER_REVIEW') {
    //     this.participantOption = 'NEEDS FURTHER REVIEW';
    //   } else {
    //     this.participantOption = this.statusControl.value;
    //   }
    // } else {
    //   this.defaultOption = false;
    // }
  }

  ngOnInit() {
    this.renderReactComponent();
    // this.subscription = this.statusControl.valueChanges
    //   .filter(status => validStatuses.includes(status))
    //   .switchMap(status => this.updateStatus(status))
    //   .subscribe();

  }

  ngOnDestroy() {
    // this.subscription.unsubscribe();
  }

  // updateStatus(status): Observable<ParticipantCohortStatus> {
    // const pid = this.participant.id;
    // const request = {status};
    // const {ns, wsid, cid} = this.route.parent.snapshot.params;
    // const cdrid = this.route.parent.snapshot.data.workspace.cdrVersionId;
    // return this.reviewAPI.updateParticipantCohortStatus(ns, wsid, cid, cdrid, pid, request);
  // }

  participantOptionChange(status) {
    // this.participantOption = status.display;
    // this.statusControl.patchValue(status.value);
    // this.defaultOption = true;
    // this.updateStatus(status.value);
  }
}

