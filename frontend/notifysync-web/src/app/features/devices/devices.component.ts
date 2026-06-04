import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';

import { DeviceService } from '../../core/services/device.service';
import { Device } from '../../core/models/models';

@Component({
  selector: 'app-devices',
  standalone: true,
  imports: [
    DatePipe, MatCardModule, MatTableModule, MatIconModule,
    MatChipsModule, MatProgressSpinnerModule, MatButtonModule,
  ],
  template: `
    <div class="page">
      <div class="head">
        <h1 class="page-title">Devices</h1>
        <button mat-stroked-button (click)="load()"><mat-icon>refresh</mat-icon> Refresh</button>
      </div>

      <mat-card>
        @if (loading()) {
          <div class="center"><mat-spinner diameter="36" /></div>
        } @else if (devices().length === 0) {
          <p class="empty text-muted">No devices registered yet. Install the Android app and register a device.</p>
        } @else {
          <table mat-table [dataSource]="devices()" class="full-width">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Device</th>
              <td mat-cell *matCellDef="let d">
                <mat-icon class="dev-icon">smartphone</mat-icon> {{ d.deviceName }}
              </td>
            </ng-container>
            <ng-container matColumnDef="identifier">
              <th mat-header-cell *matHeaderCellDef>Identifier</th>
              <td mat-cell *matCellDef="let d" class="text-muted">{{ d.deviceIdentifier }}</td>
            </ng-container>
            <ng-container matColumnDef="lastSeen">
              <th mat-header-cell *matHeaderCellDef>Last Seen</th>
              <td mat-cell *matCellDef="let d">{{ d.lastSeen ? (d.lastSeen | date:'medium') : '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let d">
                <span class="status" [class.on]="d.isOnline" [class.off]="!d.isOnline">
                  <mat-icon>{{ d.isOnline ? 'circle' : 'radio_button_unchecked' }}</mat-icon>
                  {{ d.isOnline ? 'Online' : 'Offline' }}
                </span>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
        }
      </mat-card>
    </div>
  `,
  styles: [`
    .head { display: flex; align-items: center; justify-content: space-between; }
    .center { display: grid; place-items: center; padding: 40px; }
    .empty { padding: 30px; text-align: center; }
    .dev-icon { vertical-align: middle; margin-right: 6px; color: #2f6fed; }
    .status { display: inline-flex; align-items: center; gap: 4px; font-weight: 500; }
    .status mat-icon { font-size: 12px; height: 12px; width: 12px; }
    .status.on { color: #2e9e5b; } .status.off { color: #9aa6b6; }
  `],
})
export class DevicesComponent implements OnInit {
  private deviceService = inject(DeviceService);
  loading = signal(true);
  devices = signal<Device[]>([]);
  cols = ['name', 'identifier', 'lastSeen', 'status'];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.deviceService.getAll().subscribe({
      next: (d) => { this.devices.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
