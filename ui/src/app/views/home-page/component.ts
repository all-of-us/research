import {Component, OnInit, Inject} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {DOCUMENT} from '@angular/platform-browser';
import {StringFilter, Comparator} from 'clarity-angular';

import {WorkspaceComponent} from 'app/views/workspace/component';
import {resetDateObject} from 'helper-functions';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
/*
* Search filters used by the workspace data table to
* determine which of the cohorts loaded into client side memory
* are displayed.
*/
class WorkspaceNameFilter implements StringFilter<Workspace> {
  accepts(workspace: Workspace, search: string): boolean {
    return workspace.name.toLowerCase().indexOf(search) >= 0;
  }
}

class WorkspaceNameComparator implements Comparator<Workspace> {
  compare(a: Workspace, b: Workspace) {
    return a.name.localeCompare(b.name);
  }
}

// TODO: Change to research purpose?
class WorkspaceResearchPurposeFilter implements StringFilter<Workspace> {
  accepts(workspace: Workspace, search: string): boolean {
    return workspace.description.toLowerCase().indexOf(search) >= 0;
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class HomePageComponent implements OnInit {
  private workspaceNameFilter = new WorkspaceNameFilter();
  private workspaceResearchPurposeFilter = new WorkspaceResearchPurposeFilter();
  private workspaceNameComparator = new WorkspaceNameComparator();
  repositories: Repository[] = [];
  user: User;  // to detect if logged in
  workspaceList: Workspace[] = [];
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private workspacesService: WorkspacesService,
      @Inject(DOCUMENT) private document: any
  ) {}
  ngOnInit(): void {
    this.userService.getLoggedInUser().then(user => this.user = user);
    this.workspacesService
        .getWorkspaces()
        .retry(2)
        .subscribe(
            workspacesReceived => {
              this.workspaceList = workspacesReceived.items;
              this.workspaceList.forEach(workspace => {
                workspace.lastModifiedTime = resetDateObject(workspace.lastModifiedTime);
                workspace.creationTime = resetDateObject(workspace.creationTime);
              });
            });
  }
}
