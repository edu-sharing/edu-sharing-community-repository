import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LtiComponent } from './lti.component';

describe('LtiComponent', () => {
  let component: LtiComponent;
  let fixture: ComponentFixture<LtiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LtiComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LtiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
