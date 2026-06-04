using NotifySync.Application.DTOs;

namespace NotifySync.Application.Interfaces;

public interface IAuthService
{
    Task<AuthResponse> RegisterAsync(RegisterRequest request);
    Task<AuthResponse> LoginAsync(LoginRequest request);
}

public interface IDeviceService
{
    Task<DeviceDto> RegisterAsync(int userId, RegisterDeviceRequest request);
    Task HeartbeatAsync(int userId, HeartbeatRequest request);
    Task<List<DeviceDto>> GetDevicesAsync(int userId);
    Task<int> RequestSyncAsync(int userId);
    Task<bool> ConsumeSyncAsync(int userId, string deviceIdentifier);
}

public interface INotificationService
{
    Task<NotificationDto> CreateAsync(int userId, CreateNotificationRequest request);
    Task<PagedResult<NotificationDto>> GetPagedAsync(int userId, int page, int pageSize);
    Task<PagedResult<NotificationDto>> SearchAsync(int userId, string query, int page, int pageSize);
    Task<bool> DeleteAsync(int userId, int id);
}

public interface IDashboardService
{
    Task<DashboardStatsDto> GetStatsAsync(int userId);
}

public interface ISettingsService
{
    Task<ProfileDto> GetProfileAsync(int userId);
    Task UpdateProfileAsync(int userId, UpdateProfileRequest request);
    Task<bool> ChangePasswordAsync(int userId, ChangePasswordRequest request);
    Task<NotificationSettingsDto> GetSettingsAsync(int userId);
    Task<NotificationSettingsDto> UpdateSettingsAsync(int userId, UpdateNotificationSettingsRequest request);
}
