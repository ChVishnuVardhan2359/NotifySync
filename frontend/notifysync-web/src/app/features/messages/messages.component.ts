import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { DeviceDataService } from '../../core/services/device-data.service';
import { SmsEntry } from '../../core/models/models';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [
    DatePipe, ReactiveFormsModule, MatCardModule, MatPaginatorModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="page">
      <h1 class="page-title">Messages</h1>
      <mat-card>
        <div class="toolbar">
          <mat-form-field appearance="outline" class="search">
            <mat-label>Search number or text…</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [formControl]="search" />
          </mat-form-field>
        </div>
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <div class="list">
          @for (m of items(); track m.id) {
            <div class="msg" [class.sent]="m.messageType === 'sent'">
              <div class="head">
                <mat-icon>{{ m.messageType === 'sent' ? 'send' : 'sms' }}</mat-icon>
                <span class="addr">{{ m.address }}</span>
                <span class="badge">{{ m.messageType === 'sent' ? 'Sent' : 'Received' }}</span>
                <span class="spacer"></span>
                <span class="time text-muted">{{ m.messageTime | date:'short' }}</span>
              </div>
              <div class="body">{{ m.body }}</div>
              <div class="dev text-muted">{{ m.deviceName }}</div>
            </div>
          }
        </div>
        @if (!loading() && items().length === 0) {
          <p class="empty text-muted">No messages synced yet. Sync them from the phone app's “Messages” tab.</p>
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
    .list { padding: 8px; display: flex; flex-direction: column; gap: 10px; }
    .msg { border: 1px solid var(--ns-border); border-radius: 10px; padding: 12px 14px; background: var(--ns-surface); }
    .msg.sent { border-left: 3px solid #2f6fed; }
    .head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .head mat-icon { font-size: 18px; height: 18px; width: 18px; color: #2f6fed; }
    .addr { font-weight: 600; }
    .badge { font-size: .7rem; padding: 1px 8px; border-radius: 10px; background: var(--ns-border); }
    .time { font-size: .8rem; }
    .body { white-space: pre-wrap; }
    .dev { font-size: .75rem; margin-top: 6px; }
  `],
})
export class MessagesComponent implements OnInit {
  private service = inject(DeviceDataService);
  items = signal<SmsEntry[]>([]);
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
    this.service.getMessages(this.pageIndex() + 1, this.pageSize(), this.search.value.trim()).subscribe({
      next: (r) => { this.items.set(r.items); this.total.set(r.totalCount); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void { this.pageIndex.set(e.pageIndex); this.pageSize.set(e.pageSize); this.load(); }
}
