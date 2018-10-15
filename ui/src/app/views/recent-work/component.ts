import {Component, ElementRef, HostListener, Input, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  CohortsService,
  RecentResource,
  UserMetricsService,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

import {WorkspaceData} from 'app/services/workspace-storage.service';
import {convertToResources, ResourceType} from 'app/utils/resourceActions';

@Component({
  selector: 'app-recent-work',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class RecentWorkComponent implements OnInit {
  resourceList: RecentResource[];
  resourcesLoading: boolean;
  fullList: RecentResource[] = [];
  @Input('headerText')
  headerText = 'Your Recent Work';
  @Input('workspace')
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  startIndex = 0;
  cssClass: string;
  constructor(
    private userMetricsService: UserMetricsService,
    private workspacesService: WorkspacesService,
    private cohortsService: CohortsService,
    private route: ActivatedRoute,
  ) {}
  index: Number;
  @ViewChild('recentWork')
  eMainFrame: ElementRef;
  size: number;

  ngOnInit(): void {
    this.resourcesLoading = true;
    this.size = this.calculateSize(this.eMainFrame);
    this.updateList();
  }

  @HostListener('window:resize')
  onResize() {
    const width = this.eMainFrame.nativeElement.offsetWidth;
    if ((this.resourcesLoading === false) && (width)) {
      this.size = this.calculateSize(this.eMainFrame);
      this.updateList();
    }
  }

  calculateSize(el: ElementRef): number {
    const width = el.nativeElement.offsetWidth;
    const division = Math.floor((width - 50) / 200);
    if (division >= 2) {
      return division;
    } else {
      return 2;
    }
  }

  updateList(): void {
    if (this.workspace) {
      this.cssClass = 'for-list';
      this.headerText = '';
      const wsData: WorkspaceData = this.route.snapshot.data.workspace;
      this.accessLevel = wsData.accessLevel;
      const notebookCall = this.workspacesService
        .getNoteBookList(this.workspace.namespace, this.workspace.id);
      const cohortCall = this.cohortsService
        .getCohortsInWorkspace(this.workspace.namespace, this.workspace.id);
      Observable.forkJoin(notebookCall, cohortCall)
        .subscribe(([notebooks, cohorts]) => {
          const notebookResources = convertToResources(notebooks, this.workspace.namespace,
            this.workspace.id, this.accessLevel, ResourceType.NOTEBOOK);
          const cohortResources = convertToResources(cohorts.items, this.workspace.namespace,
            this.workspace.id, this.accessLevel, ResourceType.COHORT);
          this.fullList = notebookResources.concat(cohortResources);
          this.fullList.sort((leftSide, rightSide): number => {
            if (leftSide.modifiedTime < rightSide.modifiedTime) { return 1; }
            if (leftSide.modifiedTime > rightSide.modifiedTime) { return -1; }
            return 0;
          });
          this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
          this.resourcesLoading = false;
        });
    } else {
      this.userMetricsService.getUserRecentResources().subscribe((resources) => {
        this.fullList = resources;
        this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
        this.resourcesLoading = false;
      });
    }
  }

  scrollLeft(): void {
    this.startIndex = Math.max(this.startIndex - 1, 0);
    this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
  }

  scrollRight(): void {
    this.startIndex = Math.min(this.startIndex + 1, this.fullList.length) ;
    this.resourceList = this.fullList.slice(this.startIndex, this.startIndex + this.size);
  }

  rightScrollVisible(): boolean {
    return (this.fullList.length > 3) && (this.fullList.length > this.startIndex + this.size);
  }

  leftScrollVisible(): boolean {
    return (this.fullList.length > this.size) && (this.startIndex > 0);
  }

  elementVisible(): boolean {
    return this.fullList.length > 0;
  }

  // Exposed for testing
  setUserMetricsService(svc: UserMetricsService) {
    this.userMetricsService = svc;
  }
}
