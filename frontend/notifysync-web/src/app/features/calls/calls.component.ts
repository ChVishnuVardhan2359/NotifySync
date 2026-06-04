import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { DeviceDataService } from '../../core/services/device-data.service';
import { CallEntry } from '../../core/models/models';

@Component({
  selector: 'app-calls',
  standalone: true,
  imports: [
    DatePipe, ReactiveFormsModule, MatCardModule, MatTableModule, MatPaginatorModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="page">
      <h1 class="page-title">Calls</h1>
      <mat-card>
        <div class="toolbar">
          <mat-form-field appearance="outline" class="search">
            <mat-label>Search number or name…</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [formControl]="search" />
          </mat-form-field>
        </div>
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="items()" class="full-width">
          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef>Type</th>
            <td mat-cell *matCellDef="let c">
              <span class="type" [class]="c.callType">
                <mat-icon>{{ icon(c.callType) }}</mat-icon> {{ c.callType }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="contact">
            <th mat-header-cell *matHeaderCellDef>Contact</th>
            <td mat-cell *matCellDef="let c">{{ c.name || c.number }}</td>
          </ng-container>
          <ng-container matColumnDef="number">
            <th mat-header-cell *matHeaderCellDef>Number</th>
            <td mat-cell *matCellDef="let c" class="text-muted">{{ c.number }}</td>
          </ng-container>
          <ng-container matColumnDef="duration">
            <th mat-header-cell *matHeaderCellDef>Duration</th>
            <td mat-cell *matCellDef="let c">{{ fmtDuration(c.durationSeconds) }}</td>
          </ng-container>
          <ng-container matColumnDef="device">
            <th mat-header-cell *matHeaderCellDef>Device</th>
            <td mat-cell *matCellDef="let c" class="text-muted">{{ c.deviceName }}</td>
          </ng-container>
          <ng-container matColumnDef="time">
            <th mat-header-cell *matHeaderCellDef>Time</th>
            <td mat-cell *matCellDef="let c">{{ c.callTime | date:'short' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols"></tr>
        </table>
        @if (!loading() && items().length === 0) {
          <p class="empty text-muted">No calls synced yet. Sync them from the phone app's “Calls” tab.</p>
        }
        <mat-paginator [length]="total()" [pageSize]="pageSize()" [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10,25,50,100]" (page)="onPage($event)" showFirstLastButtons />
      </mat-card>
    </div>
  `,
  styles: [`
    .toolbar { padding: 12px 4px 0; }
    .search { width: min(420px, 100%); }
    .empty { padding: 30px; text-align: center; }
    .type { display: inline-flex; align-items: center; gap: 4px; text-transform: capitalize; }
    .type mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .type.incoming { color: #2e9e5b; }
    .type.outgoing { color: #2f6fed; }
    .type.missed, .type.rejected { color: #d13438; }
  `],
})
export class CallsComponent implements OnInit {
  private service = inject(DeviceDataService);
  cols = ['type', 'contact', 'number', 'duration', 'device', 'time'];
  items = signal<CallEntry[]>([]);
  total = signal(0);
  pageIndex = signal(0);
  pageSize = signal(25);
  loading = signal(true);
  search = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    this.search.valueChanges.pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(() => { this.pageIndex.set(0); this.load(); });
  }

  load(): void {
    this.loading.set(true);
    this.service.getCalls(this.pageIndex() + 1, this.pageSize(), this.search.value.trim()).subscribe({
      next: (r) => { this.items.set(r.items); this.total.set(r.totalCount); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void { this.pageIndex.set(e.pageIndex); this.pageSize.set(e.pageSize); this.load(); }

  icon(type: string): string {
    switch (type) {
      case 'incoming': return 'call_received';
      case 'outgoing': return 'call_made';
      case 'missed': return 'call_missed';
      case 'rejected': return 'call_end';
      default: return 'call';
    }
  }

  fmtDuration(s: number): string {
    if (!s) return '—';
    const m = Math.floor(s / 60), sec = s % 60;
    return m > 0 ? `${m}m ${sec}s` : `${sec}s`;
  }
}
