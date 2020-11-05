import * as fp from 'lodash/fp';
import * as React from 'react';

import {RenameModal} from 'app/components/rename-modal';
import {Action, ResourceActionsMenu} from 'app/components/resource-actions-menu';
import {canDelete, canWrite, ResourceCard} from 'app/components/resource-card';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {ExportDataSetModal} from 'app/pages/data/data-set/export-data-set-modal';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';
import {navigate} from 'app/utils/navigation';
import {getDescription, getDisplayName, getType} from 'app/utils/resources';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {WorkspaceResource} from 'generated/fetch';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
  disableExportToNotebook: boolean;
  menuOnly: boolean;
}

interface State {
  showRenameModal: boolean;
  showExportToNotebookModal: boolean;
}

export const DatasetResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
)(class extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showRenameModal: false,
      showExportToNotebookModal: false
    };
  }

  get actions(): Action[] {
    const {resource} = this.props;
    return [
      {
        icon: 'pencil',
        displayName: 'Rename Dataset',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenRenameModal();
          this.setState({showRenameModal: true});
        },
        disabled: !canWrite(resource)
      },
      {
        icon: 'pencil',
        displayName: 'Edit',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenEditPage('From Card Snowman');
          navigate(['workspaces',
            resource.workspaceNamespace,
            resource.workspaceFirecloudName,
            'data', 'data-sets', resource.dataSet.id]);
        },
        disabled: !canWrite(resource)
      },
      {
        icon: 'clipboard',
        displayName: 'Export to Notebook',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenExportModal();
          this.setState({showExportToNotebookModal: true});
        },
        disabled: this.props.disableExportToNotebook || !canWrite(resource),
        hoverText: this.props.disableExportToNotebook && ACTION_DISABLED_INVALID_BILLING
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenDeleteModal();
          this.props.showConfirmDeleteModal(getDisplayName(resource),
            getType(resource), () => this.delete());
        },
        disabled: !canDelete(resource)
      }
    ];
  }

  delete() {
    AnalyticsTracker.DatasetBuilder.Delete();
    return dataSetApi().deleteDataSet(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.dataSet.id
    ).then(() => {
      this.props.onUpdate();
    });
  }

  rename(name, description) {
    AnalyticsTracker.DatasetBuilder.Rename();
    const dataset = this.props.resource.dataSet;

    const request = {
      ...dataset,
      name: name,
      description: description
    };

    return dataSetApi().updateDataSet(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      dataset.id,
      request
    ).then(() => {
      this.props.onUpdate();
    }).catch(error => console.error(error)
    ).finally(() => {
      this.setState({showRenameModal: false});
    });
  }

  render() {
    const {resource, menuOnly} = this.props;
    return <React.Fragment>
      {this.state.showExportToNotebookModal &&
      <ExportDataSetModal dataSet={resource.dataSet}
                          workspaceNamespace={resource.workspaceNamespace}
                          workspaceFirecloudName={resource.workspaceFirecloudName}
                          closeFunction={() => this.setState({showExportToNotebookModal: false})}/>
      }
      {this.state.showRenameModal &&
      <RenameModal onRename={(name, description) => this.rename(name, description)}
                   resourceType={getType(resource)}
                   onCancel={() => this.setState({showRenameModal: false})}
                   oldDescription={getDescription(resource)}
                   oldName={getDisplayName(resource)}
                   existingNames={this.props.existingNameList}/>
      }
      {menuOnly ? <ResourceActionsMenu actions={this.actions}/> :
          <ResourceCard
          resource={resource}
          actions={this.actions}
      />}
    </React.Fragment>;
  }
});
