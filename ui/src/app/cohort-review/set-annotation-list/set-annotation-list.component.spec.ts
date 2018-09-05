import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {AnnotationType, CohortAnnotationDefinition, CohortAnnotationDefinitionService} from 'generated';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationItemComponent} from '../set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from './set-annotation-list.component';

describe('SetAnnotationListComponent', () => {
  let component: SetAnnotationListComponent;
  let fixture: ComponentFixture<SetAnnotationListComponent>;
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ SetAnnotationItemComponent, SetAnnotationListComponent ],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: ActivatedRoute, useValue: {}},
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: ReviewStateService, useValue: {
          annotationDefinitions$: Observable.of([
            <CohortAnnotationDefinition> {
              cohortAnnotationDefinitionId: 1,
              cohortId: 2,
              columnName: 'test',
              annotationType: AnnotationType.BOOLEAN
            }
          ]),
        }},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SetAnnotationListComponent);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
