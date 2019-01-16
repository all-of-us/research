import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Header, SmallHeader} from 'app/components/headers';

import {styles} from './style';

const googleIcon = '/assets/icons/google-icon.png';

export interface LoginProps {
  signIn: () => void;
  onCreateAccount: () => void;
}

export class LoginReactComponent extends React.Component<LoginProps, {}> {

  render() {
    return <div id='login' style={{marginTop: '6.5rem',  paddingLeft: '3rem'}}>
      <div>
        <Header>
          Already have an account?
        </Header>
        <div>
          <Button type='primary' style={styles.button} onClick={() => this.props.signIn()}>
            <img src={googleIcon}
                   style={{ height: '54px', width: '54px', margin: '-3px 19px -3px -3px'}}/>
            <div>
              Sign In with Google
            </div>
          </Button>
        </div>
      </div>
      <div style={{paddingTop: '1.25rem'}}>
        <SmallHeader>
          Don't have an account?
        </SmallHeader>
        <Button type='secondary' style={{fontSize: '10px', margin: '.25rem .5rem .25rem 0'}}
                onClick={this.props.onCreateAccount}>
          Create Account
        </Button>
      </div>
    </div>;
  }
}
export default LoginReactComponent;
