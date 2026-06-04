import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="auth-wrap">
      <mat-card class="auth-card">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <div class="auth-head">
          <mat-icon class="logo">person_add</mat-icon>
          <h1>Create account</h1>
          <p class="text-muted">Start syncing your notifications</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="row">
            <mat-form-field appearance="outline">
              <mat-label>First name</mat-label>
              <input matInput formControlName="firstName" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Last name</mat-label>
              <input matInput formControlName="lastName" />
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" />
            @if (form.controls.email.hasError('email')) { <mat-error>Enter a valid email</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput [type]="hide() ? 'password' : 'text'" formControlName="password" />
            <button mat-icon-button matSuffix type="button" (click)="hide.set(!hide())">
              <mat-icon>{{ hide() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            <mat-hint>Min 8 chars, with upper, lower & a digit</mat-hint>
            @if (form.controls.password.hasError('minlength')) {
              <mat-error>At least 8 characters</mat-error>
            }
          </mat-form-field>

          @if (error()) { <p class="error-msg">{{ error() }}</p> }

          <button mat-flat-button color="primary" class="full-width submit" [disabled]="loading()">
            Create account
          </button>
        </form>

        <p class="switch text-muted">
          Already have an account? <a routerLink="/login">Sign in</a>
        </p>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-wrap { min-height: 100vh; display: grid; place-items: center; padding: 20px;
      background: linear-gradient(135deg, #2f6fed22, #1b9aaa22); }
    .auth-card { width: 100%; max-width: 440px; padding: 28px; overflow: hidden; }
    .auth-head { text-align: center; margin-bottom: 12px; }
    .auth-head h1 { margin: 6px 0 2px; }
    .logo { font-size: 42px; height: 42px; width: 42px; color: #2f6fed; }
    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .submit { height: 46px; margin-top: 8px; }
    .switch { text-align: center; margin-top: 16px; }
    .error-msg { color: #d13438; margin: 4px 0 8px; font-size: .9rem; }
  `],
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  hide = signal(true);
  loading = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set(null);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Registration failed.');
        this.loading.set(false);
      },
    });
  }
}
