import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {DetailTabTable} from 'app/cohort-review/detail-tab-table/detail-tab-table.component';
import {IndividualParticipantsCharts} from 'app/cohort-review/individual-participants-charts/individual-participants-charts';
import {filterStateStore} from 'app/cohort-review/review-state.service';
import {typeToTitle} from 'app/cohort-search/utils';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {DomainType, PageFilterType} from 'generated/fetch';
import {TabPanel, TabView} from 'primereact/tabview';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';

const styles = reactStyles({
  container: {
    width: '100%',
    margin: '0.5rem 0',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem',
  },
  col: {
    position: 'relative',
    minHeight: '8rem',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
    flex: '0 0 33.33333%',
    maxWidth: '33.33333%',
  },
});

const css = `
  body .p-tabview .p-tabview-panels {
    background-color: transparent;
    border: 0;
    padding: 0;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li,
  body .p-tabview.p-tabview-top .p-tabview-nav li:focus {
    padding: 0.571em 1em;
    box-shadow: none;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li a {
    padding: 0;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .p-tabview.p-tabview-top .p-tabview-nav li a,
  body .p-tabview.p-tabview-top .p-tabview-nav li a:hover {
    background-color: transparent;
    color: #2691D0;
    font-size: 14px;
    border: 0;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight:hover a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:focus {
    background: transparent;
    color: #2691D0;
    font-weight: bold;
    border: 0;
    box-shadow: none;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight:hover a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:focus {
    border-bottom: 3px solid #2691D0;
  }
`;

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
const sourceCode = {
  name: 'sourceCode',
  displayName: 'Source Code',
};
const value = {
  name: 'value',
  displayName: 'Value',
};
const ageAtEvent = {
  name: 'ageAtEvent',
  displayName: 'Age At Event',
};
const visitType = {
  name: 'visitType',
  displayName: 'Visit Type',
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
const graph = {
  name: 'graph',
  displayName: ' '
};

const tabs = [
  {
    name: 'All Events',
    domain: DomainType.ALLEVENTS,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, visitType, standardCode, standardVocabulary, standardName, value,
        domain, ageAtEvent
      ],
      source: [
        itemDate, visitType, sourceCode, sourceVocabulary, sourceName, value, domain, ageAtEvent
      ],
    }
  }, {
    name: 'Conditions',
    domain: DomainType.CONDITION,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, standardCode, standardVocabulary, standardName, ageAtEvent, visitType
      ],
      source: [
        itemDate, sourceCode, sourceVocabulary, sourceName, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Procedures',
    domain: DomainType.PROCEDURE,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, standardCode, standardVocabulary, standardName, ageAtEvent, visitType
      ],
      source: [
        itemDate, sourceCode, sourceVocabulary, sourceName, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Drugs',
    domain: DomainType.DRUG,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, standardName, ageAtEvent, numMentions, firstMention, lastMention, visitType
      ],
      source: [
        itemDate, standardName, ageAtEvent, numMentions, firstMention, lastMention, visitType
      ],
    }
  }, {
    name: 'Observations',
    domain: DomainType.OBSERVATION,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, standardName, standardCode, standardVocabulary, ageAtEvent, visitType
      ],
      source: [
        itemDate, sourceName, sourceCode, sourceVocabulary, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Physical Measurements',
    domain: DomainType.PHYSICALMEASUREMENT,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, standardCode, standardVocabulary, standardName, value, ageAtEvent
      ],
      source: [
        itemDate, sourceCode, sourceVocabulary, sourceName, value, ageAtEvent
      ],
    }
  }, {
    name: 'Labs',
    domain: DomainType.LAB,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
      source: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Vitals',
    domain: DomainType.VITAL,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
      source: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Surveys',
    domain: DomainType.SURVEY,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, survey, question, answer
      ],
      source: [
        itemDate, survey, question, answer
      ],
    }
  }
];

const domainList = [DomainType[DomainType.CONDITION],
  DomainType[DomainType.PROCEDURE],
  DomainType[DomainType.DRUG]];

interface Props {
  workspace: WorkspaceData;
}

interface State {
  chartData: any;
  conditionTitle: string;
  filterState: any;
  participantId: number;
  updateState: number;
}

export const DetailTabs = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        chartData: {},
        conditionTitle: null,
        filterState: null,
        participantId: null,
        updateState: 0,
      };
      this.filteredData = this.filteredData.bind(this);
    }

    componentDidMount() {
      const {cdrVersionId} = this.props.workspace;
      urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(({pid}) => !!pid)
        .switchMap(({ns, wsid, cid, pid}) => {
          const chartData = {};
          return Observable.forkJoin(
            ...domainList.map(domainName => {
              chartData[domainName] = {
                loading: true,
                conditionTitle: '',
                items: []
              };
              this.setState({chartData, participantId: pid});
              return from(cohortReviewApi()
                .getParticipantChartData(ns, wsid, cid, +cdrVersionId, pid, domainName, 10))
                .do(({items}) => {
                  chartData[domainName] = {
                    loading: false,
                    conditionTitle: typeToTitle(domainName),
                    items
                  };
                  this.setState({chartData});
                });
            })
          );
        })
        .subscribe();

      filterStateStore.subscribe(filterState => {
        let {updateState} = this.state;
        // this.vocab = filterState.vocab;
        updateState++;
        this.setState({filterState, updateState});
      });
    }

    filteredData(_domain: string, checkedItems: any) {
      const {filterState} = this.state;
      filterState[_domain] = checkedItems;
      filterStateStore.next(filterState);
    }

    render() {
      const {chartData, filterState, participantId, updateState} = this.state;
      return <React.Fragment>
        <style>{css}</style>
        <TabView style={{padding: 0}}>
          <TabPanel header='Summary'>
            <div style={styles.container}>
              <div style={styles.row}>
                {domainList.map((dom, d) => {
                  return <div key={d} style={styles.col}>
                    {chartData[dom] && <div>
                      {chartData[dom].loading && <SpinnerOverlay/>}
                      {!chartData[dom].loading && !chartData[dom].items.length && <div>
                        There are no {chartData[dom].conditionTitle} to show for this participant.
                      </div>}
                      <IndividualParticipantsCharts chartData={chartData[dom]}/>
                    </div>}
                  </div>;
                })}
              </div>
            </div>
          </TabPanel>
          {tabs.map((tab, t) => {
            return <TabPanel key={t} header={tab.name}>
              {filterState && <DetailTabTable
                getFilteredData={this.filteredData}
                filterState={filterState}
                updateState={updateState}
                tabName={tab.name}
                columns={tab.columns[filterState.vocab]}
                filterType={tab.filterType}
                domain={tab.domain}
                participantId={participantId}
              />}
            </TabPanel>;
          })}
        </TabView>
      </React.Fragment>;
    }
  }
);

@Component({
  selector: 'app-detail-tabs',
  template: '<div #root></div>'
})
export class DetailTabsComponent extends ReactWrapperBase {
  constructor() {
    super(DetailTabs, []);
  }
}
