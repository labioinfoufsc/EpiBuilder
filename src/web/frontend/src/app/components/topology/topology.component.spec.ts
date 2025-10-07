import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TopologyComponent } from './topology.component';

describe('TopologyComponent', () => {
  let component: TopologyComponent;
  let fixture: ComponentFixture<TopologyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TopologyComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TopologyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
