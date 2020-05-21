import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {DataPageComponent} from 'app/pages/data/data-page';
import {DataSetPageComponent} from 'app/pages/data/data-set/dataset-page';
import {DataUserCodeOfConductComponent} from 'app/pages/profile/data-user-code-of-conduct';
import {UserDisabledComponent} from 'app/pages/user-disabled';
import {AdminBannerComponent} from './pages/admin/admin-banner';
import {AdminInstitutionComponent} from './pages/admin/admin-institution';
import {AdminReviewWorkspaceComponent} from './pages/admin/admin-review-workspace';
import {AdminUserComponent} from './pages/admin/admin-user';
import {AdminWorkspaceComponent} from './pages/admin/admin-workspace';
import {AdminWorkspaceSearchComponent} from './pages/admin/admin-workspace-search';
import {NotebookListComponent} from './pages/analysis/notebook-list';
import {NotebookRedirectComponent} from './pages/analysis/notebook-redirect';
import {CookiePolicyComponent} from './pages/cookie-policy';
import {CohortReviewComponent} from './pages/data/cohort-review/cohort-review';
import {DetailPageComponent} from './pages/data/cohort-review/detail-page';
import {QueryReportComponent} from './pages/data/cohort-review/query-report.component';
import {TablePage} from './pages/data/cohort-review/table-page';
import {CohortActionsComponent} from './pages/data/cohort/cohort-actions';
import {ConceptHomepageComponent} from './pages/data/concept/concept-homepage';
import {ConceptSetActionsComponent} from './pages/data/concept/concept-set-actions';
import {ConceptSetDetailsComponent} from './pages/data/concept/concept-set-details';
import {HomepageComponent} from './pages/homepage/homepage';
import {SignInComponent} from './pages/login/sign-in';
import {ProfilePageComponent} from './pages/profile/profile-page';
import {SessionExpiredComponent} from './pages/session-expired';
import {SignInAgainComponent} from './pages/sign-in-again';
import {SignedInComponent} from './pages/signed-in/component';
import {WorkspaceAboutComponent} from './pages/workspace/workspace-about';
import {WorkspaceEditComponent, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceLibraryComponent} from './pages/workspace/workspace-library';
import {WorkspaceListComponent} from './pages/workspace/workspace-list';
import {WorkspaceWrapperComponent} from './pages/workspace/workspace-wrapper/component';

import {environment} from 'environments/environment';
import {NOTEBOOK_HELP_CONTENT} from './components/help-sidebar';
import {DisabledGuard} from './guards/disabled-guard.service';
import {InteractiveNotebookComponent} from './pages/analysis/interactive-notebook';
import {BreadcrumbType, NavStore} from './utils/navigation';


declare let gtag: Function;

const routes: Routes = [
  {
    path: 'login',
    component: SignInComponent,
    data: {title: 'Sign In'}
  }, {
    path: 'cookie-policy',
    component: CookiePolicyComponent,
    data: {title: 'Cookie Policy'}
  }, {
    path: 'session-expired',
    component: SessionExpiredComponent,
    data: {title: 'You have been signed out'}
  }, {
    path: 'sign-in-again',
    component: SignInAgainComponent,
    data: {title: 'You have been signed out'}
  }, {
    path: 'user-disabled',
    component: UserDisabledComponent,
    data: {title: 'Disabled'}
  }, {
    path: '',
    component: SignedInComponent,
    canActivate: [SignInGuard],
    canActivateChild: [SignInGuard, DisabledGuard, RegistrationGuard],
    runGuardsAndResolvers: 'always',
    children: [
      {
        path: '',
        component: HomepageComponent,
        data: {title: 'Homepage'},
      }, {
        path: 'nih-callback',
        component: HomepageComponent,
        data: {title: 'Homepage'},
      }, {
        path: 'data-code-of-conduct',
        component: DataUserCodeOfConductComponent,
        data: {title: 'Data User Code of Conduct'}
      }, {
        path: 'library',
        component: WorkspaceLibraryComponent,
        data: {title: 'Workspace Library'}
      }, {
        path: 'workspaces',
        children: [
          {
            path: '',
            component: WorkspaceListComponent,
            data: {
              title: 'View Workspaces',
              breadcrumb: BreadcrumbType.Workspaces
            }
          },
          {
            /* TODO The children under ./views need refactoring to use the data
             * provided by the route rather than double-requesting it.
             */
            path: ':ns/:wsid',
            component: WorkspaceWrapperComponent,
            runGuardsAndResolvers: 'always',
            children: [
              {
                path: 'about',
                component: WorkspaceAboutComponent,
                data: {
                  title: 'View Workspace Details',
                  breadcrumb: BreadcrumbType.Workspace,
                  helpContentKey: 'about'
                }
              },
              {
                path: 'edit',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Edit Workspace',
                  mode: WorkspaceEditMode.Edit,
                  breadcrumb: BreadcrumbType.WorkspaceEdit,
                  helpContentKey: 'edit'
                }
              },
              {
                path: 'duplicate',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Duplicate Workspace',
                  mode: WorkspaceEditMode.Duplicate,
                  breadcrumb: BreadcrumbType.WorkspaceDuplicate,
                  helpContentKey: 'duplicate'
                }
              },
              {
                path: 'notebooks',
                children: [
                  {
                    path: '',
                    component: NotebookListComponent,
                    data: {
                      title: 'View Notebooks',
                      breadcrumb: BreadcrumbType.Workspace,
                      helpContentKey: 'notebooks'
                    }
                  }, {
                    path: ':nbName',
                    component: NotebookRedirectComponent,
                    data: {
                      // use the (urldecoded) captured value nbName
                      pathElementForTitle: 'nbName',
                      breadcrumb: BreadcrumbType.Notebook,
                      // The iframe we use to display the Jupyter notebook does something strange
                      // to the height calculation of the container, which is normally set to auto.
                      // Setting this flag sets the container to 100% so that no content is clipped.
                      contentFullHeightOverride: true,
                      helpContentKey: NOTEBOOK_HELP_CONTENT,
                      notebookHelpSidebarStyles: true,
                      minimizeChrome: true
                    }
                  }, {
                    path: 'preview/:nbName',
                    component: InteractiveNotebookComponent,
                    data: {
                      pathElementForTitle: 'nbName',
                      breadcrumb: BreadcrumbType.Notebook,
                      helpContentKey: NOTEBOOK_HELP_CONTENT,
                      notebookHelpSidebarStyles: true,
                      minimizeChrome: true
                    }
                  }
                ]
              },
              {
                path: 'data',
                children: [
                  {
                    path: '',
                    component: DataPageComponent,
                    data: {
                      title: 'Data Page',
                      breadcrumb: BreadcrumbType.Workspace,
                      helpContentKey: 'data'
                    }
                  },
                  {
                    path: 'data-sets',
                    component: DataSetPageComponent,
                    data: {
                      title: 'Dataset Page',
                      breadcrumb: BreadcrumbType.Dataset,
                      helpContentKey: 'datasetBuilder'
                    }
                  },
                  {
                    path: 'data-sets/:dataSetId',
                    component: DataSetPageComponent,
                    data: {
                      title: 'Edit Dataset',
                      breadcrumb: BreadcrumbType.Dataset,
                      helpContentKey: 'datasetBuilder'
                    }
                  }, {
                    path: 'cohorts',
                    children: [
                      {
                        path: ':cid/actions',
                        component: CohortActionsComponent,
                        data: {
                          title: 'Cohort Actions',
                          breadcrumb: BreadcrumbType.Cohort,
                          helpContentKey: 'cohortBuilder'
                        },
                      },
                      {
                        path: 'build',
                        loadChildren: './cohort-search/cohort-search.module#CohortSearchModule',
                      },
                      {
                        path: ':cid/review',
                        children: [
                          {
                            path: '',
                            component: CohortReviewComponent,
                            data: {
                              title: 'Review Cohort Participants',
                              breadcrumb: BreadcrumbType.Cohort,
                              helpContentKey: 'reviewParticipants'
                            }
                          }, {
                            path: 'participants',
                            component: TablePage,
                            data: {
                              title: 'Review Cohort Participants',
                              breadcrumb: BreadcrumbType.Cohort,
                              helpContentKey: 'reviewParticipants'
                            }
                          }, {
                            path: 'cohort-description',
                            component: QueryReportComponent,
                            data: {
                              title: 'Review Cohort Description',
                              breadcrumb: BreadcrumbType.Cohort,
                              helpContentKey: 'cohortDescription'
                            }
                          }, {
                            path: 'participants/:pid',
                            component: DetailPageComponent,
                            data: {
                              title: 'Participant Detail',
                              breadcrumb: BreadcrumbType.Participant,
                              shouldReuse: true,
                              helpContentKey: 'reviewParticipantDetail'
                            }
                          }
                        ],
                      }
                    ]
                  },
                  {
                    path: 'concepts',
                    component: ConceptHomepageComponent,
                    data: {
                      title: 'Search Concepts',
                      breadcrumb: BreadcrumbType.SearchConcepts,
                      helpContentKey: 'conceptSets'
                    }
                  },
                  {
                    path: 'concepts/sets',
                    children: [{
                      path: ':csid',
                      component: ConceptSetDetailsComponent,
                      data: {
                        title: 'Concept Set',
                        breadcrumb: BreadcrumbType.ConceptSet,
                        helpContentKey: 'conceptSets'
                      },
                    }, {
                      path: ':csid/actions',
                      component: ConceptSetActionsComponent,
                      data: {
                        title: 'Concept Set Actions',
                        breadcrumb: BreadcrumbType.ConceptSet,
                        helpContentKey: 'conceptSets'
                      },
                    }, ]
                  }
                ]
              }]
          }]
      },
      {
        path: 'admin/review-workspace',
        component: AdminReviewWorkspaceComponent,
        data: {title: 'Review Workspaces'}
      }, {
        path: 'admin/user',
        component: AdminUserComponent,
        data: {title: 'User Admin Table'}
      }, {
        path: 'admin/institution',
        component: AdminInstitutionComponent,
        data: { title: 'Institution Admin'},
      }, {
        path: 'admin/banner',
        component: AdminBannerComponent,
        data: {title: 'Create Banner'}
      }, {
        path: 'admin/workspaces',
        component: AdminWorkspaceSearchComponent,
        data: { title: 'Workspace Admin'},
      }, {
        path: 'admin/workspaces/:workspaceNamespace',
        component: AdminWorkspaceComponent,
        data: { title: 'Workspace Admin'}
      }, {
        path: 'profile',
        component: ProfilePageComponent,
        data: {title: 'Profile'}
      }, {
        path: 'workspaces/build',
        component: WorkspaceEditComponent,
        data: {title: 'Create Workspace', mode: WorkspaceEditMode.Create}
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {onSameUrlNavigation: 'reload', paramsInheritanceStrategy: 'always'})],
  exports: [RouterModule],
  providers: [
    DisabledGuard,
    RegistrationGuard,
    SignInGuard,
  ]
})
export class AppRoutingModule {

  constructor(public router: Router) {
    NavStore.navigate = (commands, extras) => this.router.navigate(commands, extras);
    NavStore.navigateByUrl = (url, extras) => this.router.navigateByUrl(url, extras);
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', environment.gaId, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
