import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {
  BolderHeader,
  Header,
  SmallHeader
} from 'app/components/headers';
import {
  AccountCreationResendModal,
  AccountCreationUpdateModal
} from 'app/views/account-creation-modals/component';

import {Profile} from 'generated/fetch/api';

const styles = {
  buttonLinkStyling: {
    border: 'none',
    cursor: 'pointer',
    outlineColor: 'transparent',
    color: '#2691D0',
    backgroundColor: 'transparent',
  }
};

interface AccountCreationSuccessProps {
  profile: Profile;
}

interface AccountCreationSuccessState {
  resendModal: boolean;
  updateModal: boolean;
  contactEmail: string;
}

export class AccountCreationSuccess
    extends React.Component<AccountCreationSuccessProps, AccountCreationSuccessState> {

  constructor(props: AccountCreationSuccessProps) {
    super(props);
    this.state = {
      contactEmail: this.props.profile.contactEmail,
      resendModal: false,
      updateModal: false,
    };
  }

  render() {
    return <React.Fragment>
      <div style={{padding: '3rem 3rem 0 3rem', marginLeft: '-0.5rem', marginRight: '-0.5'}}>
        <BolderHeader>
          CONGRATULATIONS!
        </BolderHeader>
        <div>
          <SmallHeader>
            Your All of Us research account has been created!
          </SmallHeader>
        </div>
        <div>
          <Header style={{fontWeight: 400}}>
            Your new account
          </Header>
        </div>
        <div style={{whiteSpace: 'nowrap'}}>
          <Header style={{fontWeight: 400, marginTop: '0.5rem'}}>
            {this.props.profile.username}
          </Header>
        </div>
        <div>
          <Header style={{marginTop: '.5rem', fontWeight: 400}}>
            is hosted by Google.
          </Header>
        </div>
        <div>
          <SmallHeader>
            Check your contact email for instructions on getting started.
          </SmallHeader>
        </div>
        <div>
          <SmallHeader>
            Your contact email is: {this.state.contactEmail}
          </SmallHeader>
        </div>
        <div style={{paddingTop: '0.5rem'}}>
          <button style={styles.buttonLinkStyling}
                  onClick={() => this.setState({resendModal: true})}>
            Resend Instructions
          </button>
          |
          <button style={styles.buttonLinkStyling}
                  onClick={() => this.setState({updateModal: true})}>
            Change contact email
          </button>
        </div>
      </div>
      {this.state.resendModal && <AccountCreationResendModal
        username={this.props.profile.username}
        creationNonce={this.props.profile.creationNonce}
        onClose={() => this.setState({resendModal: false})}
      />}
      {this.state.updateModal && <AccountCreationUpdateModal
        username={this.props.profile.username}
        creationNonce={this.props.profile.creationNonce}
        onDone={(newEmail: string) => {
          this.setState({contactEmail: newEmail, updateModal: false});
        }}
        onClose={() => this.setState({updateModal: false})}
      />}
    </React.Fragment>;
  }
}

