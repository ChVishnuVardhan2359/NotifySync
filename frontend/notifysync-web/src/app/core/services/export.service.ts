import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import { NotificationItem } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ExportService {
  private toRows(items: NotificationItem[]) {
    return items.map((n) => ({
      App: n.appName,
      Package: n.packageName,
      Title: n.title,
      Message: n.message,
      Device: n.deviceName,
      Time: new Date(n.notificationTime).toLocaleString(),
      Received: new Date(n.createdAt).toLocaleString(),
    }));
  }

  exportCsv(items: NotificationItem[], filename = 'notifications.csv'): void {
    const rows = this.toRows(items);
    const sheet = XLSX.utils.json_to_sheet(rows);
    const csv = XLSX.utils.sheet_to_csv(sheet);
    this.download(new Blob([csv], { type: 'text/csv;charset=utf-8;' }), filename);
  }

  exportExcel(items: NotificationItem[], filename = 'notifications.xlsx'): void {
    const rows = this.toRows(items);
    const sheet = XLSX.utils.json_to_sheet(rows);
    const book = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(book, sheet, 'Notifications');
    XLSX.writeFile(book, filename);
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
