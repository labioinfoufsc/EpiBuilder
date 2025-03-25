import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../services/users/users.service';
import { User } from '../../models/User';
import { Modal } from 'bootstrap';
import { AbstractControl, ValidationErrors } from '@angular/forms';

export function usernameValidator(
  control: AbstractControl
): ValidationErrors | null {
  const usernameRegex = /^[a-zA-Z0-9]+$/;
  if (control.value && !usernameRegex.test(control.value)) {
    return { invalidUsername: true };
  }
  return null;
}

/**
 * Component for managing user operations such as retrieval, creation, updating, and deletion.
 */
@Component({
  selector: 'app-user',
  standalone: false,
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss'],
})
export class UsersComponent implements OnInit {
  @ViewChild('deleteModal') deleteModal!: ElementRef;
  private deleteModalInstance!: Modal;

  registrationForm: FormGroup;
  users: User[] = [];
  errorMessage: string | null = null;
  isEditMode = false;
  selectedUser: User | null = null;
  userToDelete: User | null = null;
  alertMessage: string | null = null;
  alertType: 'success' | 'danger' | null = null;

  constructor(private fb: FormBuilder, private userService: UserService) {
    this.registrationForm = this.fb.group({
      username: ['', [Validators.required]],
      password: [''],
      confirmPassword: [''],
      name: ['', Validators.required],
      role: ['USER', Validators.required],
    });
  }
  
  ngOnInit() {
    this.userService.users$.subscribe({
      next: (users) => (this.users = users),
      error: (error) => console.error('Failed to fetch users:', error),
    });
  
    this.userService.loadUsers();
  }
  
  /**
   * Handles user creation and updates.
   */
  onSave() {
    if (this.registrationForm.invalid) {
      this.showAlert('Please fill in all fields correctly.', 'danger');
      return;
    }
  
    const usernameRegex = /^[a-zA-Z0-9]+$/;
    let username = this.registrationForm.value.username.trim();
  
    if (!usernameRegex.test(username)) {
      this.showAlert(
        '❌ Username can only contain letters and numbers (no spaces, accents, or special characters).',
        'danger'
      );
      return;
    }
  
    if (!this.isEditMode) {
      if (
        this.registrationForm.value.password !==
        this.registrationForm.value.confirmPassword
      ) {
        this.showAlert('Passwords do not match.', 'danger');
        return;
      }
  
      let newUser: User = new User(
        this.registrationForm.value.name,
        username,
        this.registrationForm.value.password,
        this.registrationForm.value.role,
        undefined,
        ''
      );
  
      this.userService.addUser(newUser).subscribe({
        next: () => {
          this.showAlert('User successfully added!', 'success');
          this.resetForm();
        },
        error: (error) => {
          this.showAlert('Error adding user: ' + error.message, 'danger');
        },
      });
    } else if (this.selectedUser) {
      this.selectedUser.username = username;
      this.selectedUser.name = this.registrationForm.value.name;
      this.selectedUser.role = this.registrationForm.value.role;
  
      if (this.registrationForm.value.password) {
        if (
          this.registrationForm.value.password !==
          this.registrationForm.value.confirmPassword
        ) {
          this.showAlert('Passwords do not match.', 'danger');
          return;
        }
        this.selectedUser.password = this.registrationForm.value.password;
      }
  
      this.userService.updateUser(this.selectedUser).subscribe({
        next: () => {
          this.showAlert('User successfully updated!', 'success');
          this.resetForm();
        },
        error: (error: { message: string; }) => {
          this.showAlert('Error updating user: ' + error.message, 'danger');
        },
      });
  
      this.isEditMode = false;
      this.selectedUser = null;
    }
  }
  
  /**
   * Loads the user list from the API.
   */
  loadUsers() {
    this.userService.loadUsers();
  }
  
  /**
   * Selects a user for editing.
   */
  selectUser(user: User) {
    this.selectedUser = user;
    this.isEditMode = true;
    this.registrationForm.patchValue({
      username: user.username,
      name: user.name,
      role: user.role,
      password: '',
      confirmPassword: '',
    });
  }
  
  /**
   * Opens the delete confirmation modal.
   */
  confirmDelete(user: User) {
    this.userToDelete = user;
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
  
  /**
   * Deletes a user and updates the list.
   */
  onDelete() {
    if (this.userToDelete && this.userToDelete.id !== undefined) {
      this.userService.deleteUser(this.userToDelete.id).subscribe({
        next: () => {
          this.showAlert('User successfully deleted!', 'success');
          this.resetForm();
        },
        error: (error) => {
          this.showAlert('Error deleting user: ' + error.message, 'danger');
        },
      });
      this.userToDelete = null;
    }
    this.hideDeleteModal();
  }
  
  /**
   * Cancels edit mode and resets the form.
   */
  cancelEdit() {
    this.isEditMode = false;
    this.registrationForm.reset();
  }
  
  /**
   * Resets the registration form to its default state.
   */
  resetForm() {
    this.registrationForm.reset({
      username: '',
      password: '',
      confirmPassword: '',
      name: '',
      role: 'USER',
    });
  
    this.isEditMode = false;
    this.selectedUser = null;
    this.userToDelete = null;
  }
  
  /**
   * Displays an alert message for 5 seconds.
   */
  showAlert(message: string, type: 'success' | 'danger' | null) {
    this.alertMessage = message;
    this.alertType = type;
  
    setTimeout(() => {
      this.alertMessage = null;
    }, 5000);
  }
}
  