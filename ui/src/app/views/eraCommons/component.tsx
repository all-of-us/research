import {Component} from '@angular/core';
import {SingleCard} from 'app/components/card';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';
import EraCommon from '../eraCommon/component';
import {styles} from './style';


export class EraCommonHomePage extends React.Component<any, any> {
  constructor(props: any) {
    super(props);
  }
  render() {
    return <div style={styles.background}>
      <div style={{display: 'flex', justifyContent: 'center'}}>
        <SingleCard>
          <EraCommon/>
        </SingleCard>
      </div>
    </div>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class EraCommonComponent extends ReactWrapperBase {
  constructor() {
    super(EraCommonHomePage, []);
  }
}
