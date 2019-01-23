import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import 'rxjs/add/observable/of';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';

import {ClearButtonInMemoryFilterComponent} from 'app/cohort-review/clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
import {DetailTabTableComponent} from 'app/cohort-review/detail-tab-table/detail-tab-table.component';
import {DetailAllEventsComponent} from './detail-all-events.component';

describe('DetailAllEventsComponent', () => {
  let component: DetailAllEventsComponent;
  let fixture: ComponentFixture<DetailAllEventsComponent>;
  const activatedRouteStub = {
    data: Observable.of({
      workspace: {
        cdrVersionId: '1'
      },
      cohort: {},
      participant: {}
    })
  };

    beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ClearButtonInMemoryFilterComponent,
        DetailAllEventsComponent, DetailTabTableComponent],
      imports: [ClarityModule, NgxPopperModule, ReactiveFormsModule, FormsModule],
      providers: [
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailAllEventsComponent);
    component = fixture.componentInstance;
    component.columns = [{name: ''}];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
