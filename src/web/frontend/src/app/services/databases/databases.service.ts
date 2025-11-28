import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Database } from '../../models/Database';
import { IedbDownloadStatus } from '../../models/IedbDownloadStatus';
import { UniProtDownloadStatus } from '../../models/UniProtDownloadStatus';
@Injectable({
  providedIn: 'root',
})
export class DatabasesService {
  private apiUrl = environment.apiUrl + '/dbs';

  constructor(private http: HttpClient) { }

  triggerIedbDownload(): Observable<any> {
    const url = `${this.apiUrl}/download/iedb`;
    return this.http.post(url, {});
  }

  getIedbDownloadStatus(): Observable<IedbDownloadStatus> {
    const url = `${this.apiUrl}/download/iedb/status`;
    return this.http.get<IedbDownloadStatus>(url);
  }

  download(fileName: string): Observable<Blob> {
    const url = `${this.apiUrl}/download/${encodeURIComponent(fileName)}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  getDatabases(): Observable<Database[]> {
    return this.http.get<Database[]>(this.apiUrl);
  }

  deleteDatabase(id: number | undefined): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  uploadDatabase(file: File, alias: string): Observable<Database> {
    const formData = new FormData();

    formData.append('file', file, file.name);

    const metadata = {
      alias: alias,
    };
    formData.append('data', new Blob([JSON.stringify(metadata)], {
      type: 'application/json'
    }));

    return this.http.post<Database>(this.apiUrl, formData);
  }

  /**
   * Triggers the asynchronous download of the latest UniProt database.
   * Sends a POST request to initiate the background task.
   * Assumes the backend returns status 202 (Accepted) and a body message.
   */
  triggerUniProtDownload(): Observable<string> {
    const url = `${this.apiUrl}/download/uniprot`;
    return this.http.post(url, {}, { responseType: 'text' });
  }


  /**
   * Gets the current status and progress message for the UniProt download.
   * Used by the DatabasesComponent for polling.
   */
  getUniProtDownloadStatus(): Observable<UniProtDownloadStatus> {
    const url = `${this.apiUrl}/download/uniprot/status`;
    // Use the defined interface for type safety
    return this.http.get<UniProtDownloadStatus>(url);
  }
}