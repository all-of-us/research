import {AuthDomainApi, UpdateUserDisabledRequest} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';


export class AuthDomainApiStub extends AuthDomainApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  public updateUserDisabledStatus(request?: UpdateUserDisabledRequest): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
