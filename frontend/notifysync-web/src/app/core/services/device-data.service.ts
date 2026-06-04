import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CallEntry, PagedResult, SmsEntry } from '../models/models';

@Injectable({ providedIn: 'root' })
export class DeviceDataService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getCalls(page: number, pageSize: number, query = ''): Observable<PagedResult<CallEntry>> {
    let params = new HttpParams().set('page', page).set('pageSize', pageSize);
    if (query) params = params.set('query', query);
    return this.http.get<PagedResult<CallEntry>>(`${this.api}/calls`, { params });
  }

  getMessages(page: number, pageSize: number, query = ''): Observable<PagedResult<SmsEntry>> {
    let params = new HttpParams().set('page', page).set('pageSize', pageSize);
    if (query) params = params.set('query', query);
    return this.http.get<PagedResult<SmsEntry>>(`${this.api}/messages`, { params });
  }
}
