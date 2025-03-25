import { TestBed } from '@angular/core/testing';

import { EpitopesService } from './epitopes.service';

describe('EpitopesService', () => {
  let service: EpitopesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EpitopesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
