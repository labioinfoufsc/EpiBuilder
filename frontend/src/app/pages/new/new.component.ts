import { Component, OnInit } from "@angular/core";
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Database } from "../../models/Database";
import { DatabasesService } from "../../services/databases/databases.service";
import { EpitopesService } from "../../services/epitopes/epitopes.service";
import { LoginService } from "../../services/login/login.service";

@Component({
  selector: "app-new",
  standalone: false,
  templateUrl: "./new.component.html",
  styleUrls: ["./new.component.scss"],
})
export class NewComponent implements OnInit {
  myForm: FormGroup;
  messages: { category: string; text: string }[] = [];
  databases: Database[] = [];

  constructor(
    private fb: FormBuilder,
    private epitopesService: EpitopesService,
    private loginService: LoginService,
    private databasesService: DatabasesService
  ) {

    this.databasesService.getDatabases().subscribe((databases) => {
      this.databases = databases;
    });

    this.myForm = this.fb.group({
      runName: 'epibuilder-task',
      file: [null],
      action: 'predict',
      bepipredThreshold: 0.1512,
      minEpitopeLength: 10,
      maxEpitopeLength: 30,
      epitopeSearch: 'no_search',
      optional: {
        enableFeature: false,
        threshold: 0.5
      },
      minIdentityCutoff: [90],
      minCoverCutoff: [90],
      wordSize: [4],
      proteomes: this.fb.array([])
    });
  }
  ngOnInit(): void {
    this.addProteome();
  }

  trackByIndex(index: number, obj: any): any {
    return index;
  }

  getFormGroup(control: AbstractControl | null): FormGroup {
    return control as FormGroup;
  }

  resetForm(): void {
    this.myForm.reset({
      runName: 'epibuilder-task',
      file: [null],
      action: 'predict',
      bepipredThreshold: 0.1512,
      minEpitopeLength: 10,
      maxEpitopeLength: 30,
      epitopeSearch: 'no_search',
      optional: {
        enableFeature: false,
        threshold: 0.5
      },
      minIdentityCutoff: [90],
      minCoverCutoff: [90],
      wordSize: [4],
      proteomes: this.fb.array([])
    });

    const fileInput = document.getElementById('fileToProcess') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }

    const proteomes = this.myForm.get('proteomes') as FormArray;
    if (proteomes) {
      while (proteomes.length) {
        proteomes.removeAt(0);
      }
    }
  }

  onDBFileChange(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Você pode realizar algumas validações no arquivo, por exemplo:
      if (file.type !== 'application/x-fasta' && !file.name.endsWith('.fasta')) {
        console.error('Arquivo não suportado. Por favor, selecione um arquivo FASTA.');
        return;
      }

      this.proteomes.at(index).get('selectDBFile')?.setValue(file);

    }
  }


  addProteome(): void {
    const proteomeGroup = this.fb.group({
      selectDB: ['database'],               // Campo para selecionar a base de dados
      selectDBFile: [''],           // Campo para selecionar o arquivo da base de dados
      proteomeAlias: ['', Validators.required], // Campo para alias do proteoma
    });

    this.proteomes.push(proteomeGroup);
  }

  removeProteome(index: number): void {
    if (this.proteomes.length > 0) {
      this.proteomes.removeAt(index);
    }
  }

  showMessage(message: { category: string; text: string }): void {
    this.messages.push(message);
    setTimeout(() => {
      this.messages = this.messages.filter((msg) => msg !== message);
    }, 2000);
  }

  onSubmit(): void {
    const messages: { category: string; text: string }[] = [];
    const fileRegex = /.+/;

    if (
      !this.myForm.get("runName")?.value ||
      !this.myForm.get("file")?.value ||
      !fileRegex.test(this.myForm.get("file")?.value || "")
    ) {
      messages.push({
        category: "danger",
        text: "Error: runName and file are required and cannot be empty.",
      });
    }

    const epitopeSearch = this.myForm.get("epitopeSearch")?.value;
    if (epitopeSearch === "BLAST_search") {
      const proteomesArray = this.proteomes;
      proteomesArray.controls.forEach((control, idx) => {
        const alias = control.get('proteomeAlias')?.value;
        if (!alias) {
          messages.push({
            category: "danger",
            text: `Error: Proteome ${idx + 1} alias is required when epitopeSearch is BLAST_search.`,
          });
        }
      });
    }

    if (messages.length > 0) {
      messages.forEach(msg => this.showMessage(msg));
      return;
    }

    const fileInput = this.myForm.get("file")?.value;
    const fastaFile: File =
      fileInput instanceof File ? fileInput : fileInput?.files?.[0];

    const epitopeTaskData: any = {
      runName: this.myForm.get("runName")?.value,
      action: this.myForm.get("action")?.value.toUpperCase(),
      bepipredThreshold: this.myForm.get("bepipredThreshold")?.value,
      minEpitopeLength: this.myForm.get("minEpitopeLength")?.value,
      maxEpitopeLength: this.myForm.get("maxEpitopeLength")?.value,
      epitopeSearch: this.myForm.get("epitopeSearch")?.value,
      optional: this.myForm.get("optional")?.value,
      epitopes: [],
      user: this.loginService.getUser(),
      minIdentityCutoff: [90],
      minCoverCutoff: [90],
      wordSize: [4],
      proteomes: this.myForm.get("proteomes")?.value.map((proteome: any) => ({
        selectDB: proteome.selectDB,
        selectDBFile: proteome.selectDBFile,
        proteomeAlias: proteome.proteomeAlias,
      })),
    };

    const formData = new FormData();
    formData.append(
      "data",
      new Blob([JSON.stringify(epitopeTaskData)], { type: "application/json" })
    );
    formData.append("file", fastaFile);

    this.epitopesService.submitForm(formData).subscribe({
      next: (response) => {
        console.log("Success response:", response);
        // Reset form to default values
        this.resetForm();

        this.showMessage({ category: "success", text: response.message });
        this.epitopesService.notifyTaskListChanged();
      },
      error: (error) => {
        console.error("Error response:", error);
        const serverMsg = JSON.stringify(error?.error || error);
        this.showMessage({ category: "danger", text: serverMsg });
      },
    });
  }

  /**
   * Handles file input change and validates the file extension.
   * If the file is valid, it updates the form with the file.
   *
   * @param event The file input change event.
   */
  onFileChange(event: any): void {
    const file = event.target.files[0];
    if (file) {
      const validExtensions = ["csv", "fasta", "fa", "faa", "fna"];
      const fileExtension = file.name.split('.').pop()?.toLowerCase();

      if (fileExtension && validExtensions.includes(fileExtension)) {
        this.myForm.patchValue({ file: file });
      } else {
        this.showMessage({
          category: 'danger',
          text: 'Invalid file type! Please upload a CSV or FASTA file.'
        });
        this.myForm.get('file')?.setValue(null);
        event.target.value = '';
      }
    }
  }

  /**
   * Retrieves the proteomes form array.
   */
  get proteomes(): FormArray {
    return this.myForm.get("proteomes") as FormArray;
  }

  /**
   * Closes the specified message from the messages array.
   *
   * @param message The message to be closed.
   */
  closeMessage(message: { category: string; text: string }): void {
    this.messages = this.messages.filter((msg) => msg !== message);
  }

  /**
   * Updates the bepipredThreshold value based on the slider input.
   *
   * @param event The slider input event.
   */
  updateThreshold(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.myForm.patchValue({
      bepipredThreshold: parseFloat(inputElement.value),
    });
  }

  /**
   * Syncs the bepipredThreshold slider with the form value, ensuring it remains between 0.0 and 1.0.
   *
   * @param event The slider input event.
   */
  syncSlider(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    let value = parseFloat(inputElement.value);
    if (value < 0.0) value = 0.0;
    if (value > 1.0) value = 1.0;
    this.myForm.patchValue({ bepipredThreshold: value });
  }


  /**
   * Updates the minimum epitope length value.
   *
   * @param event The input event.
   */
  updateMinEpitopeLength(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.myForm.get("minEpitopeLength")?.setValue(value);
  }

  /**
   * Syncs the minimum epitope length value based on the input event.
   *
   * @param event The input event.
   */
  syncMinEpitopeLength(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.myForm.get("minEpitopeLength")?.setValue(value);
  }

  /**
   * Resets the minimum epitope length to the default value of 10.
   */
  resetMinEpitopeLength(): void {
    this.myForm.get("minEpitopeLength")?.setValue(10);
  }

  /**
   * Updates the maximum epitope length value.
   *
   * @param event The input event.
   */
  updateMaxEpitopeLength(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.myForm.get("maxEpitopeLength")?.setValue(value);
  }

  /**
   * Syncs the maximum epitope length value based on the input event.
   *
   * @param event The input event.
   */
  syncMaxEpitopeLength(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.myForm.get("maxEpitopeLength")?.setValue(value);
  }

  /**
   * Resets the maximum epitope length to the default value of 30.
   */
  resetMaxEpitopeLength(): void {
    this.myForm.get("maxEpitopeLength")?.setValue(30);
  }

  /**
   * Resets the bepipredThreshold to the default value of 0.1512.
   */
  resetThreshold(): void {
    this.myForm.get("bepipredThreshold")?.setValue(0.1512);
  }

  /**
   * Determines if the current action is epitope analysis.
   */
  isEpitopeAnalysis(): boolean {
    return this.myForm.get("action")?.value === "analysis";
  }
}
