import {AuthDomainApi, UpdateUserDisabledRequest} from 'generated/fetch';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';


export class AuthDomainApiStub extends AuthDomainApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw StubImplementationRequired; });
  }

  public updateUserDisabledStatus(request?: UpdateUserDisabledRequest): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
