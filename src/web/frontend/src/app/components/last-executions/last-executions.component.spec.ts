import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LastExecutionsComponent } from './last-executions.component';

describe('LastExecutionsComponent', () => {
  let component: LastExecutionsComponent;
  let fixture: ComponentFixture<LastExecutionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LastExecutionsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LastExecutionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
