import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from '@angular/router';
import {ProfileService} from 'generated';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class EraCommonGuard implements CanActivate, CanActivateChild {

  constructor(private router: Router, private profile: ProfileService) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
      Observable<boolean> | Promise<boolean> | boolean {
    if (route.routeConfig.path === 'eraCommon' ||
        route.routeConfig.path.startsWith('nih-callback/')) {
      // Leave /admin unguarded in order to allow bootstrapping of verified users.
      return true;
    }
    // add check for training as well as linkedNihUserName and expiration time
    this.profile.getMe().subscribe( (profile) => {
      if (profile.linkedNihUsername == null) {
        const params = {};
        if (state.url && state.url !== '/' && !state.url.startsWith('/eraCommon')) {
          params['from'] = state.url;
        }

        this.router.navigate(['/eraCommon', params]);
        return false;
      }
      return true;
    });
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
      Observable<boolean> | Promise<boolean> | boolean {
    return this.canActivate(route, state);
  }
}
