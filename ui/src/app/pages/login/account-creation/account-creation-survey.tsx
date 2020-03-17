import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {DemographicSurvey} from 'app/pages/profile/demographics-survey';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {convertAPIError, reportError} from 'app/utils/errors';
import {environment} from 'environments/environment';
import {ErrorResponse, Profile} from 'generated/fetch';


export interface AccountCreationSurveyProps {
  invitationKey: string;
  termsOfServiceVersion?: number;
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  captcha: boolean;
  createAccountErrorResponse?: ErrorResponse;
}

export class AccountCreationSurvey extends React.Component<AccountCreationSurveyProps, AccountCreationState> {
  constructor(props: any) {
    super(props);
    this.state = {
      captcha: !environment.enableCaptcha
    };
    this.createAccount = this.createAccount.bind(this);
  }

  async createAccount(profile, captchaToken) {
    const {invitationKey, termsOfServiceVersion, onComplete} = this.props;

    profileApi().createAccount({
      profile: profile,
      captchaVerificationToken: captchaToken,
      invitationKey: invitationKey,
      termsOfServiceVersion: termsOfServiceVersion
    }).then((newProfile) => {
      onComplete(newProfile);
      return newProfile;
    }).catch(async(error) => {
      reportError(error);
      const errorResponse = await convertAPIError(error);
      this.setState({createAccountErrorResponse: errorResponse});
    });
  }

  render() {
    return <React.Fragment>
      <DemographicSurvey
          profile={this.props.profile}
          onSubmit={(profile, captchaToken) => this.createAccount(profile, captchaToken)}
          onPreviousClick={(profile) => this.props.onPreviousClick(profile)}
          enableCaptcha={true}
          enablePrevious={true}
      />
      {this.state.createAccountErrorResponse && <Modal data-test-id='create-account-error'>
        <ModalTitle>Error creating account</ModalTitle>
        <ModalBody>
          <div>An error occurred while creating your account. The following message was returned:</div>
          <div style={{marginTop: '1rem', marginBottom: '1rem'}}>
            "{this.state.createAccountErrorResponse.message}"
          </div>
          <div>
            Please try again or contact <a href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
          </div>
        </ModalBody>
        <ModalFooter>
          <Button onClick = {() => this.setState({createAccountErrorResponse: null})}
                  type='primary'>Close</Button>
        </ModalFooter>
      </Modal>
      }
    </React.Fragment>;
  }
}
