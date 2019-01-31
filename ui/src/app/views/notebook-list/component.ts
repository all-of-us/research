import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';

import {WorkspaceData} from 'app/resolvers/workspace';
import {BugReportComponent} from 'app/views/bug-report/component';
import {NewNotebookModalComponent} from 'app/views/new-notebook-modal/component';

import {ToolTipComponent} from 'app/views/tooltip/component';
import {
  Cluster,
  FileDetail,
  PageVisit,
  ProfileService,
  RecentResource,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';

import {workspacesApi} from 'app/services/swagger-fetch-clients';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    './component.css'],
  templateUrl: './component.html',
})
export class NotebookListComponent implements OnInit, OnDestroy {

  private static PAGE_ID = 'notebook';
  notebooksLoading: boolean;
  notebookList: FileDetail[] = [];
  resourceList: RecentResource[] = [];
  workspace: Workspace;
  notebookError: boolean;
  wsNamespace: string;
  wsId: string;
  localizeNotebooksError: boolean;
  cluster: Cluster;
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
  private accessLevel: WorkspaceAccessLevel;
  showTip: boolean;
  newPageVisit: PageVisit = { page: NotebookListComponent.PAGE_ID};
  firstVisit = true;
  creatingNotebook = false;


  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;
  @ViewChild(ToolTipComponent)
  toolTip: ToolTipComponent;


  constructor(
    private route: ActivatedRoute,
    private profileService: ProfileService,
    private workspacesService: WorkspacesService,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    this.showTip = false;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.notebooksLoading = true;
    this.loadNotebookList();
    this.profileService.getMe().subscribe(
      profile => {
        if (profile.pageVisits) {
          this.firstVisit = !profile.pageVisits.some(v =>
            v.page === NotebookListComponent.PAGE_ID);
        }
      },
      error => {},
      () => {
        if (this.firstVisit) {
          this.showTip = true;
        }
        this.profileService.updatePageVisits(this.newPageVisit).subscribe();
      });
  }

  private loadNotebookList() {
    this.notebooksLoading = true;
    workspacesApi().getNoteBookList(this.wsNamespace, this.wsId)
      .then(
        fileList => {
          this.notebookList = fileList;
          this.resourceList = convertToResources(fileList, this.wsNamespace,
            this.wsId, this.accessLevel, ResourceType.NOTEBOOK);
          this.notebooksLoading = false;
        },
        error => {
          this.notebooksLoading = false;
          this.notebookError = true;
        });
  }

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
  }

  newNotebook(): void {
    this.creatingNotebook = true;
  }

  closeNotebookModal() {
    this.creatingNotebook = false;
  }

  updateList(): void {
    this.loadNotebookList();
  }

  submitNotebooksLoadBugReport(): void {
    this.notebookError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not load notebooks';
  }

  dismissTip(): void {
    this.showTip = false;
  }

  submitNotebookLocalizeBugReport(): void {
    this.localizeNotebooksError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not localize notebook.';
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }
}
