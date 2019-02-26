import {DOCUMENT} from '@angular/common';
import {Component, Inject, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {
  ActivatedRoute,
  Event as RouterEvent,
  NavigationEnd,
  NavigationError,
  Router,
} from '@angular/router';


import {cookiesEnabled} from 'app/utils';
import {queryParamsStore, routeConfigDataStore, urlParamsStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';

export const overriddenUrlKey = 'allOfUsApiUrlOverride';
export const overriddenPublicUrlKey = 'publicApiUrlOverride';


@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit {
  isSignedIn = false;
  initialSpinner = true;
  cookiesEnabled = true;
  overriddenUrl: string = null;
  private baseTitle: string;
  overriddenPublicUrl: string = null;

  constructor(
    @Inject(DOCUMENT) private doc: any,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private titleService: Title
  ) {}

  ngOnInit(): void {
    this.cookiesEnabled = cookiesEnabled();
    // Local storage breaks if cookies are not enabled
    if (this.cookiesEnabled) {
      try {
        this.overriddenUrl = localStorage.getItem(overriddenUrlKey);
        this.overriddenPublicUrl = localStorage.getItem(overriddenPublicUrlKey);
        window['setAllOfUsApiUrl'] = (url: string) => {
          if (url) {
            if (!url.match(/^https?:[/][/][a-z0-9.:-]+$/)) {
              throw new Error('URL should be of the form "http[s]://host.example.com[:port]"');
            }
            this.overriddenUrl = url;
            localStorage.setItem(overriddenUrlKey, url);
          } else {
            this.overriddenUrl = null;
            localStorage.removeItem(overriddenUrlKey);
          }
          window.location.reload();
        };
        window['setPublicApiUrl'] = (url: string) => {
          if (url) {
            if (!url.match(/^https?:[/][/][a-z0-9.:-]+$/)) {
              throw new Error('URL should be of the form "http[s]://host.example.com[:port]"');
            }
            this.overriddenPublicUrl = url;
            localStorage.setItem(overriddenPublicUrlKey, url);
          } else {
            this.overriddenPublicUrl = null;
            localStorage.removeItem(overriddenPublicUrlKey);
          }
          window.location.reload();
        };
        console.log('To override the API URLs, try:\n' +
          'setAllOfUsApiUrl(\'https://host.example.com:1234\')\n' +
          'setPublicApiUrl(\'https://host.example.com:5678\')');
      } catch (err) {
        console.log('Error setting urls: ' + err);
      }
    }

    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.
    this.baseTitle = this.titleService.getTitle();
    if (environment.displayTag) {
      this.baseTitle = `[${environment.displayTag}] ${this.baseTitle}`;
      this.titleService.setTitle(this.baseTitle);
    }

    this.router.events.subscribe((e: RouterEvent) => {
      this.setTitleFromRoute(e);
      if (e instanceof NavigationEnd || e instanceof NavigationError) {
        // Terminal navigation events.
        this.initialSpinner = false;
      }
      if (e instanceof NavigationEnd) {
        const {snapshot: {params, queryParams, routeConfig}} = this.getLeafRoute();
        urlParamsStore.next(params);
        queryParamsStore.next(queryParams);
        routeConfigDataStore.next(routeConfig.data);
      }
    });

    this.setGTagManager();
    // tcell uses local storage - don't load if not supported
    if (this.cookiesEnabled) {
      this.setTCellAgent();
    }
  }

  getLeafRoute(route = this.activatedRoute) {
    return route.firstChild ? this.getLeafRoute(route.firstChild) : route;
  }

  /**
   * Uses the title service to set the page title after nagivation events
   */
  private setTitleFromRoute(event: RouterEvent): void {
    if (event instanceof NavigationEnd) {

      let currentRoute = this.activatedRoute;
      while (currentRoute.firstChild) {
        currentRoute = currentRoute.firstChild;
      }
      if (currentRoute.outlet === 'primary') {
        currentRoute.data.subscribe(value =>
            this.titleService.setTitle(`${value.title} | ${this.baseTitle}`));
      }
    }
  }

  /**
   * Setting the Google Analytics ID here.
   * This first injects Google's gtag script via iife, then secondarily defines
   * the global gtag function.
   */
  private setGTagManager() {
    const s = this.doc.createElement('script');
    const ua = window.navigator.userAgent;
    console.log(environment.gaId);
    console.log(ua);
    s.type = 'text/javascript';
    s.innerHTML =
      '(function(w,d,s,l,i){' +
        'w[l]=w[l]||[];' +
        'var f=d.getElementsByTagName(s)[0];' +
        'var j=d.createElement(s);' +
        'var dl=l!=\'dataLayer\'?\'&l=\'+l:\'\';' +
        'j.async=true;' +
        'j.src=\'https://www.googletagmanager.com/gtag/js?id=\'+i+dl;' +
        'f.parentNode.insertBefore(j,f);' +
      '})' +
      '(window, document, \'script\', \'dataLayer\', \'' + environment.gaId + '\');' +
      'window.dataLayer = window.dataLayer || [];' +
      'function gtag(){dataLayer.push(arguments);}' +
      'gtag(\'js\', new Date());' +
      'gtag(\'set\', \'user_agent\', \'' + window.navigator.userAgent.slice(0, 100) + '\');' +
      'gtag(\'config\', \'' + environment.gaId + '\', {\'custom_map\': ' +
      '{\'dimension1\': \'user_agent\'}});';
    const head = this.doc.getElementsByTagName('head')[0];
    head.appendChild(s);
  }

  private setTCellAgent(): void {
    const s = this.doc.createElement('script');
    s.type = 'text/javascript';
    s.src = 'https://jsagent.tcell.io/tcellagent.min.js';
    s.setAttribute('tcellappid', environment.tcellappid);
    s.setAttribute('tcellapikey', environment.tcellapikey);
    const head = this.doc.getElementsByTagName('head')[0];
    head.appendChild(s);
  }

}
