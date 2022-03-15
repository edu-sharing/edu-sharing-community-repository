import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LtiAdminComponent } from './lti-admin.component';

describe('LtiAdminComponent', () => {
  let component: LtiAdminComponent;
  let fixture: ComponentFixture<LtiAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LtiAdminComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LtiAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
