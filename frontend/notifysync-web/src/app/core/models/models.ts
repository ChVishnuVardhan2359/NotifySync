export interface AuthResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  expiresAt: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Device {
  id: number;
  deviceName: string;
  deviceIdentifier: string;
  lastSeen: string | null;
  createdAt: string;
  isOnline: boolean;
}

export interface NotificationItem {
  id: number;
  deviceId: number;
  deviceName: string;
  appName: string;
  packageName: string;
  title: string;
  message: string;
  notificationTime: string;
  createdAt: string;
}

export interface PagedResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

export interface TopApp {
  appName: string;
  count: number;
}

export interface DashboardStats {
  totalNotifications: number;
  notificationsToday: number;
  activeDevices: number;
  topApps: TopApp[];
}

export interface Profile {
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface NotificationSettings {
  isSyncEnabled: boolean;
}

export interface UserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  createdAt: string;
  deviceCount: number;
  notificationCount: number;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface CallEntry {
  id: number;
  number: string;
  name: string | null;
  callType: string;
  callTime: string;
  durationSeconds: number;
  deviceName: string;
  createdAt: string;
}

export interface SmsEntry {
  id: number;
  address: string;
  body: string;
  messageType: string;
  messageTime: string;
  deviceName: string;
  createdAt: string;
}
