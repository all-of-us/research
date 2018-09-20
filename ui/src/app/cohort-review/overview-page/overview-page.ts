import {NgRedux, select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';
import {CohortReviewService, DomainType} from 'generated';
import {ReviewStateService} from '../review-state.service';

import {CohortBuilderService, DemoChartInfoListResponse, SearchRequest} from 'generated';
import {TreeSubType, TreeType} from "../../../generated";
import {typeToTitle} from "../../cohort-search/utils";
import {CohortSearchActions, CohortSearchState, isChartLoading} from "../../cohort-search/redux";


@Component({
    selector: 'app-overview-charts',
    templateUrl: './overview-page.html',
    styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  data = List();
  typesList= [DomainType[DomainType.CONDITION],DomainType[DomainType.PROCEDURE], DomainType[DomainType.MEASUREMENT],DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  private subscription: Subscription;

  constructor(
      private ngRedux: NgRedux<CohortSearchState>,
    private chartAPI: CohortBuilderService,
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    const {cdrVersionId} = this.route.parent.snapshot.data.workspace;
    this.subscription = this.state.cohort$
      .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
      .switchMap(request => this.chartAPI.getDemoChartInfo(cdrVersionId, request))
      .map(response => (<DemoChartInfoListResponse>response).items)
      .subscribe(data => this.data = fromJS(data));

    this.getDemoCharts();
    this.getCharts(); // if only one flag in future modify the method;
  }
    getCharts() {
        this.openChartContainer = true;
    }
    collapseContainer(){
        this.openChartContainer = false;
    }
    getDemoCharts (){
        this.demoGraph = true;
        this.showTitle = false;
    }

    getDifferentCharts(names){
        this.demoGraph = false;
        this.showTitle = true;
        this.title = names;
        this.fetchChartsData(names);
        return this.title;

    }

    fetchChartsData(name){
        const domain = name
        const limit = 10;
        const {ns, wsid, cid} = this.route.parent.snapshot.params;
        const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
           this.actions.fetchReviewChartsData(ns, wsid, cid, cdrid, domain, limit);
    }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
