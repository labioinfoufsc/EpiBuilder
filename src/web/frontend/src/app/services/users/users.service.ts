import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../../models/User';
@Injectable({ providedIn: 'root' })
export class UserService {
  private users = new BehaviorSubject<User[]>([]);
  users$ = this.users.asObservable();
  private apiUrl = environment.apiUrl + '/users';

  constructor(private http: HttpClient) { }

  /**
   * Retrieves all users and updates the BehaviorSubject.
   */
  loadUsers(): void {
    this.http.get<User[]>(this.apiUrl).subscribe({
      next: (users) => this.users.next(users),
      error: (error) => console.error('Failed to fetch users:', error.message),
    });
  }

  /**
   * Adds a new user to the system, ensuring no duplicates.
   * @param {User} user - The user to be added.
   * @returns {Observable<User>} The newly created user.
   */
  addUser(user: User): Observable<User> {
    if (!user) {
      return throwError(() => new Error('Invalid user'));
    }

    // Check for duplicate before adding
    const existingUsers = this.users.getValue();
    const userExists = existingUsers.some((u) => u.username === user.username);

    if (userExists) {
      return throwError(() => new Error('User already exists.'));
    }

    return this.http.post<User>(this.apiUrl, user).pipe(
      tap((newUser: any) => {
        const updatedUsers = [...this.users.getValue(), newUser];
        this.users.next(updatedUsers);
      }),
      catchError((error) =>
        throwError(() => new Error('API Error: ' + error.message))
      )
    );
  }

  /**
   * Deletes a user by ID and updates the users list.
   * @param {number} id - The ID of the user to delete.
   * @returns {Observable<void>}
   */
  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        const updatedUsers = this.users
          .getValue()
          .filter((user) => user.id !== id);
        this.loadUsers();
      }),
      catchError((error) =>
        throwError(() => new Error('Delete failed: ' + error.message))
      )
    );
  }

  /**
   * Updates an existing user in the system.
   *
   * @param {Partial<User>} updatedUser - The user object containing updated fields.
   * Must include a valid `id` to identify the user.
   * @returns {Observable<User>} An observable containing the updated user.
   *
   * @throws {Error} If the `id` is missing or the update request fails.
   */
  updateUser(updatedUser: Partial<User>): Observable<User> {
    if (!updatedUser.id) {
      return throwError(() => new Error('User ID is required for updating'));
    }

    return this.http
      .put<User>(`${this.apiUrl}/${updatedUser.id}`, updatedUser)
      .pipe(
        catchError((error) =>
          throwError(() => new Error('Update failed: ' + error.message))
        )
      );
  }
}
