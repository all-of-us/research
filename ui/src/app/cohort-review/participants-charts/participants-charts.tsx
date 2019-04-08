import {Component, Input} from '@angular/core';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore} from 'app/utils/navigation';
import * as React from 'react';

const css = `
  .graph-border {
    padding: 0.3rem;
  }
  @media print{
    .graph-border {
      padding: 2rem;
      page-break-inside:avoid;
    }
    .page-break {
      page-break-inside: avoid;
    }
  }
`;

const styles = reactStyles({
  dataBlue: {
    backgroundColor: '#216FB4',
    color: 'white',
    height: '24px',
    fontSize: '10px',
    textAlign: 'end',
    paddingRight: '0.2rem',
    fontWeight: 'bold'
  },
  lightGrey: {
    backgroundColor: '#CCCCCC',
    display: '-webkit-box',
  },
  dataBarContainer: {
    paddingLeft: '1rem',
    paddingTop: '0.5rem',
    borderLeft: '1px solid black'
  },
  dataHeading: {
    paddingTop: '0.5rem',
    width: '16rem',
    fontSize: '10px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    textAlign: 'end',
    paddingRight: '0.5rem',
  },
  dataPercent: {
    height: '24px',
    paddingTop: '0.5rem',
    whiteSpace: 'nowrap',
    fontSize: '10px',
    fontWeight: 'bold',
    color: '#4a4a4a',
  },
  count: {
    paddingLeft: '0.2rem',
    fontSize: '10px',
    fontWeight: 'bold',
    color: '#4a4a4a',
  },
  containerMargin: {
    margin: 0,
    minWidth: '100%',
  },
  chartWidth: {
    margin: 0,
    paddingTop: '1rem',
    paddingBottom: '1rem',
  },
  chartHeading: {
    textAlign: 'center',
    color: '#4A4A4A',
    fontSize: '12px',
    fontWeight: 'bold',
    whiteSpace: 'nowrap',
  },
  domainTitle: {
    paddingBottom: '0.5rem',
    fontSize: '16px',
    fontWeight: 600,
    color: '#262262',
    textTransform: 'capitalize',
    lineHeight: '22px',
  }
});

export interface ParticipantsChartsProps {
  domain: string;
  workspace: WorkspaceData;
}

export interface ParticipantsChartsState {
  data: any;
  loading: boolean;
  options: any;
}

export const ParticipantsCharts = withCurrentWorkspace()(
  class extends React.Component<ParticipantsChartsProps, ParticipantsChartsState>  {
    constructor(props: ParticipantsChartsProps) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        options: null,
      };
    }

    componentDidMount() {
      const {domain, workspace: {cdrVersionId, id, namespace}} = this.props;
      const cohort = currentCohortStore.getValue();
      cohortReviewApi().getCohortChartData(namespace, id, cohort.id, +cdrVersionId, domain, 10)
        .then(resp => {
          const data = resp.items.map(item => {
            const percentCount = Math.round((item.count / resp.count) * 100);
            return {...item, percentCount};
          });
          this.setState({data, loading: false});
        });
    }

    render() {
      const {domain} = this.props;
      const {data, loading} = this.state;
      const heading = domain.toLowerCase();
      return <React.Fragment>
        <style>{css}</style>
        {data && <div className='container page-break' style={styles.chartWidth}>
          <div style={styles.domainTitle}>Top 10 {heading}s</div>
          <div className='graph-border'>
            {data.map((item, i) => (
              <div key={i} className='row' style={{display: '-webkit-box'}}>
                {item.name.length >= 40 &&
                  <div className='col-sm-4 col-xs-4 col-lg-4 col-xl-4' style={styles.dataHeading}>
                    {item.name}
                  </div>
                }
                {item.name.length < 40 &&
                  <div className='col-sm-3 col-lg-4 col-xs-4 col-xl-4' style={styles.dataHeading}>
                    {item.name}
                  </div>
                }
                <div className='col-sm-7 col-xs-7 col-lg-7 col-xl-7'
                  style={styles.dataBarContainer}>
                  <div style={styles.lightGrey}>
                    <div style={{...styles.dataBlue, width: `${item.percentCount}%`}}>
                      {item.percentCount >= 90 && <span>{item.count}</span>}
                    </div>
                    <div style={{...styles.count, width: `${item.percentCount}%`}}>
                      {item.percentCount < 90 && <span>{item.count}</span>}
                    </div>
                  </div>
                </div>
                <div style={styles.dataPercent}>
                  {item.percentCount} % of Cohort
                </div>
              </div>
            ))}
          </div>
        </div>}
        {loading && <SpinnerOverlay />}
      </React.Fragment>;
    }
  }
);

@Component({
  selector: 'app-participants-charts',
  template: '<div #root></div>'

})
export class ParticipantsChartsComponent extends ReactWrapperBase {

  @Input('domain') domain: ParticipantsChartsProps['domain'];
  constructor() {
    super(ParticipantsCharts, ['domain']);
  }
}

