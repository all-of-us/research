import {Component, Input} from '@angular/core';
import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

interface Props {
  mode: string;
  data: any;
}

interface State {
  options: any;
}

export class ComboChart extends React.Component<Props, State> {
  readonly codeMap = {
    'M': 'Male',
    'F': 'Female',
    'No matching concept': 'Unknown'
  };

  constructor(props: Props) {
    super(props);
    this.state = {options: null};
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  componentDidUpdate(prevProps: any): void {
    if (prevProps.mode !== this.props.mode) {
      this.getChartOptions();
    }
  }

  getChartOptions() {
    const {mode} = this.props;
    const normalized = mode === 'normalized';
    const options = {
      chart: {
        height: 250,
        type: 'bar'
      },
      credits: {
        enabled: false
      },
      title: {
        text: ''
      },
      xAxis: {
        categories: this.getCategories(),
        tickLength: 0,
        tickPixelInterval: 50
      },
      yAxis: {
        labels: {
          format: '{value}' + (normalized ? '%' : '')
        },
        min: 0,
        title: {
          text: ''
        }
      },
      colors: ['#adcded', '#aae3f5', '#a27ea8', '#7aa3e5', '#a8385d'],
      legend: {
        enabled: false
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        },
        series: {
          stacking: (normalized ? 'percent' : 'normal')
        }
      },
      series: this.getSeries()
    };
    this.setState({options});
  }

  getCategories() {
    const {data} = this.props;
    return data
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => `${datum.get('gender', 'Unknown')} ${datum.get('ageRange', 'Unknown')}`)
      .keySeq()
      .toArray();
  }

  getSeries() {
    const {data} = this.props;
    return data
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => datum.get('race', 'Unknown'))
      .map((group, race) => ({name: race, data: group.map(item => item.get('count'))}))
      .sort((a, b) => a.name < b.name ? 1 : -1)
      .valueSeq()
      .toJS();
  }

  render() {
    const {options} = this.state;
    return <div style={{minHeight: 200}}>
      {options && <HighchartsReact
        highcharts={highCharts}
        options={options}
        callback={getChartObj}
      />}
    </div>;
  }
}

@Component ({
  selector: 'app-combo-chart',
  template: '<div #root></div>'
})
export class ComboChartComponent extends ReactWrapperBase {
  @Input('mode') mode: Props['mode'];
  @Input('data') data: Props['data'];
  constructor() {
    super(ComboChart, ['mode', 'data']);
  }
}

