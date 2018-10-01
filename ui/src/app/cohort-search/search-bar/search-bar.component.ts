import {NgRedux, select} from '@angular-redux/store';
import {
  Component,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {TreeSubType, TreeType} from 'generated';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {
  activeCriteriaSubtype,
  autocompleteError,
  autocompleteOptions,
  CohortSearchActions,
  CohortSearchState,
  ingredientsForBrand,
  isAutocompleteLoading,
  subtreeSelected,
} from '../redux';

import {highlightMatches, stripHtml} from '../utils';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  @select(activeCriteriaSubtype) subtype$: Observable<string>;
  @select(subtreeSelected) selected$: Observable<any>;
  @Input() _type;
  searchTerm = '';
  typedTerm: string;
  options = [];
  multiples: any;
  loading = false;
  noResults = false;
  optionSelected = false;
  error = false;
  subscription: Subscription;
  numMatches: number;
  ingredientList = [];
  highlightedOption: number;
  subtype: string;
  codes: any;

  @ViewChild('searchBar') searchBar;

  @HostListener('document:mouseup', ['$event.target'])
  onClick(targetElement) {
    const clickedInside = this.searchBar.nativeElement.contains(targetElement);
    if (!clickedInside) {
      this.hideDropdown();
    }
  }

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    this.codes = this.ngRedux.getState().getIn(['wizard', 'codes']);
    const errorSub = this.ngRedux
      .select(autocompleteError())
      .map(err => !(err === null || err === undefined))
      .subscribe(err => this.error = err);

    const loadingSub = this.ngRedux
      .select(isAutocompleteLoading())
      .subscribe(loading => this.loading = loading);

    const optionsSub = this.ngRedux
      .select(autocompleteOptions())
      .subscribe(options => {
        if (this.searchTerm.length >= 4) {
          this.options = [];
          this.multiples = {};
          const optionNames = [];
          if (options !== null) {
            options.forEach(option => {
              this.highlightedOption = null;
              if (optionNames.indexOf(option.name) === -1) {
                optionNames.push(option.name);
                option.displayName = highlightMatches([this.searchTerm], option.name);
                this.options.push(option);
              } else {
                if (this.multiples[option.name]) {
                  this.multiples[option.name].push({id: option.id, path: option.path});
                } else {
                  this.multiples[option.name] = [{id: option.id, path: option.path}];
                }
              }
            });
          }
          this.noResults = !this.optionSelected
            && !this.options.length;
        }
      });

    const ingredientSub = this.ngRedux
      .select(ingredientsForBrand())
      .subscribe(ingredients => {
        this.ingredientList = [];
        const ids = [];
        let path = [];
        ingredients.forEach(item => {
          if (!this.ingredientList.includes(item.name)) {
            this.ingredientList.push(item.name);
          }
          ids.push(item.id);
          path = path.concat(item.path.split('.'));
        });
        if (this.ingredientList.length) {
          this.actions.setCriteriaSearchTerms(this.ingredientList);
          this.actions.loadCriteriaSubtree(this._type, TreeSubType[TreeSubType.BRAND], ids, path);
        }
      });

    const subtreeSelectSub = this.selected$
      .filter(selectedIds => !!selectedIds)
      .subscribe(selectedIds => this.numMatches = selectedIds.length);

    const subtypeSub = this.subtype$
      .subscribe(subtype => {
        this.searchTerm = '';
        this.subtype = subtype;
      });

    this.subscription = errorSub;
    this.subscription.add(loadingSub);
    this.subscription.add(optionsSub);
    this.subscription.add(ingredientSub);
    this.subscription.add(subtreeSelectSub);
    this.subscription.add(subtypeSub);
  }

  ngOnDestroy() {
    this.options = [];
    this.subscription.unsubscribe();
  }

  inputChange(newVal: string) {
    this.typedTerm = newVal;
    if (this._type === TreeType[TreeType.VISIT] || this._type === TreeType[TreeType.PM]) {
      if (newVal.length > 2) {
        this.actions.setCriteriaSearchTerms([newVal]);
      } else {
        this.actions.setCriteriaSearchTerms([]);
      }
    } else {
      this.optionSelected = false;
      this.ingredientList = [];
      this.numMatches = 0;
      this.noResults = false;
      if (newVal.length >= 4) {
        const subtype = this.codes ? this.subtype : null;
        this.actions.fetchAutocompleteOptions(this._type, subtype, newVal);
      } else {
        this.actions.setCriteriaSearchTerms([]);
        this.options = [];
      }
    }
  }

  selectOption(option: any) {
    console.log(option);
    this.optionSelected = true;
    this.searchTerm = option.name;
    if (option.subtype === TreeSubType[TreeSubType.BRAND]) {
      this.actions.fetchIngredientsForBrand(option.conceptId);
    } else {
      this.actions.setCriteriaSearchTerms([option.name]);
      const ids = [option.id];
      let path = option.path.split('.');
      if (this.multiples[option.name]) {
        this.multiples[option.name].forEach(multiple => {
          ids.push(multiple.id);
          path = path.concat(multiple.path.split('.'));
        });
      }
      this.actions.loadCriteriaSubtree(this._type, option.subtype, ids, path);
    }
    this.actions.clearAutocompleteOptions();
  }

  hideDropdown() {
    this.options = [];
  }

  moveUp() {
    if (this.highlightedOption === 0) {
      this.highlightedOption = null;
      this.searchTerm = this.typedTerm;
    } else if (this.highlightedOption > 0) {
      this.highlightedOption--;
      this.searchTerm = this.options[this.highlightedOption].name;
    }
  }

  moveDown() {
    if (this.highlightedOption === null) {
      this.highlightedOption = 0;
      this.searchTerm = this.options[this.highlightedOption].name;
    } else if ((this.highlightedOption + 1) < this.options.length) {
      this.highlightedOption++;
      this.searchTerm = this.options[this.highlightedOption].name;
    }
  }

  enterSelect() {
    this.selectOption(this.options[this.highlightedOption]);
  }
}
