import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Input,
  OnInit,
  OnDestroy,
} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
  countFor,
  pathTo,
  isLoading,
} from '../redux';

import {SearchGroupItem} from 'generated';


@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchGroupItemComponent implements OnInit, OnDestroy {
  @Input() item;
  @Input() role: string;
  @Input() index: number;
  @Input() itemIndex: number;

  private count = 0;
  private loading = true;
  private subscriptions: Subscription[];

  constructor(private cd: ChangeDetectorRef,
              private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const path = pathTo(this.role, this.index, this.itemIndex);
    console.log(`SearchGroupItem at ${JSON.stringify(path, null, 2)}`);
    const countSelect = this.ngRedux.select(countFor(path));
    const loadSelect = this.ngRedux.select(isLoading(path));
    const setAndMark = (name) => (value) => {
      this[name] = value;
      this.cd.markForCheck();
    };
    this.subscriptions = [
      countSelect.subscribe(setAndMark('count')),
      loadSelect.subscribe(setAndMark('loading'))
    ];
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  get description() {
    const _type = this.item.get('type');
    return this.item.get('description', `${_type} Codes`);
  }
}
