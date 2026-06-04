using NotifySync.Application.Common;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Application.Mappings;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Services;

public class NotificationService : INotificationService
{
    private readonly INotificationRepository _notifications;
    private readonly IDeviceRepository _devices;
    private readonly INotificationSettingsRepository _settings;
    private readonly INotificationBroadcaster _broadcaster;

    public NotificationService(
        INotificationRepository notifications,
        IDeviceRepository devices,
        INotificationSettingsRepository settings,
        INotificationBroadcaster broadcaster)
    {
        _notifications = notifications;
        _devices = devices;
        _settings = settings;
        _broadcaster = broadcaster;
    }

    public async Task<NotificationDto> CreateAsync(int userId, CreateNotificationRequest request)
    {
        var settings = await _settings.GetByUserAsync(userId);
        if (settings is not null && !settings.IsSyncEnabled)
            throw new ConflictException("Notification sync is disabled for this account.");

        var device = await ResolveDeviceAsync(userId, request.DeviceId)
            ?? throw new NotFoundException($"Device '{request.DeviceId}' is not registered for this user.");

        var now = DateTime.UtcNow;
        device.LastSeen = now;
        await _devices.UpdateAsync(device);

        var notification = new Notification
        {
            UserId = userId,
            DeviceId = device.Id,
            AppName = request.AppName,
            PackageName = request.PackageName,
            Title = request.Title,
            Message = request.Message,
            NotificationTime = request.NotificationTime == default ? now : request.NotificationTime,
            CreatedAt = now
        };
        notification = await _notifications.AddAsync(notification);
        notification.Device = device;

        var dto = notification.ToDto();
        await _broadcaster.BroadcastAsync(userId, dto);
        return dto;
    }

    public async Task<PagedResult<NotificationDto>> GetPagedAsync(int userId, int page, int pageSize)
        => await QueryAsync(userId, page, pageSize, null);

    public async Task<PagedResult<NotificationDto>> SearchAsync(int userId, string query, int page, int pageSize)
        => await QueryAsync(userId, page, pageSize, query);

    public async Task<bool> DeleteAsync(int userId, int id)
    {
        var notification = await _notifications.GetByIdForUserAsync(userId, id);
        if (notification is null) return false;
        await _notifications.DeleteAsync(notification);
        return true;
    }

    private async Task<PagedResult<NotificationDto>> QueryAsync(int userId, int page, int pageSize, string? search)
    {
        page = page < 1 ? 1 : page;
        pageSize = pageSize is < 1 or > 200 ? 50 : pageSize;
        var (items, total) = await _notifications.GetPagedAsync(userId, page, pageSize, search);
        return new PagedResult<NotificationDto>(items.Select(n => n.ToDto()).ToList(), page, pageSize, total);
    }

    private async Task<Device?> ResolveDeviceAsync(int userId, string deviceId)
    {
        if (int.TryParse(deviceId, out var numericId))
        {
            var byId = await _devices.GetByIdForUserAsync(userId, numericId);
            if (byId is not null) return byId;
        }
        return await _devices.GetByIdentifierAsync(userId, deviceId);
    }
}
