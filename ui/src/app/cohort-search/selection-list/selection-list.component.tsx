import {Component, Input} from '@angular/core';
import * as React from 'react';

import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {DomainType} from 'generated/fetch';

const styles = reactStyles({
  buttonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: '0.5rem',
    padding: '0.45rem 0rem'
  },
  button: {
    border: 'none',
    borderRadius: '0.3rem',
    cursor: 'pointer',
    fontSize: '12px',
    height: '1.5rem',
    letterSpacing: '0.02rem',
    lineHeight: '0.75rem',
    margin: '0.25rem 0.5rem',
    padding: '0rem 0.75rem',
    textTransform: 'uppercase',
  },
  itemInfo: {
    width: '100%',
    minWidth: 0,
    flex: 1,
    display: 'flex',
    flexFlow: 'row nowrap',
    justifyContent: 'flex-start'
  },
  itemName: {
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.25rem',
    padding: 0
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: 'calc(100% - 150px)',
    overflowX: 'hidden',
    overflowY: 'auto',
    width: '95%',
  },
  selectionItem: {
    display: 'flex',
    fontSize: '14px',
    padding: '0.2rem 0.5rem 0',
    width: '100%',
  },
  selectionPanel: {
    background: colorWithWhiteness(colors.black, 0.95),
    height: '100%',
    padding: '0.5rem 0 0 1rem',
  },
  selectionTitle: {
    color: colors.primary,
    margin: 0,
    padding: '0.5rem 0'
  }
});

interface SelectionInfoProps {
  index: number;
  selection: any;
  removeSelection: Function;
}

interface SelectionInfoState {
  truncated: boolean;
}

export class SelectionInfo extends React.Component<SelectionInfoProps, SelectionInfoState> {
  name: HTMLDivElement;
  constructor(props: SelectionInfoProps) {
    super(props);
    this.state = {truncated: false};
  }

  componentDidMount(): void {
    const {offsetWidth, scrollWidth} = this.name;
    this.setState({truncated: scrollWidth > offsetWidth});
  }

  get showType() {
    return ![DomainType.PHYSICALMEASUREMENT, DomainType.DRUG, DomainType.SURVEY].includes(this.props.selection.domainId);
  }
  get showOr() {
    const {index, selection} = this.props;
    return index > 0 && selection.domainId !== DomainType.PERSON;
  }

  render() {
    const {selection, removeSelection} = this.props;
    const itemName = <React.Fragment>
      {this.showType && <strong>{typeDisplay(selection)}&nbsp;</strong>}
      {nameDisplay(selection)} {attributeDisplay(selection)}
    </React.Fragment>;
    return <div style={styles.selectionItem}>
      <button style={styles.removeSelection} onClick={() => removeSelection()}>
        <ClrIcon shape='times-circle'/>
      </button>
      <div style={styles.itemInfo}>
        {this.showOr && <strong>OR&nbsp;</strong>}
        {!!selection.group && <span>Group&nbsp;</span>}
        <TooltipTrigger disabled={!this.state.truncated} content={itemName}>
          <div style={styles.itemName} ref={(e) => this.name = e}>
            {itemName}
          </div>
        </TooltipTrigger>
      </div>
    </div>;
  }
}

interface Props {
  back: Function;
  cancel: Function;
  domain: DomainType;
  errors: Array<string>;
  finish: Function;
  removeSelection: Function;
  selections: Array<any>;
  setView: Function;
  view: string;
}

export class SelectionList extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

  get showModifiers() {
    return ![DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(this.props.domain);
  }

  get showNext() {
    return this.showModifiers && this.props.view !== 'modifiers';
  }

  get showBack() {
    return this.showModifiers && this.props.view === 'modifiers';
  }

  render() {
    const {back, cancel, errors, finish, removeSelection, selections, setView} = this.props;
    return <div style={styles.selectionPanel}>
      <h5 style={styles.selectionTitle}>Selected Criteria</h5>
      <div style={styles.selectionContainer}>
        {selections.map((selection, s) =>
          <SelectionInfo index={s} selection={selection} removeSelection={() => removeSelection(selection)}/>
        )}
      </div>
      <div style={styles.buttonContainer}>
        <button type='button'
          style={{...styles.button, background: 'transparent', color: colors.dark, fontSize: '14px'}}
          onClick={() => cancel()}>
          Cancel
        </button>
        {this.showNext && <button type='button'
          style={{...styles.button, background: colors.primary, color: colors.white}}
          disabled={selections.length === 0}
          onClick={() => setView('modifiers')}>
          Next
        </button>}
        {this.showBack && <button type='button'
          style={{...styles.button, background: colors.primary, color: colors.white}}
          onClick={() => back()}>
          Back
        </button>}
        <button type='button'
          style={{...styles.button, background: colors.primary, color: colors.white}}
          disabled={errors.length > 0}
          onClick={() => finish()}>
          Finish
        </button>
      </div>
    </div>;
  }
}

@Component({
  selector: 'crit-selection-list',
  template: '<div #root style="height: 100%"></div>'
})
export class SelectionListComponent extends ReactWrapperBase {
  @Input('back') back: Props['back'];
  @Input('cancel') cancel: Props['cancel'];
  @Input('domain') domain: Props['domain'];
  @Input('errors') errors: Props['errors'];
  @Input('finish') finish: Props['finish'];
  @Input('removeSelection') removeSelection: Props['removeSelection'];
  @Input('selections') selections: Props['selections'];
  @Input('setView') setView: Props['setView'];
  @Input('view') view: Props['view'];
  constructor() {
    super(SelectionList, ['back', 'cancel', 'domain', 'errors', 'finish', 'removeSelection', 'selections', 'setView', 'view']);
  }
}
