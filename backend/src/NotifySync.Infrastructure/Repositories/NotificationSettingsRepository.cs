using Microsoft.EntityFrameworkCore;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;
using NotifySync.Infrastructure.Data;

namespace NotifySync.Infrastructure.Repositories;

public class NotificationSettingsRepository : INotificationSettingsRepository
{
    private readonly AppDbContext _db;
    public NotificationSettingsRepository(AppDbContext db) => _db = db;

    public Task<NotificationSettings?> GetByUserAsync(int userId) =>
        _db.NotificationSettings.FirstOrDefaultAsync(s => s.UserId == userId);

    public async Task<NotificationSettings> AddAsync(NotificationSettings settings)
    {
        _db.NotificationSettings.Add(settings);
        await _db.SaveChangesAsync();
        return settings;
    }

    public async Task UpdateAsync(NotificationSettings settings)
    {
        _db.NotificationSettings.Update(settings);
        await _db.SaveChangesAsync();
    }
}
