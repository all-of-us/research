import {AbstractControl, ValidatorFn} from '@angular/forms';
import * as moment from 'moment';

export function negativeValidator(name: string): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    const value = control.value;
    const isValid = control.pristine || parseFloat(value) >= 0;
    const message = {
      negative: {
        message: name + ' cannot accept negative values'
      }
    };
    return isValid ? null : message;
  };
}


export function rangeValidator(name: string, min: number, max: number): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    const value = control.value;
    const isValid = control.pristine || value >= min && value <= max;
    const message = {
      range: {
        message: name + ' must be between ' + min + ' and ' + max
      }
    };
    return isValid ? null : message;
  };
}

export function integerAndRangeValidator(name: string, min: number, max: number): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    const value = control.value;
    const isInt = Number.isInteger(parseFloat(value));
    const inRange = value >= min && value <= max;
    const message: any = {};
    if (!isInt) {
      message.integer = {
        message : name + ' must be a whole number'
      };
    } else if (!inRange) {
      message.range = {
        message: name + ' must be between ' + min + ' and ' + max
      };
    }
    const isValid = control.pristine || (isInt && inRange);
    return isValid ? null : message;
  };
}

export function dateValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: any } | null => {
    const value = control.value;
    const isValid = control.pristine || moment(value, 'YYYY-MM-DD', true).isValid();
    const message: any = {
      dateFormat: {
        message: 'Dates must be in the format \'YYYY-MM-DD\''
      }
    };
    return isValid ? null : message;
  };
}
