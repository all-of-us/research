import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ClarityModule } from '@clr/angular';

/* Components */
import { ChartModule } from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import * as highcharts from 'highcharts';
import 'highcharts/highcharts-more';
import { ChartComponent } from './chart/chart.component';

import { AchillesService } from './services/achilles.service';

import { DataBrowserHeaderComponent } from './data-browser-header/data-browser-header.component';
import { TreeService } from './services/tree.service';

import { LocalStorageModule } from 'angular-2-local-storage';


import { HomeAsideComponent } from './home/home-aside/home-aside.component';
import { HomeInfoComponent } from './home/home-info/home-info.component';
import { HomeComponent } from './home/home.component';
import { LazyTreeComponent } from './lazy-tree/lazy-tree.component';
import { MobileChartsComponent } from './mobile-charts/mobile-charts.component';
import { MyConceptsComponent } from './my-concepts/my-concepts.component';
import { OneConceptComponent } from './one-concept/one-concept.component';
import { PlaceholderComponent } from './placeholder/placeholder.component';
import { SearchTableComponent } from './search-table/search-table.component';
import { SearchComponent } from './search/search.component';
import { TreeContainerComponent } from './tree-container/tree-container.component';

import {DataBrowserService} from 'generated';
import {ConceptDrawerComponent} from './concept-drawer/concept-drawer.component';

@NgModule({
  imports: [
      BrowserModule,
      FormsModule,
      ReactiveFormsModule,
      ChartModule.forRoot(highcharts),
      HttpModule,
      BrowserAnimationsModule,
      ClarityModule,
      LocalStorageModule.withConfig({
          prefix: 'my-app',
          storageType: 'localStorage'
      })
  ],
  declarations: [
      ChartComponent,
      DataBrowserHeaderComponent,
      ConceptDrawerComponent,
      SearchComponent,
      MyConceptsComponent,
      LazyTreeComponent,
      TreeContainerComponent,
      SearchTableComponent,
      HomeComponent,
      HomeAsideComponent,
      HomeInfoComponent,
      MobileChartsComponent,
      OneConceptComponent,
      PlaceholderComponent
  ],
  providers: [
      AchillesService,
      DataBrowserService,
      TreeService,
      {
        provide: HighchartsStatic,
        useValue: highcharts,
      }
  ]
})
export class DataBrowserModule {
  constructor() {}
}
