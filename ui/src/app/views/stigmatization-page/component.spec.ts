import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  signedInDependencies,
  updateAndTick
} from 'testing/test-helpers';

import {ServerConfigService} from 'app/services/server-config.service';

import {StigmatizationPageComponent} from 'app/views/stigmatization-page/component';

describe('StigmatizationPageComponent', () => {
  let fixture: ComponentFixture<StigmatizationPageComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: signedInDependencies.imports,
      declarations: [
        ...signedInDependencies.declarations,
        StigmatizationPageComponent,
      ],
      providers: [
        ...signedInDependencies.providers,
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
