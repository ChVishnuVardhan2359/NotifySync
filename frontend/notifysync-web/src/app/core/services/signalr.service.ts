import { Injectable, NgZone } from '@angular/core';
import * as signalR from '@microsoft/signalr';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationItem } from '../models/models';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class SignalrService {
  private connection?: signalR.HubConnection;
  private readonly _notification = new Subject<NotificationItem>();
  readonly notification$ = this._notification.asObservable();

  constructor(private auth: AuthService, private zone: NgZone) {}

  start(): void {
    if (this.connection) return;

    this.connection = new signalR.HubConnectionBuilder()
      .withUrl(environment.hubUrl, {
        accessTokenFactory: () => this.auth.token ?? '',
      })
      .withAutomaticReconnect()
      .configureLogging(signalR.LogLevel.Warning)
      .build();

    this.connection.on('ReceiveNotification', (item: NotificationItem) => {
      // Re-enter Angular zone so change detection picks up the pushed item.
      this.zone.run(() => this._notification.next(item));
    });

    this.connection.start().catch((err) => console.error('SignalR start error:', err));
  }

  stop(): void {
    this.connection?.stop();
    this.connection = undefined;
  }
}
