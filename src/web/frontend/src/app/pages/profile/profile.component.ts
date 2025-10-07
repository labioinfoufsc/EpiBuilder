import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { User } from '../../models/User';
import { LoginService } from '../../services/login/login.service';
import { UserService } from '../../services/users/users.service';

@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  user: User | null = null;
  alertMessage: string | null = '';
  alertType: string | null = '';

  /**
   * Initializes the profile form with the user's current data.
   * @param fb - FormBuilder for creating the form.
   * @param loginService - LoginService for retrieving user data.
   * @param userService - UserService for updating user data.
   */
  constructor(private fb: FormBuilder, private loginService: LoginService, private userService: UserService) {
    this.profileForm = this.fb.group({
      username: [{ value: '', disabled: true }],
      name: ['', Validators.required],
      password: [''],
      confirmPassword: [''],
    });
  }

  ngOnInit(): void {
    this.loadUser();
  }

  clearForm(): void {
    this.profileForm.reset();
    this.loadUser();
  }

  /**
   * Loads user data from localStorage and populates the form.
   */
  private loadUser(): void {
    this.user = this.loginService.getUser();

    this.profileForm.patchValue({
      username: this.user?.username,
      name: this.user?.name,
    });
  }

  /**
 * Validates that passwords match before submitting.
 * @returns - Returns true if passwords match, false otherwise.
 */
  validatePasswords(): boolean {
    const password = this.profileForm.get('password')?.value;
    const confirmPassword = this.profileForm.get('confirmPassword')?.value;

    if (password && confirmPassword && password !== confirmPassword) {
      this.alertMessage = 'Passwords do not match.';
      this.alertType = 'danger';
      return false;
    }

    this.alertMessage = '';
    return true;
  }

  /**
   * Handles profile update submission.
   */
  onSubmit(): void {
    if (!this.user) {
      this.alertMessage = 'User data is unavailable.';
      this.alertType = 'danger';
      return;
    }

    if (!this.profileForm.valid) {
      this.alertMessage = 'Please fill in all required fields.';
      this.alertType = 'danger';
      return;
    }

    // Validate passwords before submitting
    if (!this.validatePasswords()) {
      return;
    }

    const updatedUser: Partial<User> = {
      id: Number(this.user.id),
      username: this.user.username,
      name: this.profileForm.value.name,
      role: this.user.role,
    };

    // Only add password to the update if it's not empty
    if (this.profileForm.value.password) {
      updatedUser.password = this.profileForm.value.password;
    }

    this.userService.updateUser(updatedUser).subscribe({
      next: () => {
        // Update localStorage with new values
        localStorage.setItem('name', this.profileForm.value.name);
        localStorage.setItem('role', this.user?.role || '');

        this.alertMessage = 'Profile updated successfully!';
        this.alertType = 'success';
        setTimeout(() => (this.alertMessage = ''), 5000);
      },
      error: (error) => {
        this.alertMessage = 'Failed to update profile: ' + error.message;
        this.alertType = 'danger';
      },
    });
  }


  /**
   * Displays an alert message for 5 seconds.
   * @param message - The message to display.
   * @param type - The type of alert ('success', 'danger', or null).
   */
  showAlert(message: string, type: 'success' | 'danger' | null) {
    this.alertMessage = message;
    this.alertType = type;

    setTimeout(() => {
      this.alertMessage = null;
    }, 5000);
  }
}
