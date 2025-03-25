import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LoginService } from '../../services/login/login.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  loginForm: FormGroup;
  loginError: string = '';

  constructor(
    private fb: FormBuilder,
    private loginService: LoginService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  /**
   * Handles form submission and performs login.
   */
  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginError = 'Username and password are required';
      return;
    }

    const { username, password } = this.loginForm.value;
    this.loginService.login(username, password).subscribe({
      next: (response) => {
        if (response && response.token) {
          this.loginError = '';
          console.log(response)
          //this.router.navigate(['/new']);
        } else {
          this.loginError = response.message;
        }
      },
      error: (err) => {
        this.loginError =
          err.error?.message || 'Login failed. Please try again later.';
      },
    });
  }
}
