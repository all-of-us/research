import { Injectable } from '@angular/core';
import { Headers, Http, URLSearchParams } from '@angular/http';
import {DataBrowserService} from 'generated';
// import { Observable } from 'rxjs';
import 'rxjs/Rx'; // unlocks all rxjs operators, such as map()
import { AnalysisSection } from '../analysisClasses';
import { Analysis, AnalysisResult, IAnalysis, IAnalysisResult } from '../analysisClasses';
import { AnalysisDist, AnalysisDistResult } from '../analysisSubClasses';
import { Concept } from '../ConceptClasses';
import { DomainClass } from '../domain-class';
import { Vocabulary } from '../VocabularyClasses';


import 'rxjs/add/operator/toPromise';

@Injectable()
export class AchillesService {

  private baseUrl = 'https://cpmdev.app.vumc.org/api/public/';
//  private baseUrl = "https://dev.aou-api.org/api/public/";
  private sections = [];



  constructor(
    private http: Http, private api: DataBrowserService
  ) { }
  private handleError(error: any): Promise<any> {
    console.error('An error occurred: ', error);
    return Promise.reject(error.message || error);
  }


  getAnalysisResults(a: IAnalysis): Promise<any[]> {

    // const url = this.baseUrl + 'analysis_result';
    // var q = '?analysis_id='+a.analysis_id+'&dataType='+a.dataType;
    const q = {
      'analysis_id': a.analysis_id,
      'dataType': a.dataType
    };
    console.log('get results ', a);
    let i = 0;
    for (i = 0; i < a.stratum.length; i++) {
      /*
      Todo: split stratum one on a comma for multiple stratum id's
      */
        let makeArray;
      if (a.stratum[i] !== '') {
        if (typeof a.stratum[i] === 'string') {
          makeArray = a.stratum[i].split(',');
        } else {
          makeArray = [a.stratum[i]];
        }
        // //
        const i_name = i + 1;
        const stratumVar = 'stratum_' + i_name + '[]'; // must add [] for array params
        q[stratumVar] = makeArray;
      }
    }


    return this.api.getAnalysisResults(a.analysis_id, a.stratum[0], a.stratum[1], a.stratum[2], a.stratum[3], a.stratum[4], a.stratum[5])
      .toPromise()
      .then(response => {
        const data = response.items;

        if (!data.length) { return []; }
        const r = data.map(item => {
          let ar = null;
          if (a.dataType == 'distribution') {
            ar = new AnalysisDistResult(item);
            return ar;
          }
          if (a.chartType == 'map') {
            ar = new AnalysisResult(item);
          }

          else {
            ar = new AnalysisResult(item);
          }

          return ar;
        });

        return r;

      })
      .catch(this.handleError);

  }


  getSections(routeId: string): Promise<AnalysisSection[]> {
    // Get sections for this routeId. These are used to make the header menu
    let url = this.baseUrl + 'analysis_sections';
    url = url + '?route_id=' + routeId;

    const sectionAnalysesMap = {
      0: [1, 2, 3, 4, 12, 1101],
      100: [101, 102],
      400: [3000, 3001, 3002], // Conditions
      500: [500, 501, 505, 506],
      600: [600, 601, 606],
      700: [700, 701, 703, 706],
      800: [800, 801, 803, 805, 806],
      1100: [1101, 1103],
      2000: [2000, 2001, 2002, 2003],
      3000: [3000, 3001, 3002],
      3100: [3000, 3001, 3002], // Lifestyle survey
      3200: [3000, 3001, 3002], // Overall Health

    };
    return this.http.get(url)
      .toPromise()
      .then(response => {
        const sections: AnalysisSection[] = response.json().map(item => {
          if (sectionAnalysesMap[item.section_id]) {
            item.analyses = sectionAnalysesMap[item.section_id];
          }
          else {
            item.analyses = sectionAnalysesMap[3000];
          }
          // item.analyses.push();
          return item as AnalysisSection;
        });
        return sections;
      })
      .catch(this.handleError);
  }

  /* Get all the analysis Objects we want to run for an analysisSection
     If justThese is an array , just return ones with id in array  */


  getSectionAnalyses( aids: number[]): Promise<IAnalysis[]> {

    const url = this.baseUrl + 'analyses';
    return this.api.getAnalyses()
      .toPromise()
      .then(response => {

        let data = response.items;
        // If justThese array analysis_ids then ,  filter list to those analysis_ids
        if (aids && aids.length) {
          data = data.filter((item) => aids.indexOf(item.analysisId) !== -1);
        }

        const myanalyses =  data.map(item => {

          if (item.dataType === 'distribution') {
            const a = new AnalysisDist(item);
            //
            return a;
          }
          // Return generic count analysis
          return new Analysis(item);
        });
        return myanalyses;


      });

  }


  getConceptResults(args) {
    console.log('In get concepts ');

    const q =
      {
        concept_name: args.search,
        page: args.page,
        page_len: args.page_len,
        page_from: args.page_from,
        page_to: args.page_to,
        observed: args.observed,
      };

    if (args.observed) {
      q.observed = true;
    }
    if (args.standard_concept == 's') {
      q['standard_concept'] = 's';
    }
    //datagrid filters
    if (args.filters) {

      for (const b of args.filters) {
        q[b.property] = b.value;

      }
    }

    if (args.sort) {
      q['sortCol'] = args.sort.by;

      if (args.sort.reverse == false) {
        q['sortDir'] = 'ASC';
      } else {
        q['sortDir'] = 'DESC';
      }
    }


    const vocabs = []; // array of vocabulary_id filters
    const domains = [];
    for (const v of Object.keys(args.standard_vocabs)) {
      if (args.standard_vocabs[v] == true) {
        vocabs.push(v);
      }
    }
    for (const v of Object.keys(args.source_vocabs)) {
      if (args.source_vocabs[v] == true) {
        vocabs.push(v);
      }
    }
    if (vocabs.length > 0) {
      q['vocabulary_id[]'] = vocabs;
    }

    for (const v of Object.keys(args.domains)) {
      if (args.domains[v] == true) {
        domains.push(v);
      }
    }
    if (domains.length > 0) {
      q['domain_id[]'] = domains;
    }

    console.log('searching getConcepts' , q.concept_name);
    if (!q.concept_name) { q.concept_name = 'heart'; }
    return this.api.getConceptsSearch(q.concept_name).map(
      (response) => {

        const  data = response.items.map(item => {
          return new Concept(item);
        });
        console.log(data);
        return {data: data, totalItems: data.length};

      });

  }

  logSearchParams(args) {
    return;
    /*
    let url = this.baseUrl + "log/search"
    //URLSearchParams needed to make a post request
    let urlSearchParams = new URLSearchParams();

    for (let v of Object.keys(args.standard_vocabs)) {
      if (args.standard_vocabs[v] == true) {
        urlSearchParams.append('vocabulary[]', v);
      }
    }
    for (let v of Object.keys(args.source_vocabs)) {
      if (args.source_vocabs[v] == true) {
        urlSearchParams.append('vocabulary[]', v);
      }
    }
    for (let d of Object.keys(args.domains)) {
      if (args.domains[d] == true) {
        urlSearchParams.append('domain[]', d);
      }
    }
    urlSearchParams.append('search', args.search);
    urlSearchParams.append('advanced', args.advanced);
    return this.http.post(url, urlSearchParams).subscribe();
    */

  }
  logClickedConcept(concept, params) {
    return;
    /*
    let url = this.baseUrl + "log/concept_click";
    let urlSearchParams = new URLSearchParams();
    urlSearchParams.append('concept_id', concept.concept_id);
    urlSearchParams.append('search', params.search);
    urlSearchParams.append('toggleTree', params.toggleTree);
    urlSearchParams.append('toggleAdv', params.toggleTree);
    return this.http.post(url, urlSearchParams).subscribe();
    */
  }

  getSearchVocabFilters() {
    //
    const url = this.baseUrl + '/concept_vocabularies';
    return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();
        //
        return data.data;
      }
      );
  }

  binDataByAge(data) {

    return data;
  }

  // pass the concept obj in an array of analyses and the analyses with only that concept obj
  // or
  // pass an array of analyses with the stratum and it will run ( no concept Obj )
  runAnalysis(analyses: any[], concept?: any) {
    //
    console.log(analyses);
    for (let i = 0; i < analyses.length; i++) {
      // getAnalysisResults to get results

      if (concept && typeof concept.concept_id !== 'undefined') {
        analyses[i].stratum[0] = concept.concept_id.toString();
      }

      // Put the selected concept as the stratum for the analysis
      const arr = [];

      // analyses[i].stratum[0] = arr.join(",");
      console.log('get anares', analyses[i]);
      // Run the analysis -- getting the results in the analysis.results
      this.getAnalysisResults(analyses[i])
        .then(results => {
          analyses[i].results = results;
          ////
          analyses[i].status = 'Done';

        }); // end of .then
    }// end of for loop

  }

  cloneAnalysis(analysis: IAnalysis) {
    const aclass = analysis.constructor.name;
    //
    let a = null;
    if (aclass == 'Analysis') {
      a =  Analysis.analysisClone(analysis);

    }

    if (aclass == 'AnalysisDist') {
      a = new AnalysisDist(analysis);

    }
    const rand = Math.random();
    console.log('Clone a', a);
    a.stratum.push(rand); /* ?? */
    a.results = []; // clear out any results that were in there

    return a;

  }

  cloneConceptChildren(children) {
    const aclass = children.constructor.name;
    //
    let a = null;

    if (aclass == 'Concept') {
      a = new Concept(children);
    }


    return a;

  }

  // Pass concept_id and concept_num = 2
  // and This will Get concepts that map to the concept arg (Children of the concept ) : concept_num = 2
  // Pass concept_id and  concept_num = 1 if you want the parent(s) of the concept
  // Example -- to get the children of snomed concept_id , pass concept_num=2
  getConceptMapsTo(concept_id: number, concept_num: number) {

    return this.api.getConceptsMapsTo(concept_id, concept_num)
      .map(response => {
        const data = response.items;
        return data.map(item => {
          return new Concept(item);
        });
      }
      );

  }

  VocabShow(args: any) {
    //
    const q: any =
      {
        vocabulary_id: args.vocabulary_id,
        standard_concept: args.standard_concept,
        domain_id: args.domain_id
      };
    const url = this.baseUrl + 'vocab_show';
    return this.http.get(url, { search: q })
      .map(
      (response) => {
        const data = response.json();

        //
        return data.map(item => {
          return new Vocabulary(item);
        });

      }
      );


  }

  getDomains() {

     return this.api.getDbDomains().map(data => {
             console.log('Doamin data ', data);
             return data.items.map(item => {
                 return new DomainClass(item);
             });
         }
      );

     /*subscribe(data => {
        console.log('Domain response ', data);
        return data.items.map(item => {
          return new DomainClass(item);
        }
        );
      }
     );*/
  }

}
