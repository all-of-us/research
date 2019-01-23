import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {CohortBuilderService, CohortReview, CohortReviewService, DemoChartInfoListResponse, DomainType, SearchRequest} from 'generated';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'app-overview-charts',
  templateUrl: './overview-page.html',
  styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  @Output() dataItems = new EventEmitter<any>();
  data = List();
  typesList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG],
    DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  review: CohortReview;
  totalParticipantCount: number;
  buttonsDisableFlag = false;
  private subscription: Subscription;
  domainsData = {};
  totalCount: any;
  constructor(
    private chartAPI: CohortBuilderService,
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const {cohort, review, workspace} = this.route.snapshot.data;
    const request = <SearchRequest>(JSON.parse(cohort.criteria));
    this.chartAPI.getDemoChartInfo(workspace.cdrVersionId, request)
      .map(response => (<DemoChartInfoListResponse>response).items)
      .subscribe(data => {
        this.data = fromJS(data);
        this.dataItems.emit(this.data);
        this.buttonsDisableFlag = false;
      });
    this.review = review;
    this.totalParticipantCount = review.matchedParticipantCount;


    this.openChartContainer = true;
    this.fetchChartsData();
  }


  fetchChartsData() {
    this.demoGraph = false;

    this.buttonsDisableFlag = true;
    this.showTitle = false;
    const limit = 10;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    this.typesList.map(domainName => {
      this.domainsData[domainName] = {
              conditionTitle: '',
              loading: true
            };
      this.subscription = this.reviewAPI.getCohortChartData(ns, wsid, cid, cdrid, domainName,
        limit, null)
        .subscribe(data => {
          const chartData = data;
          this.totalCount = chartData.count;
          this.domainsData[domainName] = chartData.items;
          this.domainsData[domainName].loading = false;
        });
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}
