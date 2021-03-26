import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';

import {ConfigResponse} from 'generated/fetch';

export class ServerConfigServiceStub {
  constructor(public config: ConfigResponse) {}

  public getConfig(): Observable<ConfigResponse> {
    return new BehaviorSubject<ConfigResponse>(this.config).asObservable();
  }
}
