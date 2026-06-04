import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationSettings, Profile } from '../models/models';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly base = `${environment.apiUrl}/settings`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(`${this.base}/profile`);
  }

  updateProfile(firstName: string, lastName: string): Observable<void> {
    return this.http.put<void>(`${this.base}/profile`, { firstName, lastName });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.base}/password`, { currentPassword, newPassword });
  }

  getNotificationSettings(): Observable<NotificationSettings> {
    return this.http.get<NotificationSettings>(`${this.base}/notifications`);
  }

  updateNotificationSettings(isSyncEnabled: boolean): Observable<NotificationSettings> {
    return this.http.put<NotificationSettings>(`${this.base}/notifications`, { isSyncEnabled });
  }
}
