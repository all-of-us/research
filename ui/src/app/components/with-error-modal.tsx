import {TextModal} from 'app/components/text-modal';
import * as fp from 'lodash/fp';
import * as React from 'react';

interface State {
  show: boolean;
  title: string;
  body: string;
}

export interface WithErrorModalProps {
  showErrorModal: (title: string, body: string) => void;
}

export const withErrorModal = () => {
  return (WrappedComponent) => {
    return class ErrorModalWrapper extends React.Component<any, State> {

      constructor(props) {
        super(props);

        this.state = {
          show: false,
          title: 'Title',
          body: 'Body'
        };
      }

      show(title: string, body: string) {
        this.setState({
          show: true,
          title: title,
          body: body
        });
      }

      render() {
        return <React.Fragment>
          {this.state.show && <TextModal role='alertdialog'
                                         title={this.state.title}
                                         body={this.state.body}
                                         closeFunction={() => this.setState({show: false})}/>
          }
          <WrappedComponent showErrorModal={this.show.bind(this)} {...this.props}/>
        </React.Fragment>;
      }
    };
  };
};

export interface WithProfileErrorModalProps {
  showProfileErrorModal?: (message: string) => void;
}

export const withProfileErrorWrapper = WrappedComponent => {
  const body = ({message}) => (<React.Fragment>
    <div>An error occurred while saving your profile. The following message was returned:</div>
    <div style={{marginTop: '1rem', marginBottom: '1rem'}}>
        "{message}"
    </div>
    <div>
        Please try again or contact <a
        href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
    </div>
  </React.Fragment>);

  const ProfileErrorWrapper = ({showErrorModal, ...props}) => {
    return <WrappedComponent showProfileErrorModal={(message) => showErrorModal('Error saving profile', body({message}))} {...props}/>;
  };

  return ProfileErrorWrapper;
};

export const withProfileErrorModal = fp.flow(withProfileErrorWrapper, withErrorModal());
