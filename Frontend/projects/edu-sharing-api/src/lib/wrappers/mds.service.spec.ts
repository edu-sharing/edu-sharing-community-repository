import { TestBed } from '@angular/core/testing';

import { MdsService } from './mds.service';

describe('MdsService', () => {
  let service: MdsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MdsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
