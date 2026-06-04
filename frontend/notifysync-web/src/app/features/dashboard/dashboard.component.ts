import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatRippleModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';

import { DashboardService } from '../../core/services/dashboard.service';
import { DeviceService } from '../../core/services/device.service';
import { SignalrService } from '../../core/services/signalr.service';
import { DashboardStats, NotificationItem } from '../../core/models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    DatePipe, RouterLink, MatCardModule, MatIconModule, MatListModule,
    MatProgressSpinnerModule, MatButtonModule, MatRippleModule, MatSnackBarModule,
  ],
  template: `
    <div class="page">
      <div class="dash-head">
        <h1 class="page-title">Dashboard</h1>
        <button mat-flat-button color="primary" (click)="syncNow()" [disabled]="syncing()">
          <mat-icon>sync</mat-icon> {{ syncing() ? 'Syncing…' : 'Sync Now' }}
        </button>
      </div>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="40" /></div>
      } @else if (stats()) {
        @if (stats()!; as s) {
        <div class="cards-grid">
          <mat-card class="stat-card clickable" routerLink="/notifications" matRipple>
            <mat-icon class="stat-icon blue">notifications</mat-icon>
            <div class="stat-value">{{ s.totalNotifications }}</div>
            <div class="stat-label">Total Notifications <mat-icon class="go">chevron_right</mat-icon></div>
          </mat-card>
          <mat-card class="stat-card clickable" routerLink="/notifications" matRipple>
            <mat-icon class="stat-icon green">today</mat-icon>
            <div class="stat-value">{{ s.notificationsToday }}</div>
            <div class="stat-label">Today's Notifications <mat-icon class="go">chevron_right</mat-icon></div>
          </mat-card>
          <mat-card class="stat-card clickable" routerLink="/devices" matRipple>
            <mat-icon class="stat-icon orange">devices</mat-icon>
            <div class="stat-value">{{ s.activeDevices }}</div>
            <div class="stat-label">Devices Connected <mat-icon class="go">chevron_right</mat-icon></div>
          </mat-card>
          <mat-card class="stat-card clickable" routerLink="/notifications" matRipple>
            <mat-icon class="stat-icon purple">apps</mat-icon>
            <div class="stat-value">{{ s.topApps.length }}</div>
            <div class="stat-label">Active Apps <mat-icon class="go">chevron_right</mat-icon></div>
          </mat-card>
        </div>

        <div class="lower">
          <mat-card class="panel">
            <h2 class="panel-title">Top Apps</h2>
            @if (s.topApps.length === 0) {
              <p class="text-muted">No data yet.</p>
            } @else {
              @for (app of s.topApps; track app.appName) {
                <div class="app-row">
                  <span class="app-name">{{ app.appName }}</span>
                  <div class="bar-track">
                    <div class="bar-fill" [style.width.%]="pct(app.count, s.topApps[0].count)"></div>
                  </div>
                  <span class="app-count">{{ app.count }}</span>
                </div>
              }
            }
          </mat-card>

          <mat-card class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Live Feed</h2>
              <span class="live-badge">● LIVE</span>
            </div>
            @if (live().length === 0) {
              <p class="text-muted">Waiting for incoming notifications…</p>
            } @else {
              <mat-list>
                @for (n of live(); track n.id) {
                  <mat-list-item routerLink="/notifications" class="clickable-row">
                    <mat-icon matListItemIcon>notifications_active</mat-icon>
                    <span matListItemTitle>{{ n.appName }} — {{ n.title }}</span>
                    <span matListItemLine class="text-muted">{{ n.message }} · {{ n.notificationTime | date:'short' }}</span>
                  </mat-list-item>
                }
              </mat-list>
            }
            <a mat-button color="primary" routerLink="/notifications">View all →</a>
          </mat-card>
        </div>
        }
      }
    </div>
  `,
  styles: [`
    .dash-head { display: flex; align-items: center; justify-content: space-between; }
    .center { display: grid; place-items: center; padding: 60px; }
    .stat-card { padding: 20px; display: flex; flex-direction: column; gap: 4px; }
    .stat-card.clickable { cursor: pointer; transition: transform .12s ease, box-shadow .12s ease; }
    .stat-card.clickable:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,.12); }
    .stat-label .go { font-size: 16px; height: 16px; width: 16px; vertical-align: middle; opacity: .5; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover { background: rgba(47,111,237,.08); }
    .stat-icon { font-size: 34px; height: 34px; width: 34px; }
    .blue { color: #2f6fed; } .green { color: #2e9e5b; }
    .orange { color: #e8833a; } .purple { color: #8a4fff; }
    .stat-value { font-size: 2rem; font-weight: 600; }
    .stat-label { color: var(--ns-muted); font-size: .9rem; }
    .lower { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
    @media (max-width: 900px) { .lower { grid-template-columns: 1fr; } }
    .panel { padding: 20px; }
    .panel-head { display: flex; align-items: center; justify-content: space-between; }
    .panel-title { font-size: 1.1rem; margin: 0 0 14px; }
    .live-badge { color: #2e9e5b; font-size: .75rem; font-weight: 700; }
    .app-row { display: grid; grid-template-columns: 120px 1fr 40px; align-items: center; gap: 10px; margin-bottom: 10px; }
    .app-name { font-size: .9rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .bar-track { height: 8px; background: var(--ns-border); border-radius: 6px; overflow: hidden; }
    .bar-fill { height: 100%; background: linear-gradient(90deg, #2f6fed, #1b9aaa); }
    .app-count { text-align: right; font-weight: 600; }
  `],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboard = inject(DashboardService);
  private signalr = inject(SignalrService);
  private devices = inject(DeviceService);
  private snack = inject(MatSnackBar);

  loading = signal(true);
  syncing = signal(false);
  stats = signal<DashboardStats | null>(null);
  live = signal<NotificationItem[]>([]);
  private subs = new Subscription();

  syncNow(): void {
    this.syncing.set(true);
    this.devices.requestSync().subscribe({
      next: (res) => {
        const msg = res.devices > 0
          ? `Sync requested — your ${res.devices} device(s) will push notifications, calls & SMS automatically.`
          : 'No devices registered yet. Install the app and sign in first.';
        this.snack.open(msg, 'OK', { duration: 5000 });
        // The phone picks up the request within ~12s; refresh a couple of times.
        setTimeout(() => this.load(), 8000);
        setTimeout(() => { this.load(); this.syncing.set(false); }, 15000);
      },
      error: () => {
        this.snack.open('Could not request sync.', 'Dismiss', { duration: 3000 });
        this.syncing.set(false);
      },
    });
  }

  ngOnInit(): void {
    this.load();
    // Live push: prepend new notifications and refresh counts instantly.
    this.subs.add(
      this.signalr.notification$.subscribe((n) => {
        this.live.update((list) => [n, ...list].slice(0, 8));
        this.load();
      }),
    );
    // Safety-net polling so the dashboard stays current even without a push.
    this.subs.add(interval(15000).subscribe(() => this.load()));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  private load(): void {
    this.dashboard.getStats().subscribe({
      next: (s) => { this.stats.set(s); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  pct(value: number, max: number): number {
    return max > 0 ? Math.max(8, (value / max) * 100) : 0;
  }
}
