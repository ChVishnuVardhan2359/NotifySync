import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

import { NotificationService } from '../../core/services/notification.service';
import { ExportService } from '../../core/services/export.service';
import { SignalrService } from '../../core/services/signalr.service';
import { NotificationItem } from '../../core/models/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    DatePipe, ReactiveFormsModule, MatCardModule, MatTableModule, MatPaginatorModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule, MatMenuModule,
    MatProgressBarModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div class="page">
      <div class="head">
        <h1 class="page-title">Notifications</h1>
        <div class="actions">
          <button mat-stroked-button [matMenuTriggerFor]="exportMenu">
            <mat-icon>download</mat-icon> Export
          </button>
          <mat-menu #exportMenu="matMenu">
            <button mat-menu-item (click)="exportCsv()"><mat-icon>description</mat-icon> Export CSV</button>
            <button mat-menu-item (click)="exportExcel()"><mat-icon>table_chart</mat-icon> Export Excel</button>
          </mat-menu>
        </div>
      </div>

      <mat-card>
        <div class="toolbar">
          <mat-form-field appearance="outline" class="search">
            <mat-label>Search app, title, message…</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [formControl]="search" />
            @if (search.value) {
              <button mat-icon-button matSuffix (click)="search.setValue('')"><mat-icon>close</mat-icon></button>
            }
          </mat-form-field>
        </div>

        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }

        <table mat-table [dataSource]="items()" matSort class="full-width">
          <ng-container matColumnDef="appName">
            <th mat-header-cell *matHeaderCellDef>App</th>
            <td mat-cell *matCellDef="let n">
              <div class="app-cell"><mat-icon class="app-ic">android</mat-icon>{{ n.appName }}</div>
            </td>
          </ng-container>
          <ng-container matColumnDef="title">
            <th mat-header-cell *matHeaderCellDef>Title</th>
            <td mat-cell *matCellDef="let n">{{ n.title }}</td>
          </ng-container>
          <ng-container matColumnDef="message">
            <th mat-header-cell *matHeaderCellDef>Message</th>
            <td mat-cell *matCellDef="let n" class="msg">
              {{ n.message || '—' }}
            </td>
          </ng-container>
          <ng-container matColumnDef="deviceName">
            <th mat-header-cell *matHeaderCellDef>Device</th>
            <td mat-cell *matCellDef="let n" class="text-muted">{{ n.deviceName }}</td>
          </ng-container>
          <ng-container matColumnDef="time">
            <th mat-header-cell *matHeaderCellDef>Time</th>
            <td mat-cell *matCellDef="let n">{{ n.notificationTime | date:'short' }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let n">
              <button mat-icon-button color="warn" (click)="remove(n)" matTooltip="Delete">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols"></tr>
        </table>

        @if (!loading() && items().length === 0) {
          <p class="empty text-muted">No notifications found.</p>
        }

        <mat-paginator
          [length]="total()"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 25, 50, 100]"
          (page)="onPage($event)"
          showFirstLastButtons />
      </mat-card>
    </div>
  `,
  styles: [`
    .head { display: flex; align-items: center; justify-content: space-between; }
    .toolbar { padding: 12px 4px 0; }
    .search { width: min(420px, 100%); }
    .app-cell { display: flex; align-items: center; gap: 6px; }
    .app-ic { color: #2e9e5b; font-size: 18px; height: 18px; width: 18px; }
    .msg { max-width: 460px; white-space: normal; word-break: break-word; padding: 8px 0; }
    .empty { padding: 30px; text-align: center; }
  `],
})
export class NotificationsComponent implements OnInit, OnDestroy {
  private service = inject(NotificationService);
  private exporter = inject(ExportService);
  private signalr = inject(SignalrService);
  private snack = inject(MatSnackBar);

  cols = ['appName', 'title', 'message', 'deviceName', 'time', 'actions'];
  items = signal<NotificationItem[]>([]);
  total = signal(0);
  pageIndex = signal(0);
  pageSize = signal(25);
  loading = signal(true);
  search = new FormControl('', { nonNullable: true });
  private sub = new Subscription();

  ngOnInit(): void {
    this.load();
    this.sub.add(
      this.search.valueChanges
        .pipe(debounceTime(350), distinctUntilChanged())
        .subscribe(() => { this.pageIndex.set(0); this.load(); }),
    );
    this.sub.add(
      this.signalr.notification$.subscribe(() => {
        if (this.pageIndex() === 0 && !this.search.value) this.load();
      }),
    );
  }

  ngOnDestroy(): void { this.sub.unsubscribe(); }

  load(): void {
    this.loading.set(true);
    const page = this.pageIndex() + 1;
    const size = this.pageSize();
    const query = this.search.value.trim();
    const obs = query
      ? this.service.search(query, page, size)
      : this.service.getPaged(page, size);
    obs.subscribe({
      next: (res) => {
        this.items.set(res.items);
        this.total.set(res.totalCount);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.load();
  }

  remove(n: NotificationItem): void {
    this.service.delete(n.id).subscribe({
      next: () => {
        this.snack.open('Notification deleted', 'OK', { duration: 2500 });
        this.load();
      },
      error: () => this.snack.open('Delete failed', 'Dismiss', { duration: 3000 }),
    });
  }

  exportCsv(): void { this.exporter.exportCsv(this.items()); }
  exportExcel(): void { this.exporter.exportExcel(this.items()); }
}
