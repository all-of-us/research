import * as React from 'react';
import {WorkspaceEditSection} from './workspace-edit-section';
import {ResearchPurposeQuestion} from './workspace-edit-text';
import {TextAreaWithCharLimit} from "app/components/TextAreaWithCharLimit";

interface Props {
  index: string;
  onChange: Function;
  researchPurpose: ResearchPurposeQuestion;
  researchValue: string;
  rowId: string;
}

export class WorkspaceResearchSummary extends React.Component<Props> {

  constructor(props: Props) {
    super(props);
  }

  //TODO eric: do we need id/name on the TextAreaWithCharLimit component

  render() {
    const {index, researchPurpose, rowId} = this.props;
    return <WorkspaceEditSection data-test-id={rowId}
                                 header={researchPurpose.header}
                                 description={researchPurpose.description}
                                 index={index}
                                 indent>
      <TextAreaWithCharLimit
        value={this.props.researchValue}
        onChange={(v) => {
          this.props.onChange(v);
        }}
        minCharCount={50}
        maxCharCount={1000}
        tooShortWarningMessage='The description you entered seems too short. Please consider adding more descriptive
          details to help the Program and your fellow Researchers understand your work.'
      />
    </WorkspaceEditSection>;
  }
}
