import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {
  UserRole,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';

interface UserRoleRow {
  fullName: string;
  email: string;
  role: WorkspaceAccessLevel;
}

class WorkspaceSharePage {
  fixture: ComponentFixture<WorkspaceShareComponent>;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  roleNamePairsOnPage: Array<UserRoleRow>;
  emailField: DebugElement;
  permissionsField: DebugElement;
  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceShareComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.fixture.componentRef.instance.sharing = true;

    this.workspacesService.getWorkspace(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID).subscribe((response: WorkspaceResponse) => {
        this.fixture.componentInstance.workspace = response.workspace;
    });
    tick();
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);

    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    const de = this.fixture.debugElement;
    const setOfUsers = de.queryAll(By.css('.collaborator'));
    this.roleNamePairsOnPage = [];
    setOfUsers.forEach((user) => {
      this.roleNamePairsOnPage.push({fullName: user.children[0].nativeElement.innerText,
          email: user.children[1].nativeElement.innerText,
          role: user.children[2].nativeElement.innerText});
    });
    this.emailField = de.query(By.css('.input'));
    this.fixture.componentRef.instance.input = this.emailField;
    this.permissionsField = de.query(By.css('.permissions-button'));
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspaces'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    }
  }
};

const userValuesStub = {
  workspaceEtag: undefined,
  items: [
    {
      email: 'sampleuser1@fake-research-aou.org',
      role: 'OWNER'
    },
    {
      email: 'sampleuser2@fake-research-aou.org',
      role: 'WRITER'
    },
    {
      email: 'sampleuser3@fake-research-aou.org',
      role: 'READER'
    },
    {
      email: 'sampleuser4@fake-research-aou.org',
      role: 'WRITER'
    }
  ]
};

function convertToUserRoleRow(userRoles: UserRole[]): UserRoleRow[] {
  const roleNamePairs: UserRoleRow[] = [];
  userRoles.forEach((userRole) => {
    roleNamePairs.push({
      fullName: userRole.givenName + ' ' + userRole.familyName,
      email: userRole.email,
      role: userRole.role
    });
  });

  return roleNamePairs;
}

describe('WorkspaceShareComponent', () => {
  let workspaceSharePage: WorkspaceSharePage;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        WorkspaceShareComponent
      ],
      providers: [
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]}).compileComponents().then(() => {
        workspaceSharePage = new WorkspaceSharePage(TestBed);
        workspaceSharePage.fixture.componentRef.instance.profileStorageService.reload();
    });
      tick();
  }));


  it('displays correct information in default workspace sharing', fakeAsync(() => {
    workspaceSharePage.readPageData();
    const roleNamePairs =
      convertToUserRoleRow(workspaceSharePage.fixture.componentRef.instance.workspace.userRoles);
    expect(workspaceSharePage.roleNamePairsOnPage)
        .toEqual(roleNamePairs);
  }));

  it('adds users correctly', fakeAsync(() => {
    workspaceSharePage.readPageData();
    simulateInput(workspaceSharePage.fixture, workspaceSharePage.emailField, 'sampleuser4');
    workspaceSharePage.fixture.componentRef.instance.setAccess('Writer');


    simulateClick(workspaceSharePage.fixture,
        workspaceSharePage.fixture.debugElement.query(By.css('.add-button')));
    workspaceSharePage.readPageData();
    const roleNamePairs =
      convertToUserRoleRow(workspaceSharePage.fixture.componentRef.instance.workspace.userRoles);
    expect(workspaceSharePage.roleNamePairsOnPage)
        .toEqual(roleNamePairs);
    expect(workspaceSharePage.roleNamePairsOnPage.length)
        .toBe(4);
  }));

  it('removes users correctly and does not allow self removal', fakeAsync(() => {
    workspaceSharePage.fixture.componentRef.instance.userEmail
        = 'sampleuser1@fake-research-aou.org';
    workspaceSharePage.fixture.componentRef.instance.accessLevel
        = WorkspaceAccessLevel.OWNER;
    workspaceSharePage.readPageData();
    const de = workspaceSharePage.fixture.debugElement;
    de.queryAll(By.css('.remove-button')).forEach((removeButton) => {
      simulateClick(workspaceSharePage.fixture, removeButton);
    });
    workspaceSharePage.readPageData();
    const roleNamePairs =
      convertToUserRoleRow(workspaceSharePage.fixture.componentRef.instance.workspace.userRoles);
    expect(workspaceSharePage.roleNamePairsOnPage)
        .toEqual(roleNamePairs);
    expect(workspaceSharePage.roleNamePairsOnPage.length)
        .toBe(1);
    expect(workspaceSharePage.roleNamePairsOnPage[0].fullName)
        .toBe('Sample User1');
    expect(workspaceSharePage.roleNamePairsOnPage[0].role)
        .toEqual(WorkspaceAccessLevel.OWNER);
  }));

  it('validates and allows usernames', fakeAsync(() => {
    spyOn(TestBed.get(WorkspacesService), 'shareWorkspace')
      .and.callThrough();

    workspaceSharePage.readPageData();
    simulateInput(workspaceSharePage.fixture, workspaceSharePage.emailField, 'sampleuser4');
    workspaceSharePage.fixture.componentRef.instance.setAccess('Writer');


    simulateClick(workspaceSharePage.fixture,
      workspaceSharePage.fixture.debugElement.query(By.css('.add-button')));
    workspaceSharePage.readPageData();
    expect(TestBed.get(WorkspacesService).shareWorkspace)
        .toHaveBeenCalledWith('defaultNamespace', '1', userValuesStub);
  }));

  it('validates and allows email addresses', fakeAsync(() => {
    spyOn(TestBed.get(WorkspacesService), 'shareWorkspace')
      .and.callThrough();

    workspaceSharePage.readPageData();
    simulateInput(workspaceSharePage.fixture, workspaceSharePage.emailField,
      'sampleuser4@fake-research-aou.org');
    workspaceSharePage.fixture.componentRef.instance.setAccess('Writer');


    simulateClick(workspaceSharePage.fixture,
      workspaceSharePage.fixture.debugElement.query(By.css('.add-button')));
    workspaceSharePage.readPageData();
    expect(TestBed.get(WorkspacesService).shareWorkspace)
      .toHaveBeenCalledWith('defaultNamespace', '1', userValuesStub);
  }));
});
