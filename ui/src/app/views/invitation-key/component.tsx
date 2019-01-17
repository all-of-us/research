import {fullUrl} from '../../utils/fetch';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {BoldHeader} from 'app/components/headers';
import {FormInput} from 'app/components/inputs';

import {FetchArgs,
  InvitationVerificationRequest,
  ProfileApiFetchParamCreator
} from 'generated/fetch/api';

import * as React from 'react';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

export interface InvitationKeyProps {
  onInvitationKeyVerify: (invitationKey: any) => void;
}

interface InvitationKeyState {
  invitationKey: string;
  invitationKeyReq: boolean;
  invitationKeyInvalid: boolean;
}

export class InvitationKeyReact extends React.Component<InvitationKeyProps, InvitationKeyState> {

  inputElement: any;

  constructor(props: InvitationKeyProps) {
    super(props);
    this.state = {
      invitationKey: '',
      invitationKeyReq: false,
      invitationKeyInvalid: false
    };
    this.inputElement = React.createRef();
  }

  next() {
    this.setState({
      invitationKeyReq: false,
      invitationKeyInvalid: false
    });

    if (isBlank(this.state.invitationKey)) {
      this.setState({
        invitationKeyReq: true
      });
      this.inputElement.current.focus();
      return;
    }

    const request: InvitationVerificationRequest = {
      invitationKey: this.state.invitationKey
    };

    const args: FetchArgs = ProfileApiFetchParamCreator().invitationKeyVerification(request);
    fetch(fullUrl(args.url), args.options)
      .then(response => {
        if (response.status === 400 ) {
          this.setState({
            invitationKeyInvalid: true
          });
          this.inputElement.current.focus();
        } else {
          this.props.onInvitationKeyVerify(this.state.invitationKey);
          return;
        }})
      .catch(error => console.log(error));
  }

  render() {
    return <div data-test-id='invitation' style={{padding: '3rem 3rem 0 3rem'}}>
      <div style={{marginTop: '0', paddingTop: '.5rem'}}>
        <BoldHeader>
          Enter your Invitation Key:
        </BoldHeader>
        <FormInput type='text' id='invitationKey' value={this.state.invitationKey}
                   placeholder='Invitation Key' onChange={input =>
                                                 this.setState({invitationKey: input.target.value})}
                   inputref={this.inputElement} autoFocus/>
        {this.state.invitationKeyReq &&
         <AlertDanger>
           <div style={{fontWeight: 'bolder'}}>Invitation Key is required.</div>
         </AlertDanger>
        }
        {this.state.invitationKeyInvalid &&
         <AlertDanger>
           <div style={{fontWeight: 'bolder'}}>
             Invitation Key is not Valid.
           </div>
         </AlertDanger>
        }
        <div>
          <Button style={{width: '10rem', height: '2rem', margin: '.25rem .5rem .25rem 0'}}
                  onClick={() => this.next()}>
            Next
          </Button>
        </div>
      </div>
    </div>;
  }
}

export default InvitationKeyReact;
