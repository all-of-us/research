import * as Color from 'color';
import * as React from 'react';
import * as ReactModal from 'react-modal';

import colors from 'app/styles/colors';
import {reactStyles, withStyle} from 'app/utils/index';
import {animated, useSpring} from 'react-spring';
import {SpinnerOverlay} from './spinners';
import {Button} from 'app/components/buttons';
import * as fp from 'lodash/fp';


const styles = reactStyles({
  modal: {
    borderRadius: 8, position: 'relative',
    padding: '1rem', margin: 'auto', outline: 'none',
    backgroundColor: 'white', boxShadow: '0 1px 2px 2px rgba(0,0,0,.2)'
  },

  overlay: {
    backgroundColor: Color(colors.dark).alpha(0.85).toString(), padding: '1rem', display: 'flex',
    position: 'fixed', left: 0, right: 0, top: 0, bottom: 0, overflowY: 'auto',
    // Keep z-index in sync with popups.tsx.
    zIndex: 105
  },

  modalTitle: {
    fontSize: '20px',
    color: colors.primary,
    fontWeight: 600,
    marginBottom: '1rem'
  },

  modalBody: {
    fontSize: '14px',
    lineHeight: '.8rem',
    marginTop: '3%',
    fontWeight: 400

  },

  modalFooter: {
    display: 'flex' as 'flex',
    justifyContent: 'flex-end' as 'flex-end',
    marginTop: '1rem'
  }
});

export const Modal = ({width = 450, loading = false, ...props}) => {
  return <ReactModal
    parentSelector={() => document.getElementById('popup-root')}
    isOpen
    style={{overlay: styles.overlay, content: props.contentStyleOverride || {...styles.modal, width}}}
    ariaHideApp={false}
    {...props}
  >
    {props.children}
    {loading && <SpinnerOverlay/>}
  </ReactModal>;
};


export const AnimatedModal = ({width = 450, ...props}) => {
  const style = {...styles.modal, width};
  const styleSpring = useSpring({...style, from: {width: 450}});

  return <Modal contentStyleOverride={{margin: 'auto'}} {...props}>
    <div>
      <animated.div style={styleSpring}>
        {props.children}
      </animated.div>
    </div>
  </Modal>;
};

export const ModalTitle = withStyle(styles.modalTitle)('div');
export const ModalBody = withStyle(styles.modalBody)('div');
export const ModalFooter = withStyle(styles.modalFooter)('div');

export const ResponseModal = ({onDismiss, title, message}) => {
  return <Modal>
    <ModalTitle>{title}</ModalTitle>
    <ModalBody>{message}</ModalBody>
    <ModalFooter>
      <Button onClick={onDismiss}>OK</Button>
    </ModalFooter>
  </Modal> 
}

export interface Result {
  title: string, 
  message: string,
  errorTitle?: string,
  errorMessage?: string,
  error?: boolean,
  onDismiss?: () => any
};

export const withResponseHandling = fp.curry(
  (setConfirm, {title, message, errorTitle, errorMessage, onDismiss = fp.noop}: Result, wrappedFn) => async (...args) => {
  try {
    const result = await wrappedFn(...args);
    setConfirm({error: false, title, message, onDismiss})
    return result
  } catch (e) {
    setConfirm({error: true, title: errorTitle, message: errorMessage});
  }
})

