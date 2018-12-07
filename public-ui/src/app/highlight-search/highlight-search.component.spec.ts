import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import { HighlightSearchComponent } from './highlight-search.component';

describe('HighlightSearchComponent', () => {
  let component: HighlightSearchComponent;
  let fixture: ComponentFixture<HighlightSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ HighlightSearchComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HighlightSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
