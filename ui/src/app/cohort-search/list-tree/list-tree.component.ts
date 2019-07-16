import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {ListNodeComponent} from 'app/cohort-search/list-node/list-node.component';
import {DomainType} from 'generated';

/*
 * The TreeComponent bootstraps the criteria tree; it has no display except for
 * a list of children (and a loading spinner), and does not defer loading those
 * children until "expanded" - expansion is basically its default state.
 */
@Component({
  selector: 'crit-list-tree',
  templateUrl: './list-tree.component.html',
  styleUrls: ['./list-tree.component.css']
})
export class ListTreeComponent extends ListNodeComponent implements OnInit, OnChanges {
  @Input() back: Function;
  ingredients: any;

  ngOnInit() {
    super.ngOnInit();
    setTimeout(() => super.loadChildren(true));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.node && !changes.node.firstChange) {
      // reload children from api when switching between standard and source trees
      super.loadChildren(true);
    }
  }

  get showSearch() {
    return this.node.domainId !== DomainType[DomainType.PERSON]
      && this.node.domainId !== DomainType.VISIT;
  }

  get showHeader() {
    return this.node.domainId !== DomainType.PHYSICALMEASUREMENT
      && this.node.domainId !== DomainType.SURVEY
      && this.node.domainId !== DomainType.VISIT;
  }

  get isEmpty() {
    return !this.loading && (this.empty || this.error);
  }
}
