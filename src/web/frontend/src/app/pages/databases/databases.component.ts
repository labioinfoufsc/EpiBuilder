import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Modal } from 'bootstrap';
import { saveAs } from 'file-saver';
import { Database } from '../../models/Database';
import { DatabasesService } from '../../services/databases/databases.service';

@Component({
  selector: 'app-databases',
  standalone: false,
  templateUrl: './databases.component.html',
  styleUrls: ['./databases.component.scss']
})
export class DatabasesComponent implements OnInit, OnDestroy {
  newDatabase?: Database;
  selectedFile!: File;
  files: Database[] = [];
  columns: string[] = ['Database', 'Creation Date', 'Number of Sequences', 'Delete', 'Download'];
  alertMessage: string | null = null;
  alertType: "success" | "danger" | null = null;
  fileToDelete: Database | null = null;
  databaseAlias: string = '';

  @ViewChild('fileInput') fileInput?: ElementRef;
  @ViewChild("deleteModal") deleteModal!: ElementRef;
  private deleteModalInstance!: Modal;
  isLoading = false;

  constructor(private databasesService: DatabasesService) { }

  ngOnInit(): void {
    this.loadDatabases();
  }

  ngOnDestroy(): void {
  }

  // --- Common ---
  downloadFile(file: Database): void {
    this.databasesService.download(file.fileName).subscribe({
      next: (blob: Blob) => saveAs(blob, file.fileName),
      error: (err) => {
        console.error('Error while downloading file:', err);
        this.showAlert("Error while downloading file.", "danger");
      }
    });
  }

  onFileChange(event: Event): void {
    const target = event.target as HTMLInputElement;

    if (target.files && target.files.length > 0) {
      this.selectedFile = target.files[0];

      const fileName = this.selectedFile.name;
      const lastDotIndex = fileName.lastIndexOf('.');

      const alias = lastDotIndex !== -1
        ? fileName.substring(0, lastDotIndex)
        : fileName;

      this.databaseAlias = alias.substring(0, 30);
    }
  }

  onSubmit(databaseForm: NgForm): void {
    this.isLoading = true;
    if (!this.selectedFile || !databaseForm.value.alias) {
      this.showAlert("Please select a file and provide an alias", "danger");
      this.isLoading = false;
      return;
    }
    const alias = databaseForm.value.alias.trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-zA-Z0-9_-]/g, '_')
      .toLowerCase();

    const databaseToUpload: Partial<Database> = {
      alias,
      fileName: this.selectedFile.name,
    };

    this.databasesService.uploadDatabase(this.selectedFile, databaseToUpload.alias!).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res) {
          this.showAlert("Database successfully uploaded! Starting BLAST index.", "success");
          this.loadDatabases();
          this.resetForm();
          databaseForm.resetForm();
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.showAlert("Error uploading database", "danger");
        console.error(err);
      }
    });
  }

  resetForm(): void {
    this.newDatabase = new Database();
    this.selectedFile = undefined!;
    this.alertMessage = null;
    this.databaseAlias = '';
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  deleteDatabase(): void {
    if (!this.fileToDelete) return;
    this.hideDeleteModal();
    const id = this.fileToDelete.id;
    this.databasesService.deleteDatabase(id).subscribe({
      next: () => {
        this.files = this.files.filter((file) => file.id !== id);
        this.showAlert("Database deleted successfully", "success");
      },
      error: (err: any) => {
        this.showAlert("Error deleting database", "danger");
        console.error(err);
      },
    });
  }
  confirmDelete(file: Database): void {
    this.fileToDelete = file;
    this.showDeleteModal();
  }

  showDeleteModal(): void {
    if (this.deleteModal) {
      this.deleteModalInstance = new Modal(this.deleteModal.nativeElement);
      this.deleteModalInstance.show();
    }
  }

  hideDeleteModal(): void {
    if (this.deleteModalInstance) {
      this.deleteModalInstance.hide();
    }
  }

  showAlert(message: string, type: "success" | "danger" | null): void {
    this.alertMessage = message;
    this.alertType = type;
    setTimeout(() => {
      this.alertMessage = null;
    }, 5000);
  }

  loadDatabases(): void {
    this.databasesService.getDatabases().subscribe({
      next: (data: Database[]) => this.files = data,
      error: () => this.showAlert("Error loading databases", "danger")
    });
  }
}
