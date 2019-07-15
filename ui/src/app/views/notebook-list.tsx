import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {CardButton} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';

import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {convertToResource, ResourceType} from 'app/utils/resourceActionsReact';
import {WorkspaceData} from 'app/utils/workspace-data';
import {NewNotebookModal} from 'app/views/new-notebook-modal';
import {ResourceCard} from 'app/views/resource-card';

import {FileDetail, WorkspaceAccessLevel} from 'generated/fetch';

const styles = {
  heading: {
    color: colors.primary,
    fontSize: 20, fontWeight: 600, lineHeight: '24px'
  }
};

export const NotebookList = withCurrentWorkspace()(class extends React.Component<{
  workspace: WorkspaceData
}, {
  notebooks: FileDetail[],
  creating: boolean,
  loading: boolean
}> {
  constructor(props) {
    super(props);
    this.state = {notebooks: [], creating: false, loading: false};
  }

  componentDidMount() {
    profileApi().updatePageVisits({page: 'notebook'});
    this.loadNotebooks();
  }

  componentDidUpdate(prevProps) {
    if (this.workspaceChanged(prevProps)) {
      this.loadNotebooks();
    }
  }

  private workspaceChanged(prevProps) {
    return this.props.workspace.namespace !== prevProps.workspace.namespace ||
      this.props.workspace.id !== prevProps.workspace.id;
  }

  private async loadNotebooks() {
    try {
      const {workspace: {namespace, id}} = this.props;
      this.setState({loading: true});
      const notebooks = await workspacesApi().getNoteBookList(namespace, id);
      this.setState({notebooks});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({loading: false});
    }
  }

  render() {
    const {workspace, workspace: {namespace, id, accessLevel}} = this.props;
    const {notebooks, creating, loading} = this.state;
    // TODO Remove this cast when we switch to fetch types
    const al = accessLevel as unknown as WorkspaceAccessLevel;
    const canWrite = fp.includes(al, [WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER]);
    return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
      <div style={styles.heading}>
        Notebooks&nbsp;
        <TooltipTrigger
          content={`A Notebook is a computational environment where you
            can analyze data with basic programming knowledge in R or Python.`}
        ><InfoIcon size={16} /></TooltipTrigger>
      </div>
      <div style={{display: 'flex', alignItems: 'flex-start'}}>
        <TooltipTrigger content={!canWrite && 'Write permission required to create notebooks'}>
          <CardButton
            disabled={!canWrite}
            type='small'
            onClick={() => this.setState({creating: true})}
          >
            Create a<br/>New Notebook
            <ClrIcon shape='plus-circle' size={21} style={{marginTop: 5}} />
          </CardButton>
        </TooltipTrigger>
        <div style={{display: 'flex', flexWrap: 'wrap', justifyContent: 'flex-start' }}>
          {notebooks.map(notebook => {
            return <ResourceCard key={notebook.path}
              onDuplicateResource={(duplicating) =>
                this.setState({loading: duplicating})
              }
              resourceCard={convertToResource(notebook, namespace, id, al, ResourceType.NOTEBOOK)}
              onUpdate={() => this.loadNotebooks()}
            />;
          })}
        </div>
      </div>
      {loading && <SpinnerOverlay />}
      {creating && <NewNotebookModal
        workspace={workspace}
        existingNotebooks={notebooks}
        onClose={() => this.setState({creating: false})}
      />}
    </FadeBox>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class NotebookListComponent extends ReactWrapperBase {
  constructor() {
    super(NotebookList, []);
  }
}
