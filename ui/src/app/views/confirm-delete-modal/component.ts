import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-confirm-delete-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class ConfirmDeleteModalComponent {
  public deleting = false;

  @Input() resourceType: string;
  @Output() receiveDelete = new EventEmitter<any>();
  @Input() resource: {name: string};

  loading = false;

  open(): void {
    this.deleting = true;
    this.loading = false;
  }

  close(): void {
    this.deleting = false;
  }

  emitDelete(resource: any): void {
    if (!this.loading) {
      this.loading = true;
      this.receiveDelete.emit(resource);
    }
  }
}
