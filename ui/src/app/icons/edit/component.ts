import {Component, Input} from '@angular/core';



@Component({
  selector: 'app-edit',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class EditComponent {
  @Input() editHover: boolean;
  constructor() {}
}
