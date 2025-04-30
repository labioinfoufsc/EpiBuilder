import { ChangeDetectorRef, Component, OnInit } from "@angular/core";
import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";
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
export class NewComponent {
  myForm: FormGroup;
  messages: { category: string; text: string }[] = [];
  databases: Database[] = [];
  uploadedDBFiles: (File | undefined)[] = [];
  selectedFile: File | null = null;
  databasesLoaded: boolean = false;
  isLoading: boolean = false;
  sequenceCount: number | null = null;
  fileType: 'fasta' | 'csv' | null = null;

  constructor(
    private fb: FormBuilder,
    private epitopesService: EpitopesService,
    private loginService: LoginService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private databasesService: DatabasesService
  ) {
    this.myForm = this.fb.group({
      runName: 'epibuilder-task',
      file: [null],
      inputType: 'file',
      actionType: 'default',
      manualSequence: '',
      databaseFile: [''],
      bepipredThreshold: 0.1512,
      minEpitopeLength: 10,
      maxEpitopeLength: 30,
      doBlast: 'no_search',
      minIdentityCutoff: [90],
      minCoverCutoff: [90],
      wordSize: [4],
      proteomes: this.fb.array([]),
    });

    this.resetForm();
  }

  loadDatabases() {
    this.databasesService.getDatabases().subscribe({
      next: (databases) => {
        this.databases = databases;
        this.databasesLoaded = true;
      },
      error: (error) => {
        console.error('Failed to load databases', error);
        this.databasesLoaded = true; // Para evitar bloqueio eterno
      }
    });
  }

  trackByIndex(index: number, obj: any): any {
    return index;
  }

  getFormGroup(control: AbstractControl | null): FormGroup {
    return control as FormGroup;
  }

  resetForm(): void {
    this.myForm = this.fb.group({
      runName: 'epibuilder-task',
      file: [null],
      inputType: 'file',
      manualSequence: '',
      actionType: 'default',
      databaseFile: [''],
      bepipredThreshold: 0.1512,
      minEpitopeLength: 10,
      maxEpitopeLength: 30,
      doBlast: 'no_search',
      minIdentityCutoff: [90],
      minCoverCutoff: [90],
      wordSize: [4],
      proteomes: this.fb.array([]),
    });

    const fileInput = document.getElementById('fileToProcess') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }

    this.isLoading = false;
    this.uploadedDBFiles = [];
    this.selectedFile = null;
    this.epitopesService.notifyTaskListChanged();
    this.proteomes.clear();
    this.databasesLoaded = false;
    this.messages = [];
    this.loadDatabases();


    const proteomes = this.myForm.get('proteomes') as FormArray;
    if (proteomes) {
      while (proteomes.length) {
        proteomes.removeAt(0);
      }
    }

    this.addProteome();
  }

  onDBFileChange(event: any, index: number) {
    const file = event.target.files[0];
    if (file) {
      this.uploadedDBFiles[index] = file;
      const proteomeGroup = this.proteomes.at(index) as FormGroup;
      proteomeGroup.patchValue({
        fastaFile: file.name,
        sourceType: 'fasta_blast'
      });
    }
  }

  addProteome(): void {
    const proteomeGroup = this.fb.group({
      sourceType: ['database'],
      proteomeAlias: [''],
      databaseFile: [null, Validators.required], // Adicione validação
      fastaFile: [null]
    });

    this.proteomes.push(proteomeGroup);
    this.uploadedDBFiles.push(undefined); // Mantenha sincronizado com os proteomas
  }

  removeProteome(index: number): void {
    if (this.proteomes.length > 0) {
      this.proteomes.removeAt(index);
    }
  }

  // Método auxiliar para acessar os controles do FormArray
  get proteomes(): FormArray {
    return this.myForm.get("proteomes") as FormArray;
  }


  showMessage(message: { category: string; text: string }): void {
    this.messages.push(message);
    setTimeout(() => {
      this.messages = this.messages.filter((msg) => msg !== message);
    }, 2000);
  }


  onSubmit() {

    this.isLoading = true;

    if (!this.myForm.get('runName')?.value) {
      this.showMessage({ text: 'Run name is required.', category: 'danger' });
      this.isLoading = false;
      return;
    }

    if (!this.myForm.get('actionType')?.value) {
      this.showMessage({ text: 'Action type is required.', category: 'danger' });
      this.isLoading = false;
      return;
    }

    const inputType = this.myForm.get('inputType')?.value;
    if (inputType === 'file') {
      if (!this.selectedFile) {
        this.showMessage({ text: 'Input file is missing.', category: 'danger' });
        this.isLoading = false;
        return;
      }

    } else if (inputType === 'manual') {
      const seq = this.myForm.get('manualSequence')?.value;
      if (!seq || !seq.startsWith('>')) {
        console.error('Invalid sequence format.');
        return;
      }
    }



    // Criação do objeto `EpitopeTaskData` que será enviado como 'data'
    const taskData: any = {
      user: this.loginService.getUser(),
      runName: this.myForm.get('runName')?.value,
      actionType: this.myForm.get('actionType')?.value.toUpperCase(),
      doBlast: this.myForm.get('doBlast')?.value === 'no_search' ? false : true,
    };

    // Configuração dos parâmetros padrão ou personalizados
    if (this.myForm.get('actionType')?.value === 'default') {
      taskData.bepipredThreshold = 0.1512;
      taskData.minEpitopeLength = 10;
      taskData.maxEpitopeLength = 30;
    } else if (this.myForm.get('actionType')?.value === 'customized') {
      taskData.bepipredThreshold = this.myForm.get('bepipredThreshold')?.value;
      taskData.minEpitopeLength = this.myForm.get('minEpitopeLength')?.value;
      taskData.maxEpitopeLength = this.myForm.get('maxEpitopeLength')?.value;
    }

    const formData = new FormData();
    if (taskData.doBlast == true) {
      // Valida parâmetros do BLAST
      if (!this.validateBlastParameters()) {
        return;
      }

      taskData.blastMinIdentityCutoff = this.myForm.get('minIdentityCutoff')?.value;
      taskData.blastMinCoverCutoff = this.myForm.get('minCoverCutoff')?.value;
      taskData.blastWordSize = this.myForm.get('wordSize')?.value;

      // Processa proteomas
      const proteomesResult = this.processProteomes();
      if (proteomesResult === null) {
        this.showMessage({
          text: 'Please select at least one proteome for BLAST.',
          category: 'danger'
        });
        return;
      }

      taskData.proteomes = proteomesResult.proteomeMeta;

      // Adiciona arquivos ao formData
      proteomesResult?.proteomeFiles.forEach(file => {
        formData.append('proteomes', file, file.name);
      });
    }

    // Adiciona o objeto 'taskData' como um JSON Blob
    formData.append('data', new Blob([JSON.stringify(taskData)], { type: 'application/json' }));

    if (this.selectedFile) {
      formData.append('file', this.selectedFile, this.selectedFile.name);
    } else if (this.myForm.get('manualSequence')?.value) {
      const manualSequence = this.myForm.get('manualSequence')?.value;
      const fileFromManual = new File([manualSequence], 'manual_sequence.fasta', { type: 'text/plain' });
      formData.append('file', fileFromManual, fileFromManual.name);
    }


    // Envia a requisição
    this.epitopesService.submitForm(formData).subscribe({
      next: (success) => {
        this.showMessage({

          text: 'Task submitted successfully!',
          category: 'success'
        });

        this.resetForm();

      },
      error: (error) => {
        console.error('Submission failed:', error);

        if (error.toLowerCase().includes('Login expired')) {
          this.loginService.logout();
          this.router.navigate(["/"]);
        }

        this.isLoading = false;
        const errorMessage = error?.message || 'An unexpected error occurred.';
        this.showMessage({
          text: `Task submission failed. ${errorMessage}`,
          category: 'danger'
        });
      }
    });
  }


  private validateBlastParameters(): boolean {
    if (this.myForm.get('minIdentityCutoff')?.value < 0 || this.myForm.get('minIdentityCutoff')?.value > 100) {
      this.showMessage({ text: 'Minimum identity cutoff must be between 0 and 100.', category: 'danger' });
      this.isLoading = false;
      return false;
    }

    if (this.myForm.get('minCoverCutoff')?.value < 0 || this.myForm.get('minCoverCutoff')?.value > 100) {
      this.showMessage({ text: 'Minimum coverage cutoff must be between 0 and 100.', category: 'danger' });
      this.isLoading = false;
      return false;
    }

    if (this.myForm.get('wordSize')?.value < 0 || this.myForm.get('wordSize')?.value > 100) {
      this.showMessage({ text: 'Word size must be between 0 and 100.', category: 'danger' });
      this.isLoading = false;
      return false;
    }

    return true;
  }

  // Método para processar os proteomas e retornar os arquivos e metadados
  /* private processProteomes(): { proteomeFiles: File[], proteomeMeta: any[] } | null {
     const proteomeMeta: any[] = [];
     const proteomeFiles: File[] = [];
 
     for (let i = 0; i < this.proteomes.length; i++) {
       const proteomeGroup = this.proteomes.at(i) as FormGroup;
       const sourceType = proteomeGroup.get('sourceType')?.value;
       const alias = proteomeGroup.get('proteomeAlias')?.value;
 
       console.log(sourceType);
       if (sourceType === 'database') {
         const dbPath = proteomeGroup.get('databaseFile')?.value;
         console.log(dbPath);
         if (dbPath === null || dbPath === undefined) {
           this.showMessage({
             text: `Please select a proteome ${i + 1} database for BLAST`,
             category: 'danger'
           });
           return null;
         }
 
         const selectedDb = this.databases.find(db => db.absolutePath === dbPath);
         if (selectedDb) {
           proteomeMeta.push({
             sourceType: 'database',
             databaseFile: selectedDb.absolutePath,
             alias: alias || selectedDb.alias
           });
           console.log('Selected DB:', selectedDb);
         }
       } else if (sourceType === 'fasta_blast') {
         const file = this.uploadedDBFiles[i];
         if (!file) {
           this.showMessage({
             text: `Please upload a FASTA file for Proteome ${i + 1}`,
             category: 'danger'
           });
           return null;
         }
         if (!alias || alias.trim() === '') {
           this.showMessage({
             text: `Please provide an alias for Proteome ${i + 1}`,
             category: 'danger'
           });
           return null;
         }
         proteomeMeta.push({
           sourceType: 'fasta_blast',
           alias: alias || file.name,
           originalName: file.name
         });
         proteomeFiles.push(file);
       }
     }
 
     console.log('Values');
     console.log('Proteome files:', proteomeFiles.length);
     console.log('Proteome metadata:', proteomeMeta.length);
 
     if (proteomeFiles.length === 0) {
       this.showMessage({
         text: 'Please upload at least one proteome for BLAST.',
         category: 'danger'
       });
       return null;
     }
 
     if (proteomeMeta.length === 0) {
       this.showMessage({
         text: 'Please select at least one proteome for BLAST.',
         category: 'danger'
       });
       return null;
     }
 
     return { proteomeFiles, proteomeMeta };
   }*/

  private processProteomes(): { proteomeFiles: File[], proteomeMeta: any[] } | null {
    const proteomeMeta: any[] = [];
    const proteomeFiles: File[] = [];
    let hasValidProteome = false;

    for (let i = 0; i < this.proteomes.length; i++) {
      const proteomeGroup = this.proteomes.at(i) as FormGroup;

      // Verifica se o grupo tem dados válidos antes de processar
      if (this.isProteomeGroupValid(proteomeGroup)) {
        const sourceType = proteomeGroup.get('sourceType')?.value;
        const alias = proteomeGroup.get('proteomeAlias')?.value;

        if (sourceType === 'database') {
          const dbPath = proteomeGroup.get('databaseFile')?.value;
          const selectedDb = this.databases.find(db => db.absolutePath === dbPath);

          if (selectedDb) {
            proteomeMeta.push({
              sourceType: 'database',
              databaseFile: selectedDb.absolutePath,
              alias: alias || selectedDb.alias
            });
            hasValidProteome = true;
            this.isLoading = false;
          }
        } else if (sourceType === 'fasta_blast') {
          const file = this.uploadedDBFiles[i];
          proteomeMeta.push({
            sourceType: 'fasta_blast',
            alias: alias || file?.name,
            originalName: file?.name
          });
          if (file) {
            proteomeFiles.push(file);
          }
          hasValidProteome = true;
          this.isLoading = false;
        }
      }
    }

    if (!hasValidProteome) {
      // Mensagem genérica que cobre ambos os casos (database e FASTA)
      this.showMessage({
        text: 'Please configure at least one valid proteome (database or FASTA file) for BLAST.',
        category: 'danger'
      });
      this.isLoading = false;
      return null;
    }

    return { proteomeFiles, proteomeMeta };
  }

  // Novo método auxiliar para verificar validade do grupo
  private isProteomeGroupValid(group: FormGroup): boolean {
    const sourceType = group.get('sourceType')?.value;

    if (sourceType === 'database') {
      return !!group.get('databaseFile')?.value;
    } else if (sourceType === 'fasta_blast') {
      const index = this.proteomes.controls.indexOf(group);
      return !!this.uploadedDBFiles[index] && !!group.get('proteomeAlias')?.value?.trim();
    }

    return false;
  }

  logSelectedDb(index: number, value: string) {
    console.log(`Proteome ${index + 1} selected DB:`, value);
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
        this.selectedFile = file;
        this.myForm.patchValue({ fileToProcess: file });

        const extension = file.name.split('.').pop()?.toLowerCase();

        const reader = new FileReader();

        reader.onload = () => {
          const content = reader.result as string;

          if (['fasta', 'fa', 'faa', 'fna'].includes(extension || '')) {
            this.fileType = 'fasta';
            const lines = content.split('\n');
            const count = lines.filter(line => line.trim().startsWith('>')).length;
            this.sequenceCount = count;
            console.log('Number of sequences (lines starting with ">"):', count);

          } else if (extension === 'csv') {
            this.fileType = 'csv';
            const lines = content.trim().split('\n');
            const uniqueFirstColumn = new Set<string>();

            for (const line of lines) {
              const columns = line.split(',');
              if (columns.length > 0) {
                uniqueFirstColumn.add(columns[0].trim());
              }
            }

            this.sequenceCount = uniqueFirstColumn.size;
            console.log('Unique values in the 1st column:', uniqueFirstColumn);
          } else {
            this.fileType = null;
            this.sequenceCount = null;
            console.warn('Unsupported file extension.');
          }
        };

        reader.onerror = () => {
          console.error('Error reading the file:', reader.error);
          this.sequenceCount = null;
          this.fileType = null;
        };

        reader.readAsText(file);

      } else {
        this.showMessage({
          category: 'danger',
          text: 'Invalid file type! Please upload a CSV or FASTA file.'
        });
        this.selectedFile = null;
        this.myForm.get('fileToProcess')?.setValue(null);
        event.target.value = '';
      }
    }
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
   * Updates the minimum epitope length value.
   *
   * @param event The input event.
   */
  updateMinEpitopeLength(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.myForm.get("minEpitopeLength")?.setValue(value);
  }

  loadExampleFile(event: Event) {
    console.log('Loading example FASTA file')
    event.preventDefault(); // evita recarregar a página

    fetch('assets/example.fasta')
      .then(response => response.blob())
      .then(blob => {
        const file = new File([blob], 'example.fasta', { type: blob.type });
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);

        const fileInput = document.getElementById('fileToProcess') as HTMLInputElement;
        if (fileInput) {
          fileInput.files = dataTransfer.files;

          // Dispara o evento (change) manualmente
          fileInput.dispatchEvent(new Event('change'));
        }


        const reader = new FileReader();

        //Show the content file
        reader.onload = () => {
          const content = reader.result as string;
          console.log('Fasta file content:\n', content);
        };

        reader.onerror = () => {
          console.error('Error while reading file:', reader.error);
        };

        reader.readAsText(file);
      })
      .catch(error => {
        console.error('Error while loading example file:', error);
      });
  }

  loadExampleManual(event: Event): void {
    console.log('Loading example FASTA content into manual input');
    event.preventDefault(); // evita recarregar a página

    fetch('assets/example.fasta')
      .then(response => response.text())
      .then(text => {
        const manualControl = this.myForm.get('manualSequence');
        if (manualControl) {
          manualControl.setValue(text);
        }
      })
      .catch(error => {
        console.error('Error while loading example file:', error);
      });
  }



  createFastaFile() {
    const content = this.myForm.get('manualSequence')?.value;
    if (!content) {
      console.error('Empty sequence');
      return;
    }

    const blob = new Blob([content], { type: 'text/plain' });
    const file = new File([blob], 'manual.fasta', { type: 'text/plain' });

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);

    const fileInput = document.getElementById('fileToProcess') as HTMLInputElement;
    if (fileInput) {
      fileInput.files = dataTransfer.files;
      fileInput.dispatchEvent(new Event('change'));
    }

    const reader = new FileReader();
    reader.onload = () => {
      const fasta = reader.result as string;
      console.log('Fasta file content:\n', fasta);
      const lines = fasta.split('\n');
      const count = lines.filter(line => line.startsWith('>')).length;
      this.sequenceCount = count;
      this.fileType = 'fasta';
    };

    reader.onerror = () => {
      console.error('Error reading generated FASTA file:', reader.error);
    };

    reader.readAsText(file);
    this.selectedFile = file;
  }

}
