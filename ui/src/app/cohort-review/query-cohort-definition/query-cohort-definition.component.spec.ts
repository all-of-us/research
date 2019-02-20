import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {QueryCohortDefinitionComponent} from 'app/cohort-review/query-cohort-definition/query-cohort-definition.component';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {CohortBuilderService} from 'generated';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';


describe('QueryCohortDefinitionComponent', () => {
  let component: QueryCohortDefinitionComponent;
  let fixture: ComponentFixture<QueryCohortDefinitionComponent>;


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        QueryCohortDefinitionComponent,
      ],
      providers: [
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
      ]
    })
      .compileComponents();
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryCohortDefinitionComponent);
    component = fixture.componentInstance;
    // route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
