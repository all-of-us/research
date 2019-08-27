import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {SnowmanIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';

export interface ResourceCardMenuProps {
  resourceType: ResourceType;
  onRenameResource?: Function;
  onCopyConceptSet?: Function;
  canDelete: boolean;
  onDeleteResource?: Function;
  canEdit: boolean;
  onEdit?: Function;
  onExportDataSet: Function;
}

export class ResourceCardMenu extends React.Component<ResourceCardMenuProps> {
  render() {
    return <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={
        switchCase(this.props.resourceType,
          ['cohortReview', () => {
            return <React.Fragment>
              <MenuItem icon='note'
                        onClick={this.props.onRenameResource}
                        disabled={!this.props.canEdit}
              >
                Rename
              </MenuItem>
              <MenuItem icon='trash'
                        onClick={this.props.onDeleteResource}
                        disabled={!this.props.canDelete}
              >
                Delete
              </MenuItem>
            </React.Fragment>;
          }],
          ['conceptSet', () => {
            return <React.Fragment>
              <MenuItem icon='pencil'
                        onClick={this.props.onEdit}
                        disabled={!this.props.canEdit}
              >
                Rename
              </MenuItem>
              <MenuItem icon='copy'
                        onClick={this.props.onCopyConceptSet}
              >
                Copy to another workspace
              </MenuItem>
              <MenuItem icon='trash'
                        onClick={this.props.onDeleteResource}
                        disabled={!this.props.canDelete}
              >
                Delete
              </MenuItem>
            </React.Fragment>;
          }],
          ['dataSet', () => {
            return <React.Fragment>
              <MenuItem icon='pencil'
                        onClick={this.props.onRenameResource}
                        disabled={!this.props.canEdit}
              >
                Rename Data Set
              </MenuItem>
              <MenuItem icon='pencil'
                        onClick={this.props.onEdit}
                        disabled={!this.props.canEdit}
              >
                Edit
              </MenuItem>
              <MenuItem icon='clipboard'
                        onClick={this.props.onExportDataSet}
                        disabled={!this.props.canEdit}
              >
                Export to Notebook
              </MenuItem>
              <MenuItem icon='trash'
                        onClick={this.props.onDeleteResource}
                        disabled={!this.props.canDelete}
              >
                Delete
              </MenuItem>
            </React.Fragment>;
          }]
        )
      }
    >
      <Clickable data-test-id='resource-menu'>
        <SnowmanIcon />
      </Clickable>
    </PopupTrigger>;
  }
}
