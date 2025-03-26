import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RealtimeExecutionsComponent } from './realtime-executions.component';

describe('RealtimeExecutionsComponent', () => {
  let component: RealtimeExecutionsComponent;
  let fixture: ComponentFixture<RealtimeExecutionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [RealtimeExecutionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RealtimeExecutionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
