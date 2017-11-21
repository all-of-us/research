import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { AchillesService } from '../services/achilles.service'
import { NgForm, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { State } from "clarity-angular";

@Component({
  selector: 'app-search-table-advanced',
  templateUrl: './search-table-advanced.component.html',
  styleUrls: ['./search-table-advanced.component.css']
})
export class SearchTableAdvancedComponent implements OnInit {

  /* Todo et these dynamically possibly */
  sourceConceptFilters = [{ vocabulary_id: 'ICD9CM' }, { vocabulary_id: 'ICD10' }, { vocabulary_id: 'ICD10CM'} ];
  standardConceptFilters = [{ vocabulary_id: 'SNOMED' }, { vocabulary_id: 'RxNorm' }, { vocabulary_id: 'CPT4' }];
  domainFilters = [{ domain_id: 'Condition' }, { domain_id: 'Procedure' }, { domain_id: 'Drug' }, { domain_id: 'Visit' }];


  filterValueAr = []
  source_vocabs_model = {}
  standard_vocabs_model = {} // model of standard for form
  conceptResults;

  redraw: number[] = []; // flag te redraw analysis , indexed exactly like analyses
  analyses = []
  routeId: string;
  colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4']
  counter = 0;
  page_len = 10;
  cur_page = 1;
  loading: boolean = true;
  totalItems: number;
  searchParams: any = null;
  standardConceptCheck: boolean
  filters = false;


  retainedString

  @Output() onItemSelected: EventEmitter<any>;
  @Output() emittSearchString: EventEmitter<any>;
  @Input() savedSearchStringAdv
  @Input() itemFromHeader
  @Input() toggleAdv
  @Input() domain_id
  @Input() vocabulary_id

  myForm: FormGroup;


  constructor(private achillesService: AchillesService, private route: ActivatedRoute, fb: FormBuilder) {
    // instantiate our event emitter Output
    this.onItemSelected = new EventEmitter();
    this.emittSearchString = new EventEmitter();
    // Build the form
    let source_vocabs_fg = {}
    let standard_vocabs_fg = {}
    let domains_fg = {}
    for (let v of this.standardConceptFilters) {
      standard_vocabs_fg[v.vocabulary_id] = false;
      //this.standard_vocabs_model[v.vocabulary_id] = { checked: false };
    }
    for (let v of this.sourceConceptFilters) {
      source_vocabs_fg[v.vocabulary_id] = false;
    }
    for (let v of this.domainFilters) {
      domains_fg[v.domain_id] = false;
    }

    this.savedSearchStringAdv = null;
    this.myForm = fb.group({
      search: [''],
      standard_concept: ['S'], // Default to searching only standard concepts
      observed: 'true',
      standard_vocabs: fb.group(standard_vocabs_fg),
      source_vocabs: fb.group(source_vocabs_fg),
      domains: fb.group(domains_fg),
      standard_checked: [false]
      });

  }

  resetSearchForm(runSearch) {
    // Reset all but search string
    this.savedSearchStringAdv = this.myForm.value.search;
    this.myForm.reset({search: this.savedSearchStringAdv});
    if (runSearch) {
      this.searchData(this.myForm.value);
    }
  }

  ngOnInit() {

  //  this.retainedString = this.savedSearchStringAdv;
  }



  ngOnChanges(changes) {
    // Set our input search if we have it
    this.myForm.value.search = this.savedSearchStringAdv;
    this.retainedString = this.savedSearchStringAdv

    if (changes.toggleAdv) {
      // When toggle adv is turned off, reset all the filters on th eform to ignore them
      if (changes.toggleAdv.currentValue == false && changes.toggleAdv.previousValue == true)  {
        this.resetSearchForm(false);
      }

      // Run search again
      this.searchData(this.myForm.value);
    }
  }



  // Pager clicked on grid, sends state object
  refresh(state: State) {
    // Note , this calls search on page load . So if it is first time, initialize search params with form defaults
    if (!this.searchParams) {
      this.searchParams = this.myForm.value;
    }

    // We convert the filters from an array to a map,
    // because that's what our backend-calling service is expecting
    // May want to have filters
    /*let filters:{[prop:string]: any[]} = {};
    if (state.filters) {
        for (let filter of state.filters) {
            let {property, value} = <{property: string, value: string}>filter;
            filters[property] = [value];
        }
    }*/

    this.searchParams.sort = state.sort ? state.sort : null;
    this.searchParams.filters = state.filters ? state.filters : null;

    // Default page
    let curPage = 1;
    this.searchParams.page_len = this.page_len;
    this.searchParams.page_from = 0
    this.searchParams.page_to = 1
    if (state.page) {
      this.searchParams.page_len = state.page.size ? state.page.size : this.page_len;
      this.searchParams.page_from = state.page.from? state.page.from : 0;
      this.searchParams.page_to = state.page.to ? state.page.to : this.page_len - 1;
      if (state.page.from > 0) {
        curPage = state.page.from / state.page.size + 1;
      }
    }
    this.searchParams.page = curPage;
    this.searchData(this.searchParams);

  }

  selectedRow;
  // Send concept click out to world
  itemClick(concept: any, index) {
    this.selectedRow = index;
    this.onItemSelected.emit(concept);
  }

  // Send search string out to world
  sendString(string) {
    this.emittSearchString.emit(string);
  }



  //
  submitSearchForm(form) {
    // Search form submit has domain and vocab filters, plus search text and other options
    // Form logic is as so:
    // If no form.domains are checked then we search all domains {}
    // Set domain id if we have one
    this.searchData(form)
  }
  searchData(params) {
    // Takes params and runs search.
    // SearhcParams are set in the submit search form and refresh() pager and filter call back of the grid
    // This just runs the search
    this.searchParams = params ;


    // Concept_name goes in search for now. may expand search outside of just concept name
    if (!this.searchParams.search) { this.searchParams.search = ''};
    this.searchParams.concept_name = this.searchParams.search;

    // Search only standard concepts when not adv
    if (!this.toggleAdv) {
      this.searchParams.standard_concept = "s"
    }
    else {
      this.searchParams.standard_concept = "";
    }

    this.loading = true;
    this.achillesService.getConceptResults(this.searchParams)
      .subscribe(results => {
        this.conceptResults = results.data;
        this.totalItems = results.totalItems;

       this.loading = false;
      });
  }

  logSearchParams(params){
    //Log search params to back end
    params['advanced']=this.toggleAdv;
    this.achillesService.logSearchParams(params);
  }

}
