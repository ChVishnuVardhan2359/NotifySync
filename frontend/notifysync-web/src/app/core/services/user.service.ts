import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateUserRequest, UserSummary } from '../models/models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly base = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(this.base);
  }

  create(request: CreateUserRequest): Observable<UserSummary> {
    return this.http.post<UserSummary>(this.base, request);
  }
}
