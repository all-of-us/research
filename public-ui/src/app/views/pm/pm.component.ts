import { Component, OnDestroy, OnInit } from '@angular/core';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';
import {DbConstantsService} from '../../utils/db-constants.service';

@Component({
  selector: 'app-physical-measurements',
  templateUrl: './pm.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './pm.component.css']
})
export class PhysicalMeasurementsComponent implements OnInit, OnDestroy {
  title = 'Browse Program Physical Measurements';
  pageImage = '/assets/db-images/man-standing.png';
  private subscriptions: ISubscription[] = [];
  loadingStack: any = [];

  // Todo put constants in a class for use in other views
  chartType = 'histogram';

  // Total analyses
  genderAnalysis: Analysis = null;
  raceAnalysis: Analysis = null;
  ethnicityAnalysis: Analysis = null;

  // Chart titles for our gender histograms.
  maleGenderChartTitle =  '';
  femaleGenderChartTitle = '';
  otherGenderChartTitle = '';

  conceptGroups = [
    { group: 'blood-pressure', groupName: 'Mean Blood Pressure', concepts: [
      {conceptId: '903118', conceptName: 'Systolic ', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0},
      {conceptId: '903115', conceptName: 'Diastolic', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'height', groupName: 'Height', concepts: [
      {conceptId: '903133', conceptName: 'Height', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0}
    ]},
    { group: 'weight', groupName: 'Weight', concepts: [
      {conceptId: '903121', conceptName: 'Weight', analyses: null, chartType: this.chartType,
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'mean-waist', groupName: 'Mean waist circumference', concepts: [
      { conceptId: '903135', conceptName: 'Mean waist circumference', analyses: null,
        chartType: this.chartType, maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'mean-hip', groupName: 'Mean hip circumference', concepts: [
      {conceptId: '903136', conceptName: 'Mean hip circumference', analyses: null,
        chartType: this.chartType, maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'mean-heart-rate', groupName: 'Mean heart rate', concepts: [
      {conceptId: '903126', conceptName: 'Mean heart rate', analyses: null,
        chartType: this.chartType, maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'wheel-chair', groupName: 'Wheel chair use', concepts: [
      {conceptId: '903111', conceptName: 'Wheel chair use', analyses: null, chartType: 'column',
        maleCount: 0, femaleCount: 0, otherCount: 0},
    ]},
    { group: 'pregnancy', groupName: 'Pregnancy', concepts: [
      {conceptId: '903120', conceptName: 'Pregnancy', analyses: null, chartType: 'column',
       maleCount: 0, femaleCount: 0, otherCount: 0},
    ]}
  ];

  // Initialize to first group and concept, adjust order in groups array above
  selectedGroup = this.conceptGroups[0];
  selectedConcept = this.selectedGroup.concepts[0];

  // we save the total gender counts
  femaleCount = 0;
  maleCount = 0;
  otherCount = 0;

  constructor(private api: DataBrowserService, public dbc: DbConstantsService) { }

  loading() {
    return this.loadingStack.length > 0;
  }

  ngOnInit() {
    this.showMeasurement(this.selectedGroup, this.selectedConcept);
    // Get demographic totals
    this.loadingStack.push(true);
    this.subscriptions.push(this.api.getGenderAnalysis()
      .subscribe({
        next: result => {
          this.genderAnalysis = result;
          for (const g of this.genderAnalysis.results) {
            if (g.stratum1 === this.dbc.FEMALE_GENDER_ID) {
              this.femaleCount = g.countValue;
            } else if (g.stratum1 === this.dbc.MALE_GENDER_ID) {
              this.maleCount = g.countValue;
            } else {
              this.otherCount += g.countValue;
            }
          }
          this.loadingStack.pop();
        },
        error: err =>  {
          this.loadingStack.pop();
          console.log('Error: ', err);
        }}));

    this.loadingStack.push(true);
    this.subscriptions.push(this.api.getRaceAnalysis()
      .subscribe({
          next: result => {
            this.raceAnalysis = result;
            this.loadingStack.pop();
          },
          error: err =>  {
            this.loadingStack.pop();
            console.log('Error: ', err);
          }
      }));

    this.loadingStack.push(true);
    this.subscriptions.push(this.api.getEthnicityAnalysis()
      .subscribe({
        next: result => {
          this.ethnicityAnalysis = result;
          this.loadingStack.pop();
        },
        error: err =>  {
          this.loadingStack.pop();
          console.log('Error: ', err);
        }
      }));
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  showMeasurement(group: any, concept: any) {
    this.selectedGroup = group;
    this.selectedConcept = concept;

    if (!this.selectedConcept.analyses) {
      this.loadingStack.push(true);
      this.subscriptions.push(this.api.getConceptAnalysisResults(
        [this.selectedConcept.conceptId])
        .subscribe({
          next: result => {
            this.selectedConcept.analyses = result.items[0];
            // Organize, massage the data for ui graphing
            // for example, pregnant has only 1 result for pregnant,
            // we add a not pregnant to make display better
            this.arrangeConceptAnalyses(this.selectedConcept);
            this.loadingStack.pop();
          },
          error: err => {
            this.loadingStack.pop();
            console.log('Error: ', err);
          }
        }));
    } else {
      // Don't get the analyses
    }
  }

  arrangeConceptAnalyses(concept: any) {

    if (concept.conceptId === this.dbc.PREGNANCY_CONCEPT_ID) {
      // Delete any male results so we don't look dumb with dumb data
      concept.analyses.measurementValueMaleAnalysis = null;
      concept.analyses.measurementValueOtherGenderAnalysis = null;
      concept.analyses.genderAnalysis.results =
        concept.analyses.genderAnalysis.results.filter(result =>
        result.stratum2 === this.dbc.FEMALE_GENDER_ID );

      // Add not pregnant result to the female value results
      // because this concept is just a one value Yes
      const pregnantResult  = concept.analyses.measurementValueFemaleAnalysis.results[0];

      const notPregnantResult: AchillesResult = {
        analysisId: pregnantResult.analysisId,
        stratum1: pregnantResult.stratum1,
        stratum2: pregnantResult.stratum2,
        stratum3: pregnantResult.stratum3,
        stratum4: 'Not Pregnant',
        stratum5: pregnantResult.stratum5,
        countValue: this.femaleCount - pregnantResult.countValue
      };

      // Add Not pregnant to results,
      concept.analyses.measurementValueFemaleAnalysis.results.push(notPregnantResult);

    }
    if (concept.conceptId === this.dbc.WHEEL_CHAIR_CONCEPT_ID) {
      // Todo What to do about this boolean concept , wait for design
    }

    if (concept.analyses.genderAnalysis) {
      this.organizeGenders(concept);
    }
  }
  // Put the gender analysis in the order we want to show them
  // Sum up the other genders and make a result for that
  // Put the gender counts on selected concept for easy use in templates
  organizeGenders(concept: any) {
    const analysis: Analysis = concept.analyses.genderAnalysis;
    let male = null;
    let female = null;
    const others = [];

    // No need to do anything if only one gender
    if (analysis.results.length <= 1) {
      return;
    }
    const results = [];
    for (const g of analysis.results) {
      if (g.stratum2 === this.dbc.MALE_GENDER_ID) {
        male = g;
        concept.maleCount = g.countValue;
        this.maleGenderChartTitle = g.analysisStratumName + ' - ' + g.countValue.toLocaleString();
      } else if (g.stratum2 === this.dbc.FEMALE_GENDER_ID) {
        female = g;
        this.femaleGenderChartTitle = g.analysisStratumName + ' - ' + g.countValue.toLocaleString();
        concept.femaleCount = g.countValue;
      } else {
        concept.otherCount += g.countValue;
      }
    }

    // Order genders how we want to display  Male, Female , Others
    if (male) { results.push(male); }
    if (female) { results.push(female); }
    if (concept.otherCount > 0) {
      // Make Other results,
      const otherResult: AchillesResult =  {
        analysisId: male.analysisId,
        stratum1: male.stratum1,
        stratum2: this.dbc.OTHER_GENDER_ID,
        analysisStratumName: 'Other',
        countValue: concept.otherCount
      };
      results.push(otherResult);
      this.otherGenderChartTitle = otherResult.analysisStratumName + ' - ' +
        otherResult.countValue.toLocaleString();
    }
    analysis.results = results;
  }

}
