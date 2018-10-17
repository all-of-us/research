import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

export interface Breadcrumb {
  label: string;
  type: string;
  url: string;
}
@Component({
  selector: 'app-breadcrumb',
  templateUrl: './component.html',
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    './component.css']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  subscription: Subscription;
  breadcrumbs: Breadcrumb[];
  ROUTE_DATA_BREADCRUMB = 'breadcrumb';
  ROUTE_DATA_INTERMIEDIATE_BREADCRUMB = 'intermediateBreadcrumb';
  constructor(
      private activatedRoute: ActivatedRoute,
      private router: Router) {}

  /**
   * Generate a breadcrumb using the default label and url. Uses the route's
   * paramMap to do any necessary variable replacement. For example, if we
   * have a label value of ':wsid' as defined in a route's breadcrumb, we can
   * do substitution with the 'wsid' value in the route's paramMap.
   */
  private static makeBreadcrumb(label: string,
                                type: string,
                                url: string,
                                route: ActivatedRoute): Breadcrumb {
    let newLabel = label;
    // Perform variable substitution in label only if needed.
    if (newLabel.indexOf(':') >= 0) {
      const paramMap = route.snapshot.paramMap;
      for (const k of paramMap.keys) {
        newLabel = newLabel.replace(':' + k, paramMap.get(k));
      }
    }
    return {
      label: newLabel,
      type: type,
      url: url
    };
  }

  ngOnInit() {
    this.breadcrumbs = this.buildBreadcrumbs(this.activatedRoute.root);

    this.subscription = this.router.events.filter(event => event instanceof NavigationEnd)
      .subscribe(event => {
        this.breadcrumbs = this.filterBreadcrumbs(this.buildBreadcrumbs(this.activatedRoute.root));
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  /**
   * Returns array of Breadcrumb objects that represent the breadcrumb trail.
   * Derived from current route in conjunction with the overall route structure.
   */
  private buildBreadcrumbs(route: ActivatedRoute,
                           url: string = '',
                           breadcrumbs: Breadcrumb[] = []): Array<Breadcrumb> {
    const children: ActivatedRoute[] = route.children;
    if (children.length === 0) {
      return breadcrumbs;
    }
    for (const child of children) {
      if ((!child.snapshot.data.hasOwnProperty(this.ROUTE_DATA_BREADCRUMB))
          && (!child.snapshot.data.hasOwnProperty(this.ROUTE_DATA_INTERMIEDIATE_BREADCRUMB))) {
        return this.buildBreadcrumbs(child, url, breadcrumbs);
      }
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');
      if (routeURL.length > 0) {
        url += `/${routeURL}`;
      }

      let label;
      let breadcrumbType;
      if (child.snapshot.data[this.ROUTE_DATA_BREADCRUMB] != null) {
        label = child.snapshot.data[this.ROUTE_DATA_BREADCRUMB];
        breadcrumbType = this.ROUTE_DATA_BREADCRUMB;
      } else {
        label = child.snapshot.data[this.ROUTE_DATA_INTERMIEDIATE_BREADCRUMB];
        breadcrumbType = this.ROUTE_DATA_INTERMIEDIATE_BREADCRUMB;
      }

      if (label === 'Param: Workspace Name') {
        label = child.snapshot.data['workspace'].name;
      }
      if (label === 'Param: Cohort Name') {
        label = child.snapshot.data['cohort'].name;
      }
      if (label === 'Param: Concept Set Name') {
        label = child.snapshot.data['conceptSet'].name;
      }
      // Prevent processing children with duplicate urls
      if (!breadcrumbs.some(b => b.url === url)) {
        const breadcrumb = BreadcrumbComponent.makeBreadcrumb(label, breadcrumbType, url, child);
        breadcrumbs.push(breadcrumb);
      }
      return this.buildBreadcrumbs(child, url, breadcrumbs);
    }
  }

  /**
   * Filters an array of Breadcrumbs so that the last element is never an intermediateBreadcrumb
   * This ensures that breadcrumb headers are displayed correctly while still tracking
   * intermediate pages.
   */
  private filterBreadcrumbs(breadcrumbs: Breadcrumb[]): Array<Breadcrumb> {
    let last = breadcrumbs[breadcrumbs.length - 1];
    while ((breadcrumbs.length > 1) && (last.type === this.ROUTE_DATA_INTERMIEDIATE_BREADCRUMB)) {
      breadcrumbs.pop();
      last = breadcrumbs[breadcrumbs.length - 1];
    }
    return breadcrumbs;
  }

}
