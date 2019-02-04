import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from 'testing/test-helpers';

import {ServerConfigService} from 'app/services/server-config.service';

import {SignedInComponent} from 'app/views/signed-in/component';
import {StigmatizationPageComponent} from 'app/views/stigmatization-page/component';

describe('StigmatizationPageComponent', () => {
  let fixture: ComponentFixture<StigmatizationPageComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [
        SignedInComponent,
        StigmatizationPageComponent,
      ],
      providers: [
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(StigmatizationPageComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
