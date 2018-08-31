import {NgRedux, select} from '@angular-redux/store';
import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';
import {CRITERIA_SUBTYPES, CRITERIA_TYPES} from '../constant';

import {activeParameterList, CohortSearchActions, CohortSearchState, demoCriteriaChildren} from '../redux';

import {Attribute, CohortBuilderService, Operator} from 'generated';

const minAge = 18;
const maxAge = 120;

/*
 * Sorts a plain JS array of plain JS objects first by a 'count' key and then
 * by a 'name' key
 */
function sortByCountThenName(critA, critB) {
  const A = critA.count || 0;
  const B = critB.count || 0;
  const diff = B - A;
  return diff === 0
    ? (critA.name > critB.name ? 1 : -1)
    : diff;
}

@Component({
  selector: 'crit-demographics',
  templateUrl: './demographics.component.html',
  // Buttons styles picked up from parent (wizard.ts)
  styleUrls: [
    './demographics.component.css',
    '../../styles/buttons.css',
  ]
})
export class DemographicsComponent implements OnInit, OnDestroy {
  @Output() cancel = new EventEmitter<boolean>();
  @Output() finish = new EventEmitter<boolean>();
  @select(activeParameterList) selection$;
  readonly minAge = minAge;
  readonly maxAge = maxAge;
  loading = false;
  subscription = new Subscription();
  hasSelection = false;

  /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([this.minAge, this.maxAge]),
    deceased: new FormControl(),
  });
  get ageRange() { return this.demoForm.get('ageRange'); }
  get deceased() { return this.demoForm.get('deceased'); }

  /* Storage for the demographics options (fetched via the API) */
  ageNode;
  deceasedNode;

  genderNodes = List();
  initialGenders = List();

  raceNodes = List();
  initialRaces = List();

  ethnicityNodes = List();
  initialEthnicities = List();

  constructor(
    private route: ActivatedRoute,
    private api: CohortBuilderService,
    private actions: CohortSearchActions,
    private ngRedux: NgRedux<CohortSearchState>
  ) {}

  ngOnInit() {
    // Set back to false at the end of loadNodesFromApi (i.e. the end of the
    // initialization routine)
    this.loading = true;

    this.subscription = this.selection$.subscribe(sel => this.hasSelection = sel.size > 0);
    this.initAgeControls();

    this.selection$.first().subscribe(selections => {
      /*
       * Each subtype of DEMO requires subtly different initialization, which
       * is handled by special-case methods which each receive any selected
       * criteria already in the state (i.e. if we're editing a search group
       * item).  Finally we load the relevant criteria from the API.
       */
      this.initialGenders = selections.filter(s => s.get('subtype') === CRITERIA_SUBTYPES.GEN);
      this.initialRaces = selections.filter(s => s.get('subtype') === CRITERIA_SUBTYPES.RACE);
      this.initialEthnicities = selections.filter(s => s.get('subtype') === CRITERIA_SUBTYPES.ETH);
      this.initDeceased(selections);
      this.initAgeRange(selections);
      this.loadNodesFromApi();
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi() {
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;
    /*
     * Each subtype's possible criteria is loaded via the API.  Race and Gender
     * criteria nodes become options in their respective dropdowns; deceased
     * and age are used as templates for constructing relevant seach
     * parameters.  Upon load we immediately map the criteria to immutable
     * objects complete with deterministically generated `parameterId`s and
     * sort them by count, then by name.
     */
    const calls = [
      CRITERIA_SUBTYPES.AGE,
      CRITERIA_SUBTYPES.DEC,
      CRITERIA_SUBTYPES.GEN,
      CRITERIA_SUBTYPES.RACE,
      CRITERIA_SUBTYPES.ETH
    ].map(code => {
      this.subscription.add(this.ngRedux.select(demoCriteriaChildren(CRITERIA_TYPES.DEMO, code))
        .subscribe(options => {
          if (options.size) {
            this.loadOptions(options, code);
          } else {
            this.api.getCriteriaByTypeAndSubtype(cdrid, CRITERIA_TYPES.DEMO, code)
              .subscribe(response => {
                const items = response.items
                  .filter(item => item.parentId !== 0 || code === CRITERIA_SUBTYPES.DEC);
                items.sort(sortByCountThenName);
                const nodes = fromJS(items).map(node => {
                  if (node.get('subtype') !== CRITERIA_SUBTYPES.AGE) {
                    const paramId = `param${node.get('conceptId', node.get('code'))}`;
                    node = node.set('parameterId', paramId);
                  }
                  return node;
                });
                this.actions.loadDemoCriteriaRequestResults(CRITERIA_TYPES.DEMO, code, nodes);
              });
          }
        })
      );
    });
  }

  loadOptions(nodes: any, subtype: string) {
    switch (subtype) {
      /* Age and Deceased are single nodes we use as templates */
      case CRITERIA_SUBTYPES.AGE:
        this.ageNode = nodes.get(0);
        break;
      case CRITERIA_SUBTYPES.DEC:
        this.deceasedNode = nodes.get(0);
        break;
      /* Gender, Race, and Ethnicity are all used to generate option lists */
      case CRITERIA_SUBTYPES.GEN:
        this.genderNodes = nodes;
        break;
      case CRITERIA_SUBTYPES.RACE:
        this.raceNodes = nodes;
        break;
      case CRITERIA_SUBTYPES.ETH:
        this.ethnicityNodes = nodes;
        break;
    }
    this.loading = false;
  }

  /*
    * We want the two inputs to mirror the slider, so here we're wiring all
    * three inputs together using the valueChanges Observable and the
    * emitEvent option.  Setting emitEvent to false will prevent the other
    * Observables from firing when a control is updated this way, hence
    * preventing any infinite update cycles.
    */
  initAgeControls() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    this.subscription.add(this.ageRange.valueChanges.subscribe(([lo, hi]) => {
      min.setValue(lo, {emitEvent: false});
      max.setValue(hi, {emitEvent: false});
    }));
    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
      }
    }));
  }

  checkMax() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    if (max.value < min.value) {
      max.setValue(min.value);
    }
  }

  checkMin() {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    if (min.value > max.value) {
      min.setValue(max.value);
    } else if (min.value < this.minAge) {
      min.setValue(this.minAge);
    }
  }

  /*
   * The next four initialization methods do the following: if a value exists
   * for that subtype already (i.e. we're editing), set that value on the
   * relevant form control.  Also set up a subscriber to the observable stream
   * coming from that control's `valueChanges` that will fire ADD_PARAMETER or
   * REMOVE_PARAMETER events as appropriate.
   *
   * The exact ordering of these operations is slightly different per type.
   * For race and gender, we watch the valueChanges stream in pairs so that we
   * can generate added and removed lists, so they get their initialization
   * _after_ the change listener is attached (otherwise we would never detect
   * the _first_ selection, which would be dropped by `pairwise`).
   *
   * For Age, since the slider emits an event with every value, and there can
   * be many values very quickly, we debounce the event emissions by 1/4 of a
   * second.  Furthermore, we generate the correct parameter ID as a hash from
   * the given user input so that we can determine if the age range has changed
   * or not.
   *
   * (TODO: can we reduce all age criterion to 'between'?  Or should we be
   * determining different attributes (operators, really) be examining the
   * bounds and the diff between low and high?  And should we be generating the
   * parameterId by stringifying the attribute (which may be more stable than
   * using a hash?)
   */
  initAgeRange(selections) {
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');

    const existent = selections.find(s => s.get('subtype') === CRITERIA_SUBTYPES.AGE);
    if (existent) {
      const range = existent.getIn(['attributes', '0', 'operands']).toArray();
      this.ageRange.setValue(range);
      min.setValue(range[0]);
      max.setValue(range[1]);
    }

    const selectedAge = this.selection$
      .map(selectedNodes => selectedNodes
        .find(node => node.get('subtype') === CRITERIA_SUBTYPES.AGE)
      );

    const ageDiff = this.ageRange.valueChanges
      .debounceTime(250)
      .distinctUntilChanged()
      .map(([lo, hi]) => {
        const attr = fromJS(<Attribute>{
          name: 'Age',
          operator: Operator.BETWEEN,
          operands: [lo, hi],
          conceptId: this.ageNode.get('conceptId', null)
        });
        const paramId = `age-param${attr.hashCode()}`;
        return this.ageNode
          .set('parameterId', paramId)
          .set('attributes', [attr]);
      })
      .withLatestFrom(selectedAge)
      .filter(([newNode, oldNode]) => {
        if (oldNode) {
          return oldNode.get('parameterId') !== newNode.get('parameterId');
        }
        return true;
      });
    this.subscription.add(ageDiff.subscribe(([newNode, oldNode]) => {
      if (oldNode) {
        this.actions.removeParameter(oldNode.get('parameterId'));
      }
      this.actions.addParameter(newNode);
    }));
  }

  initDeceased(selections) {
    const existent = selections.find(s => s.get('subtype') === CRITERIA_SUBTYPES.DEC);
    if (existent !== undefined) {
      this.deceased.setValue(true);
    }
    this.subscription.add(this.deceased.valueChanges.subscribe(includeDeceased => {
      if (!this.deceasedNode) {
        console.warn('No node from which to make parameter for deceased status');
        return ;
      }
      includeDeceased
        ? this.actions.addParameter(this.deceasedNode)
        : this.actions.removeParameter(this.deceasedNode.get('parameterId'));
    }));
  }
}
