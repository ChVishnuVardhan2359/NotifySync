using Microsoft.EntityFrameworkCore;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;
using NotifySync.Infrastructure.Data;

namespace NotifySync.Infrastructure.Repositories;

public class NotificationRepository : INotificationRepository
{
    private readonly AppDbContext _db;
    public NotificationRepository(AppDbContext db) => _db = db;

    public async Task<Notification> AddAsync(Notification notification)
    {
        _db.Notifications.Add(notification);
        await _db.SaveChangesAsync();
        return notification;
    }

    public async Task<(List<Notification> Items, int Total)> GetPagedAsync(
        int userId, int page, int pageSize, string? search)
    {
        var query = _db.Notifications
            .Include(n => n.Device)
            .Where(n => n.UserId == userId);

        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = search.Trim();
            query = query.Where(n =>
                EF.Functions.ILike(n.AppName, $"%{term}%") ||
                EF.Functions.ILike(n.Title, $"%{term}%") ||
                EF.Functions.ILike(n.Message, $"%{term}%") ||
                EF.Functions.ILike(n.PackageName, $"%{term}%"));
        }

        var total = await query.CountAsync();
        var items = await query
            .OrderByDescending(n => n.NotificationTime)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, total);
    }

    public Task<Notification?> GetByIdForUserAsync(int userId, int id) =>
        _db.Notifications.FirstOrDefaultAsync(n => n.Id == id && n.UserId == userId);

    public async Task DeleteAsync(Notification notification)
    {
        _db.Notifications.Remove(notification);
        await _db.SaveChangesAsync();
    }

    public Task<int> CountAsync(int userId) =>
        _db.Notifications.CountAsync(n => n.UserId == userId);

    public Task<int> CountSinceAsync(int userId, DateTime since) =>
        _db.Notifications.CountAsync(n => n.UserId == userId && n.CreatedAt >= since);

    public async Task<List<TopAppDto>> TopAppsAsync(int userId, int take)
    {
        var grouped = await _db.Notifications
            .Where(n => n.UserId == userId)
            .GroupBy(n => n.AppName)
            .Select(g => new { AppName = g.Key, Count = g.Count() })
            .OrderByDescending(x => x.Count)
            .Take(take)
            .ToListAsync();

        return grouped.Select(x => new TopAppDto(x.AppName, x.Count)).ToList();
    }
}
