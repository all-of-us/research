import {Component, Input} from '@angular/core';

import * as React from 'react';

import {ReactWrapperBase} from 'app/utils';

interface EditComponentProps {
  disabled: boolean;
  style: object;
}
interface EditComponentState {
  style: object;
}

const defaultStyle = {
    height: 19,
    width: 19,
    marginLeft: '.5rem',
    marginTop: '1.1rem',
    fill: '#2691D0',
    cursor: 'pointer'
};

const hoverStyle = {...defaultStyle, fill: '#83C3EC'};

const disabledStyle = {...defaultStyle, fill: '#c3c3c3'};

export class EditComponentReact extends React.Component<EditComponentProps, EditComponentState> {

  constructor(props: EditComponentProps) {
    super(props);
    this.state = {
      style: {...defaultStyle, ...this.props.style}
    };
  }

  mouseOver(): void {
    this.setState({style: {...hoverStyle, ...this.props.style}});
  }

  mouseLeave(): void {
    this.setState({style: {...defaultStyle, ...this.props.style}});
  }

  render() {
    return (
      <svg
        style={this.props.disabled ? {...this.state.style, ...disabledStyle} : this.state.style}
        version='1.1'
        viewBox='0 0 36 36'
        preserveAspectRatio='xMidYMid meet'
        xmlns='http://www.w3.org/2000/svg'
        xmlnsXlink='http://www.w3.org/1999/xlink'
        onMouseOver={() => this.mouseOver()}
        onMouseLeave={() => this.mouseLeave()}>
      <title>Edit</title>
        <path d="M4.22,23.2l-1.9,8.2a2.06,2.06,0,0,0,2,2.5,2.14,2.14,0,0,0,.43,0L13,32,28.84,16.22,
                 20,7.4Z"/>
        <path d="M33.82,8.32l-5.9-5.9a2.07,2.07,0,0,0-2.92,0L21.72,5.7l8.83,8.83,3.28-3.28A2.07,
                 2.07,0,0,0,33.82,8.32Z"/>
        <rect x='0' y='0'
              width={this.state.style['width']}
              height={this.state.style['height']}
              fillOpacity='0'/>
      </svg>
    );
  }
}

@Component({
  selector: 'app-edit-icon',
  template: '<div #root></div>',
})
export class EditComponent extends ReactWrapperBase {

  @Input('disabled')
  disabled: EditComponentProps['disabled'];

  @Input('style')
  style: EditComponentProps['style'];

  constructor() {
    super(EditComponentReact, ['disabled', 'style']);
  }

}
