import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';

import {updateAndTick} from 'testing/test-helpers';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {CohortsService} from 'generated/api/cohorts.service';
import {UserMetricsService} from 'generated/api/userMetrics.service';
import {WorkspacesService} from 'generated/api/workspaces.service';

import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {EditModalComponent} from 'app/views/edit-modal/component';
import {HomepageComponent} from 'app/views/homepage/component';
import {RecentWorkComponent} from 'app/views/recent-work/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {ResourceCardComponent} from 'app/views/resource-card/component';

import {LeftScrollComponent} from 'app/icons/left-scroll/component';
import {RightScrollComponent} from 'app/icons/right-scroll/component';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ReactiveFormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        HomepageComponent,
        RecentWorkComponent,
        LeftScrollComponent,
        RightScrollComponent,
        ResourceCardComponent,
        ConfirmDeleteModalComponent,
        RenameModalComponent,
        EditModalComponent,
      ],
      providers: [
        {provide: CohortsService},
        {provide: ProfileService, useValue: new ProfileServiceStub()},
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {provide: WorkspacesService },
        {provide: UserMetricsService, useValue: new UserMetricsServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(HomepageComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
