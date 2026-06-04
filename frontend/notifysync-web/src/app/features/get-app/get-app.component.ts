import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import * as QRCode from 'qrcode';
import { environment } from '../../../environments/environment';

interface AppInfo {
  available: boolean;
  fileName: string;
  version: string;
  sizeBytes: number;
}

@Component({
  selector: 'app-get-app',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatSnackBarModule, MatListModule, MatTooltipModule],
  template: `
    <div class="page">
      <h1 class="page-title">Get the App</h1>
      <p class="text-muted intro">
        Install the NotifySync Android app on your phone, point it at this server, and your
        notifications will sync here automatically.
      </p>

      <div class="grid">
        <!-- Download + server -->
        <mat-card class="panel">
          <h2 class="panel-title"><mat-icon>android</mat-icon> 1 · Download</h2>
          <a mat-flat-button color="primary" class="dl-btn" [href]="downloadUrl" download>
            <mat-icon>download</mat-icon> Download APK
            @if (info(); as i) { <span class="size">({{ formatSize(i.sizeBytes) }})</span> }
          </a>
          <p class="text-muted small">Version {{ info()?.version || '1.0.0' }} · Android 8.0+</p>

          <h2 class="panel-title mt"><mat-icon>dns</mat-icon> 2 · Server address</h2>
          <p class="text-muted small">Enter this in the app's “Server address” field:</p>
          <div class="addr-row">
            <code class="addr">{{ serverAddress() }}</code>
            <button mat-icon-button (click)="copy()" matTooltip="Copy"><mat-icon>content_copy</mat-icon></button>
          </div>
          @if (isLocalhost()) {
            <p class="warn">
              <mat-icon>warning</mat-icon>
              You opened the site on <b>localhost</b>. A phone can't reach “localhost”.
              Open this dashboard using your computer's LAN IP (e.g. http://192.168.x.x:4200)
              so the correct address shows here.
            </p>
          }
        </mat-card>

        <!-- QR + steps -->
        <mat-card class="panel">
          <h2 class="panel-title"><mat-icon>qr_code_2</mat-icon> Scan the server address</h2>
          @if (qr()) {
            <img class="qr" [src]="qr()!" alt="Server address QR code" />
          }
          <p class="text-muted small center">Point your phone's camera here to copy the address.</p>

          <h2 class="panel-title mt"><mat-icon>checklist</mat-icon> 3 · Connect &amp; sync</h2>
          <ol class="steps">
            <li>Install the APK (allow “unknown sources” if prompted).</li>
            <li>Open NotifySync, type/scan the <b>Server address</b> above.</li>
            <li>Sign in with your account — the device is assigned to you automatically.</li>
            <li>Tap <b>Choose</b> and select the apps to monitor.</li>
            <li>Tap <b>Grant Notification Access</b> and enable NotifySync.</li>
            <li>Done — new notifications appear on the dashboard live.</li>
          </ol>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .intro { max-width: 720px; margin-bottom: 20px; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
    @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
    .panel { padding: 22px; }
    .panel-title { display: flex; align-items: center; gap: 8px; font-size: 1.1rem; margin: 0 0 12px; }
    .panel-title.mt { margin-top: 24px; }
    .dl-btn { height: 48px; }
    .size { margin-left: 6px; opacity: .85; font-weight: 400; }
    .small { font-size: .85rem; }
    .addr-row { display: flex; align-items: center; gap: 8px; }
    .addr { flex: 1; background: var(--ns-border); padding: 10px 12px; border-radius: 8px;
      font-size: .95rem; overflow-x: auto; white-space: nowrap; }
    .warn { display: flex; align-items: flex-start; gap: 6px; color: #b26a00; font-size: .85rem; margin-top: 12px; }
    .warn mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .qr { display: block; width: 200px; height: 200px; margin: 8px auto; border-radius: 8px; background: #fff; padding: 8px; }
    .center { text-align: center; }
    .steps { margin: 8px 0 0; padding-left: 20px; line-height: 1.9; }
  `],
})
export class GetAppComponent implements OnInit {
  private http = inject(HttpClient);
  private snack = inject(MatSnackBar);

  readonly downloadUrl = `${environment.apiUrl}/app/download`;
  info = signal<AppInfo | null>(null);
  serverAddress = signal('');
  isLocalhost = signal(false);
  qr = signal<string | null>(null);

  ngOnInit(): void {
    const host = window.location.hostname;
    this.isLocalhost.set(host === 'localhost' || host === '127.0.0.1');
    const address = `http://${host}:5080/`;
    this.serverAddress.set(address);

    QRCode.toDataURL(address, { width: 220, margin: 1 })
      .then((url) => this.qr.set(url))
      .catch(() => this.qr.set(null));

    this.http.get<AppInfo>(`${environment.apiUrl}/app/info`).subscribe({
      next: (i) => this.info.set(i),
      error: () => this.info.set(null),
    });
  }

  copy(): void {
    navigator.clipboard?.writeText(this.serverAddress()).then(() =>
      this.snack.open('Server address copied', 'OK', { duration: 2000 }),
    );
  }

  formatSize(bytes: number): string {
    if (!bytes) return '';
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
