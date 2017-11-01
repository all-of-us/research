import {
  Component,
  Input,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Wizard} from 'clarity-angular';
import {Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  isCriteriaLoading,
  activeCriteriaList,
  activeRole,
  activeGroupId,
  activeItem,
} from '../redux';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent {
  @Input() open: boolean;
  @Input() criteriaType: string;

  private rootsAreLoading = true;
  // Zero is default parent ID for criteria tree roots
  private readonly parentId = 0;

  @select() criteriaErrors$;

  criteriaErrors = [
    {kind: 'icd9', parentId: 0},
    {kind: 'icd9', parentId: 1},
  ];

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  setLoading(value: boolean) {
    this.rootsAreLoading = value;
  }

  /* TODO(jms) hook all this up to actually listen for errors */
  get hasErrors() {
    return this.criteriaErrors.length > 0;
  }

  closeAlert(error) {
    this.criteriaErrors = this.criteriaErrors.filter(err => err !== error);
  }
  /* end todo */

  get rootNode() {
    return Map({type: this.criteriaType, id: this.parentId});
  }

  get critPageTitle() {
    let _type = this.criteriaType;
    if (_type.match(/^DEMO.*/i)) {
      _type = 'Demographics';
    } else if (_type.match(/^(ICD|CPT).*/i)) {
      _type = _type.toUpperCase();
    }
    return `Choose ${_type} Codes`;
  }

  cancel() {
    this.actions.cancelWizard();
  }

  finish() {
    const state = this.ngRedux.getState();
    const role = activeRole(state);
    const groupId = activeGroupId(state);
    const itemId = activeItem(state).get('id');
    const selections = activeCriteriaList(state);
    this.actions.finishWizard();

    if (!selections.isEmpty()) {
      this.actions.requestItemCount(role, itemId);
      this.actions.requestGroupCount(role, groupId);
      this.actions.requestTotalCount(groupId);
    }
  }
}
