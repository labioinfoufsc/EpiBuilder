import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { User } from '../../models/User';

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  private apiUrl = 'http://localhost:8080/auth/login';
  private user: User | undefined;

  constructor(private http: HttpClient) {}

  /**
   * Sends login request to the backend API and stores user data in local storage.
   * @param username - The username entered by the user.
   * @param password - The password entered by the user.
   * @returns An Observable containing the login response.
   */
  login(username: string, password: string): Observable<any> {
    return this.http.post<any>(this.apiUrl, { username, password }).pipe(
      tap((response) => {
        if (response && response.token) {
          this.user = new User(
            response.id,
            response.username,
            response.name,
            response.role,
            response.token
          );
          localStorage.setItem('id', response.id);
          localStorage.setItem('username', response.username);
          localStorage.setItem('name', response.name);
          localStorage.setItem('role', response.role);
          localStorage.setItem('token', response.token);
        }
      })
    );
  }

  /**
   * Retrieves the logged-in user.
   * @returns The username object.
   */
  getUser(): User | undefined {
    return this.user;
  }

  /**
   * Logs out the current user by clearing stored credentials.
   */
  logout(): void {
    localStorage.removeItem('id');
    localStorage.removeItem('username');
    localStorage.removeItem('name');
    localStorage.removeItem('role');
    localStorage.removeItem('token');
  }
}
