import {reactStyles, withStyle} from 'app/utils';
import * as React from 'react';

const styles = reactStyles({
  card: {
    padding: '1rem',
    borderRadius: '0.2rem',
    boxShadow: '0 0.125rem 0.125rem 0 #d7d7d7',
    backgroundColor: '#fff',
    border: '1px solid #d7d7d7',
    display: 'flex',
    flexDirection: 'column'
  },
  single: {
    height: '33.4%',
    width: '87.34%',
    maxWidth: '70rem',
    minHeight: '18rem',
    maxHeight: '26rem',
    display: 'flex',
    flexDirection: 'column',
    borderRadius: '5px',
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    border: 'none'
  }
});

export const Card = withStyle(styles.card)('div');

export const SingleCard = withStyle(styles.single)(Card);
