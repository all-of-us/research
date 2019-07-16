import {Component, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {FormControl} from '@angular/forms';
import {autocompleteStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

const trigger = 3;

@Component({
  selector: 'app-list-search-bar',
  templateUrl: './list-search-bar.component.html',
  styleUrls: ['./list-search-bar.component.css']
})
export class ListSearchBarComponent implements OnInit, OnDestroy {
  @Input() node: any;
  @Output() ingredients = new EventEmitter<any>();
  searchTerm: FormControl = new FormControl();
  typedTerm: string;
  options = [];
  loading = false;
  noResults = false;
  optionSelected = false;
  error = false;
  subscription: Subscription;
  highlightedOption: number;
  subtype: string;

  @ViewChild('searchBar') searchBar;
  @HostListener('document:mouseup', ['$event.target'])
  onClick(targetElement) {
    const clickedInside = this.searchBar.nativeElement.contains(targetElement);
    if (!clickedInside) {
      this.hideDropdown();
    }
  }

  ngOnInit() {
    this.subscription = autocompleteStore.subscribe(searchTerm => {
      this.searchTerm.setValue(searchTerm, {emitEvent: false});
    });
    this.subscription.add(this.searchTerm.valueChanges
      .debounceTime(300)
      .distinctUntilChanged()
      .subscribe( value => {
        if (this.node.domainId === DomainType.PHYSICALMEASUREMENT) {
          autocompleteStore.next(this.searchTerm.value);
        } else {
          if (value.length >= trigger) {
            this.inputChange();
          } else {
            this.options = [];
            this.noResults = false;
          }
        }
      })
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  inputChange() {
    this.typedTerm = this.searchTerm.value;
    this.loading = true;
    this.optionSelected = false;
    this.noResults = false;
    const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const {domainId, isStandard, type} = this.node;
    const apiCall = domainId === DomainType.DRUG
      ? cohortBuilderApi().getDrugBrandOrIngredientByValue(cdrId, this.searchTerm.value)
      : cohortBuilderApi().getCriteriaAutoComplete(
        cdrId, domainId, this.searchTerm.value, type, isStandard
      );
    apiCall.then(resp => {
      this.options = [];
      this.noResults = resp.items.length === 0;
      const optionNames: Array<string> = [];
      this.highlightedOption = null;
      resp.items.forEach(option => {
        if (optionNames.indexOf(option.name) === -1) {
          optionNames.push(option.name);
          this.options.push(option);
        }
      });
      this.loading = false;
    }, (err) => this.error = err);
  }

  get showOverflow() {
    return this.options && this.options.length <= 10;
  }

  selectOption(option: any) {
    if (option) {
      this.optionSelected = true;
      this.searchTerm.reset('');
      this.searchTerm.setValue(option.name, {emitEvent: false});
      if (option.type === CriteriaType[CriteriaType.BRAND]) {
        const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
        cohortBuilderApi().getDrugIngredientByConceptId(cdrId, option.conceptId)
          .then(resp => {
            if (resp.items.length) {
              const ingredients = resp.items.map(it => it.name);
              this.ingredients.emit(ingredients);
              // just grabbing the first one on the list for now
              const {path, id} = resp.items[0];
              subtreePathStore.next(path.split('.'));
              subtreeSelectedStore.next(id);
            }
          });
      } else {
        this.ingredients.emit(null);
        autocompleteStore.next(option.name);
        subtreePathStore.next(option.path.split('.'));
        subtreeSelectedStore.next(option.id);
      }
    }
  }

  hideDropdown() {
    this.options = [];
  }

  moveUp() {
    if (this.highlightedOption === 0) {
      this.highlightedOption = null;
      this.searchTerm.setValue(this.typedTerm, {emitEvent: false});
    } else if (this.highlightedOption > 0) {
      this.highlightedOption--;
      this.searchTerm.setValue(this.options[this.highlightedOption].name, {emitEvent: false});
    }
  }

  moveDown() {
    if (this.highlightedOption === null) {
      this.highlightedOption = 0;
      this.searchTerm.setValue(this.options[this.highlightedOption].name, {emitEvent: false});
    } else if ((this.highlightedOption + 1) < this.options.length) {
      this.highlightedOption++;
      this.searchTerm.setValue(this.options[this.highlightedOption].name, {emitEvent: false});
    }
  }

  enterSelect() {
    this.selectOption(this.options[this.highlightedOption]);
  }
}
