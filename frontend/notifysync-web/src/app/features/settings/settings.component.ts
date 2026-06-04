import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { SettingsService } from '../../core/services/settings.service';
import { AuthService } from '../../core/services/auth.service';
import { UserManagementComponent } from '../users/user-management.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSlideToggleModule, MatSnackBarModule, MatTabsModule,
    UserManagementComponent,
  ],
  template: `
    <div class="page">
      <h1 class="page-title">Settings</h1>

      <mat-tab-group>
        <!-- Profile -->
        <mat-tab label="Profile">
          <mat-card class="tab-card">
            <form [formGroup]="profileForm" (ngSubmit)="saveProfile()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email" readonly />
                <mat-icon matSuffix>lock</mat-icon>
              </mat-form-field>
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
              <button mat-flat-button color="primary" [disabled]="profileForm.invalid">Save profile</button>
            </form>
          </mat-card>
        </mat-tab>

        <!-- Password -->
        <mat-tab label="Change Password">
          <mat-card class="tab-card">
            <form [formGroup]="passwordForm" (ngSubmit)="changePassword()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Current password</mat-label>
                <input matInput type="password" formControlName="currentPassword" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>New password</mat-label>
                <input matInput type="password" formControlName="newPassword" />
                <mat-hint>Minimum 8 characters</mat-hint>
                @if (passwordForm.controls.newPassword.hasError('minlength')) {
                  <mat-error>At least 8 characters</mat-error>
                }
              </mat-form-field>
              <button mat-flat-button color="primary" [disabled]="passwordForm.invalid">Update password</button>
            </form>
          </mat-card>
        </mat-tab>

        <!-- Preferences -->
        <mat-tab label="Notification Preferences">
          <mat-card class="tab-card">
            <div class="pref-row">
              <div>
                <div class="pref-title">Notification Sync</div>
                <div class="text-muted">When enabled, your devices may push notifications to NotifySync.</div>
              </div>
              <mat-slide-toggle [checked]="syncEnabled()" (change)="toggleSync($event.checked)" />
            </div>
          </mat-card>
        </mat-tab>

        <!-- Manage Users (admin only) -->
        @if (isAdmin()) {
          <mat-tab label="Manage Users">
            <div class="users-tab">
              <app-user-management />
            </div>
          </mat-tab>
        }
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .tab-card { padding: 24px; margin-top: 16px; max-width: 640px; }
    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 600px) { .row { grid-template-columns: 1fr; } }
    .pref-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
    .pref-title { font-weight: 500; margin-bottom: 4px; }
    .users-tab { margin-top: 16px; }
  `],
})
export class SettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private settings = inject(SettingsService);
  private snack = inject(MatSnackBar);
  private auth = inject(AuthService);

  isAdmin = computed(() => this.auth.user()?.role === 'Admin');

  syncEnabled = signal(true);

  profileForm = this.fb.nonNullable.group({
    email: [{ value: '', disabled: false }],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
  });

  passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.settings.getProfile().subscribe((p) =>
      this.profileForm.patchValue({ email: p.email, firstName: p.firstName, lastName: p.lastName }),
    );
    this.settings.getNotificationSettings().subscribe((s) => this.syncEnabled.set(s.isSyncEnabled));
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    const { firstName, lastName } = this.profileForm.getRawValue();
    this.settings.updateProfile(firstName, lastName).subscribe({
      next: () => this.snack.open('Profile updated', 'OK', { duration: 2500 }),
      error: () => this.snack.open('Update failed', 'Dismiss', { duration: 3000 }),
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.settings.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.snack.open('Password updated', 'OK', { duration: 2500 });
        this.passwordForm.reset();
      },
      error: (err) =>
        this.snack.open(err?.error?.message ?? 'Update failed', 'Dismiss', { duration: 3000 }),
    });
  }

  toggleSync(enabled: boolean): void {
    this.settings.updateNotificationSettings(enabled).subscribe({
      next: (s) => {
        this.syncEnabled.set(s.isSyncEnabled);
        this.snack.open(`Sync ${s.isSyncEnabled ? 'enabled' : 'disabled'}`, 'OK', { duration: 2000 });
      },
      error: () => this.snack.open('Update failed', 'Dismiss', { duration: 3000 }),
    });
  }
}
