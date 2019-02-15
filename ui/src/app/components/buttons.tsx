import {styles as cardStyles} from 'app/components/card';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as Interactive from 'react-interactive';

export const styles = {
  base: {
    display: 'inline-flex', justifyContent: 'space-around', alignItems: 'center',
    height: '1.5rem', minWidth: '3rem', maxWidth: '15rem',
    fontWeight: 500, fontSize: 12, letterSpacing: '0.02rem', textTransform: 'uppercase',
    overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis',
    userSelect: 'none',
    margin: 0, padding: '0rem 0.77rem',
  },
  baseNew: {
    display: 'inline-flex', justifyContent: 'space-around', alignItems: 'center',
    minWidth: '3rem', maxWidth: '15rem',
    height: 50,
    fontWeight: 500, fontSize: 14, textTransform: 'uppercase', lineHeight: '18px',
    overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis',
    userSelect: 'none',
    margin: 0, padding: '0 22px',
    borderRadius: 5,
    boxSizing: 'border-box'
  }
};

const buttonVariants = {
  primary: {
    style: {
      ...styles.base,
      borderRadius: '0.3rem',
      backgroundColor: colors.purple[0], color: '#fff',
    },
    disabledStyle: {backgroundColor: colors.gray[4]},
    hover: {backgroundColor: colors.purple[1]}
  },
  secondary: {
    style: {
      ...styles.base,
      border: '2px solid', borderRadius: '0.2rem', borderColor: colors.purple[0],
      backgroundColor: 'transparent',
      color: colors.purple[0],
    },
    disabledStyle: {
      borderColor: colors.gray[4],
      backgroundColor: colors.backgroundGrey, color: colors.gray[4]
    },
    hover: {backgroundColor: colors.purple[0], color: '#ffffff'}
  },
  darklingPrimary: {
    style: {
      ...styles.base,
      borderRadius: '0.2rem',
      backgroundColor: colors.purple[0], color: '#ffffff'
    },
    disabledStyle: {backgroundColor: colors.gray[4]},
    hover: {backgroundColor: 'rgba(255,255,255,0.3)'}
  },
  darklingSecondary: {
    style: {
      ...styles.base,
      borderRadius: '0.2rem',
      backgroundColor: '#0079b8', color: '#ffffff'
    },
    disabledStyle: {backgroundColor: colors.gray[4]},
    hover: {backgroundColor: '#50ACE1'}
  },
  purplePrimary: {
    style: {
      ...styles.baseNew,
      backgroundColor: colors.purple[0], color: '#fff',
    },
    disabledStyle: {backgroundColor: colors.gray[4]},
    hover: {backgroundColor: colors.purple[1]}
  },
  purpleSecondary: {
    style: {
      ...styles.baseNew,
      border: '1px solid', borderColor: colors.purple[0],
      backgroundColor: 'transparent',
      color: colors.purple[0],
    },
    disabledStyle: {
      borderColor: colors.gray[4],
      backgroundColor: colors.backgroundGrey, color: colors.gray[4]
    },
    hover: {backgroundColor: colors.purple[1], color: '#fff', borderColor: colors.purple[1]}
  }
};

const computeStyle = ({style = {}, hover = {}, disabledStyle = {}}, {disabled}) => {
  return {
    style: {...style, ...(disabled ? disabledStyle : {})},
    hover: disabled ? undefined : hover
  };
};

export const Clickable = ({as = 'div', disabled = false, onClick = null, ...props}) => {
  return <Interactive
    as={as} {...props}
    onClick={(...args) => onClick && !disabled && onClick(...args)}
  />;
};

export const Button = ({type = 'primary', style = {}, disabled = false, ...props}) => {
  return <Clickable
    disabled={disabled} {...props}
    {...fp.merge(computeStyle(buttonVariants[type], {disabled}), {style})}
  />;
};

export const MenuItem = ({icon, tooltip = '', disabled = false, children, ...props}) => {
  return <TooltipTrigger side='left' content={tooltip}>
    <Clickable
      data-test-id={icon}
      disabled={disabled}
      style={{
        display: 'flex', alignItems: 'center', justifyContent: 'start',
        fontSize: 12, minWidth: 125, height: 32,
        color: disabled ? colors.gray[2] : 'black',
        padding: '0 12px',
        cursor: disabled ? 'not-allowed' : 'pointer'
      }}
      hover={!disabled ? {backgroundColor: colors.blue[3]} : undefined}
      {...props}
    >
      <ClrIcon shape={icon} style={{marginRight: 8}} size={15}/>
      {children}
    </Clickable>
  </TooltipTrigger>;
};

const cardButtonStyle = {
  style: {
    ...cardStyles.card,
    alignItems: 'flex-start', alignContent: 'left',
    marginTop: '1.9rem', marginRight: '106px', cursor: 'pointer',
    justifyContent: 'center', padding: '0 1rem', color: colors.blue[0],
    fontSize: 18, fontWeight: 500, lineHeight: '22px',
  },
  disabledStyle: {color: '#c3c3c3', backgroundColor: '#f1f2f2', cursor: 'not-allowed'}
};

export const CardButton = ({disabled = false, style = {}, children, ...props}) => {
  return <Clickable
    disabled={disabled} {...props}
    {...fp.merge(computeStyle(cardButtonStyle, {disabled}), {style})}
  >{children}</Clickable>;
};
