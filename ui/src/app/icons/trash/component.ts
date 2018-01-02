import {Component, Input} from '@angular/core';



@Component({
  selector: 'app-trash',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class TrashComponent {
  @Input() trashHover: boolean;
  constructor() {}
}
