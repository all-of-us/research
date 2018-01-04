import {DataAccessLevel} from 'generated';
import {Profile} from 'generated';
import {Observable} from 'rxjs/Observable';
import {InvitationVerificationRequest} from '../../generated/model/invitationVerificationRequest';

export class ProfileStubVariables {
  static PROFILE_STUB = {
    username: 'testers!@#$%^&*()><script>alert("hello");</script>',
    contactEmail: 'tester@mactesterson.edu🀓⚚><script>alert("hello");</script>',
    enabledInFireCloud: true,
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    dataAccessLevel: DataAccessLevel.Registered,
    fullName:  'Tester MacTesterson><script>alert("hello");</script>',
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999',
    invitationKey: 'dummyKey'
  };
}

export class ProfileServiceStub {
  public profile: Profile;

  constructor() {
    this.profile = ProfileStubVariables.PROFILE_STUB;
  }

  getMe(): Observable<Profile> {
    return new Observable<Profile>(observer => {
      setTimeout(() => {
        observer.next(this.profile);
        observer.complete();
      }, 0);
    });
  }

  public invitationKeyVerification(invitationVerificationRequest?: InvitationVerificationRequest)
      : Observable<{}> {
    if (invitationVerificationRequest.invitationKey === 'dummy') {
      const observable = new Observable(observer => {
          observer.next(this.profile);
      });
      return observable;
      }
    const observable = new Observable(observer => {
          observer.error(new Error(`Invalid invitation code`));
    });
    return observable;
   }
}
