import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {DetailPage} from 'app/cohort-review/detail-page/detail-page';
import {PageLayout} from 'app/cohort-review/page-layout/page-layout';
import {TablePage} from 'app/cohort-review/table-page/table-page';

import {DemographicConceptMapsResolver} from './demographic-concept-maps.resolver';
import {ParticipantAnnotationsResolver} from './participant-annotations.resolver';
import {ParticipantResolver} from './participant.resolver';

import {QueryReportComponent} from 'app/cohort-review/query-report/query-report.component';
import {AnnotationDefinitionsResolver} from 'app/resolvers/annotation-definitions';
import {ReviewResolver} from 'app/resolvers/review';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {
    title: 'Review Cohort Participants'
  },
  resolve: {
    review: ReviewResolver,
  },
  children: [{
    path: 'participants',
    component: TablePage,
    resolve: {
      concepts: DemographicConceptMapsResolver,
    },
    data: {
      breadcrumb: {
        value: 'Participants',
        intermediate: true
      },
    }
  }, {
    path: 'participants/:pid',
    component: DetailPage,
    resolve: {
      annotationDefinitions: AnnotationDefinitionsResolver,
      participant: ParticipantResolver,
      annotations: ParticipantAnnotationsResolver,
    },
    data: {
      breadcrumb: {
        value: 'Participant :pid',
        intermediate: false
      }
    }
  }, {
    path: 'report',
    component: QueryReportComponent,
    data: {
      breadcrumb: {
        value: 'Query Report',
        intermediate: false
      }
    }
  }],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AnnotationDefinitionsResolver,
    DemographicConceptMapsResolver,
    ParticipantResolver,
    ParticipantAnnotationsResolver,
    ReviewResolver,
  ],
})
export class CohortReviewRoutingModule {}
