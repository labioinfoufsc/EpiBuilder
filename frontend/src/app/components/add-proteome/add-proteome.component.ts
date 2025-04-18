import { Component, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Database } from '../../models/Database';
import { DatabasesService } from '../../services/databases/databases.service';

@Component({
  selector: 'add-proteome',
  standalone: false,
  templateUrl: './add-proteome.component.html',
  styleUrl: './add-proteome.component.scss',
})
export class AddProteomeComponent implements OnInit {
  myForm: FormGroup;
  dbs: Database[] = [];
  messages: { category: string; text: string }[] = [];

  constructor(
    private fb: FormBuilder,
    private databasesService: DatabasesService
  ) {
    this.databasesService.getDatabases().subscribe((data) => {
      this.dbs = data;
    });

    this.myForm = this.fb.group({
      proteomes: this.fb.array([]),
    });
  }
  ngOnInit(): void {
    this.addProteome();
  }

  get proteomes(): FormArray {
    return this.myForm.get('proteomes') as FormArray;
  }

  addProteome() {
    const proteomeGroup = this.fb.group({
      selectDB: new FormControl('', Validators.required),
      selectDBFile: new FormControl(''),
      proteomeAlias: new FormControl('', Validators.required),
    });
    this.proteomes.push(proteomeGroup);
  }

  removeProteome(index: number) {
    this.proteomes.removeAt(index);
  }

  onDBFileChange(event: any, index: number) {
    const file = event.target.files[0];
    if (file) {
      this.proteomes.at(index).get('selectDBFile')?.setValue(file.fileName);
      console.log(`File selected for proteome ${index + 1}:`, file.fileName);
    }
  }

  trackByIndex(index: number, obj: any): any {
    return index;
  }

  getFormGroup(control: AbstractControl | null): FormGroup {
    return control as FormGroup;
  }
}
