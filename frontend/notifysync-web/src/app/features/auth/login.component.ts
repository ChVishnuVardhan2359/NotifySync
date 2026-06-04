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
  selector: 'app-login',
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
          <mat-icon class="logo">notifications_active</mat-icon>
          <h1>NotifySync</h1>
          <p class="text-muted">Sign in to your dashboard</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email" />
            <mat-icon matSuffix>mail</mat-icon>
            @if (form.controls.email.hasError('required') && form.controls.email.touched) {
              <mat-error>Email is required</mat-error>
            }
            @if (form.controls.email.hasError('email')) { <mat-error>Enter a valid email</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput [type]="hide() ? 'password' : 'text'" formControlName="password" autocomplete="current-password" />
            <button mat-icon-button matSuffix type="button" (click)="hide.set(!hide())">
              <mat-icon>{{ hide() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (form.controls.password.hasError('required') && form.controls.password.touched) {
              <mat-error>Password is required</mat-error>
            }
          </mat-form-field>

          @if (error()) { <p class="error-msg">{{ error() }}</p> }

          <button mat-flat-button color="primary" class="full-width submit" [disabled]="loading()">
            Sign In
          </button>
        </form>

        <p class="switch text-muted">
          Don't have an account? <a routerLink="/register">Create one</a>
        </p>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-wrap { min-height: 100vh; display: grid; place-items: center; padding: 20px;
      background: linear-gradient(135deg, #2f6fed22, #1b9aaa22); }
    .auth-card { width: 100%; max-width: 400px; padding: 28px; overflow: hidden; }
    .auth-head { text-align: center; margin-bottom: 12px; }
    .auth-head h1 { margin: 6px 0 2px; }
    .logo { font-size: 42px; height: 42px; width: 42px; color: #2f6fed; }
    .submit { height: 46px; margin-top: 8px; }
    .switch { text-align: center; margin-top: 16px; }
    .error-msg { color: #d13438; margin: 4px 0 8px; font-size: .9rem; }
  `],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  hide = signal(true);
  loading = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Login failed. Check your credentials.');
        this.loading.set(false);
      },
    });
  }
}
