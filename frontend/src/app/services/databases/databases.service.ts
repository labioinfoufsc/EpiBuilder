import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Database } from '../../models/Database';

@Injectable({
  providedIn: 'root',
})
export class DatabasesService {
  private dbs: Database[] = [
    new Database('path/to/db1', 'Database 1'),
    new Database('path/to/db2', 'Database 2'),
  ];

  constructor() {}

  getDatabases(): Observable<Database[]> {
    return of(this.dbs);
  }
}
