import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  Renderer2,
  ViewChild,
} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {CreateReviewComponent} from '../create-review/create-review.component';
import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit {
  @ViewChild('createReviewModal') createReviewModal: CreateReviewComponent;
  @ViewChild('fullPageDiv') fullPageDiv: ElementRef;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private renderer: Renderer2,
  ) {}

  ngOnInit() {
    const {annotationDefinitions, cohort, review} = this.route.snapshot.data;
    this.state.annotationDefinitions.next(annotationDefinitions);
    this.state.cohort.next(cohort);
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.createReviewModal.modal.open();
    }
    this.updateWrapperDimensions();
  }

  @HostListener('window:resize')
  onResize() {
    this.updateWrapperDimensions();
  }

  updateWrapperDimensions() {
    const nativeEl = this.fullPageDiv.nativeElement;
    const {top} = nativeEl.getBoundingClientRect();
    const minHeight = pixel(window.innerHeight - top - ONE_REM);
    this.renderer.setStyle(nativeEl, 'min-height', minHeight);
  }
}
