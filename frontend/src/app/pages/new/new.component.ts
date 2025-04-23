import { Component } from "@angular/core";
import { FormArray, FormBuilder, FormGroup } from "@angular/forms";
import { EpitopesService } from "../../services/epitopes/epitopes.service";
import { LoginService } from "../../services/login/login.service";

@Component({
  selector: "app-new",
  standalone: false,
  templateUrl: "./new.component.html",
  styleUrls: ["./new.component.scss"],
})
export class NewComponent {
  myForm: FormGroup;
  messages: { category: string; text: string }[] = [];
  defaultFormValues = {
    runName: "epibuilder-task",
    file: null,
    action: "predict",
    bepipredThreshold: 0.1512,
    minEpitopeLength: 10,
    maxEpitopeLength: 30,
    epitopeSearch: "no_search",
    proteomes: [],
    optional: {
      enableFeature: false,
      threshold: 0.5,
    },
    algpredThreshold: 0.3,
    algPredPredictionModelType: "1",
    algPredDisplayMode: "1",
  };


  constructor(
    private fb: FormBuilder,
    private epitopesService: EpitopesService,
    private loginService: LoginService
  ) {
    this.myForm = this.fb.group({
      runName: ["epibuilder-task"],
      file: [null],
      action: ["predict"],
      bepipredThreshold: [0.1512],
      minEpitopeLength: [10],
      maxEpitopeLength: [30],
      epitopeSearch: ["no_search"],
      proteomes: this.fb.array([]),
      optional: this.fb.group({
        enableFeature: [false],
        threshold: [0.5],
      }),
      algpredThreshold: [0.3],
      algPredPredictionModelType: ["1"],
      algPredDisplayMode: ["1"],
    });
  }

  updateAlgpredThreshold(event: Event): void {
    const value = parseFloat((event.target as HTMLInputElement).value);
    this.myForm.get("algpredThreshold")?.setValue(value, { emitEvent: false });
  }

  syncAlgpredThreshold(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = parseFloat(input.value);
    if (value < 0) value = 0;
    if (value > 1) value = 1;
    this.myForm.get("algpredThreshold")?.setValue(value, { emitEvent: false });
  }

  resetAlgpredThreshold(): void {
    this.myForm.get("algpredThreshold")?.setValue(0.3, { emitEvent: false });
  }

  /**
   * Handles form submission and validates required fields.
   * If validation passes, it sends data to the Epitope service.
   */
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
      const proteomeAliases = ["proteome1", "proteome2", "proteome3"];
      for (const alias of proteomeAliases) {
        if (!this.myForm.get(alias)?.value) {
          messages.push({
            category: "danger",
            text: `Error: The field ${alias} is required when epitopeSearch is BLAST_search.`,
          });
        }
      }
    }

    if (messages.length > 0) {
      this.messages = messages;
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
      subcell: this.myForm.get("subcell")?.value,
      interpro: this.myForm.get("interpro")?.value,
      epitopeSearch: this.myForm.get("epitopeSearch")?.value,
      optional: this.myForm.get("optional")?.value,
      executionDate: new Date(),
      epitopes: [],
      user: this.loginService.getUser(),
      algpredThreshold: this.myForm.get("threshold")?.value,
      algPredPredictionModelType: this.myForm.get("model")?.value,
      algPredDisplayMode: this.myForm.get("display")?.value,
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
        this.myForm.reset(this.defaultFormValues);
        this.messages = [{ category: "success", text: response.message }];

        this.epitopesService.notifyTaskListChanged();
      },
      error: (error) => {
        console.error("Error response:", error);
        const serverMsg = error?.error?.message || "Failed to submit the form. Please try again.";
        this.messages = [{ category: "danger", text: serverMsg }];
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
      const fileExtension = file.name.split(".").pop()?.toLowerCase();

      if (fileExtension && validExtensions.includes(fileExtension)) {
        this.myForm.patchValue({ file: file });
      } else {
        alert("Invalid file type! Please upload a CSV or FASTA file.");
        event.target.value = "";
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
