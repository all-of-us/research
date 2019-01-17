import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';

import {
  CohortSearchActions,
  CohortSearchState,
  OPEN_WIZARD,
  openWizard,
  REMOVE_GROUP,
  removeGroup,
} from '../redux';
import {SearchGroupItemComponent} from '../search-group-item/search-group-item.component';
import {SearchGroupComponent} from './search-group.component';

import {CohortBuilderService, TreeType} from 'generated';

const itemA = fromJS({
  id: 'itemA',
  count: null,
  isRequesting: false,
  type: TreeType[TreeType.ICD9],
  searchParameters: [],
  modifiers: [],
});

const itemB = fromJS({
  id: 'itemB',
  count: null,
  isRequesting: false,
  type: TreeType[TreeType.ICD9],
  searchParameters: [],
  modifiers: [],
});

const group = fromJS({
  id: 'include0',
  count: null,
  isRequesting: false,
  items: ['itemA', 'itemB'],
  status: 'active',
});

class MockActions {
  @dispatch() removeGroup = removeGroup;
  @dispatch() openWizard = openWizard;

  generateId(prefix?: string): string {
    return 'TestId';
  }
}

describe('SearchGroupComponent', () => {
  let fixture: ComponentFixture<SearchGroupComponent>;
  let comp: SearchGroupComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          SearchGroupComponent,
          SearchGroupItemComponent,
        ],
        imports: [
          ClarityModule,
          NgxPopperModule,
        ],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortBuilderService, useValue: {}},
          {provide: CohortSearchActions, useValue: new MockActions()},
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();

    fixture = TestBed.createComponent(SearchGroupComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.group = group;
    comp.role = 'includes';
    fixture.detectChanges();
  });

  it('Should render', () => {
    // sanity check
    expect(comp).toBeTruthy();
    const items = fixture.debugElement.queryAll(By.css('app-search-group-item'));
    expect(items.length).toBe(2);
  });

  it('Should dispatch WIZARD_OPEN when a Criteria is selected', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    comp.launchWizard({type: TreeType[TreeType.ICD9]});
    expect(spy).toHaveBeenCalledWith({
      type: OPEN_WIZARD,
      itemId: 'TestId',
      itemType: TreeType[TreeType.ICD9],
      context: {
        criteriaType: TreeType[TreeType.ICD9],
        criteriaSubtype: null,
        role: 'includes',
        groupId: 'include0',
        itemId: 'TestId',
        fullTree: false,
        codes: false
      }
    });
  });

  it('Should dispatch REMOVE_GROUP on remove button click', fakeAsync(() => {
    fixture.detectChanges();
    const spy = spyOn(mockReduxInst, 'dispatch');

    const dropdown = fixture.debugElement.query(By.css('.dropdown-toggle'));
    dropdown.triggerEventHandler('click', null);

    const removeButton = fixture.debugElement
      .query(By.css('button[clrdropdownitem]:nth-of-type(2)'));
    removeButton.triggerEventHandler('click', null);
    jasmine.clock().tick(3000)
    fixture.detectChanges()
    expect(spy).toHaveBeenCalledWith({
      type: REMOVE_GROUP,
      role: 'includes',
      groupId: 'include0'
    });
  }));

  it('Should render group count if group count', () => {
    comp.group = group.set('count', 25);
    fixture.detectChanges();

    const footer = fixture.debugElement.query(By.css('div.card-footer'));
    const spinner = fixture.debugElement.query(By.css('span.spinner'));
    const text = footer.nativeElement.textContent.replace(/\s+/g, ' ').trim();

    expect(text).toEqual('Group Count: 25');
    expect(spinner).toBeNull();
  });

  it('Should render a spinner if requesting', () => {
    comp.group = group.set('isRequesting', true);
    fixture.detectChanges();
    const spinner = fixture.debugElement.query(By.css('span.spinner'));
    expect(spinner).not.toBeNull();
  });
});
