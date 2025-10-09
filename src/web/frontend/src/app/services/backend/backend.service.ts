import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  private apiUrl = environment.apiUrl + '/auth/status';

  constructor(private http: HttpClient) { }

  checkStatus(): Observable<any> {
    return this.http.get(this.apiUrl);
  }

}
