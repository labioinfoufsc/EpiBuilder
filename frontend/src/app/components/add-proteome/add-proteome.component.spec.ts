import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddProteomeComponent } from './add-proteome.component';

describe('AddProteomeComponent', () => {
  let component: AddProteomeComponent;
  let fixture: ComponentFixture<AddProteomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AddProteomeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddProteomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
