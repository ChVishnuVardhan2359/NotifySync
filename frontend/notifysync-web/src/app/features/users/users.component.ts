import { Component } from '@angular/core';
import { UserManagementComponent } from './user-management.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [UserManagementComponent],
  template: `
    <div class="page">
      <h1 class="page-title">Users</h1>
      <app-user-management />
    </div>
  `,
})
export class UsersComponent {}
