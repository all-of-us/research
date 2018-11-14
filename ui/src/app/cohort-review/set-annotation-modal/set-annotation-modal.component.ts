import {Component} from '@angular/core';

import {ReviewStateService} from '../review-state.service';

@Component({
  selector: 'app-set-annotation-modal',
  templateUrl: './set-annotation-modal.component.html',
  styleUrls: ['./set-annotation-modal.component.css']
})
export class SetAnnotationModalComponent {

  /*
   * The modal displays a list or a form. Default is always list.
   */
  mode: 'list' | 'create' = 'list';

  /*
   * Is the modal open? A surprisingly tricky question, given that it needs to
   * be openable from places fairly remote from it in the DOM.  We use a flag
   * sent through the app-global state service and bind it to the clr-modal
   * component.
   */
  // private _open = false;

  get open() {
    return this.state.annotationManagerOpen.getValue();
  }

  set open(value: boolean) {
    this.state.annotationManagerOpen.next(value);
  }

    get openEdit() {
        return this.state.editAnnotationManagerOpen.getValue();
    }

    set openEdit(value: boolean) {
        this.state.editAnnotationManagerOpen.next(value);
    }

  get modalTitle() {
    return {
      'list': 'Edit Cohort-wide Annotation',
      'create': 'Create a Cohort-wide Annotation',
    }[this.mode];
  }

  constructor(private state: ReviewStateService) {}
}
