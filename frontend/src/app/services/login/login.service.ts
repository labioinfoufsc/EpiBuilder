import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable, tap } from "rxjs";
import { environment } from '../../../environments/environment';
import { User } from "../../models/User";

@Injectable({
  providedIn: "root",
})
export class LoginService {
  private apiUrl = environment.apiUrl + '/auth/login';

  private userSubject: BehaviorSubject<User | null> =
    new BehaviorSubject<User | null>(null);

  constructor(private http: HttpClient) {
    // Tenta recuperar o usuário do localStorage quando o serviço é inicializado
    const userFromStorage = this.loadUserFromStorage();
    if (userFromStorage) {
      this.userSubject.next(userFromStorage);
    }
  }

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
          const user = new User();
          user.id = response.id;
          user.username = response.username;
          user.name = response.name;
          user.role = response.role;
          user.token = response.token;
          this.saveUserToStorage(user);
          this.userSubject.next(user);
        }
      })
    );
  }

  /**
   * Retrieves the logged-in user.
   * @returns The user object if logged in, or null.
   */
  getUser(): User | null {
    return this.userSubject.value;
  }

  /**
   * Verifies if the user is logged in.
   * @returns true if the user is logged in, otherwise false.
   */
  isLoggedIn(): boolean {
    return this.userSubject.value !== null;
  }

  /**
   * Logs out the current user by clearing stored credentials.
   */
  logout(): void {
    this.clearUserFromStorage();
    this.userSubject.next(null);
  }

  /**
   * Helper method to load user data from localStorage.
   */
  private loadUserFromStorage(): User | null {
    const id = localStorage.getItem("id");
    const username = localStorage.getItem("username");
    const name = localStorage.getItem("name");
    const role = localStorage.getItem("role");
    const token = localStorage.getItem("token");

    if (id && username && name && role && token) {
      let user = new User();
      user.id = Number(id);
      user.username = username;
      user.name = name;
      user.role = role;
      user.token = token;
      return user;
    }
    return null;
  }

  /**
   * Helper method to save user data to localStorage.
   */
  private saveUserToStorage(user: User): void {
    localStorage.setItem("id", user.id?.toString() || "");
    localStorage.setItem("username", user.username);
    localStorage.setItem("name", user.name);
    localStorage.setItem("role", user.role);
    localStorage.setItem("token", user.token?.toString() || "");
  }

  /**
   * Helper method to clear user data from localStorage.
   */
  private clearUserFromStorage(): void {
    localStorage.removeItem("id");
    localStorage.removeItem("username");
    localStorage.removeItem("name");
    localStorage.removeItem("role");
    localStorage.removeItem("token");
  }
}
