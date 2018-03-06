import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {Comparator, StringFilter} from '@clr/angular';

import {
  ErrorResponse,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';

/*
* Search filters used by the workspace data table to
* determine which of the cohorts loaded into client side memory
* are displayed.
*/
class WorkspaceNameFilter implements StringFilter<WorkspaceResponse> {
  accepts(workspaceResponse: WorkspaceResponse, search: string): boolean {
    return workspaceResponse.workspace.name.toLowerCase().indexOf(search) >= 0;
  }
}

class WorkspaceNameComparator implements Comparator<WorkspaceResponse> {
  compare(a: WorkspaceResponse, b: WorkspaceResponse) {
    return a.workspace.name.localeCompare(b.workspace.name);
  }
}

// TODO: Change to research purpose?
class WorkspaceResearchPurposeFilter implements StringFilter<WorkspaceResponse> {
  accepts(workspaceResponse: WorkspaceResponse, search: string): boolean {
    return workspaceResponse.workspace.description.toLowerCase().indexOf(search) >= 0;
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class HomePageComponent implements OnInit {
  errorText : string;
  workspaceList: WorkspaceResponse[] = [];
  workspacesLoading = false;
  workspaceAccessLevel = WorkspaceAccessLevel;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private route: ActivatedRoute,
      private router: Router,
      private workspacesService: WorkspacesService,
  ) {}
  ngOnInit(): void {
    this.errorText = null;
    this.workspacesLoading = true;
    this.workspacesService.getWorkspaces()
        .subscribe(
            workspacesReceived => {
              this.workspaceList = workspacesReceived.items;
              this.workspacesLoading = false;
            },
            error => {
              // if loading workspaces throws an error, display to the user
              const response: ErrorResponse = this.errorHandlingService.convertAPIError(error);
              if (response.message !== null) {
                this.errorText = response.message;
              }
              this.workspacesLoading = false;
            });
  }

  addWorkspace(): void {
    this.router.navigate(['workspace/build'], {relativeTo : this.route});
  }
}
