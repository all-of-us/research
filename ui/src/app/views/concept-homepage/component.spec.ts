import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptHomepageComponent} from 'app/views/concept-homepage/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';
import {SlidingFabComponent} from 'app/views/sliding-fab/component';
import {ToolTipComponent} from 'app/views/tooltip/component';
import {TopBoxComponent} from 'app/views/top-box/component';

import {HighlightSearchPipe} from 'app/utils/highlight-search.pipe';


import {
  ConceptSetsService,
  ConceptsService,
  DomainInfo,
  StandardConceptFilter,
  WorkspaceAccessLevel,
} from 'generated';

import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ConceptsServiceStub, DomainStubVariables} from 'testing/stubs/concepts-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {simulateClick, simulateInput, updateAndTick} from 'testing/test-helpers';


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

function isSelectedDomain(
  domain: DomainInfo, fixture: ComponentFixture<ConceptHomepageComponent>): boolean {
    if (fixture.debugElement.query(
      By.css('.domain-selector-button.active'))
      .children[0].nativeNode.textContent.trim() === domain.name) {
        return true;
    }
    return false;
}

describe('ConceptHomepageComponent', () => {
  let fixture: ComponentFixture<ConceptHomepageComponent>;
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
        ConceptHomepageComponent,
        ConceptTableComponent,
        HighlightSearchPipe,
        SlidingFabComponent,
        ToolTipComponent,
        TopBoxComponent,
      ],
      providers: [
        { provide: ConceptsService, useValue: new ConceptsServiceStub() },
        { provide: ConceptSetsService, useValue: new ConceptSetsServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]}).compileComponents().then(() => {
        fixture = TestBed.createComponent(ConceptHomepageComponent);
        // This tick initializes the component.
        tick();
        // This finishes the API calls.
        updateAndTick(fixture);
        // This finishes the page reloading.
        updateAndTick(fixture);
      });
  }));

  it('should have one card per domain.', fakeAsync(() => {
    expect(fixture.debugElement.queryAll(By.css('.card.item-card')).length)
      .toBe(DomainStubVariables.STUB_DOMAINS.length);
  }));

  it('should default to standard concepts only, and performs a full search', fakeAsync(() => {
    const spy = spyOn(TestBed.get(ConceptsService), 'searchConcepts')
      .and.callThrough();

    const searchTerm = 'test';

    simulateInput(fixture, fixture.debugElement.query(By.css('#concept-search-input')), searchTerm);
    simulateClick(fixture, fixture.debugElement.query(By.css('.btn-search')));
    updateAndTick(fixture);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const includeDomainCounts = isSelectedDomain(domain, fixture);
      const expectedRequest = {
        query: searchTerm,
        // Tests that it searches only standard concepts.
        standardConceptFilter: StandardConceptFilter.STANDARDCONCEPTS,
        domain: domain.domain,
        includeDomainCounts: includeDomainCounts,
        includeVocabularyCounts: true,
        maxResults: fixture.componentInstance.maxConceptFetch
      };
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest);
    });
    // Tests that it makes a call for each domain.
    expect(spy).toHaveBeenCalledTimes(DomainStubVariables.STUB_DOMAINS.length);

    // Tests that it switches to the datagrid view.
    expect(fixture.debugElement.query(By.css('clr-datagrid'))).toBeTruthy();
    expect(fixture.debugElement.queryAll(By.css('.concept-row')).length).toBe(1);
    const firstDomainRowName =
      fixture.debugElement.queryAll(By.css('.concept-name'))[0].nativeNode.textContent;

    // Tests that it changes the table when a new domain is selected.
    simulateClick(fixture, fixture.debugElement.queryAll(By.css('.domain-selector-button'))[1]);
    updateAndTick(fixture);
    expect(fixture.debugElement.queryAll(By.css('.concept-name'))[0].nativeNode.textContent)
      .not.toBe(firstDomainRowName);
  }));

  it('should changes search criteria when standard only not checked', fakeAsync(() => {
    const spy = spyOn(TestBed.get(ConceptsService), 'searchConcepts')
      .and.callThrough();

    const searchTerm = 'test';
    simulateClick(fixture, fixture.debugElement
      .query(By.css('.standard-concepts-checkbox')).children[0]);

    simulateInput(fixture, fixture.debugElement.query(By.css('#concept-search-input')), searchTerm);
    simulateClick(fixture, fixture.debugElement.query(By.css('.btn-search')));
    updateAndTick(fixture);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const includeDomainCounts = isSelectedDomain(domain, fixture);
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        {
          query: searchTerm,
          // Tests that it searches all concepts.
          standardConceptFilter: StandardConceptFilter.ALLCONCEPTS,
          domain: domain.domain,
          includeDomainCounts: includeDomainCounts,
          includeVocabularyCounts: true,
          maxResults: fixture.componentInstance.maxConceptFetch
        });
    });

    // Test that it pulls back more concepts when all concepts allowed.
    expect(fixture.debugElement.queryAll(By.css('.concept-row')).length).toBe(2);
  }));

  it('should display the selected concepts on header', fakeAsync(() => {
    const spy = spyOn(TestBed.get(ConceptsService), 'searchConcepts')
        .and.callThrough();
    const searchTerm = 'test';
    simulateClick(fixture, fixture.debugElement
        .query(By.css('.standard-concepts-checkbox')).children[0]);
    simulateInput(fixture, fixture.debugElement.query(By.css('#concept-search-input')), searchTerm);
    simulateClick(fixture, fixture.debugElement.query(By.css('.btn-search')));
    updateAndTick(fixture);
    const dataRow = fixture.debugElement.queryAll(By.css('.concept-row'));
    const checkBox = dataRow[0].queryAll(By.css('.datagrid-select'))[0]
        .query(By.css('.checkbox')).children;
    simulateClick(fixture, checkBox[0]);
    updateAndTick(fixture);
    const pillValue = fixture.debugElement.query(By.css('.pill')).childNodes[0]
        .nativeNode.nodeValue.trim();
    expect(pillValue).toBe('1');
  }));

  it('should display the selected concepts on sliding button', fakeAsync(() => {
    const spy = spyOn(TestBed.get(ConceptsService), 'searchConcepts')
        .and.callThrough();

    const searchTerm = 'test';
    simulateClick(fixture, fixture.debugElement
        .query(By.css('.standard-concepts-checkbox')).children[0]);
    simulateInput(fixture, fixture.debugElement.query(By.css('#concept-search-input')), searchTerm);
    simulateClick(fixture, fixture.debugElement.query(By.css('.btn-search')));
    updateAndTick(fixture);
    const button = fixture.debugElement.query(By.css('.sliding-button'))
       .query(By.css('.text'));
    let buttonText = button.nativeNode.innerHTML;
    // Default value to be Add to set
    expect(buttonText).toBe('Add to set');
    const dataRow = fixture.debugElement.queryAll(By.css('.concept-row'));
    const checkBox = dataRow[0].queryAll(By.css('.datagrid-select'))[0]
        .query(By.css('.checkbox')).children;
    simulateClick(fixture, checkBox[0]);
    updateAndTick(fixture);
    buttonText = button.nativeNode.innerHTML;

    // After select add the number of selected concepts
    expect(buttonText).toBe('Add (1) to set');
  }));

  it('should clear selected count after adding', fakeAsync(() => {
    const de = fixture.debugElement;

    simulateClick(fixture, de.query(By.css('.standard-concepts-checkbox')).children[0]);
    simulateInput(fixture, de.query(By.css('#concept-search-input')), 'test');
    simulateClick(fixture, de.query(By.css('.btn-search')));
    updateAndTick(fixture);
    const dataRow = de.queryAll(By.css('.concept-row'));
    const checkBox = dataRow[0].queryAll(By.css('.datagrid-select'))[0]
        .query(By.css('.checkbox')).children;
    simulateClick(fixture, checkBox[0]);
    updateAndTick(fixture);

    simulateClick(fixture, de.query(By.css('.sliding-button')));
    updateAndTick(fixture);

    // Create a new concept set to avoid any dependency on existing stub data.
    de.query(By.css('#select-create')).nativeElement.click();
    updateAndTick(fixture);
    simulateInput(fixture, de.query(By.css('#new-name')), 'foo');
    updateAndTick(fixture);
    simulateClick(fixture, de.query(By.css('.btn-primary')));
    updateAndTick(fixture);

    const addButton = de.query(By.css('.sliding-button'));
    expect(addButton.classes['disable']).toBeTruthy();

    // Run out the "added" notification timer.
    tick(10000);
  }));
});
