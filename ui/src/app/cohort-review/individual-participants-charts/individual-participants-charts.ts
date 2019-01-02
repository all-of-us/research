import {Component, Input, OnChanges, ViewChild} from '@angular/core';
import * as moment from 'moment';
@Component({
  selector: 'app-individual-participants-charts',
  templateUrl: './individual-participants-charts.html',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsChartsComponent implements OnChanges {
  chartOptions = {};
  @ViewChild('chartRef') chartRef;
  @Input() chartData = [];
  @Input() chartHeader: string;
  trimmedData = [];
  duplicateItems = [];
  yAxisNames = [''];
  chart: any;

  constructor() {}

  ngOnChanges() {
    this.yAxisNames = [''];
    if (this.chartData) {
      this.setYaxisValue();
    }

  }

  setYaxisValue() {
    this.trimmedData = [];
    this.chartOptions = {};
    this.duplicateItems = [];
    this.yAxisNames = [''];
    let yAxisValue = 1;
    this.chartData.reverse().map(items => { // find standardName in duplicate items
      const duplicateFound = this.duplicateItems.find(
       findName =>
         findName.name === items.standardName
       );
      // console.log(duplicateFound)
      // duplicate items found return true otherwise push the the item in duplicateItems array
      if (duplicateFound) {
        Object.assign(items, {
          yAxisValue: duplicateFound.yAxisValue,
          startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
        });
        return true;
      }
      this.duplicateItems.push({name: items.standardName, yAxisValue});
      Object.assign(items, {
        yAxisValue,
        startDate: moment(items.startDate, 'YYYY-MM-DD').unix() // format date to unix timestamp
      });
      yAxisValue ++;
    });
    this.chartData.map(i => {
      const temp = {
        x: i.startDate,
        y: i.yAxisValue,
        standardName: i.standardName,
        ageAtEvent: i.ageAtEvent,
        rank: i.rank,
        startDate: moment.unix(i.startDate).format('MM-DD-YYYY'),
        standardVocabulary: i.standardVocabulary,
      };
      this.trimmedData.push(temp);
      this.trimmedData.reverse();
    });
    this.duplicateItems.map(d => {
      this.yAxisNames.push(d.name.substring(0, 13));
    });

    if (this.trimmedData.length) {
      this.getChartsData();
    }
  }


  getChartsData() {
    const names = this.yAxisNames;
    this.chartOptions = {
      chart: {
        type: 'scatter',
        zoomType: 'xy',
      },
      credits: {
        enabled: false
      },
      title: {
        text: 'Top' + ' ' + this.chartHeader + ' ' + 'over Time',
      },
      xAxis: {
        title: {
          enabled: true,
          text: 'Entry Date',
        },
        labels: {
          formatter: function () {
            return moment.unix(this.value).format('YYYY');
          },
        },
        startOnTick: true,
        endOnTick: true,
        tickInterval: 40 * 3600 * 1000,
      },
      yAxis: [{
        title: {
          enabled: true,
          text: this.chartHeader,
        },
        labels: {
          formatter: function () {
            return names[this.value];
          }
        },
        tickInterval: 1,
        lineWidth: 1,
      },
      {
        title: {
          enabled: false,
        },
        opposite: true,
        lineWidth: 1,
      }],
      plotOptions: {
        scatter: {
          marker: {
            radius: 5,
            states: {
              hover: {
                enabled: true,
                lineColor: 'rgb(100,100,100)'
              }
            }
          },
          states: {
            hover: {
              marker: {
                enabled: false
              }
            }
          },
        }
      },
      tooltip: {
        pointFormat: '<div>' +
        '<b>Date:</b>{point.startDate}<br/>' +
        '<b>Standard Vocab:</b>{point.standardVocabulary}<br/>' +
        '<b>Standard Name:</b>{point.standardName}<br/>' +
        '<b>Age at Event:</b>{point.ageAtEvent}<br/>' +
        '</div>',
        style: {
          color: '#565656',
          fontSize: 12
        },
        shared: true
      },
      series: [{
        type: 'scatter',
        name: 'Details',
        data: this.trimmedData,
        turboThreshold: 5000,
        showInLegend: false,
      }],
    };
  }

  getChartObj(chartObj: any) {
    this.chart = chartObj;
    // check that ResizeObserver is supported
    if (this.chart && typeof ResizeObserver === 'function') {
      // Unbind window.onresize handler so we don't do double redraws
      if (this.chart.unbindReflow) {
        this.chart.unbindReflow();
      }
      // create observer to redraw charts on div resize
      const ro = new ResizeObserver(() => this.chart.reflow());
      ro.observe(this.chartRef.element.nativeElement);
    }
  }
}

