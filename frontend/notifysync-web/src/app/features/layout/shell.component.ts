import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { SignalrService } from '../../core/services/signalr.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule,
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav #drawer mode="side" opened class="sidenav">
        <div class="brand">
          <mat-icon class="brand-icon">notifications_active</mat-icon>
          <span class="brand-text">NotifySync</span>
        </div>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/notifications" routerLinkActive="active-link">
            <mat-icon matListItemIcon>notifications</mat-icon>
            <span matListItemTitle>Notifications</span>
          </a>
          <a mat-list-item routerLink="/calls" routerLinkActive="active-link">
            <mat-icon matListItemIcon>call</mat-icon>
            <span matListItemTitle>Calls</span>
          </a>
          <a mat-list-item routerLink="/messages" routerLinkActive="active-link">
            <mat-icon matListItemIcon>sms</mat-icon>
            <span matListItemTitle>Messages</span>
          </a>
          <a mat-list-item routerLink="/devices" routerLinkActive="active-link">
            <mat-icon matListItemIcon>devices</mat-icon>
            <span matListItemTitle>Devices</span>
          </a>
          <a mat-list-item routerLink="/get-app" routerLinkActive="active-link">
            <mat-icon matListItemIcon>download_for_offline</mat-icon>
            <span matListItemTitle>Get the App</span>
          </a>
          @if (auth.user()?.role === 'Admin') {
            <a mat-list-item routerLink="/users" routerLinkActive="active-link">
              <mat-icon matListItemIcon>group</mat-icon>
              <span matListItemTitle>Users</span>
            </a>
          }
          <a mat-list-item routerLink="/settings" routerLinkActive="active-link">
            <mat-icon matListItemIcon>settings</mat-icon>
            <span matListItemTitle>Settings</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary" class="toolbar">
          <button mat-icon-button (click)="drawer.toggle()"><mat-icon>menu</mat-icon></button>
          <span class="spacer"></span>
          <button mat-icon-button (click)="theme.toggle()" [attr.aria-label]="'Toggle theme'">
            <mat-icon>{{ theme.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
          </button>
          <button mat-button [matMenuTriggerFor]="menu">
            <mat-icon>account_circle</mat-icon>
            <span class="user-name">{{ auth.user()?.firstName }}</span>
          </button>
          <mat-menu #menu="matMenu">
            <div class="menu-email">{{ auth.user()?.email }}</div>
            <button mat-menu-item routerLink="/settings"><mat-icon>person</mat-icon> Profile</button>
            <button mat-menu-item (click)="logout()"><mat-icon>logout</mat-icon> Logout</button>
          </mat-menu>
        </mat-toolbar>

        <main><router-outlet /></main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { height: 100vh; }
    .sidenav { width: 240px; border-right: 1px solid var(--ns-border); background: var(--ns-surface); }
    .brand { display: flex; align-items: center; gap: 10px; padding: 18px 20px; font-size: 1.25rem; font-weight: 600; }
    .brand-icon { color: #2f6fed; }
    .toolbar { position: sticky; top: 0; z-index: 5; }
    .user-name { margin-left: 6px; }
    .menu-email { padding: 8px 16px; font-size: .8rem; opacity: .7; }
    .active-link { background: rgba(47,111,237,.12); }
    main { display: block; }
  `],
})
export class ShellComponent implements OnInit {
  auth = inject(AuthService);
  theme = inject(ThemeService);
  private signalr = inject(SignalrService);
  private router = inject(Router);

  ngOnInit(): void {
    this.signalr.start();
  }

  logout(): void {
    this.signalr.stop();
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
