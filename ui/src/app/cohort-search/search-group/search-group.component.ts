import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {
  CohortSearchActions,
  CohortSearchState,
  getTemporalGroupItems,
  groupError
} from 'app/cohort-search/redux';
import {SearchRequest, TemporalMention, TemporalTime, TreeType} from 'generated';
import {List, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';



@Component({
  selector: 'app-search-group',
  templateUrl: './search-group.component.html',
  styleUrls: [
    './search-group.component.css',
    '../../styles/buttons.css',
  ]
})
export class SearchGroupComponent implements OnInit, OnDestroy {
  @Input() group;
  @Input() role: keyof SearchRequest;
  nonTemporalItems = [];
  temporalItems = [];
  error: boolean;
  whichMention = [TemporalMention.ANYMENTION,
    TemporalMention.FIRSTMENTION,
    TemporalMention.LASTMENTION];

  timeDropDown = [TemporalTime.DURINGSAMEENCOUNTERAS,
    TemporalTime.XDAYSAFTER,
    TemporalTime.XDAYSBEFORE,
    TemporalTime.WITHINXDAYSOF];
  name = this.whichMention[0];
  subscription: Subscription;
  itemSubscription: Subscription;
  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;
  itemId: any;
  treeType = [];

  constructor(private actions: CohortSearchActions, private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupError(this.group.get('id')))
      .subscribe(error => {
        this.error = error;
      });

    this.itemSubscription = this.ngRedux.select(getTemporalGroupItems(this.group.get('id')))
      .filter(temporal => !!temporal)
      .subscribe(i => {
        console.log(i.type);
        this.treeType = i.type;
        this.nonTemporalItems = i.nonTemporalItems;
        this.temporalItems = i.temporalItems;
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
    this.itemSubscription.unsubscribe();
  }

  get typeFlag() {
    let flag = true;
    this.treeType.map(m => {
      if ( m === TreeType[TreeType.PM]) {
        flag = false;
      }
    });
    return flag;
  }

  get isRequesting() {
    return this.group.get('isRequesting', false);
  }

  get temporalFlag() {
    return this.group.get('temporal');
  }

  get groupId() {
    return this.group.get('id');
  }

  remove(event) {
    this.actions.removeGroup(this.role, this.groupId);
  }

  get mention() {
    return this.group.get('mention');
  }

  get time() {
    return this.group.get('time');
  }

  get timeValue() {
    return this.group.get('timeValue');
  }

  get items() {
    return this.group.get('items', List());
  }

  launchWizard(criteria: any, tempGroup?: number) {
    const itemId = this.actions.generateId('items');
    const criteriaType = criteria.codes ? criteria.codes[0].type : criteria.type;
    const criteriaSubtype = criteria.codes ? criteria.codes[0].subtype : null;
    const fullTree = criteria.fullTree || false;
    const codes = criteria.codes || false;
    const {role, groupId} = this;
    const context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
    this.actions.openWizard(itemId, criteria.type, context, tempGroup);
  }

  getTemporal(e) {
    e.target.checked && this.mention === '' ?
      this.getMentionTitle(this.whichMention[0]) : this.getMentionTitle(this.mention);
    e.target.checked && this.time === '' ?
      this.getTimeTitle(this.timeDropDown[0]) : this.getTimeTitle(this.time);

    this.actions.updateTemporal(e.target.checked, this.groupId);
  }

  getMentionTitle(mentionName) {
    this.actions.updateWhichMention(mentionName, this.groupId);
  }

  getTimeTitle(time) {
    this.actions.updateTemporalTime(time, this.groupId);
  }

  getTimeValue(e) {
    this.actions.updateTemporalTimeValue(e.target.value, this.groupId);
  }

  formatStatus(options) {
    switch (options) {
      case 'ANY_MENTION' :
        return 'Any Mention';
      case 'FIRST_MENTION' :
        return 'First Mention';
      case 'LAST_MENTION' :
        return 'Last Mention';
      case 'DURING_SAME_ENCOUNTER_AS' :
        return 'During same encounter as';
      case 'X_DAYS_BEFORE' :
        return 'X Days before';
      case 'X_DAYS_AFTER' :
        return 'X Days after';
      case 'WITHIN_X_DAYS_OF' :
        return 'Within X Days of';
    }
  }
}
