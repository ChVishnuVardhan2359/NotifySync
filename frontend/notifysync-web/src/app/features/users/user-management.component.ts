import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { UserService } from '../../core/services/user.service';
import { UserSummary } from '../../core/models/models';

/**
 * Reusable create-user form + users table. Used both on the standalone Users page
 * and inside the Settings "Manage Users" tab.
 */
@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    DatePipe, ReactiveFormsModule, MatCardModule, MatTableModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule,
  ],
  template: `
    <mat-card class="create-card">
      <h2 class="panel-title"><mat-icon>person_add</mat-icon> Create a user</h2>
      <form [formGroup]="form" (ngSubmit)="create()" class="create-form">
        <mat-form-field appearance="outline">
          <mat-label>First name</mat-label>
          <input matInput formControlName="firstName" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Last name</mat-label>
          <input matInput formControlName="lastName" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Password</mat-label>
          <input matInput type="password" formControlName="password" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Role</mat-label>
          <mat-select formControlName="role">
            <mat-option value="User">User</mat-option>
            <mat-option value="Admin">Admin</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-flat-button color="primary" class="create-btn" [disabled]="form.invalid || saving()">
          <mat-icon>add</mat-icon> Create
        </button>
      </form>
      @if (error()) { <p class="error-msg">{{ error() }}</p> }
    </mat-card>

    <mat-card>
      <div class="tbl-head">
        <span class="count">{{ users().length }} user(s)</span>
        <button mat-stroked-button (click)="load()"><mat-icon>refresh</mat-icon> Refresh</button>
      </div>
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else {
        <table mat-table [dataSource]="users()" class="full-width">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Name</th>
            <td mat-cell *matCellDef="let u">
              <mat-icon class="u-icon">account_circle</mat-icon> {{ u.firstName }} {{ u.lastName }}
            </td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let u" class="text-muted">{{ u.email }}</td>
          </ng-container>
          <ng-container matColumnDef="role">
            <th mat-header-cell *matHeaderCellDef>Role</th>
            <td mat-cell *matCellDef="let u">
              <span class="role" [class.admin]="u.role === 'Admin'">{{ u.role }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="devices">
            <th mat-header-cell *matHeaderCellDef>Devices</th>
            <td mat-cell *matCellDef="let u">{{ u.deviceCount }}</td>
          </ng-container>
          <ng-container matColumnDef="notifications">
            <th mat-header-cell *matHeaderCellDef>Notifications</th>
            <td mat-cell *matCellDef="let u">{{ u.notificationCount }}</td>
          </ng-container>
          <ng-container matColumnDef="created">
            <th mat-header-cell *matHeaderCellDef>Created</th>
            <td mat-cell *matCellDef="let u">{{ u.createdAt | date:'mediumDate' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols"></tr>
        </table>
      }
    </mat-card>
  `,
  styles: [`
    .create-card { padding: 20px; margin-bottom: 18px; }
    .panel-title { display: flex; align-items: center; gap: 8px; font-size: 1.1rem; margin: 0 0 14px; }
    .create-form { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; align-items: start; }
    .create-btn { height: 56px; }
    .tbl-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 8px 4px; }
    .count { color: var(--ns-muted); font-size: .9rem; }
    .center { display: grid; place-items: center; padding: 40px; }
    .u-icon { vertical-align: middle; margin-right: 6px; color: #2f6fed; }
    .role { padding: 2px 10px; border-radius: 12px; font-size: .8rem; background: var(--ns-border); }
    .role.admin { background: #2f6fed; color: #fff; }
    .error-msg { color: #d13438; margin: 8px 0 0; }
  `],
})
export class UserManagementComponent implements OnInit {
  private userService = inject(UserService);
  private fb = inject(FormBuilder);
  private snack = inject(MatSnackBar);

  cols = ['name', 'email', 'role', 'devices', 'notifications', 'created'];
  users = signal<UserSummary[]>([]);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['User', [Validators.required]],
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.userService.getAll().subscribe({
      next: (u) => { this.users.set(u); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    this.userService.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.snack.open('User created', 'OK', { duration: 2500 });
        this.form.reset({ role: 'User' });
        this.saving.set(false);
        this.load();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not create user.');
        this.saving.set(false);
      },
    });
  }
}
