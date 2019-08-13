import {NgModule} from '@angular/core';

import {ExpandComponent} from 'app/icons/expand/component';
import {ReminderComponent} from 'app/icons/reminder';
import {ShareComponent} from 'app/icons/share/component';
import {ShrinkComponent} from 'app/icons/shrink/component';
import {TrashComponent} from 'app/icons/trash/component';

@NgModule({
  imports: [],
  declarations: [
    TrashComponent,
    ExpandComponent,
    ReminderComponent,
    ShareComponent,
    ShrinkComponent
  ],
  exports: [
    TrashComponent,
    ExpandComponent,
    ReminderComponent,
    ShareComponent,
    ShrinkComponent
  ],
  providers: []
})
export class IconsModule {}
