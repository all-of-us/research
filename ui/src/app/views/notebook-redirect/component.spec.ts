import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {Response, ResponseOptions} from '@angular/http';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, convertToParamMap} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {AsyncSubject} from 'rxjs/AsyncSubject';
import {Observable} from 'rxjs/Observable';

import {NotebookComponent} from 'app/icons/notebook/component';
import {Kernels} from 'app/utils/notebook-kernels';
import {NotebookRedirectComponent} from 'app/views/notebook-redirect/component';
import {TopBoxComponent} from 'app/views/top-box/component';
import {environment} from 'environments/environment';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {JupyterServiceStub} from 'testing/stubs/jupyter-service-stub';
import {LeoClusterServiceStub} from 'testing/stubs/leo-cluster-service-stub';
import {NotebooksServiceStub} from 'testing/stubs/notebooks-service-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {simulateClick, updateAndTick} from 'testing/test-helpers';

import {
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterService,
  ClusterStatus
} from 'generated';
import {
  ClusterService as LeoClusterService,
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

class BlockingNotebooksStub extends NotebooksServiceStub {
  private blocker = new AsyncSubject<null>();

  public block() {
    this.blocker = new AsyncSubject<null>();
  }

  public release() {
    this.blocker.next(null);
    this.blocker.complete();
  }

  public setCookieWithHttpInfo(
      googleProject: string, clusterName: string,
      extraHttpRequestParams?: any): Observable<Response> {
    return this.blocker.flatMap(() => {
      return super.setCookieWithHttpInfo(
        googleProject, clusterName, extraHttpRequestParams);
    });
  }
}

class BlockingClusterStub extends ClusterServiceStub {
  private blocker = new AsyncSubject<null>();

  public block() {
    this.blocker = new AsyncSubject<null>();
  }

  public release() {
    this.blocker.next(null);
    this.blocker.complete();
  }

  localize(projectName: string, clusterName: string,
      req: ClusterLocalizeRequest, extraHttpRequestParams?: any): Observable<{}> {
    return this.blocker.flatMap(() => {
      return super.localize(projectName, clusterName, req, extraHttpRequestParams);
    });
  }
}

describe('NotebookRedirectComponent', () => {
  let fixture: ComponentFixture<NotebookRedirectComponent>;
  let blockingClusterStub: BlockingClusterStub;
  let blockingNotebooksStub: BlockingNotebooksStub;

  beforeEach(fakeAsync(() => {
    blockingClusterStub = new BlockingClusterStub();
    blockingClusterStub.cluster.status = ClusterStatus.Creating;
    blockingNotebooksStub = new BlockingNotebooksStub();

    TestBed.configureTestingModule({
      declarations: [
        NotebookComponent,
        NotebookRedirectComponent,
        TopBoxComponent
      ],
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      providers: [
        { provide: ClusterService, useFactory: () => blockingClusterStub },
        { provide: LeoClusterService, useValue: new LeoClusterServiceStub() },
        { provide: NotebooksService, useFactory: () => blockingNotebooksStub },
        { provide: JupyterService, useValue: new JupyterServiceStub() },
        { provide: ActivatedRoute, useValue: {
          snapshot: {
            params: {
              'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
              'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
              'nbName': 'blah%20blah'
            },
            queryParams: {
              'kernelType': Kernels.R,
              'creating': true
            },
            queryParamMap: convertToParamMap({
              'kernelType': Kernels.R,
              'creating': true
            })
          }
        }},
      ]}).compileComponents().then(() => {
      fixture = TestBed.createComponent(NotebookRedirectComponent);
      spyOn(window.history, 'replaceState').and.stub();
      blockingClusterStub.release();
      blockingNotebooksStub.release();
    });
  }));

  function spinner() {
    return fixture.debugElement.query(By.css('.spinner-sm'))
      .nativeElement;
  }

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentRef).toBeTruthy();
    // Tears down the retrying subscription.
    fixture.destroy();
  }));

  it('should redirect', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentInstance.leoUrl).toBeFalsy();
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    tick(100000);
    updateAndTick(fixture);
    expect(fixture.componentInstance.leoUrl).toMatch(environment.leoApiUrl);
  }));

  it('should set spinner "Initializing" until ready', fakeAsync(() => {
    updateAndTick(fixture);
    fixture.detectChanges();
    const initBox = fixture.debugElement.queryAll(By.css('#initializing'))[0];
    expect(initBox.children[0].nativeElement === spinner());

    tick(10000);
    fixture.detectChanges();
    expect(initBox.children[0].nativeElement).toBe(spinner());

    blockingClusterStub.cluster.status = ClusterStatus.Running;
    tick(100000);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(1);
  }));

  it('should display resuming message until resumed', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Stopped;
    updateAndTick(fixture);
    fixture.detectChanges();
    const initBox = fixture.debugElement.queryAll(By.css('#initializing'))[0];
    expect(initBox.children[1].nativeElement.innerText)
        .toMatch('Resuming notebook server, may take up to 1 minute');

    tick(10000);
    fixture.detectChanges();
    expect(initBox.children[1].nativeElement.innerText)
        .toMatch('Resuming notebook server, may take up to 1 minute');

    blockingClusterStub.cluster.status = ClusterStatus.Running;
    tick(100000);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(1);
  }));

  it('should set spinner to "Authenticating" while setting cookies', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    blockingNotebooksStub.block();
    updateAndTick(fixture);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('#authenticating'))[0]
        .children[0].nativeElement).toBe(spinner());

    blockingNotebooksStub.release();
    tick(100000);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(1);
  }));

  it('should set spinner to "Creating" while creating a new notebook', fakeAsync(() => {

    blockingClusterStub.cluster.status = ClusterStatus.Running;
    blockingClusterStub.block();
    updateAndTick(fixture);
    tick(1000);
    fixture.detectChanges();
    updateAndTick(fixture);
    tick(10000);
    expect(fixture.debugElement.queryAll(By.css('#creating'))[0]
        .children[0].nativeElement).toBe(spinner());

    blockingClusterStub.release();
    tick(1000);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(1);
  }));

  it('should set spinner to "Copying" while localizing', fakeAsync(() => {
    updateAndTick(fixture);
    fixture.componentInstance.notebookName = 'foo.ipynb';
    fixture.componentInstance.creating = false;
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    blockingClusterStub.block();
    tick(10000);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('#copying'))[0]
        .children[0].nativeElement).toBe(spinner());

    blockingClusterStub.release();
    tick(1000);
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(1);
  }));

  it('should display spinners to match progress status when creating a notebook', fakeAsync(() => {
    spyOn(window, 'setTimeout').and.stub();
    updateAndTick(fixture);
    fixture.componentInstance.progress = fixture.componentInstance.Progress.Initializing;
    updateAndTick(fixture);
    tick(1000);
    expect(fixture.debugElement.queryAll(By.css('#initializing'))[0]
        .children[0].nativeElement).toBe(spinner());

    fixture.componentInstance.progress = fixture.componentInstance.Progress.Authenticating;
    updateAndTick(fixture);
    tick(1000);
    expect(fixture.debugElement.queryAll(By.css('#authenticating'))[0]
        .children[0].nativeElement).toBe(spinner());

    fixture.componentInstance.progress = fixture.componentInstance.Progress.Creating;
    updateAndTick(fixture);
    tick(1000);
    expect(fixture.debugElement.queryAll(By.css('#creating'))[0]
        .children[0].nativeElement).toBe(spinner());

    fixture.componentInstance.progress = fixture.componentInstance.Progress.Redirecting;
    updateAndTick(fixture);
    tick(10000);
    expect(fixture.debugElement.queryAll(By.css('#redirecting'))[0]
        .children[0].nativeElement).toBe(spinner());

  }));

  it('should display spinners to match progress status when loading a notebook', fakeAsync(() => {
    spyOn(window, 'setTimeout').and.stub();
    updateAndTick(fixture);
    fixture.componentInstance.notebookName = 'foo.ipynb';
    fixture.componentInstance.creating = false;
    fixture.detectChanges();
    fixture.componentInstance.progress = fixture.componentInstance.Progress.Resuming;
    updateAndTick(fixture);
    expect(fixture.debugElement.queryAll(By.css('#initializing'))[0]
        .children[0].nativeElement).toBe(spinner());

    fixture.componentInstance.progress = fixture.componentInstance.Progress.Authenticating;
    updateAndTick(fixture);
    expect(fixture.debugElement.queryAll(By.css('#authenticating'))[0]
        .children[0].nativeElement).toBe(spinner());

    fixture.componentInstance.progress = fixture.componentInstance.Progress.Copying;
    updateAndTick(fixture);
    expect(fixture.debugElement.queryAll(By.css('#copying'))[0]
        .children[0].nativeElement).toBe(spinner());

    fixture.componentInstance.progress = fixture.componentInstance.Progress.Redirecting;
    updateAndTick(fixture);
    expect(fixture.debugElement.queryAll(By.css('#redirecting'))[0]
        .children[0].nativeElement).toBe(spinner());

  }));

  it('should escape notebook names', fakeAsync(() => {
    fixture.componentInstance.route.snapshot.params['nbName'] = '1%2B1.ipynb';
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    updateAndTick(fixture);
    fixture.detectChanges();
    tick(10000);
    expect(fixture.debugElement.queryAll(By.css('#leo-iframe'))[0]
      .properties['src']).toMatch(/1\+1.ipynb/);
  }));

  it('should properly display notebooks names', fakeAsync(() => {
    fixture.componentInstance.creating = false;
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    updateAndTick(fixture);
    fixture.detectChanges();
    tick(10000);
    expect(fixture.debugElement.queryAll(By.css('.notebooks-header'))[0].nativeElement.textContent)
      .toMatch('blah blah');
  }));

  it('should display iframe on redirect', fakeAsync(() => {
    blockingClusterStub.cluster.status = ClusterStatus.Running;
    updateAndTick(fixture);
    fixture.detectChanges();
    tick(10000);
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(1);
  }));


  it('should display notebook name if loading a notebook', fakeAsync(() => {
    spyOn(window, 'setTimeout').and.stub();
    updateAndTick(fixture);
    fixture.componentInstance.notebookName = 'foo.ipynb';
    fixture.componentInstance.creating = false;
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.notebooks-header'))[0].nativeElement.textContent)
      .toMatch('Loading Notebook \'foo.ipynb\'');
  }));

  it('should return to the notebooks list page if we cancel notebook creation', fakeAsync(() => {
    spyOn(window, 'setTimeout').and.stub();
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.queryAll(By.css('#cancelButton'))[0]);
    tick(10000);

    // expect notebook not to have loaded
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.i-frame')).length).toBe(0);
  }));
});
