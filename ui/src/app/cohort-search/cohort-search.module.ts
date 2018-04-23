import {NgReduxModule} from '@angular-redux/store';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {ComboChartComponent} from './combo-chart/combo-chart.component';
import {GenderChartComponent} from './gender-chart/gender-chart.component';
import {ModalComponent} from './modal/modal.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from './search-group-select/search-group-select.component';
import {SearchGroupComponent} from './search-group/search-group.component';

/* Other Objects */
import {CriteriaWizardModule} from './criteria-wizard/criteria-wizard.module';
import {CohortSearchActions, CohortSearchEpics, ConfigureStore} from './redux';


const routes: Routes = [{
  path: '',
  component: CohortSearchComponent,
  data: {title: 'Build Cohort Criteria'},
}];


@NgModule({
  imports: [
    // Angular
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    // 3rd Party
    ClarityModule,
    NgReduxModule,
    NgxChartsModule,
    NgxPopperModule,
    // Ours
    CriteriaWizardModule,
  ],
  declarations: [
    CohortSearchComponent,
    ComboChartComponent,
    GenderChartComponent,
    ModalComponent,
    OverviewComponent,
    SearchGroupComponent,
    SearchGroupItemComponent,
    SearchGroupListComponent,
    SearchGroupSelectComponent,
  ],
  exports: [
    // TODO: Remove this once no longer needed by CohortReviewModule.
    ComboChartComponent
  ],
  providers: [
    CohortSearchActions,
    CohortSearchEpics,
    ConfigureStore,
  ]
})
export class CohortSearchModule {
  constructor(store: ConfigureStore) {}
}
