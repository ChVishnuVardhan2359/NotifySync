import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Device } from '../models/models';

@Injectable({ providedIn: 'root' })
export class DeviceService {
  private readonly base = `${environment.apiUrl}/device`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Device[]> {
    return this.http.get<Device[]>(this.base);
  }

  /** Ask this user's device(s) to push their current notifications. */
  requestSync(): Observable<{ devices: number }> {
    return this.http.post<{ devices: number }>(`${this.base}/request-sync`, {});
  }
}
