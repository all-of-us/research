import {select} from '@angular-redux/store';
import {AfterContentChecked, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup, NgForm, Validators} from '@angular/forms';

import {Operator, TreeSubType, TreeType} from 'generated';
import {fromJS, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {PM_UNITS} from '../constant';
import {
attributesPreviewStatus,
CohortSearchActions,
isAttributeLoading,
nodeAttributes,
previewError,
} from '../redux';
import {stripHtml} from '../utils';
import {numberAndNegativeValidator, rangeValidator} from '../validators';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnDestroy, OnInit {
  @select(attributesPreviewStatus) preview$;
  @select(isAttributeLoading) loading$;
  @select(nodeAttributes) node$;
  @select(previewError) error$;
  node: Map<any, any>;
  units = PM_UNITS;
  attrs = {EXISTS: false, NUM: [], CAT: []};
  attributes: any;
  dropdowns = {
      selected: ['', ''],
      oldVals: ['', ''],
      labels: ['', '']
  };
  preview = Map();
  subscription: Subscription;
  loading: boolean;
  selectedCode: any;
  sysOption: any;
  diaOption: any;
  resetDisable = false;
  options = [
    {value: 'EQUAL', name: 'Equals', display: '= ', code: '01'},
    {value: 'GREATER_THAN_OR_EQUAL_TO', name: 'Greater than or Equal to', display: '>= ', code: '02'},
    {value: 'LESS_THAN_OR_EQUAL_TO', name: 'Less than or Equal to', display: '<= ', code: '03'},
    {value: 'BETWEEN', name: 'Between', display: '', code: '04'},
  ];

  form = new FormGroup({
    EXISTS: new FormControl(false),
    NUM: new FormGroup({}),
    CAT: new FormGroup({})
  });

  constructor(private actions: CohortSearchActions, private cdref: ChangeDetectorRef) {}

  ngOnInit() {
    this.subscription = this.preview$.subscribe(prev => this.preview = prev);
    this.subscription.add(this.loading$.subscribe(loading => this.loading = loading));
    this.subscription.add(this.node$.subscribe(node => {
      this.node = node;
      const numControls = new FormGroup({
        operator: new FormControl(),
        valueA: new FormControl(),
        valueB: new FormControl(),
      })
      if (this.isMeasurement) {
        this.node.get('attributes').forEach(attr => {
          switch (attr.type) {
            case 'NUM':
              const NUM = <FormGroup>this.form.controls.NUM;
              if (!this.attrs.NUM.length) {
                NUM.addControl('num0', numControls);
                this.dropdowns.labels[0] = 'Numeric Values';
                this.attrs.NUM.push({
                  name: 'NUM',
                  operator: null,
                  operands: [null],
                  conceptId: attr.conceptId,
                  [attr.conceptName]: attr.estCount
                });
              } else {
                this.attrs.NUM[0][attr.conceptName] = attr.estCount;
              }
              break;
            case 'CAT':
              if (parseInt(attr.estCount, 10) > 0) {
                attr['checked'] = false;
                this.attrs.CAT.push(attr);
              }
          }
        });
      } else {
        this.options.unshift({value: 'ANY', name: 'Any', display: 'Any', code: 'Any'});
        this.attrs.NUM = this.node.get('attributes');
        if (this.attrs.NUM) {
          const NUM = <FormGroup>this.form.controls.NUM;
          NUM.addControl('num0', numControls);
          this.selectedCode = 'Any';
          this.attrs.NUM.forEach((attr, i) => {
            attr.operator = 'ANY';
            this.dropdowns.selected[i] = 'ANY';
            this.dropdowns.oldVals[i] = 'ANY';
            if (this.node.get('subtype') === TreeSubType[TreeSubType.BP]) {
              NUM.addControl('num1', numControls);
              this.dropdowns.labels[i] = attr.name;
            }
          });
          this.preview = this.preview.set('count', this.node.get('count'));
        }
      }
    }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  radioChange() {
    this.form.controls.NUM.reset();
    this.resetDisable = true;
    this.selectedCode = 'Any';
    this.preview = this.preview.set('count', this.node.get('count'));
  }

  selectChange(index: number, option: any) {
    this.resetDisable = true;
    this.attrs.NUM[index].operator = option.value;
    this.dropdowns.selected[index] = option.name;
    if (this.node.get('subtype') === 'BP' && this.dropdowns.oldVals[index] !== option.value) {
      const other = index === 0 ? 1 : 0;
      if (other === 0) {
        if (this.diaOption === undefined) {
          this.diaOption = option.code;
          this.sysOption = option.code;
        } else {
          this.sysOption = option.code;
        }
      } else if (other === 1) {
        if (this.sysOption === undefined) {
          this.sysOption = option.code;
          this.diaOption = option.code;
        } else {
          this.diaOption = option.code;
        }
      }
      if (this.sysOption && this.diaOption) {
        this.selectedCode = (this.sysOption + this.diaOption);
      }
      if (option.value === 'ANY') {
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = 'ANY';
        this.dropdowns.selected[other] = 'Any';
      } else if (this.dropdowns.oldVals[index] === 'ANY') {
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = option.value;
        this.dropdowns.selected[other] = option.name;
      }
      this.dropdowns.oldVals[index] = option.value;
    } else {
      if (option.value !== 'BETWEEN') {
        this.form.controls.NUM.get(['num' + index, 'valueB']).reset();
      }
      this.selectedCode = option.code;
    }
    this.setValidation(index, option.name);
    this.preview = option.value === 'ANY'
      ? this.preview.set('count', this.node.get('count')) : Map();
  }

  setValidation(index: number, option: string) {
    if (option === 'Any') {
      this.form.controls.NUM.get(['num' + index, 'valueA']).clearValidators();
      this.form.controls.NUM.get(['num' + index, 'valueB']).clearValidators();
      this.form.controls.NUM.reset();
    } else {
      const validators = [Validators.required];
      if (this.isMeasurement) {
        const min = parseFloat(this.attrs.NUM[0].MIN);
        const max = parseFloat(this.attrs.NUM[0].MAX);
        validators.push(rangeValidator('Values', min, max));
      } else {
        validators.push(numberAndNegativeValidator('Form'));
      }
      this.form.controls.NUM.get(['num' + index, 'valueA']).setValidators(validators);
      if (option === 'Between') {
        this.form.controls.NUM.get(['num' + index, 'valueB']).setValidators(validators);
      } else {
        this.form.controls.NUM.get(['num' + index, 'valueB']).clearValidators();
        this.form.controls.NUM.get(['num' + index, 'valueB']).reset();
      }
      this.cdref.detectChanges();
    }
  }

  inputChange() {
    this.preview = Map();
  }

  get isValid() {
    if (this.isPM || !this.form.valid) {
      return this.form.valid;
    }
    let valid = false;
    this.attrs.NUM.forEach(num => {
      if (num.operator) {
        valid = true;
      }
    });
    this.attrs.CAT.forEach(cat => {
      if (cat.checked) {
        valid = true;
      }
    });
    return valid;
  }

  refresh() {
    this.preview = Map();
    this.form.reset();
    this.attrs.EXISTS = false;
    this.resetDisable = false;
    this.attrs.NUM.forEach(num => {
      num.operator = null;
      num.operands = [null];
    });
    this.attrs.CAT.forEach(cat => {
      cat.checked = false;
    });
  }

  get paramId() {
    return `param${this.node.get('conceptId')
        ? (this.node.get('conceptId') + (this.selectedCode))
        : (this.node.get('id') + (this.selectedCode))}`;
  }

  get displayName() {
    return stripHtml(this.node.get('name'));
  }

  getParamWithAttributes() {
    let name = this.node.get('name', '') + ' (';
    let attrs = [];
    if (this.attrs.EXISTS) {
      name += 'Any)';
      attrs.push({
        name: 'ANY',
        operator: null,
        operands: [null],
        conceptId: this.node.get('conceptId')
      });
    } else {
      this.attrs.NUM.forEach((attr, i) => {
        const paramAttr = {
          name: attr.name,
          operator: attr.operator,
          operands: attr.operands,
          conceptId: attr.conceptId
        };
        if (this.form.value.NUM['num' + i].operator === 'ANY') {
          paramAttr.name = 'ANY';
          name += 'Any';
        } else {
          if (i > 0) {
            name += ' / ';
          }
          if (this.node.get('subtype') === TreeSubType[TreeSubType.BP]) {
            name += attr.name + ' ';
          }
          name += this.options.find(option => option.value === attr.operator).display
            + attr.operands.join('-');
        }
        attrs.push(paramAttr);
      });

      const catOperands = this.attrs.CAT.reduce((checked, current) => {
        if (current.checked) {
          checked.push(current.valueAsConceptId);
        }
        return checked;
      }, []);
      if (catOperands.length) {
        attrs.push({name: 'CAT', operator: Operator.IN, operands: catOperands});
      }
      if (this.attrs.NUM.length && catOperands.length) {
        attrs = attrs.map(attr => {
          attr.name = 'BOTH';
          return attr;
        });
      }
      name += (this.isPM && attrs[0].name !== 'ANY'
        ? this.units[this.node.get('subtype')]
        : '') + ')';
    }
    return this.node
      .set('parameterId', this.paramId)
      .set('name', name)
      .set('attributes', fromJS(attrs));
  }

  requestPreview() {
    const param = this.getParamWithAttributes();
    this.actions.requestAttributePreview(param);
  }

  addAttrs() {
    const param = this.getParamWithAttributes();
    this.actions.addParameter(param);
    this.actions.hideAttributesPage();
  }

  cancel() {
    this.actions.hideAttributesPage();
  }

  showInput(index: number) {
    return this.attrs.NUM[index].operator
      && this.form.value.NUM['num' + index].operator !== 'ANY';
  }

  isBetween(index: number) {
    return this.form.value.NUM['num' + index].operator === Operator.BETWEEN;
  }

  get hasUnits() {
    return typeof PM_UNITS[this.node.get('subtype')] !== 'undefined';
  }

  get isMeasurement() {
    return this.node.get('type') === TreeType[TreeType.MEAS];
  }

  get isPM() {
    return this.node.get('type') === TreeType[TreeType.PM];
  }

  get showCalc() {
    let notAny = true;
    if (this.isPM) {
      notAny = this.attrs.NUM[0].operator !== 'ANY';
    }
    return !this.attrs.EXISTS && notAny;
  }

  get showAdd() {
    let any = false;
    if (this.isPM) {
      any = this.attrs.NUM[0].operator === 'ANY';
    }
    return (this.preview.get('count') && !this.preview.get('requesting')) || any;
  }
}
