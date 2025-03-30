import { Component } from '@angular/core';

declare var bootstrap: any;

interface DatabaseFile {
  name: string;
  date: string;
  size_mb: number;
  ext: string;
}

@Component({
  selector: 'app-databases',
  standalone: false,
  templateUrl: './databases.component.html',
  styleUrls: ['./databases.component.css']
})
export class DatabasesComponent {
  files: DatabaseFile[] = [];
  newDatabase = {
    file: null as File | null,
    name: ''
  };

  /**
   * Handles form submission to add a new database file.
   */
  onSubmit() {
    if (!this.newDatabase.file || !this.newDatabase.name) return;

    const newFile: DatabaseFile = {
      name: this.newDatabase.name,
      date: new Date().toISOString(),
      size_mb: parseFloat((this.newDatabase.file.size / (1024 * 1024)).toFixed(2)), 
      ext: this.newDatabase.file.name.split('.').pop() || ''
    };

    this.files.push(newFile);
    this.newDatabase = { file: null, name: '' };
  }

  /**
   * Handles file input change event.
   * @param event - The file input change event.
   */
  onFileChange(event: any) {
    this.newDatabase.file = event.target.files[0];
  }

  /**
   * Opens the modal for database conversion.
   * @param actionUrl - The URL where the conversion action will be performed.
   */
  openConversionModal(actionUrl: string) {
    const modalElement = document.getElementById('confirmModal');
    if (modalElement) {
      const modal = new bootstrap.Modal(modalElement);
      modal.show();
    }
  }
}
