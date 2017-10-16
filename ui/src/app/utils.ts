import {Observable} from 'rxjs/Observable';

// Don't retry API calls when the status code is 500 or 400.
export function retryApi(observable: Observable<any>,
    toRun: number): Observable<any> {
  let numberRuns = 0;
  return observable.retryWhen((errors) => {
    return errors.do((e) => {
      numberRuns++;
      if (numberRuns === toRun) {
        throw e;
      }
      if (e.status === 500) {
        window['handleFiveHundred']();
      }
      if (e.status !== 503) {
        throw e;
      }
    });
  });
}
