import * as React from 'react';

import colors from 'app/styles/colors';
import {withStyle} from 'app/utils/index';


export const styles = {
  h1: {
    color: colors.purple[0],
    fontWeight: 500,
    fontSize: '1.6rem',
  },
  h2: {
    color: colors.purple[0],
    fontWeight: 800,
    fontSize: '1.25rem',
  },
  h3: {
    color: colors.purple[0],
    fontWeight: 800,
    fontSize: '1rem',
  },
  h4: {
    fontWeight: 300,
    color: '#000',
  },
  formLabel: {
    color: colors.purple[0],
    fontWeight: 600,
    marginTop: '0.5rem',
    marginBottom: '0.125rem'
  },
  listPageHeader: {
    color: '#2F2E7E',
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
    paddingTop: '0.5rem'
  }
};

export const BolderHeader = withStyle(styles.h1)('h1');
export const BoldHeader = withStyle(styles.h2)('h2');
export const Header = withStyle(styles.h3)('h3');
export const SmallHeader = withStyle(styles.h4)('h4');
export const ListPageHeader = withStyle(styles.listPageHeader)('h3');
