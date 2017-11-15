import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {By} from '@angular/platform-browser';
import {ClarityModule} from 'clarity-angular';
import {MockNgRedux} from '@angular-redux/store/testing';
import {Map, fromJS} from 'immutable';
import {NgRedux} from '@angular-redux/store';
import {ReactiveFormsModule} from '@angular/forms';

import {
  CohortSearchActions,
  WIZARD_CANCEL,
  WIZARD_FINISH,
} from '../../redux';

import {AttributesModule} from '../attributes/attributes.module';
import {AlertsComponent} from '../alerts/alerts.component';
import {ExplorerComponent} from '../explorer/explorer.component';
import {QuickSearchComponent} from '../quicksearch/quicksearch.component';
import {QuickSearchResultsComponent
} from '../quicksearch-results/quicksearch-results.component';
import {RootSpinnerComponent} from '../root-spinner/root-spinner.component';
import {SelectionComponent} from '../selection/selection.component';
import {TreeComponent} from '../tree/tree.component';
import {WizardComponent} from './wizard.component';

import {CohortBuilderService} from 'generated';


describe('WizardComponent', () => {
  let fixture: ComponentFixture<WizardComponent>;
  let comp: WizardComponent;

  let mockReduxInst;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          AlertsComponent,
          ExplorerComponent,
          QuickSearchComponent,
          QuickSearchResultsComponent,
          RootSpinnerComponent,
          SelectionComponent,
          TreeComponent,
          WizardComponent,
        ],
        imports: [
          AttributesModule,
          BrowserAnimationsModule,
          ClarityModule,
          ReactiveFormsModule,
        ],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortBuilderService, useValue: {}},
          CohortSearchActions,
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();

    fixture = TestBed.createComponent(WizardComponent);
    comp = fixture.componentInstance;
    comp.open = true;
    comp.criteriaType = 'icd9';
    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(comp).toBeTruthy();
  });

  it('Should dispatch WIZARD_CANCEL on cancel', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    comp.cancel();
    expect(spy).toHaveBeenCalledWith({type: WIZARD_CANCEL});
  });

  it('Should dispatch WIZARD_FINISH on finish', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    comp.finish();
    expect(spy).toHaveBeenCalledWith({type: WIZARD_FINISH});
  });
});
