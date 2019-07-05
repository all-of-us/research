import * as Cookies from 'js-cookie';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {CheckBox} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import colors from 'app/styles/colors';

export interface Props {
  onCancel: Function;
  onContinue: Function;
}

interface State {
  checked: boolean;
}

export class ConfirmPlaygroundModeModal extends React.Component<Props, State> {

  public static DO_NOT_SHOW_AGAIN = 'DO_NOT_SHOW_AGAIN_CONFIRM_PLAYGROUND_MODE_MODAL';

  constructor(props: Props) {
    super(props);

    this.state = {
      checked:  Cookies.get(ConfirmPlaygroundModeModal.DO_NOT_SHOW_AGAIN) === String(true)
    };
  }

  toggleChecked() {
    const newState = !this.state.checked;
    this.setState({checked: newState});
    Cookies.set(ConfirmPlaygroundModeModal.DO_NOT_SHOW_AGAIN, String(newState));
  }

  render() {
    return <Modal onRequestClose={() => { this.props.onCancel(); }}>
      <ModalTitle>Playground Mode</ModalTitle>
      <ModalBody>
        <p style={{color: colors.primary}}>
          Playground mode allows you to explore, change and run the code,
          but your edits will not be saved.
        </p>
        <p style={{color: colors.primary}}>
          To save your work, choose <b>Make a Copy</b> from the <b>File Menu</b>
          to make your own version.
        </p>
      </ModalBody>
      <ModalFooter style={{alignItems: 'center'}}>
        <CheckBox checked={this.state.checked}
                  onChange={() => { this.toggleChecked(); }} />
        <label style={{marginLeft: '8px', marginRight: 'auto', color: colors.primary}}>
          Don't show again
        </label>
        <Button type='secondary'
                style={{margin: '0 10px'}}
                onClick={() => { this.props.onCancel(); }}>
          Cancel
        </Button>
        <Button type='primary'
                onClick={() => { this.props.onContinue(); }}>
          Continue
        </Button>
      </ModalFooter>
    </Modal>;
  }

}
