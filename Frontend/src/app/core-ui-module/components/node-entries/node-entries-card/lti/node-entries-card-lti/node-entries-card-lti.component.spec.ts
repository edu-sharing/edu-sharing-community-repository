import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NodeEntriesCardLtiComponent } from './node-entries-card-lti.component';

describe('NodeEntriesCardLtiComponent', () => {
  let component: NodeEntriesCardLtiComponent;
  let fixture: ComponentFixture<NodeEntriesCardLtiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NodeEntriesCardLtiComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NodeEntriesCardLtiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
