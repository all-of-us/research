import {Component, Input} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store';
import {SearchGroupItem} from 'generated';


@Component({
  selector: 'app-search-result',
  templateUrl: 'search-result.component.html',
  styleUrls: ['search-result.component.css']
})
export class SearchResultComponent {
  @Input() item: SearchGroupItem;
  @Input() index: number;

  private _description: any;
  private _count: number;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  get description() {
    return 'Describe the thing here';
  }

  get count() {
    return 42;
  }
}
