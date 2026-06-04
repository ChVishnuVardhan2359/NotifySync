using NotifySync.Application.DTOs;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Interfaces;

public interface IUserRepository
{
    Task<User?> GetByIdAsync(int id);
    Task<User?> GetByEmailAsync(string email);
    Task<bool> EmailExistsAsync(string email);
    Task<User> AddAsync(User user);
    Task UpdateAsync(User user);
    Task<List<UserSummaryDto>> GetAllSummariesAsync();
}

public interface IDeviceRepository
{
    Task<Device?> GetByIdForUserAsync(int userId, int id);
    Task<Device?> GetByIdentifierAsync(int userId, string identifier);
    Task<List<Device>> GetByUserAsync(int userId);
    Task<int> CountActiveAsync(int userId, DateTime since);
    Task<Device> AddAsync(Device device);
    Task UpdateAsync(Device device);
}

public interface INotificationRepository
{
    Task<Notification> AddAsync(Notification notification);
    Task<(List<Notification> Items, int Total)> GetPagedAsync(int userId, int page, int pageSize, string? search);
    Task<Notification?> GetByIdForUserAsync(int userId, int id);
    Task DeleteAsync(Notification notification);
    Task<int> CountAsync(int userId);
    Task<int> CountSinceAsync(int userId, DateTime since);
    Task<List<TopAppDto>> TopAppsAsync(int userId, int take);
}

public interface INotificationSettingsRepository
{
    Task<NotificationSettings?> GetByUserAsync(int userId);
    Task<NotificationSettings> AddAsync(NotificationSettings settings);
    Task UpdateAsync(NotificationSettings settings);
}

public interface ICallLogRepository
{
    Task<HashSet<string>> ExistingSourceKeysAsync(int userId, IEnumerable<string> sourceKeys);
    Task AddRangeAsync(IEnumerable<CallLogEntry> entries);
    Task<(List<CallLogEntry> Items, int Total)> GetPagedAsync(int userId, int page, int pageSize, string? search);
    Task<int> CountAsync(int userId);
}

public interface ISmsRepository
{
    Task<HashSet<string>> ExistingSourceKeysAsync(int userId, IEnumerable<string> sourceKeys);
    Task AddRangeAsync(IEnumerable<SmsMessage> messages);
    Task<(List<SmsMessage> Items, int Total)> GetPagedAsync(int userId, int page, int pageSize, string? search);
    Task<int> CountAsync(int userId);
}
