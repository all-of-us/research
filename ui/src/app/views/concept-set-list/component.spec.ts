import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptSetListComponent} from 'app/views/concept-set-list/component';
import {CreateConceptSetModalComponent} from 'app/views/conceptset-create-modal/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {CreateReviewModalComponent} from 'app/views/create-review-modal/create-review-modal';
import {EditModalComponent} from 'app/views/edit-modal/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {ResourceCardComponent} from 'app/views/resource-card/component';
import {TopBoxComponent} from 'app/views/top-box/component';



import {
  CohortReviewService,
  CohortsService,
  ConceptSetsService,
  ConceptsService,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ConceptsServiceStub} from 'testing/stubs/concepts-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {simulateClick, simulateInput, updateAndTick} from 'testing/test-helpers';

import {SignInService} from 'app/services/sign-in.service';
import {ToolTipComponent} from '../tooltip/component';


const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspaces'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
      {path: 'concepts'}
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    },
    data: {
      workspace: {
        ...WorkspacesServiceStub.stubWorkspace(),
        accessLevel: WorkspaceAccessLevel.OWNER,
      }
    }
  }
};

describe('ConceptSetListComponent', () => {
  let fixture: ComponentFixture<ConceptSetListComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        ConceptAddModalComponent,
        ConceptSetListComponent,
        ConfirmDeleteModalComponent,
        CreateConceptSetModalComponent,
        CreateReviewModalComponent,
        EditModalComponent,
        RenameModalComponent,
        ResourceCardComponent,
        ToolTipComponent,
        TopBoxComponent,
      ],
      providers: [
        {provide: CohortsService},
        {provide: WorkspacesService},
        {provide: SignInService},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: ConceptsService, useValue: new ConceptsServiceStub()},
        {provide: ConceptSetsService, useValue: new ConceptSetsServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub}
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(ConceptSetListComponent);
      // This tick initializes the component.
      tick();
      // This finishes the API calls.
      updateAndTick(fixture);
      // This finishes the page reloading.
      updateAndTick(fixture);
    });
  }));


  it('should render.', fakeAsync(() => {
    expect(fixture).toBeTruthy();
  }));

  it('displays correct concept sets', fakeAsync(() => {
    const de = fixture.debugElement;
    const conceptCards = de.queryAll(By.css('.item-card'));
    expect(conceptCards.length).toEqual(3);
    expect(conceptCards[0].nativeElement.innerText).toMatch('Mock Concept Set');
    expect(conceptCards[0].nativeElement.innerText).toMatch('Mocked for tests');
  }));

  it('displays correct information when concept set renamed', fakeAsync(() => {
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.resource-menu')));
    tick();
    simulateClick(fixture, de.query(By.css('.pencil')));
    updateAndTick(fixture);
    simulateInput(fixture, de.query(By.css('.name-input')), 'testMockConcept');
    simulateClick(fixture, de.query(By.css('.btn-save')));
    tick();
    updateAndTick(fixture);
    const conceptCards = de.queryAll(By.css('.item-card'));
    expect(conceptCards[0].nativeElement.innerText).toMatch('testMockConcept');
  }));

  it('displays correct information when concept set deleted', fakeAsync(() => {
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.resource-menu')));
    simulateClick(fixture, de.query(By.css('.trash')));
    updateAndTick(fixture);
    simulateClick(fixture, de.query(By.css('.confirm-delete-btn')));
    updateAndTick(fixture);
    const conceptCards = de.queryAll(By.css('.item-card'));
    expect(conceptCards.length).toBe(2);
  }));
});
