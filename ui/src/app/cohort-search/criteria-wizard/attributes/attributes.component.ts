import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  Input,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import {fromJS, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../../redux';
import {AttributesDirective} from './attributes.directive';
import {AttributeFormComponent} from './attributes.interface';

import {Attribute} from 'generated';

import {DummyComponent} from './dummy.component';


@Component({
  selector: 'crit-attributes',
  template: `
    <div [style.margin]="'0 1rem 0 1.25rem'">
      <ng-template critAttrFormHost></ng-template>
    </div>
  `
})
export class AttributesComponent implements AfterViewInit, OnDestroy {
  @Input() node: Map<any, any>;
  @ViewChild(AttributesDirective) attrFormHost: AttributesDirective;
  private subscription: Subscription;

  constructor(
    private actions: CohortSearchActions,
    private resolver: ComponentFactoryResolver,
  ) {}

  ngAfterViewInit() {
    // This setTimeout works around an Angular dev mode bug; we have to wait a
    // tick or the second change detection pass will complain
    setTimeout(_ => this.createForm());
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  createForm() {
    /*
     * TODO attribute selection logic here to determine which component to
     * instantiate based on type of selected criteria instead of just passing
     * in DummyComponent
     */
    const component = DummyComponent;
    const factory = this.resolver.resolveComponentFactory(component);

    this.attrFormHost.container.clear();
    const form = this.attrFormHost.container.createComponent(factory);

    const [cancel$, submit$] = form.instance.attribute.partition(value => value === null);

    this.subscription = cancel$.subscribe(v => {
      this.cleanup();
      this.createForm();
    });

    this.subscription.add(submit$.subscribe(arg => {
      const attr = fromJS(arg);
      const parameterId = `param${attr.hashCode()}`;
      const param = this.node
        .set('attribute', attr)
        .set('parameterId', parameterId);
      this.actions.addParameter(param);
      this.cleanup();
      this.createForm();
    }));
  }

  cleanup() {
    this.actions.clearWizardFocus();
    this.attrFormHost.container.clear();
    this.subscription.unsubscribe();
  }
}
