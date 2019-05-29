import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {ActionCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {environment} from 'environments/environment';
import {ConceptSet} from 'generated/fetch';
import * as React from 'react';

const styles = reactStyles({
  conceptSetsHeader: {
    color: colors.blue[7],
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
  },
  cardArea: {
    display: 'flex',
    alignItems: 'center',
    width: '100%'
  },
  card: {
    marginTop: '0.5rem',
    justifyContent: 'space-between',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem',
    boxShadow: '0 0 0 0'
  },
  cardName: {
    fontSize: '18px', fontWeight: 600, lineHeight: '22px', color: colors.blue[7],
    wordBreak: 'break-all', textOverflow: 'ellipsis', overflow: 'hidden',
    display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical'
  },
  cardDescription: {
    marginTop: '0.5rem', textOverflow: 'ellipsis', overflow: 'hidden', display: '-webkit-box',
    WebkitLineClamp: 4, WebkitBoxOrient: 'vertical'
  },
  cardButton: {
    margin: '1rem 0',
    height: '2rem'
  }
});

const disabledButton = {
  ...styles.cardButton,
  cursor: 'not-allowed'
};

const actionCards = [
  {
    title: 'Export to a Notebook',
    description: `Data can be exported to a cloud-based Jupyter notebook for analysis using R or
       Python programming language.`,
    action: 'notebook'
  },
  {
    title: 'Create a Data Set',
    description: `Here, you can build and preview a data set for one or more cohorts by
       selecting the desired concept sets and values for the cohorts.`,
    action: 'dataSet'
  },
  {
    title: 'Create another Concept Set',
    description: `Here, you can create another concept set for the same or a different domain.`,
    action: 'conceptSet'
  },
];

interface State {
  conceptSet: ConceptSet;
}

interface Props {
  workspace: WorkspaceData
}

export const ConceptSetActions = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        conceptSet: undefined
      };
    }

    componentDidMount(): void {
      const {conceptSet} = this.state;
      if (!conceptSet) {
        const csid = urlParamsStore.getValue().csid;
        if (csid) {
          const {namespace, id} = this.props.workspace;
          conceptSetsApi().getConceptSet(namespace, id, csid).then(cs => {
            if (cs) {
              this.setState({conceptSet: cs});
            } else {
              navigate(['workspaces', namespace, id, 'concepts']);
            }
          });
        }
      }
    }

    navigateTo(action: string): void {
      const {namespace, id} = this.props.workspace;
      let url = `/workspaces/${namespace}/${id}/`;
      switch (action) {
        case 'conceptSet':
          url += `concepts`;
          break;
        case 'notebook':
          url += 'notebooks';
          break;
        case 'dataset':
          url += 'data/datasets';
          break;
      }
      navigateByUrl(url);
    }

    render() {
      const {conceptSet} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {conceptSet && <React.Fragment>
          <h3 style={styles.conceptSetsHeader}>Concept Set Saved Successfully</h3>
          <div style={{marginTop: '0.25rem'}}>
            The concept set
            <a style={{color: '#5DAEE1', margin: '0 4px'}}>
              {conceptSet.name}
            </a>
            has been saved and can now be used in analysis and concept sets.
          </div>
          <h3 style={{...styles.conceptSetsHeader, marginTop: '1.5rem'}}>What Next?</h3>
          <div style={styles.cardArea}>
            {actionCards.map((card, i) => {
              const disabled = card.action === 'notebook' ||
                (card.action === 'dataset' && !environment.enableDatasetBuilder);
              return <ActionCardBase key={i} style={styles.card}>
                <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
                  <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
                    <div style={styles.cardName}>{card.title}</div>
                  </div>
                  <div style={styles.cardDescription}>{card.description}</div>
                </div>
                <div>
                  <Button
                    type='primary'
                    style={disabled ? disabledButton : styles.cardButton}
                    disabled={disabled}
                    onClick={() => this.navigateTo(card.action)}>
                    {card.title}
                  </Button>
                </div>
              </ActionCardBase>;
            })}
          </div>
        </React.Fragment>}
      </FadeBox>;
    }
  }
);

@Component({
  template: '<div #root></div>'
})
export class ConceptSetActionsComponent extends ReactWrapperBase {

  constructor() {
    super(ConceptSetActions, []);
  }
}
