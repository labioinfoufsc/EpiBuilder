import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NewComponent } from './new.component';

describe('NewComponent', () => {
  let component: NewComponent;
  let fixture: ComponentFixture<NewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [NewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

      it('should create', () => {
        expect(component).toBeTruthy();
      });

    it('should default to BepiPred', () => {
      expect(component.myForm.get('predictionType')?.value)
        .toBe('bepipred');
    });

    it('should update the threshold when EpiDope is selected', () => {
      component.myForm.get('predictionType')?.setValue('epidope');

      component.onPredictionTypeChange();

      expect(component.myForm.get('bepipredThreshold')?.value)
        .toBe(0.818);
    });
});
