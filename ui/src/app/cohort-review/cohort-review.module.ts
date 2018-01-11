import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {ClarityModule} from 'clarity-angular';

import {AnnotationFormComponent} from './annotation-form/annotation-form.component';
import {AnnotationsComponent} from './annotations/annotations.component';
import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {CreateReviewComponent} from './create-review/create-review.component';
import {CreateSetAnnotationComponent
} from './create-set-annotation/create-set-annotation.component';
import {EditSetAnnotationsComponent} from './edit-set-annotations/edit-set-annotations.component';
import {OverviewComponent} from './overview/overview.component';
import {ParticipantDetailComponent} from './participant-detail/participant-detail.component';
import {ParticipantStatusComponent} from './participant-status/participant-status.component';
import {ParticipantTableComponent} from './participant-table/participant-table.component';
import {ReviewNavComponent} from './review-nav/review-nav.component';

import {FullPageDirective} from './directives/fullPage.directive';
import {SidebarDirective} from './directives/sidebar.directive';
import {ReviewStateService} from './review-state.service';

import {AnnotationDefnResolver} from './guards/annotation-defn-resolver.guard';
import {CohortResolver} from './guards/cohort-resolver.guard';
import {ParticipantResolver} from './guards/participant-resolver.guard';
import {ReviewResolver} from './guards/review-resolver.guard';

import {
  CohortAnnotationDefinitionService,
  CohortReviewService,
} from 'generated';
import { AnnotationManagerComponent } from './annotation-manager/annotation-manager.component';


const routes = [{
  path: 'workspace/:ns/:wsid/cohorts/:cid/review',
  component: CohortReviewComponent,
  data: {title: 'Review Cohort Participants'},
  children: [
    {
      path: '',
      redirectTo: 'overview',
      pathMatch: 'full',
    }, {
      path: 'overview',
      component: OverviewComponent,
    }, {
      path: 'participants',
      component: ParticipantTableComponent,
    }, {
      path: 'participants/:pid',
      component: ParticipantDetailComponent,
      resolve: {
        participant: ParticipantResolver,
      }
    }
  ],
  resolve: {
    review: ReviewResolver,
    cohort: CohortResolver,
    annotations: AnnotationDefnResolver,
  }
}];

@NgModule({
  imports: [
    ClarityModule,
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
  ],
  declarations: [
    AnnotationFormComponent,
    AnnotationsComponent,
    CohortReviewComponent,
    CreateReviewComponent,
    CreateSetAnnotationComponent,
    EditSetAnnotationsComponent,
    FullPageDirective,
    OverviewComponent,
    SidebarDirective,
    ParticipantDetailComponent,
    ParticipantStatusComponent,
    ParticipantTableComponent,
    ReviewNavComponent,
    AnnotationManagerComponent,
  ],
  providers: [
    CohortAnnotationDefinitionService,
    CohortReviewService,
    ReviewStateService,

    AnnotationDefnResolver,
    CohortResolver,
    ReviewResolver,
    ParticipantResolver,
  ]
})
export class CohortReviewModule {}
