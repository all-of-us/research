import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {
  activeParameterList,
  CohortSearchActions,
  /* tslint:disable-next-line:no-unused-variable */
  CohortSearchState,
  criteriaChildren,
  criteriaError,
  isCriteriaLoading,
} from '../../redux';

@Component({
  selector: 'crit-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css'],
})
export class TreeComponent implements OnInit {
  @Input() node;
  @select(activeParameterList) selected$: Observable<List<any>>;

  /* Selections derived from `node` */
  private loading$: Observable<boolean>;
  private hasError$: Observable<boolean>;
  private children$: Observable<List<any>>;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    const _type = this.node.get('type').toLowerCase();
    const _parentId = this.node.get('id');

    this.hasError$ = this.ngRedux
      .select(criteriaError(_type, _parentId))
      .map(err => !(err === null || err === undefined));

    this.loading$ = this.ngRedux.select(isCriteriaLoading(_type, _parentId));
    this.children$ = this.ngRedux.select(criteriaChildren(_type, _parentId));

    this.actions.fetchCriteria(_type, _parentId);
  }
}
