import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, ProtectedRoutes, withFullHeight, withRouteData} from 'app/components/app-router';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import { ReactWrapperBase } from 'app/utils';
import {authStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {CookiePolicy} from './pages/cookie-policy';
import {UserDisabled} from "./pages/user-disabled";


const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const DUCC = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);

export const AppRoutingComponent: React.FunctionComponent = () => {
  const {authLoaded = false} = useStore(authStore);

  return authLoaded && <AppRouter>
    <AppRoute path='/cookie-policy' component={CookiePolicy}/>
    <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute
        path='/data-code-of-conduct'
          component={ () => <DUCC routeData={{
            title: 'Data User Code of Conduct',
            minimizeChrome: true
          }} />}
        />
    </ProtectedRoutes>
    <AppRoute path='/user-disabled' component={UserDisabled}/>
  </AppRouter>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor() {
    super(AppRoutingComponent, []);
  }
}
