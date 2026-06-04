import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationItem, PagedResult } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly base = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  getPaged(page: number, pageSize: number): Observable<PagedResult<NotificationItem>> {
    const params = new HttpParams().set('page', page).set('pageSize', pageSize);
    return this.http.get<PagedResult<NotificationItem>>(this.base, { params });
  }

  search(query: string, page: number, pageSize: number): Observable<PagedResult<NotificationItem>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page)
      .set('pageSize', pageSize);
    return this.http.get<PagedResult<NotificationItem>>(`${this.base}/search`, { params });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
