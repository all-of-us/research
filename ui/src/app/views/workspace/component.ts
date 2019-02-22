import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Comparator, StringFilter} from '@clr/angular';

import {WorkspaceData} from 'app/resolvers/workspace';
import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {SignInService} from 'app/services/sign-in.service';
import {currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ResearchPurposeItems} from 'app/views/workspace-edit/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {NewNotebookModalComponent} from 'app/views/new-notebook-modal/component';
import {ToolTipComponent} from 'app/views/tooltip/component';
import {
  CdrVersion,
  Cohort,
  CohortsService,
  FileDetail,
  PageVisit,
  ProfileService,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

/*
 * Search filters used by the cohort and notebook data tables to
 * determine which of the cohorts loaded into client side memory
 * are displayed.
 */
class CohortNameFilter implements StringFilter<Cohort> {
  accepts(cohort: Cohort, search: string): boolean {
    return cohort.name.toLowerCase().indexOf(search) >= 0;
  }
}
class CohortDescriptionFilter implements StringFilter<Cohort> {
  accepts(cohort: Cohort, search: string): boolean {
    return cohort.description.toLowerCase().indexOf(search) >= 0;
  }
}
class NotebookNameFilter implements StringFilter<FileDetail> {
  accepts(notebook: FileDetail, search: string): boolean {
    return notebook.name.toLowerCase().indexOf(search) >= 0;
  }
}

/*
 * Sort comparators used by the cohort and notebook data tables to
 * determine the order that the cohorts loaded into client side memory
 * are displayed.
 */
class CohortNameComparator implements Comparator<Cohort> {
  compare(a: Cohort, b: Cohort) {
    return a.name.localeCompare(b.name);
  }
}
class CohortDescriptionComparator implements Comparator<Cohort> {
  compare(a: Cohort, b: Cohort) {
    return a.description.localeCompare(b.description);
  }
}
class NotebookNameComparator implements Comparator<FileDetail> {
  compare(a: FileDetail, b: FileDetail) {
    return a.name.localeCompare(b.name);
  }
}

enum Tabs {
  Cohorts,
  Notebooks,
}

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/headers.css',
    '../../styles/cards.css',
    '../../styles/tooltip.css',
    './component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit, OnDestroy {
  private static PAGE_ID = 'workspace';

  @ViewChild(ToolTipComponent) toolTip: ToolTipComponent;
  sharing = false;
  showTip: boolean;
  workspace: Workspace;
  cdrVersion: CdrVersion;
  wsId: string;
  wsNamespace: string;
  cohortsLoading = true;
  cohortsError = false;
  cohortList: Cohort[] = [];
  accessLevel: WorkspaceAccessLevel;
  notebooksLoading = true;
  notebookError = false;
  notebookList: FileDetail[] = [];
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
  tabOpen = Tabs.Notebooks;
  researchPurposeArray: String[] = [];
  leftResearchPurposes: String[];
  rightResearchPurposes: String[];
  newPageVisit: PageVisit = { page: WorkspaceComponent.PAGE_ID};
  firstVisit = true;
  username = '';
  creatingNotebook = false;

  // @ViewChild(BugReportComponent)
  // bugReportComponent: BugReportComponent;
  bugReportOpen: boolean;
  bugReportDescription = '';

  constructor(
    private cohortsService: CohortsService,
    private signInService: SignInService,
    private workspacesService: WorkspacesService,
    private cdrVersionStorageService: CdrVersionStorageService,
    private profileService: ProfileService,
  ) {
    this.closeNotebookModal = this.closeNotebookModal.bind(this);
  }

  ngOnInit(): void {
    const wsData = currentWorkspaceStore.getValue();
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    Object.keys(ResearchPurposeItems).forEach((key) => {
      if (this.workspace.researchPurpose[key]) {
        let shortDescription = ResearchPurposeItems[key].shortDescription;
        if (key === 'diseaseFocusedResearch') {
          shortDescription += ': ' + this.workspace.researchPurpose.diseaseOfFocus;
        }
        this.researchPurposeArray.push(shortDescription);
      }
    });
    this.leftResearchPurposes =
      this.researchPurposeArray.slice(0, Math.ceil(this.researchPurposeArray.length / 2));
    this.rightResearchPurposes =
      this.researchPurposeArray.slice(
        this.leftResearchPurposes.length,
        this.researchPurposeArray.length);
    this.showTip = false;
    const {ns, wsid} = urlParamsStore.getValue();
    this.wsNamespace = ns;
    this.wsId = wsid;
    // TODO: RW-1057
    this.profileService.getMe().subscribe(
      profile => {
        this.username = profile.username;
        if (profile.pageVisits) {
          this.firstVisit = !profile.pageVisits.some(v =>
            v.page === WorkspaceComponent.PAGE_ID);
        }
      },
      error => {},
      () => {
        if (this.firstVisit) {
          this.showTip = true;
        }
        this.profileService.updatePageVisits(this.newPageVisit).subscribe();
      });
    this.cohortsService.getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(
        cohortsReceived => {
          for (const coho of cohortsReceived.items) {
            this.cohortList.push(coho);
          }
          this.cohortsLoading = false;
        },
        error => {
          this.cohortsLoading = false;
          this.cohortsError = true;
        });
    this.loadNotebookList();
    this.cdrVersionStorageService.cdrVersions$.subscribe(resp => {
      this.cdrVersion = resp.items.find(v => v.cdrVersionId === this.workspace.cdrVersionId);
    });
  }

  private loadNotebookList() {
    this.workspacesService.getNoteBookList(this.wsNamespace, this.wsId)
      .subscribe(
        fileList => {
          this.notebookList = fileList;
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

  buildCohort(): void {
    navigate(['/workspaces', this.wsNamespace, this.wsId, 'cohorts', 'build']);
  }

  get workspaceCreationTime(): string {
    const asDate = new Date(this.workspace.creationTime);
    return asDate.toDateString();
  }

  get workspaceLastModifiedTime(): string {
    const asDate = new Date(this.workspace.lastModifiedTime);
    return asDate.toDateString();
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  share(): void {
    this.sharing = true;
  }

  closeShare(): void {
    this.sharing = false;
    // TODO: RW-1919 - remove this
    window.location.reload();
  }

  updateAclList(userRoleList: UserRole[]): void {
    this.workspace.userRoles = userRoleList;
  }

  dismissTip(): void {
    this.showTip = false;
  }

  submitNotebooksLoadBugReport(): void {
    this.notebookError = false;
    // this.bugReportComponent.reportBug();
    this.bugReportDescription = 'Could not load notebooks';
    this.bugReportOpen = true;
  }
}
