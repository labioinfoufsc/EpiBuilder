import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Modal } from 'bootstrap';
import { saveAs } from 'file-saver';
import { Subscription, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { Database } from '../../models/Database';
import { UniProtDownloadStatus } from '../../models/UniProtDownloadStatus';
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
  columns: string[] = ['Database', 'Creation Date', 'Number of Sequences', 'Action'];
  alertMessage: string | null = null;
  alertType: "success" | "danger" | null = null;
  fileToDelete: Database | null = null;
  databaseAlias: string = '';

  // State variables for the Asynchronous Download
  isDownloadInProgress: boolean = false;
  downloadMessage: string | null = null;
  private downloadSubscription: Subscription | undefined;

  @ViewChild('fileInput') fileInput?: ElementRef;
  @ViewChild("deleteModal") deleteModal!: ElementRef;
  private deleteModalInstance!: Modal;
  isLoading: boolean = false;

  constructor(private databasesService: DatabasesService) { }

  ngOnInit(): void {
    this.loadDatabases();
    this.checkInitialDownloadStatus();
  }

  ngOnDestroy(): void {
    if (this.downloadSubscription) {
      this.downloadSubscription.unsubscribe();
    }
  }

  checkInitialDownloadStatus(): void {
    this.databasesService.getUniProtDownloadStatus().subscribe((status: UniProtDownloadStatus) => {
      if (status.inProgress) {
        this.downloadMessage = status.progressMessage;
        this.startDownloadStatusPolling();
      } else if (status.success === false) {
        this.showAlert("Last UniProt update failed. Check backend logs.", "danger");
      }
    });
  }

  startUniProtDownload(): void {
    this.isDownloadInProgress = true;
    this.downloadMessage = "Initiating UniProt download...";

    this.databasesService.triggerUniProtDownload().subscribe({
      next: () => {
        this.showAlert("UniProt download initiated in the background.", "success");
        this.startDownloadStatusPolling();
      },
      error: (err) => {
        this.isDownloadInProgress = false;
        const errorMessage = err.error || "Failed to start UniProt download.";
        this.showAlert(errorMessage, "danger");
        this.downloadMessage = null;
      }
    });
  }

  startDownloadStatusPolling(): void {
    this.isDownloadInProgress = true;

    if (this.downloadSubscription) {
      this.downloadSubscription.unsubscribe();
    }

    this.downloadSubscription = interval(5000)
      .pipe(switchMap(() => this.databasesService.getUniProtDownloadStatus()))
      .subscribe({
        next: (status: UniProtDownloadStatus) => {
          this.downloadMessage = status.progressMessage;

          if (!status.inProgress) {
            this.isDownloadInProgress = false;
            this.downloadSubscription?.unsubscribe();
            this.loadDatabases();

            if (status.success) {
              this.showAlert("UniProt updated successfully!", "success");
              this.downloadMessage = null;
            } else {
              this.showAlert("UniProt update failed! Check logs.", "danger");
              this.downloadMessage = null;
            }
          }
        },
        error: () => {
          this.isDownloadInProgress = false;
          this.downloadSubscription?.unsubscribe();
          this.showAlert("Failed to get download status.", "danger");
          this.downloadMessage = null;
        }
      });
  }

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
      const nameWithoutExt = lastDotIndex !== -1 ? fileName.substring(0, lastDotIndex) : fileName;
      this.databaseAlias = nameWithoutExt;
    }
  }

  onSubmit(databaseForm: NgForm): void {
    this.isLoading = true;

    if (!this.selectedFile || !databaseForm.value.alias) {
      this.showAlert("Please select a file and provide an alias", "danger");
      this.isLoading = false;
      return;
    }

    const alias = databaseForm.value.alias.trim();
    const sanitizedAlias = alias
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-zA-Z0-9_-]/g, '_')
      .toLowerCase();

    const databaseToUpload: Partial<Database> = {
      alias: sanitizedAlias,
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
    // Mantém alertas temporários, mas não interfere no downloadMessage
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
