import {Component, OnDestroy, OnInit} from '@angular/core';
import * as fp from 'lodash/fp';

import {typeToTitle} from 'app/cohort-search/utils';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {CohortReviewService} from 'generated';
import {
  DomainType,
  PageFilterType,
} from 'generated';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

/* The most common column types */
const itemDate = {
  name: 'itemDate',
  classNames: ['date-col'],
  displayName: 'Date',
};
const itemTime = {
  name: 'itemTime',
  classNames: ['time-col'],
  displayName: 'Time',
};
const domain = {
  name: 'domain',
  displayName: 'Domain',
};
const standardVocabulary = {
  name: 'standardVocabulary',
  classNames: ['vocab-col'],
  displayName: 'Standard Vocabulary',
};
const standardName = {
  name: 'standardName',
  displayName: 'Standard Name',
};
const standardCode = {
  name: 'standardCode',
  displayName: 'Standard Code',
};
const sourceVocabulary = {
  name: 'sourceVocabulary',
  classNames: ['vocab-col'],
  displayName: 'Source Vocabulary',
};
const sourceName = {
  name: 'sourceName',
  displayName: 'Source Name',
};
const value = {
  name: 'value',
  displayName: 'Value',
};
const sourceCode = {
  name: 'sourceCode',
  displayName: 'Source Code',
};
const ageAtEvent = {
  name: 'ageAtEvent',
  displayName: 'Age At Event',
};
const visitType = {
  name: 'visitType',
  displayName: 'Visit Type',
};
const visitId = {
  name: 'visitId',
  displayName: 'Visit ID',
};
const numMentions = {
  name: 'numMentions',
  displayName: 'Number Of Mentions',
};
const firstMention = {
  name: 'firstMention',
  displayName: 'Date First Mention',
};
const lastMention = {
  name: 'lastMention',
  displayName: 'Date Last Mention',
};

const dose = {
  name: 'dose',
  displayName: 'Dose',
};
const strength = {
  name: 'strength',
  displayName: 'Strength',
};
const dataRoute = {
  name: 'route',
  displayName: 'Route',
};
const unit = {
  name: 'unit',
  displayName: 'Units',
};
const refRange = {
  name: 'refRange',
  displayName: 'Reference Range',
};
const survey = {
  name: 'survey',
  displayName: 'Survey Name',
};
const question = {
  name: 'question',
  displayName: 'Question',
};
const answer = {
  name: 'answer',
  displayName: 'Answer',
};

@Component({
  selector: 'app-detail-tabs',
  templateUrl: './detail-tabs.component.html',
  styleUrls: ['./detail-tabs.component.css']
})
export class DetailTabsComponent implements OnInit, OnDestroy {
  subscription: Subscription;
  data;
  participantsId: any;
  chartData = {};
  domainList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG]];
  conditionTitle: string;
  chartLoadedSpinner = false;
  summaryActive = false;
  readonly allEvents = {
    name: 'All Events',
    domain: DomainType.ALLEVENTS,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, visitType, standardCode, standardVocabulary, standardName, sourceCode,
      sourceVocabulary, sourceName, dataRoute, dose, strength, value, unit, refRange,
      domain, ageAtEvent, numMentions, firstMention, lastMention
    ],
    reverseEnum: {
      Date: itemDate,
      Time: itemDate,
      visitType: visitType,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      dataRoute: dataRoute,
      quantity: dose,
      strength: strength,
      value: value,
      unit: unit,
      refRange: refRange,
      domain: domain,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
    }
  };

  readonly tabs = [{
    name: 'Conditions',
    domain: DomainType.CONDITION,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, sourceCode, sourceVocabulary,
      sourceName, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
      visitType: visitType,
    }
  }, {
    name: 'Procedures',
    domain: DomainType.PROCEDURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, sourceCode, sourceVocabulary,
      sourceName, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
      visitType: visitType,
    }
  }, {
    name: 'Drugs',
    domain: DomainType.DRUG,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, dataRoute, dose, strength, ageAtEvent, numMentions,
      firstMention, lastMention, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      dataRoute: dataRoute,
      dose: dose,
      strength: strength,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
      visitType: visitType,
    }
  }, {
    name: 'Observations',
    domain: DomainType.OBSERVATION,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, standardCode, standardVocabulary, ageAtEvent, sourceName,
      sourceCode, sourceVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      age: ageAtEvent,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Physical Measurements',
    domain: DomainType.PHYSICALMEASURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, value, unit, ageAtEvent
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      value: value,
      unit: unit,
      age: ageAtEvent,
    }
  }, {
    name: 'Labs',
    domain: DomainType.LAB,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, itemTime, standardName, value, unit, refRange, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      itemTime: itemTime,
      standardName: standardName,
      value: value,
      unit: unit,
      refRange: refRange,
      age: ageAtEvent,
      visitType: visitType
    }
  }, {
    name: 'Vitals',
    domain: DomainType.VITAL,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, itemTime, standardName, value, unit, refRange, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      itemTime: itemTime,
      standardName: standardName,
      value: value,
      unit: unit,
      refRange: refRange,
      age: ageAtEvent,
      visitType: visitType
    }
  }, {
    name: 'Surveys',
    domain: DomainType.SURVEY,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, survey, question, answer
    ],
    reverseEnum: {
      itemDate: itemDate,
      survey: survey,
      question: question,
      answer: answer
    }
  }];

  constructor(
    private reviewAPI: CohortReviewService,
  ) {}

  ngOnInit() {
    this.subscription = Observable
      .combineLatest(urlParamsStore, currentWorkspaceStore)
      .map(([{ns, wsid, cid, pid}, {cdrVersionId}]) => ({ns, wsid, cid, pid, cdrVersionId}))
      .distinctUntilChanged(fp.isEqual)
      .switchMap(({ns, wsid, cid, pid, cdrVersionId}) => {
        return Observable.forkJoin(
          ...this.domainList.map(domainName => {
            this.chartData[domainName] = {
              loading: true,
              conditionTitle: '',
              items: []
            };
            return this.reviewAPI
              .getParticipantChartData(ns, wsid, cid, cdrVersionId, pid, domainName, 10)
              .do(({items}) => {
                this.chartData[domainName] = {
                  loading: false,
                  conditionTitle: typeToTitle(domainName),
                  items
                };
              });
          })
        );
      })
      .subscribe();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
