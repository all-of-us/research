import {InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {AouTitle} from 'app/components/text-wrappers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';

const styles = reactStyles({
  label: {
    marginLeft: '0.3rem',
    color: colors.accent,
    fontSize: '12px',
    fontWeight: 400
  },
  icon: {
    marginLeft: '0.3rem',
    width: '0.7rem',
    marginBottom: '0.17rem'
  }
});

const css = `
  .pointer {
     position: relative;
     height: 29px;
     width: 7rem;
     background-color: ` + colorWithWhiteness(colors.accent, 0.75) + `;
     line-height:1.2rem;
   }

  .pointer::after {
     position: absolute;
     left: 100%;
     top:0rem;
     width:0rem;
     height:0rem;
     border-top:0.6rem solid transparent;
     border-bottom:0.6rem solid transparent;
  }
  .pointer:before {
     content:"";
     position: absolute;
     right: 100%;
     top:0rem;
     width:0rem;
     height:0rem;
     border-top:0.6rem solid transparent;
     border-right:0.7rem solid ` + colorWithWhiteness(colors.accent, 0.75) + `;
     border-bottom:0.6rem solid transparent;
   }
 `;

const toolTipContent = <span>The <AouTitle/> Research Program seeks to be transparent with
  participants about who can access their data and for what purpose. Your answer to this question
  will be displayed in the Research Projects Directory on our public website.</span>;

export const PubliclyDisplayed = (props) => {
  return <div>
    <style>
      {css}
    </style>
    <div className='pointer' style={props.style}>
      <label style={styles.label}>
        Publicly displayed
        <TooltipTrigger content={toolTipContent} side='right'>
          <InfoIcon style={styles.icon}/>
        </TooltipTrigger>
      </label>
    </div>
   </div>;
};
