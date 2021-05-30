import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, Navigate, ProtectedRoutes, withFullHeight, withRouteData} from 'app/components/app-router';
import {AccessRenewalPage} from 'app/pages/access/access-renewal-page';
import {WorkspaceAudit} from 'app/pages/admin/admin-workspace-audit';
import {UserAudit} from 'app/pages/admin/user-audit';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {UserDisabled} from 'app/pages/user-disabled';
import {SignInService} from 'app/services/sign-in.service';
import {ReactWrapperBase} from 'app/utils';
import {authStore, profileStore, useStore} from 'app/utils/stores';
import {serverConfigStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect} from 'react-router';
import {NOTEBOOK_PAGE_KEY} from './components/help-sidebar';
import {NotificationModal} from './components/modals';
import {AdminBanner} from './pages/admin/admin-banner';
import {AdminInstitution} from './pages/admin/admin-institution';
import {AdminInstitutionEdit} from './pages/admin/admin-institution-edit';
import {AdminNotebookView} from './pages/admin/admin-notebook-view';
import {AdminReviewWorkspace} from './pages/admin/admin-review-workspace';
import {AdminUser} from './pages/admin/admin-user';
import {AdminUsers} from './pages/admin/admin-users';
import {AdminWorkspace} from './pages/admin/admin-workspace';
import {AdminWorkspaceSearch} from './pages/admin/admin-workspace-search';
import {InteractiveNotebook} from './pages/analysis/interactive-notebook';
import {NotebookList} from './pages/analysis/notebook-list';
import {NotebookRedirect} from './pages/analysis/notebook-redirect';
import {CohortReview} from './pages/data/cohort-review/cohort-review';
import {DetailPage} from './pages/data/cohort-review/detail-page';
import {QueryReport} from './pages/data/cohort-review/query-report.component';
import {ParticipantsTable} from './pages/data/cohort-review/table-page';
import {CohortActions} from './pages/data/cohort/cohort-actions';
import {ConceptHomepage} from './pages/data/concept/concept-homepage';
import {ConceptSetActions} from './pages/data/concept/concept-set-actions';
import {DataComponent} from './pages/data/data-component';
import {DatasetPage} from './pages/data/data-set/dataset-page';
import {Homepage} from './pages/homepage/homepage';
import {SignIn} from './pages/login/sign-in';
import {ProfilePage} from './pages/profile/profile-page';
import {WorkspaceAbout} from './pages/workspace/workspace-about';
import {WorkspaceEdit, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceLibrary} from './pages/workspace/workspace-library';
import {WorkspaceList} from './pages/workspace/workspace-list';
import {hasRegisteredAccess} from './utils/access-tiers';
import {AnalyticsTracker} from './utils/analytics';
import {BreadcrumbType} from './utils/navigation';


const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const registrationGuard: Guard = {
  allowed: (): boolean => hasRegisteredAccess(profileStore.get().profile.accessTierShortNames),
  redirectPath: '/'
};

const AdminBannerPage = withRouteData(AdminBanner);
const AdminNotebookViewPage = withRouteData(AdminNotebookView);
const AdminReviewWorkspacePage = withRouteData(AdminReviewWorkspace);
const CohortActionsPage = withRouteData(CohortActions);
const CohortReviewPage = withRouteData(CohortReview);
const ConceptHomepagePage = withRouteData(ConceptHomepage);
const ConceptSetActionsPage = withRouteData(ConceptSetActions);
const CookiePolicyPage = withRouteData(CookiePolicy);
const DataComponentPage = withRouteData(DataComponent);
const DataSetComponentPage = withRouteData(DatasetPage);
const DataUserCodeOfConductPage = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);
const DetailPagePage = withRouteData(DetailPage);
const HomepagePage = withRouteData(Homepage); // this name is bad i am sorry
const InstitutionAdminPage = withRouteData(AdminInstitution);
const InstitutionEditAdminPage = withRouteData(AdminInstitutionEdit);
const InteractiveNotebookPage = withRouteData(InteractiveNotebook);
const NotebookListPage = withRouteData(NotebookList);
const NotebookRedirectPage = withRouteData(NotebookRedirect);
const ParticipantsTablePage = withRouteData(ParticipantsTable);
const QueryReportPage = withRouteData(QueryReport);
const SessionExpiredPage = withRouteData(SessionExpired);
const SignInAgainPage = withRouteData(SignInAgain);
const SignInPage = withRouteData(SignIn);
const UserAdminPage = withRouteData(AdminUser);
const UsersAdminPage = withRouteData(AdminUsers);
const UserAuditPage = withRouteData(UserAudit);
const UserDisabledPage = withRouteData(UserDisabled);
const WorkspaceAboutPage = withRouteData(WorkspaceAbout);
const WorkspaceAdminPage = withRouteData(AdminWorkspace);
const WorkspaceAuditPage = withRouteData(WorkspaceAudit);
const WorkspaceEditPage = withRouteData(WorkspaceEdit);
const WorkspaceLibraryPage = withRouteData(WorkspaceLibrary);
const WorkspaceListPage = withRouteData(WorkspaceList);
const WorkspaceSearchAdminPage = withRouteData(AdminWorkspaceSearch);

interface RoutingProps {
  onSignIn: () => void;
  signIn: () => void;
}

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = ({onSignIn, signIn}) => {
  const {authLoaded = false} = useStore(authStore);

  return authLoaded && <React.Fragment>
    <NotificationModal/>
    <AppRouter>
      <AppRoute
          path='/cookie-policy'
          component={() => <CookiePolicyPage routeData={{title: 'Cookie Policy'}}/>}
      />
      <AppRoute
          path='/login'
          component={() => <SignInPage routeData={{title: 'Sign In'}} onSignIn={onSignIn} signIn={signIn}/>}
      />
      <AppRoute
          path='/session-expired'
          component={() => <SessionExpiredPage routeData={{title: 'You have been signed out'}} signIn={signIn}/>}
      />
      <AppRoute
          path='/sign-in-again'
          component={() => <SignInAgainPage routeData={{title: 'You have been signed out'}} signIn={signIn}/>}
      />
      <AppRoute
          path='/user-disabled'
          component={() => <UserDisabledPage routeData={{title: 'Disabled'}}/>}
      />

      <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute
          path='/'
            component={() => <HomepagePage routeData={{title: 'Homepage'}}/>}
        />
        <AppRoute path='/access-renewal' component={() => serverConfigStore.get().config.enableAccessRenewal
          ? <AccessRenewalPage routeData={{title: 'Access Renewal'}}/>
          : <Navigate to={'/profile'}/>
          }/>
        <AppRoute
            path='/admin/banner'
            component={() => <AdminBannerPage routeData={{title: 'Create Banner'}}/>}
        />
        <AppRoute
            path='/admin/institution'
            component={() => <InstitutionAdminPage routeData={{title: 'Institution Admin'}}/>}
        />
        <AppRoute
            path='/admin/institution/add'
            component={() => <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>}
        />
        <AppRoute
            path='/admin/institution/edit/:institutionId'
            component={() => <InstitutionEditAdminPage routeData={{title: 'Institution Admin'}}/>}
        />
        <AppRoute
            path='/admin/user' // included for backwards compatibility
            component={() => <UsersAdminPage routeData={{title: 'User Admin Table'}}/>}
        />
        <AppRoute
            path='/admin/review-workspace'
            component={() => <AdminReviewWorkspacePage routeData={{title: 'Review Workspaces'}}/>}
        />
        <AppRoute
            path='/admin/users'
            component={() => <UsersAdminPage routeData={{title: 'User Admin Table'}}/>}
        />
        <AppRoute
            path='/admin/users/:usernameWithoutGsuiteDomain'
            component={() => <UserAdminPage routeData={{title: 'User Admin'}}/>}
        />
        <AppRoute
            path='/admin/user-audit'
            component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}
        />
        <AppRoute
            path='/admin/user-audit/:username'
            component={() => <UserAuditPage routeData={{title: 'User Audit'}}/>}
        />
        <AppRoute
            path='/admin/workspaces'
            component={() => <WorkspaceSearchAdminPage routeData={{title: 'Workspace Admin'}}/>}
        />
        <AppRoute
            path='/admin/workspaces/:workspaceNamespace'
            component={() => <WorkspaceAdminPage routeData={{title: 'Workspace Admin'}}/>}
        />
        <AppRoute
            path='/admin/workspace-audit'
            component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}
        />
        <AppRoute
            path='/admin/workspace-audit/:workspaceNamespace'
            component={() => <WorkspaceAuditPage routeData={{title: 'Workspace Audit'}}/>}
        />
        <AppRoute
            path='/admin/workspaces/:workspaceNamespace/:nbName'
            component={() => <AdminNotebookViewPage routeData={{
              pathElementForTitle: 'nbName',
              minimizeChrome: true
            }}/>}
        />
        <AppRoute
            path='/data-code-of-conduct'
            component={() => <DataUserCodeOfConductPage routeData={{
              title: 'Data User Code of Conduct',
              minimizeChrome: true
            }} />}
        />
        <AppRoute path='/profile' component={() => <ProfilePage routeData={{title: 'Profile'}}/>}/>
        <AppRoute path='/nih-callback' component={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />
        <AppRoute path='/ras-callback' component={() => <HomepagePage routeData={{title: 'Homepage'}}/>} />

        <ProtectedRoutes guards={[registrationGuard]}>
          <AppRoute
            path='/library'
            component={() => <WorkspaceLibraryPage routeData={{title: 'Workspace Library', minimizeChrome: false}}/>}
          />
          <AppRoute
            path='/workspaces'
            component={() => <WorkspaceListPage
                routeData={{
                  title: 'View Workspaces',
                  breadcrumb: BreadcrumbType.Workspaces
                }}
            />}
          />
          <AppRoute
              path='/workspaces/build'
              component={() => <WorkspaceEditPage
                  routeData={{title: 'Create Workspace'}}
                  workspaceEditMode={WorkspaceEditMode.Create}
              />}
          />
          <AppRoute
              path='/workspaces/:ns/:wsid/about'
              component={() => <WorkspaceAboutPage
                  routeData={{
                    title: 'View Workspace Details',
                    breadcrumb: BreadcrumbType.Workspace,
                    pageKey: 'about'
                  }}
              />}
          />
          <AppRoute
              path='/workspaces/:ns/:wsid/duplicate'
              component={() => <WorkspaceEditPage
                  routeData={{
                    title: 'Duplicate Workspace',
                    breadcrumb: BreadcrumbType.WorkspaceDuplicate,
                    pageKey: 'duplicate'
                  }}
                  workspaceEditMode={WorkspaceEditMode.Duplicate}
              />}
          />
          <AppRoute
              path='/workspaces/:ns/:wsid/edit'
              component={() => <WorkspaceEditPage
                  routeData={{
                    title: 'Edit Workspace',
                    breadcrumb: BreadcrumbType.WorkspaceEdit,
                    pageKey: 'edit'
                  }}
                  workspaceEditMode={WorkspaceEditMode.Edit}
              />}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/notebooks'
            component={() => <NotebookListPage routeData={{
              title: 'View Notebooks',
              pageKey: 'notebooks',
              breadcrumb: BreadcrumbType.Workspace
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/notebooks/preview/:nbName'
            component={() => <InteractiveNotebookPage routeData={{
              pathElementForTitle: 'nbName',
              breadcrumb: BreadcrumbType.Notebook,
              pageKey: NOTEBOOK_PAGE_KEY,
              minimizeChrome: true
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/notebooks/:nbName'
            component={() => <NotebookRedirectPage routeData={{
              pathElementForTitle: 'nbName', // use the (urldecoded) captured value nbName
              breadcrumb: BreadcrumbType.Notebook,
              // The iframe we use to display the Jupyter notebook does something strange
              // to the height calculation of the container, which is normally set to auto.
              // Setting this flag sets the container to 100% so that no content is clipped.
              contentFullHeightOverride: true,
              pageKey: NOTEBOOK_PAGE_KEY,
              minimizeChrome: true
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data'
            component={() => <DataComponentPage routeData={{
              title: 'Data Page',
              breadcrumb: BreadcrumbType.Workspace,
              pageKey: 'data'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/data-sets'
            component={() => <DataSetComponentPage routeData={{
              title: 'Dataset Page',
              breadcrumb: BreadcrumbType.Dataset,
              pageKey: 'datasetBuilder'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/data-sets/:dataSetId'
            component={() => <DataSetComponentPage routeData={{
              title: 'Edit Dataset',
              breadcrumb: BreadcrumbType.Dataset,
              pageKey: 'datasetBuilder'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/cohorts/:cid/actions'
            component={() => <CohortActionsPage routeData={{
              title: 'Cohort Actions',
              breadcrumb: BreadcrumbType.Cohort,
              pageKey: 'cohortBuilder'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/cohorts/:cid/review/participants'
            component={() => <ParticipantsTablePage routeData={{
              title: 'Review Cohort Participants',
              breadcrumb: BreadcrumbType.Cohort,
              pageKey: 'reviewParticipants'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/cohorts/:cid/review/participants/:pid'
            component={() => <DetailPagePage routeData={{
              title: 'Participant Detail',
              breadcrumb: BreadcrumbType.Participant,
              pageKey: 'reviewParticipantDetail'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/cohorts/:cid/review/cohort-description'
            component={() => <QueryReportPage routeData={{
              title: 'Review Cohort Description',
              breadcrumb: BreadcrumbType.Cohort,
              pageKey: 'cohortDescription'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/cohorts/:cid/review'
            component={() => <CohortReviewPage routeData={{
              title: 'Review Cohort Participants',
              breadcrumb: BreadcrumbType.Cohort,
              pageKey: 'reviewParticipants'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/concepts'
            component={() => <ConceptHomepagePage routeData={{
              title: 'Search Concepts',
              breadcrumb: BreadcrumbType.SearchConcepts,
              pageKey: 'searchConceptSets'
            }}/>}
          />
          <AppRoute
            path='/workspaces/:ns/:wsid/data/concepts/sets/:csid/actions'
            component={() => <ConceptSetActionsPage routeData={{
              title: 'Concept Set Actions',
              breadcrumb: BreadcrumbType.ConceptSet,
              pageKey: 'conceptSetActions'
            }}/>}
          />
        </ProtectedRoutes>
      </ProtectedRoutes>
    </AppRouter>
  </React.Fragment>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(AppRoutingComponent, ['onSignIn', 'signIn']);
    this.onSignIn = this.onSignIn.bind(this);
    this.signIn = this.signIn.bind(this);
  }

  onSignIn(): void {
    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        return <Redirect to='/'/>;
      }
    });
  }

  signIn(): void {
    AnalyticsTracker.Registration.SignIn();
    this.signInService.signIn();
  }
}
