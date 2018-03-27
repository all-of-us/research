import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

import {AlertsComponent} from './alerts/alerts.component';
import {AttributesModule} from './attributes/attributes.module';
import {DemoFormComponent} from './demo-form/demo-form.component';
import {ExplorerComponent} from './explorer/explorer.component';
import {LeafComponent} from './leaf/leaf.component';
import {
  QuickSearchResultsComponent
} from './quicksearch-results/quicksearch-results.component';
import {QuickSearchComponent} from './quicksearch/quicksearch.component';
import {RootSpinnerComponent} from './root-spinner/root-spinner.component';
import {SelectionComponent} from './selection/selection.component';
import {TreeComponent} from './tree/tree.component';
import {WizardComponent} from './wizard/wizard.component';


@NgModule({
  imports: [
    AttributesModule,
    CommonModule,
    ClarityModule,
    ReactiveFormsModule,
    NgxPopperModule,
    NouisliderModule,
  ],
  exports: [WizardComponent],
  declarations: [
    AlertsComponent,
    DemoFormComponent,
    ExplorerComponent,
    LeafComponent,
    QuickSearchComponent,
    QuickSearchResultsComponent,
    RootSpinnerComponent,
    SelectionComponent,
    TreeComponent,
    WizardComponent,
  ],
})
export class CriteriaWizardModule { }
