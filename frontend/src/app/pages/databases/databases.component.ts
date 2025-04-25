import { Component, ElementRef, ViewChild } from '@angular/core';
import { Modal } from 'bootstrap';
import { Database } from '../../models/Database';
import { DatabasesService } from '../../services/databases/databases.service';

@Component({
  selector: 'app-databases',
  standalone: false,
  templateUrl: './databases.component.html',
  styleUrls: ['./databases.component.scss']
})
export class DatabasesComponent {
  newDatabase?: Database;
  selectedFile!: File;
  files: Database[] = [];
  columns: string[] = ['Database', 'Creation Date', 'Action'];
  alertMessage: string | null = null;
  alertType: "success" | "danger" | null = null;
  fileToDelete: Database | null = null;
  @ViewChild('fileInput') fileInput?: ElementRef;

  @ViewChild("deleteModal") deleteModal!: ElementRef;
  private deleteModalInstance!: Modal;

  constructor(private databasesService: DatabasesService) { }

  ngOnInit(): void {
    this.loadDatabases();
  }

  onFileChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.selectedFile = target.files[0];
    }
  }

  onSubmit(): void {
    if (!this.selectedFile) {
      this.showAlert("Please select a file", "danger");
      return;
    }

    const originalName = this.selectedFile.name;
    const fileNameWithoutExtension = originalName.replace(/\.[^/.]+$/, "");
    const formattedAlias = fileNameWithoutExtension
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-zA-Z0-9_-]/g, '_')
      .toLowerCase();

    const databaseToUpload: Partial<Database> = {
      alias: formattedAlias,
      fileName: this.selectedFile.name,
    };

    this.databasesService.uploadDatabase(this.selectedFile, databaseToUpload.alias!).subscribe({
      next: (res) => {
        if (res) {
          this.showAlert("Database successfully uploaded!", "success");
          this.loadDatabases();
        }
      },
      error: (err) => {
        if (err.status) {
          this.showAlert(err.status, "danger");
        }
      }
    });
  }

  resetForm() {
    this.newDatabase = new Database();
    this.selectedFile = undefined!;
    this.alertMessage = null;
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
        this.showAlert(err, "danger");
      },
    });
  }

  confirmDelete(file: Database): void {
    this.fileToDelete = file;
    this.showDeleteModal();
  }

  showDeleteModal() {
    if (this.deleteModal) {
      this.deleteModalInstance = new Modal(this.deleteModal.nativeElement);
      this.deleteModalInstance.show();
    }
  }

  hideDeleteModal() {
    if (this.deleteModalInstance) {
      this.deleteModalInstance.hide();
    }
  }

  showAlert(message: string, type: "success" | "danger" | null) {
    this.alertMessage = message;
    this.alertType = type;

    setTimeout(() => {
      this.alertMessage = null;
    }, 5000);
  }



  loadDatabases(): void {
    this.databasesService.getDatabases().subscribe({
      next: (data: Database[]) => this.files = data,
      error: (err: any) => this.showAlert(err, "danger")
    });
  }


}
