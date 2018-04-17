import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {Workspace, WorkspaceAccessLevel, WorkspacesService} from 'generated';

/**
 * Flatten a layer of nesting
 */
export interface WorkspaceData extends Workspace {
  accessLevel: WorkspaceAccessLevel;
}

@Injectable()
export class WorkspaceResolver implements Resolve<WorkspaceData> {
  constructor(
    private api: WorkspacesService,
    private router: Router
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<WorkspaceData> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;

    // console.log(`Resolving Workspace ${ns}/${wsid}:`);
    // console.dir(route);

    const call = this.api
      .getWorkspace(ns, wsid)
      .map(({workspace, accessLevel}) => ({...workspace, accessLevel}))
      .catch(
        (e) => {
          this.router.navigate(['workspace', ns, wsid, 'notfound']);
          return Observable.of({error: e});
        }
      );
    return (call as Observable<WorkspaceData>);
  }
}
