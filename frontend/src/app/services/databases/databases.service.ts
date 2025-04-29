import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Database } from '../../models/Database';

@Injectable({
  providedIn: 'root',
})
export class DatabasesService {
  private apiUrl = 'http://localhost:8080/dbs';

  constructor(private http: HttpClient) { }

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

}
