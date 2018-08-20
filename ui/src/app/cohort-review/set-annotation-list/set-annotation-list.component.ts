import {Component, ChangeDetectorRef, ChangeDetectionStrategy, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {CohortAnnotationDefinition} from 'generated';

type DefnId = CohortAnnotationDefinition['cohortAnnotationDefinitionId'];

@Component({
    selector: 'app-set-annotation-list',
    templateUrl: './set-annotation-list.component.html',
    styleUrls: ['./set-annotation-list.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SetAnnotationListComponent implements OnInit, OnDestroy {
    subscription: Subscription;
    definitions: CohortAnnotationDefinition[];
    postSet: Set<DefnId> = new Set<DefnId>();
    // cancelFlag = false;
    // saveButtonEvent = false;
    // posting = false;
    constructor(private state: ReviewStateService,
                private cdr: ChangeDetectorRef) {}

    ngOnInit() {
        this.subscription = this.state.annotationDefinitions$
            .subscribe(defns => this.definitions = defns);
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    isPosting(flag: boolean, defn: CohortAnnotationDefinition): void {
        const id = defn.cohortAnnotationDefinitionId;
        // this.cancelFlag = false;
        // this.saveButtonEvent = false;
        // this.cdr.detectChanges();
        if (flag) {
            this.postSet.add(id);
        } else if (this.postSet.has(id)) {
            this.postSet.delete(id);
        }
    }

    get posting() {
        return this.postSet.size > 0;
    }

    get openEdit() {
        return this.state.editAnnotationManagerOpen.getValue();
    }

    set openEdit(value: boolean) {
        this.state.editAnnotationManagerOpen.next(value);
    }

    cancelFromChange () {
      //  this.cancelFlag = true;
        this.openEdit = false;
      //  this.saveButtonEvent = false;
    }


    saveFromChange() {
      //  this.saveButtonEvent = true;
        setTimeout(()=>{
           this.cancelFromChange ();
            },5000);
    }
}
