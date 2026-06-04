using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Application.Mappings;

namespace NotifySync.Application.Services;

public class DashboardService : IDashboardService
{
    private readonly INotificationRepository _notifications;
    private readonly IDeviceRepository _devices;

    public DashboardService(INotificationRepository notifications, IDeviceRepository devices)
    {
        _notifications = notifications;
        _devices = devices;
    }

    public async Task<DashboardStatsDto> GetStatsAsync(int userId)
    {
        var now = DateTime.UtcNow;
        var total = await _notifications.CountAsync(userId);
        var today = await _notifications.CountSinceAsync(userId, now.Date);
        var activeDevices = await _devices.CountActiveAsync(userId, now - MappingExtensions.OnlineWindow);
        var topApps = await _notifications.TopAppsAsync(userId, 5);
        return new DashboardStatsDto(total, today, activeDevices, topApps);
    }
}
