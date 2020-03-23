import {FlexRow} from 'app/components/flex';
import {TextArea} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';

const styles = reactStyles({
  textArea: {
    height: '100%',
    resize: 'none',
    //width: '50rem',
    borderRadius: '3px 3px 0 0',
    borderColor: colorWithWhiteness(colors.dark, 0.5)
  },
  textBoxCharRemaining: {
    justifyContent: 'space-between',
    //width: '50rem',
    backgroundColor: colorWithWhiteness(colors.primary, 0.95),
    fontSize: 12,
    colors: colors.primary,
    padding: '0.25rem',
    borderRadius: '0 0 3px 3px',
    marginTop: '-0.5rem',
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`
  },
});

interface Props {
  onChange: Function;
  value: string;
  placeholder?: string;
  minCharCount: number;
  maxCharCount: number;
  tooShortWarningMessage?: string;
}

interface State {
  showWarningMessage: boolean;
  textColor: string;
}

export class TextAreaWithCharLimit extends React.Component<Props, State > {

  static defaultProps = {
    tooShortWarningMessage: 'The text you entered seems too short. Please add more details.'
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      showWarningMessage: false,
      textColor: colors.disabled
    };
  }

  onTextUpdate(value) {
    const {minCharCount, maxCharCount} = this.props;
    const {showWarningMessage, textColor} = this.state;

    if (showWarningMessage && value.length >= minCharCount) {
      this.setState({showWarningMessage: false});
    }
    if (value.length > maxCharCount) {
      value = value.substring(0, maxCharCount);
    }
    if (value.length >= maxCharCount - 50 && textColor !== colors.danger) {
      this.setState({textColor: colors.danger});
    } else if (value.length < maxCharCount - 50 && textColor !== colors.disabled) {
      this.setState({textColor: colors.disabled});
    }
    this.props.onChange(value);
  }

  onBlur() {
    if (this.props.value.length < this.props.minCharCount) {
      this.setState({showWarningMessage: true});
    } else {
      this.setState({showWarningMessage: false});
    }
  }

  render() {
    const {textColor} = this.state;
    return <React.Fragment>
      <TextArea style={styles.textArea}
                value={this.props.value}
                placeholder={this.props.placeholder}
                onBlur={() => this.onBlur()}
                onChange={v => this.onTextUpdate(v)}/>

      <FlexRow style={styles.textBoxCharRemaining}>
        {this.state.showWarningMessage &&
          <label data-test-id='warningMsg'
                 style={{color: colors.danger, justifyContent: 'flex-start'}}>
            {this.props.tooShortWarningMessage}
          </label>
        }

        {!this.props.value &&
          <div data-test-id='characterMessage'
               style={{color: textColor, marginLeft: 'auto'}}>
            {this.props.maxCharCount} characters remaining
          </div>
        }

        {this.props.value &&
          <div data-test-id='characterMessage'
               style={{color: textColor, marginLeft: 'auto'}}>
            {this.props.maxCharCount - this.props.value.length} characters remaining
          </div>
        }

        {this.props.value && this.props.value.length >= this.props.maxCharCount &&
          <label data-test-id='characterLimit' style={{color: colors.danger}}>
            You have reached the character limit for this question</label>
        }
      </FlexRow>
    </React.Fragment>;
  }
}