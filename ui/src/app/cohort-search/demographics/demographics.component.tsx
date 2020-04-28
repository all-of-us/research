import {Component, Input} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import Nouislider from 'nouislider-react';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {ageCountStore} from 'app/cohort-search/search-state.service';
import {mapParameter, typeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {AttrName, CriteriaType, DomainType, Operator} from 'generated/fetch';

const styles = reactStyles({
  ageContainer: {
    border: '1px solid #cccccc',
    borderRadius: '5px',
    margin: '0.5rem 1rem',
    maxHeight: '15rem',
    padding: '0.5rem 0 1.5rem 1rem'
  },
  ageInput: {
    border: `1px solid ${colors.black}`,
    borderRadius: '3px',
    fontSize: '0.5rem',
    fontWeight: 300,
    marginTop: '0.25rem',
    padding: '0 0.5rem',
    width: '1rem',
  },
  ageLabel: {
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary
  },
  calculateBtn: {
    background: colors.primary,
    border: 'none',
    borderRadius: '0.3rem',
    color: colors.white,
    cursor: 'pointer',
    fontSize: '12px',
    height: '1.5rem',
    letterSpacing: '0.02rem',
    lineHeight: '0.75rem',
    margin: '0.25rem 0.5rem 0.25rem 0',
    padding: '0rem 0.75rem',
    textTransform: 'uppercase',
  },
  count: {
    alignItems: 'center',
    background: colors.accent,
    borderRadius: '10px',
    color: colors.white,
    display: 'inline-flex',
    fontSize: '10px',
    height: '0.625rem',
    justifyContent: 'center',
    lineHeight: 'normal',
    margin: '0 0.25rem',
    minWidth: '0.675rem',
    padding: '0 4px',
    verticalAlign: 'middle'
  },
  countPreview: {
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
    padding: '0.5rem',
    margin: '0 2.5%',
    position: 'absolute',
    width: '95%',
    bottom: '0.5rem',
  },
  option: {
    color: colors.black,
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: 400,
    marginBottom: '0.5rem',
    padding: '0 0.25rem',
    textTransform: 'capitalize',
  },
  resultText: {
    color: colors.primary,
    fontWeight: 500,
  },
  selectIcon: {
    color: colors.select,
    marginRight: '0.25rem'
  },
  selected: {
    cursor: 'not-allowed',
    opacity: 0.4
  },
  selectList: {
    alignItems: 'center',
    display: 'flex',
    marginRight: '1rem',
    maxHeight: '15rem',
    padding: '0.5rem 0 0 1rem'
  },
  slider: {
    flex: 1,
    padding: '0 0.5rem',
    margin: '0 1rem',
  },
  sliderContainer: {
    alignItems: 'center',
    display: 'flex',
    marginRight: '1rem',
    paddingLeft: '1rem',
    width: '96%',
  }
});

const ageNode = {
  hasAncestorData: false,
  attributes: [],
  code: '',
  domainId: DomainType.PERSON,
  group: false,
  name: 'Age',
  parameterId: 'age-param',
  isStandard: true,
  type: CriteriaType.AGE,
  value: ''
};

const defaultMinAge = '18';
const defaultMaxAge = '120';

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
interface Props {
  select: Function;
  selectedIds: Array<string>;
  selections: Array<any>;
  wizard: any;
}

interface State {
  ageType: any;
  ageTypeNodes: any;
  calculating: boolean;
  count: number;
  loading: boolean;
  maxAge: string;
  minAge: string;
  nodes: Array<any>;
  sliderStart: Array<number>;
}

export class Demographics extends React.Component<Props, State> {
  readonly criteriaType = CriteriaType;
  subscription = new Subscription();
  selectedNode: any;
  enableCBAgeTypeOptions = serverConfigStore.getValue().enableCBAgeTypeOptions;
  ageTypes = [
    {label: 'Current Age', type: AttrName.AGE.toString()},
    {label: 'Age at Consent', type: AttrName.AGEATCONSENT.toString()},
    {label: 'Age at CDR Date', type: AttrName.AGEATCDR.toString()}
  ];

    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([]),
    ageType: new FormControl(AttrName.AGE.toString()),
  });
  get ageRange() { return this.demoForm.get('ageRange'); }

  constructor(props: Props) {
    super(props);
    this.state = {
      ageType: undefined,
      ageTypeNodes: undefined,
      calculating: false,
      count: null,
      loading: true,
      maxAge: defaultMaxAge,
      minAge: defaultMinAge,
      nodes: undefined,
      sliderStart: [+defaultMinAge, +defaultMaxAge],
    };
  }

  componentDidMount(): void {
    if (this.props.wizard.type === CriteriaType.AGE) {
      this.initAgeControls();
      this.initAgeRange();
      if (this.enableCBAgeTypeOptions) {
        this.loadAgeNodesFromApi();
      } else {
        this.setState({loading: false});
      }
    } else {
      this.loadNodesFromApi();
    }
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const {selections, wizard} = this.props;
    if (selections !== prevProps.selections && wizard.type !== CriteriaType.AGE) {
      this.calculate();
    }
  }

  componentWillUnmount(): void {
    this.subscription.unsubscribe();
  }

  loadNodesFromApi() {
    const {selections, wizard} = this.props;
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.setState({loading: true});
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, DomainType.PERSON.toString(), wizard.type).then(response => {
      const nodes = response.items
        .filter(item => item.parentId !== 0)
        .sort(sortByCountThenName)
        .map(node => ({...node, parameterId: `param${node.conceptId || node.code}`}));
      if (selections.length) {
        this.calculate(true);
      }
      this.setState({loading: false, nodes});
    });
  }

  loadAgeNodesFromApi() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const initialValue = {[AttrName.AGE.toString()]: [], 'AGE_AT_CONSENT': [], 'AGE_AT_CDR': []};
    cohortBuilderApi().findAgeTypeCounts(+cdrVersionId).then(response => {
      const ageTypeNodes = response.items.reduce((acc, item) => {
        acc[item.ageType].push(item);
        return acc;
      }, initialValue);
      setTimeout(() => this.centerAgeCount());
      this.setState({loading: false, ageTypeNodes});
    });
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
      if (this.enableCBAgeTypeOptions) {
        if (!!this.state.ageTypeNodes) {
          this.centerAgeCount();
        }
      } else {
        this.setState({count: null});
      }
    }));

    this.subscription.add(min.valueChanges.subscribe(value => {
      const [_, hi] = [...this.ageRange.value];
      if (value <= hi && value >= this.state.minAge) {
        this.ageRange.setValue([value, hi], {emitEvent: false});
        if (!this.enableCBAgeTypeOptions) {
          this.setState({count: null});
        }
      }
    }));
    this.subscription.add(max.valueChanges.subscribe(value => {
      const [lo, _] = [...this.ageRange.value];
      if (value >= lo) {
        this.ageRange.setValue([lo, value], {emitEvent: false});
        if (!this.enableCBAgeTypeOptions) {
          this.setState({count: null});
        }
      }
    }));
    if (this.enableCBAgeTypeOptions) {
      this.subscription.add(this.demoForm.get('ageType').valueChanges.subscribe(name => {
        this.calculateAge();
        this.props.select(this.selectedNode);
      }));
    }
  }

  onMinChange(min: string) {
    const {maxAge} = this.state;
    this.setState({minAge: min, sliderStart: [+min, +maxAge]}, () => this.updateAgeSelection());
  }

  onMaxChange(max: string) {
    const {minAge} = this.state;
    this.setState({maxAge: max, sliderStart: [+minAge, +max]}, () => this.updateAgeSelection());
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
    } else if (min.value < this.state.minAge) {
      min.setValue(this.state.minAge);
    }
  }

  updateAgeSelection() {
    const {maxAge, minAge} = this.state;
    const selectedNode = {
      ...ageNode,
      name: `Age In Range ${minAge} - ${maxAge}`,
      attributes: [{
        name: AttrName.AGE,
        operator: Operator.BETWEEN,
        operands: [minAge, maxAge]
      }],
    };
    this.props.select(selectedNode);
  }

  initAgeRange() {
    const {selections, wizard} = this.props;
    if (selections.length) {
      const {attributes} = selections[0];
      const {operands} = attributes[0];
      this.setState({count: wizard.count, minAge: operands[0], maxAge: operands[1], sliderStart: [+operands[0], +operands[1]]});
      if (this.enableCBAgeTypeOptions) {
        this.setState({ageType: attributes[0].name});
      }
    } else {
      this.updateAgeSelection();
    }
    if (!this.enableCBAgeTypeOptions) {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      if (!ageCountStore.getValue()[cdrVersionId]) {
        // Get total age count for this cdr version if it doesn't exist in the store yet
        this.calculateAge(true);
      } else if (this.setTotalAge) {
        this.setState({count:  ageCountStore.getValue()[cdrVersionId]});
      }
    }
  }

  centerAgeCount() {
    if (this.enableCBAgeTypeOptions) {
      this.calculateAge();
      const slider = document.getElementsByClassName('noUi-connect')[0] as HTMLElement;
      const wrapper = document.getElementById('count-wrapper');
      const count = document.getElementById('age-count');
      wrapper.setAttribute(
        'style', 'width: ' + slider.offsetWidth + 'px; left: ' + slider.offsetLeft + 'px;'
      );
      // set style properties also for cross-browser compatibility
      wrapper.style.width = slider.offsetWidth.toString();
      wrapper.style.left = slider.offsetLeft.toString();
      if (!!count && slider.offsetWidth < count.offsetWidth) {
        const margin = (slider.offsetWidth - count.offsetWidth) / 2;
        count.setAttribute('style', 'margin-left: ' + margin + 'px;');
        count.style.marginLeft = margin.toString();
      }
    }
  }

  selectOption = (opt: any) => {
    triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(opt.type)} - ${opt.name}`);
    this.props.select({...opt, name: `${typeToTitle(opt.type)} - ${opt.name}`});
  }

  calculate(init?: boolean) {
    let count = 0;
    this.props.selections.forEach(sp => {
      if (init) {
        const node = this.state.nodes.find(n => n.conceptId === sp.conceptId);
        if (node) {
          sp.count = node.count;
        }
      }
      count += sp.count;
    });
    this.setState({count});
  }

  calculateAge(init?: boolean) {
    const {maxAge, minAge} = this.state;
    if (this.enableCBAgeTypeOptions) {
      const ageType = this.demoForm.get('ageType').value;
      const min = this.demoForm.get('ageMin').value;
      const max = this.demoForm.get('ageMax').value;
      const count = this.state.ageTypeNodes[ageType]
        .filter(node => node.age >= min && node.age <= max)
        .reduce((acc, node) => acc + node.count, 0);
      this.setState({count});
    } else {
      if (!init || this.setTotalAge) {
        this.setState({calculating: true});
      }
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const parameter = init ? {
        ...ageNode,
        name: `Age In Range ${minAge} - ${maxAge}`,
        attributes: [{
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [minAge, maxAge]
        }],
      } : this.selectedNode;
      const request = {
        excludes: [],
        includes: [{
          items: [{
            type: DomainType.PERSON.toString(),
            searchParameters: [mapParameter(parameter)],
            modifiers: []
          }],
          temporal: false
        }],
        dataFilters: []
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(response => {
        if (init) {
          const ageCounts = ageCountStore.getValue();
          ageCounts[cdrVersionId] = response;
          ageCountStore.next(ageCounts);
          if (this.setTotalAge) {
            this.setState({count: response});
          }
        } else {
          this.setState({count: response});
        }
        this.setState({calculating: false});
      }, (err) => {
        console.error(err);
        this.setState({calculating: false});
      });
    }
  }

  get showPreview() {
    const {selections, wizard} = this.props;
    return !this.state.loading
      && (selections && selections.length > 0)
      && !(wizard.type === CriteriaType.AGE && this.enableCBAgeTypeOptions);
  }

  // Checks if form is in its initial state and if a count already exists before setting the total age count
  get setTotalAge() {
    const {maxAge, minAge} = this.state;
    const min = this.demoForm.get('ageMin');
    const max = this.demoForm.get('ageMax');
    return min.value === minAge && max.value === maxAge && !this.state.count;
  }

  render() {
    const {selectedIds, wizard} = this.props;
    const {calculating, count, loading, maxAge, minAge, nodes, sliderStart} = this.state;
    const isAge = wizard.type === CriteriaType.AGE;
    return loading
      ? <div style={{textAlign: 'center'}}><Spinner style={{marginTop: '3rem'}}/></div>
      : <React.Fragment>
        {isAge
          ? <div style={styles.ageContainer}>
            <div style={styles.ageLabel}>
              Age Range
            </div>
            <div style={styles.sliderContainer}>
              <input style={styles.ageInput}
                type='number'
                id='min-age'
                min={defaultMinAge} max={defaultMaxAge}
                value={minAge}
                onBlur={() => this.checkMin()}
                onChange={(e) => this.onMinChange(e.target.value)}/>
              <div style={styles.slider}>
                {this.enableCBAgeTypeOptions && <div id='count-wrapper'>
                  {calculating
                    ? <Spinner size={16}/>
                    : <span style={styles.count} id='age-count'>
                      {count.toLocaleString()}
                    </span>
                  }
                </div>}
                <Nouislider range={{min: +defaultMinAge, max: +defaultMaxAge}}
                  onChange={() => this.updateAgeSelection()}
                  onUpdate={(v) => this.setState({maxAge: v[1].split('.')[0], minAge: v[0].split('.')[0]})}
                  start={sliderStart}
                  step={1}
                  connect
                  onSlide={() => this.centerAgeCount()}
                  behaviour='drag'/>
              </div>
              <input style={styles.ageInput}
                type='number'
                id='max-age'
                min={defaultMinAge} max={defaultMaxAge}
                value={maxAge}
                onBlur={() => this.checkMax()}
                onChange={(e) => this.onMaxChange(e.target.value)}/>
            </div>
            {serverConfigStore.getValue().enableCBAgeTypeOptions && <div style={{marginLeft: '1rem'}}>
              {this.ageTypes.map((ageType, a) => <div key={a} className='radio-inline'>
                <input type='radio' id={`age_${a}`} value={ageType.type}/>
                <label>{ageType.label}</label>
              </div>)}
            </div>}
          </div>
          : <div style={styles.selectList}>
            <div style={{margin: '0.25rem 0', overflow: 'auto', width: '100%'}}>
              {nodes.map((opt, o) => <div key={o} style={styles.option} onClick={() => this.selectOption(opt)}>
                {selectedIds.includes(opt.parameterId)
                  ? <ClrIcon shape='check-circle' size='20' style={{...styles.selectIcon, ...styles.selected}}/>
                  : <ClrIcon shape='plus-circle'  size='20' style={styles.selectIcon}/>
                }
                {opt.name}
                {!!opt.count && <span style={styles.count}>
                  {opt.count.toLocaleString()}
                </span>}
              </div>)}
            </div>
          </div>
        }
        {this.showPreview && <div style={isAge
          ? {...styles.countPreview, minWidth: '50%', padding: '0.25rem 1rem', width: 'auto'}
          : styles.countPreview}>
          {isAge && <div style={{float: 'left', marginRight: '0.25rem'}}>
            <button style={styles.calculateBtn} disabled={calculating || count !== null} onClick={() => this.calculateAge()}>
              Calculate
            </button>
          </div>}
          {(count !== null || isAge) && <div style={{float: 'left', fontSize: '14px'}}>
            <div style={{color: colors.primary, fontWeight: 500}}>
              Results
            </div>
            <div>
              Number Participants:
              <b style={{color: colors.dark}}>
                &nbsp;{count !== null ? count.toLocaleString() : ' -- '}
              </b>
            </div>
          </div>}
        </div>}
      </React.Fragment>;
  }
}

@Component({
  selector: 'crit-demographics',
  template: '<div #root></div>'
})
export class DemographicsComponent extends ReactWrapperBase {
  @Input('select') select: Props['select'];
  @Input('selectedIds') selectedIds: Props['selectedIds'];
  @Input('selections') selections: Props['selections'];
  @Input('wizard') wizard: Props['wizard'];

  constructor() {
    super(Demographics, ['select', 'selectedIds', 'selections', 'wizard']);
  }
}
