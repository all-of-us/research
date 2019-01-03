import {NgModule} from '@angular/core';
import {NgxChartsModule} from '@swimlane/ngx-charts';

import {CohortResolver} from '../resolvers/cohort';
import {ComboChartComponent} from './combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from './validator-errors/validator-errors.component';

@NgModule({
  imports: [
    NgxChartsModule,
  ],
  declarations: [
    ComboChartComponent,
    ValidatorErrorsComponent
  ],
  exports: [
    // TODO: This could be moved back to CohortSearchModule once no longer
    // needed in CohortReviewModule.
    ComboChartComponent,
    ValidatorErrorsComponent
  ],
  providers: [
    CohortResolver
  ]
})
export class CohortCommonModule {}
