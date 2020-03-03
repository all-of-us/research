import * as React from "react";
import {Button} from "app/components/buttons";
import {Modal, ModalBody, ModalFooter, ModalTitle} from "app/components/modals";
import {Workspace} from "generated/fetch";

interface Props {
  onClose: Function;
  onSubmitBugReport: Function;
  workspace: Workspace;
}

export class WorkspaceDeletionErrorModal extends React.Component<Props, {}> {
  constructor(props) {
    super(props);
  }

  render() {
    return <Modal>
      <ModalTitle>Error</ModalTitle>
      <ModalBody>
        Could not delete workspace {this.props.workspace.name}. Please <a onClick={() => this.props.onSubmitBugReport()}>submit a bug report.</a>
      </ModalBody>
      <ModalFooter>
        <Button onClick={() => this.props.onClose()}>Ok</Button>
      </ModalFooter>
    </Modal>
  }
}
