using NotifySync.Application.DTOs;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Mappings;

public static class MappingExtensions
{
    /// <summary>A device counts as "online" if it sent a heartbeat within this window.</summary>
    public static readonly TimeSpan OnlineWindow = TimeSpan.FromMinutes(2);

    public static DeviceDto ToDto(this Device device, DateTime utcNow)
    {
        var isOnline = device.LastSeen.HasValue && (utcNow - device.LastSeen.Value) <= OnlineWindow;
        return new DeviceDto(
            device.Id,
            device.DeviceName,
            device.DeviceIdentifier,
            device.LastSeen,
            device.CreatedAt,
            isOnline);
    }

    public static NotificationDto ToDto(this Notification n) => new(
        n.Id,
        n.DeviceId,
        n.Device?.DeviceName ?? string.Empty,
        n.AppName,
        n.PackageName,
        n.Title,
        n.Message,
        n.NotificationTime,
        n.CreatedAt);
}
