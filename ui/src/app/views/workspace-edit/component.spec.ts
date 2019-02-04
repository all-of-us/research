import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {WorkspaceStorageService} from 'app/services/workspace-storage.service';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {ToolTipComponent} from 'app/views/tooltip/component';
import {WorkspaceEditComponent, WorkspaceEditMode} from 'app/views/workspace-edit/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {WorkspaceWrapperComponent} from 'app/views/workspace-wrapper/component';

import {CdrVersionStorageServiceStub} from 'testing/stubs/cdr-version-storage-service-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {
  signedInDependencies,
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {
  DataAccessLevel,
  UnderservedPopulationEnum,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';


describe('WorkspaceEditComponent', () => {
  let activatedRouteStub: any;
  let router: Router;
  let testComponent: WorkspaceEditComponent;
  let fixture: ComponentFixture<WorkspaceEditComponent>;
  let workspacesService: WorkspacesServiceStub;

  function setupComponent(mode: WorkspaceEditMode) {
    activatedRouteStub.routeConfig.data.mode = mode;
    fixture = TestBed.createComponent(WorkspaceEditComponent);
    testComponent = fixture.componentInstance;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  beforeEach(fakeAsync(() => {
    activatedRouteStub = {
      snapshot: {
        url: [
          {path: 'workspaces'},
          {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
          {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
          {path: 'clone'}
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
      },
      routeConfig: {data: {}},
      root: {children: []}
    };
    workspacesService = new WorkspacesServiceStub();
    TestBed.configureTestingModule({
      declarations: [
        ...signedInDependencies.declarations,
        ConfirmDeleteModalComponent,
        ToolTipComponent,
        WorkspaceEditComponent,
        WorkspaceNavBarComponent,
        WorkspaceWrapperComponent,
        WorkspaceShareComponent
      ],
      imports: [
        ...signedInDependencies.imports,
        BrowserAnimationsModule,
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      providers: [
        ...signedInDependencies.providers,
        { provide: WorkspacesService, useValue: workspacesService },
        { provide: WorkspaceStorageService, useClass: WorkspaceStorageService },
        // Wrap in a factory function so we can later mutate the value if needed
        // for testing.
        { provide: ActivatedRoute, useFactory: () => activatedRouteStub },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        {
          provide: CdrVersionStorageService,
          useValue: new CdrVersionStorageServiceStub({
            items: [{
              name: 'cdr1',
              cdrVersionId: '1',
              dataAccessLevel: DataAccessLevel.Registered,
              creationTime: 0
            }, {
              name: 'cdr2',
              cdrVersionId: '2',
              dataAccessLevel: DataAccessLevel.Registered,
              creationTime: 0
            }],
            defaultCdrVersionId: '2'
          })
        },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]}).compileComponents().then(() => {
        router = TestBed.get(Router);
      });
  }));

  it('should show a conflict-specific error when creating a name conflict workspace',
    fakeAsync(() => {
      setupComponent(WorkspaceEditMode.Create);
      testComponent.workspace.namespace = WorkspaceStubVariables.DEFAULT_WORKSPACE_NS;
      testComponent.workspace.name = WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME;
      testComponent.workspace.id = WorkspaceStubVariables.DEFAULT_WORKSPACE_ID;
      testComponent.workspace.description = WorkspaceStubVariables.DEFAULT_WORKSPACE_DESCRIPTION;
      const originalSize = workspacesService.workspaces.length;
      fixture.detectChanges();
      fixture.debugElement.query(By.css('.add-button'))
        .triggerEventHandler('click', null);
      updateAndTick(fixture);
      updateAndTick(fixture);
      expect(workspacesService.workspaces.length).toBe(originalSize);
      const modalTitle = fixture.debugElement.query(By.css('.modal-title'));
      const modalBody = fixture.debugElement.query(By.css('.modal-body'));
      expect(modalTitle.nativeElement.textContent).toEqual('Error:');
      const errorMsg = 'You already have a workspace named ' + testComponent.workspace.name
      + '. Please choose another name.';
      expect(modalBody.nativeElement.textContent).toEqual(errorMsg);
    }));

  it('should show a generic error when creating an id conflict workspace', fakeAsync(() => {
    setupComponent(WorkspaceEditMode.Create);
    testComponent.workspace.namespace = WorkspaceStubVariables.DEFAULT_WORKSPACE_NS;
    testComponent.workspace.name = 'non-default name';
    testComponent.workspace.id = WorkspaceStubVariables.DEFAULT_WORKSPACE_ID;
    testComponent.workspace.description = WorkspaceStubVariables.DEFAULT_WORKSPACE_DESCRIPTION;
    const originalSize = workspacesService.workspaces.length;
    fixture.detectChanges();
    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    updateAndTick(fixture);
    updateAndTick(fixture);
    expect(workspacesService.workspaces.length).toBe(originalSize);
    const modalTitle = fixture.debugElement.query(By.css('.modal-title'));
    const modalBody = fixture.debugElement.query(By.css('.modal-body'));
    expect(modalTitle.nativeElement.textContent).toEqual('Error:');
    const errorMsg = 'Could not create workspace.';
    expect(modalBody.nativeElement.textContent).toEqual(errorMsg);
  }));

  it('should support updating a workspace', fakeAsync(() => {
    setupComponent(WorkspaceEditMode.Edit);
    testComponent.workspace.name = 'edited';
    fixture.detectChanges();
    expect(workspacesService.workspaces[0].name).not.toBe('edited');

    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    fixture.detectChanges();
    tick();
    expect(workspacesService.workspaces.length).toBe(1);
    expect(workspacesService.workspaces[0].name).toBe('edited');
  }));

  it('should support creating a workspace', fakeAsync(() => {
    spyOn(TestBed.get(Router), 'navigate');
    workspacesService.workspaces = [];
    setupComponent(WorkspaceEditMode.Create);

    testComponent.workspace.namespace = 'foo';
    testComponent.workspace.name = 'created';
    testComponent.workspace.id = 'created';
    testComponent.workspace.description = 'description';
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    fixture.detectChanges();
    tick();
    expect(workspacesService.workspaces.length).toBe(1);
    expect(workspacesService.workspaces[0].name).toBe('created');
    expect(TestBed.get(Router).navigate)
      .toHaveBeenCalledWith(['workspaces', 'foo', 'created']);
  }));

  it('should support cloning a workspace', fakeAsync(() => {
    workspacesService.workspaceAccess.set(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
    setupComponent(WorkspaceEditMode.Clone);
    fixture.componentRef.instance.profileStorageService.reload();
    tick();
    expect(testComponent.workspace.name).toBe(
      `Clone of ${WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME}`);
    expect(testComponent.hasPermission).toBeTruthy(
      'cloner should be able to edit cloned workspace');

    const spy = spyOn(router, 'navigate');
    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    fixture.detectChanges();
    tick();
    expect(workspacesService.workspaces.length).toBe(2);
    const got = workspacesService.workspaces.find(w => w.name === testComponent.workspace.name);
    expect(got).not.toBeNull();
    expect(got.namespace).toBe(
      ProfileStubVariables.PROFILE_STUB.freeTierBillingProjectName);
    expect(spy).toHaveBeenCalled();
  }));

  it('should support cloning a workspace with cdr version change', fakeAsync(() => {
    workspacesService.workspaceAccess.set(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
    setupComponent(WorkspaceEditMode.Clone);
    fixture.componentRef.instance.profileStorageService.reload();
    tick();
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.cdr-version-dropdown')));
    const cdr1Option = de.queryAll(By.css('.cdr-version-item'))
      .find(d => d.nativeElement.textContent.includes('cdr1'));
    simulateClick(fixture, cdr1Option);

    const spy = spyOn(router, 'navigate');
    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    fixture.detectChanges();
    tick();
    expect(workspacesService.workspaces.length).toBe(2);
    const got = workspacesService.workspaces.find(w => w.name === testComponent.workspace.name);
    expect(got).not.toBeNull();
    expect(got.cdrVersionId).toBe('1');
    expect(spy).toHaveBeenCalled();
  }));

  it('should use default CDR version for workspace creation', fakeAsync(() => {
    workspacesService.workspaceAccess.set(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
    setupComponent(WorkspaceEditMode.Create);
    fixture.componentRef.instance.profileStorageService.reload();
    tick();

    const cdrDropdown = fixture.debugElement.query(By.css('.cdr-version-dropdown'));
    expect(cdrDropdown.nativeElement.textContent).toContain('cdr2');
  }));

  it('should not create a workspace without description and fill later checkbox not selected',
    fakeAsync(() => {
      spyOn(TestBed.get(Router), 'navigate');
      workspacesService.workspaces = [];
      setupComponent(WorkspaceEditMode.Create);

      testComponent.workspace.namespace = 'foo';
      testComponent.workspace.name = 'created';
      testComponent.workspace.id = 'created';
      testComponent.workspace.description = '';
      testComponent.fillDetailsLater = false;
      fixture.detectChanges();

      fixture.debugElement.query(By.css('.add-button'))
        .triggerEventHandler('click', null);
      fixture.detectChanges();
      tick();
      expect(workspacesService.workspaces.length).toBe(0);
    }));

  it('should create a workspace without description and fill later checkbox selected',
    fakeAsync(() => {
      spyOn(TestBed.get(Router), 'navigate');
      workspacesService.workspaces = [];
      setupComponent(WorkspaceEditMode.Create);

      testComponent.workspace.namespace = 'foo';
      testComponent.workspace.name = 'created';
      testComponent.workspace.id = 'created';
      testComponent.workspace.description = '';
      testComponent.fillDetailsLater = true;
      fixture.detectChanges();

      fixture.debugElement.query(By.css('.add-button'))
        .triggerEventHandler('click', null);
      fixture.detectChanges();
      tick();
      expect(workspacesService.workspaces.length).toBe(1);
      expect(workspacesService.workspaces[0].name).toBe('created');
      expect(workspacesService.workspaces[0].description).toBe('');
    }));

  it('should not create a workspace with name greater than 80 characters', fakeAsync(() => {
    spyOn(TestBed.get(Router), 'navigate');
    workspacesService.workspaces = [];
    setupComponent(WorkspaceEditMode.Create);
    simulateInput(fixture, fixture.debugElement.query(By.css('.input.name')),
      'this is more than 80 characters look at how long this is ' +
      'and why dont I add some more characters and make this really very long');
    simulateInput(fixture, fixture.debugElement.query(By.css('.input.description')),
      'foo');
    testComponent.fillDetailsLater = false;
    fixture.detectChanges();
    tick();
    expect(fixture.debugElement.query(By.css('.add-button')).properties.disabled).toBeTruthy();
  }));

  it('should allow editing unset underserved population on clone', fakeAsync(() => {
    spyOn(TestBed.get(Router), 'navigate');
    setupComponent(WorkspaceEditMode.Clone);
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.underserved-icon')));
    fixture.detectChanges();

    simulateClick(fixture, de.query(By.css(
      '.underserved-checkbox[ng-reflect-name="AGE_OLDER_ADULTS"]')));
    fixture.detectChanges();

    simulateClick(fixture, de.query(By.css('.add-button')));
    tick();

    expect(workspacesService.workspaces.length).toBe(2);
    expect(workspacesService.workspaces[1].researchPurpose.underservedPopulationDetails)
      .toContain(UnderservedPopulationEnum.AGEOLDERADULTS);
  }));

  it('should not allow duplicate workspace name while cloning', fakeAsync(() => {
    workspacesService.workspaceAccess.set(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
    setupComponent(WorkspaceEditMode.Clone);
    fixture.componentRef.instance.profileStorageService.reload();
    tick();
    let de = fixture.debugElement;
    const workspaceName = WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME;
    simulateInput(fixture, de.query(By.css('.name')), workspaceName );
    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    updateAndTick(fixture);
    updateAndTick(fixture);
    de = fixture.debugElement;
    const errorText = de.query(By.css('.modal-body')).childNodes[0].nativeNode.data;
    expect(errorText).toBe(
      'You already have a workspace named '
        + workspaceName + '. Please choose another name.');
  }));
});
