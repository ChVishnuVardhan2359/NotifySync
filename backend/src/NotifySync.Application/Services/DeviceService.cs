using NotifySync.Application.Common;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Application.Mappings;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Services;

public class DeviceService : IDeviceService
{
    private readonly IDeviceRepository _devices;
    private readonly ISyncSignalStore _syncSignals;

    public DeviceService(IDeviceRepository devices, ISyncSignalStore syncSignals)
    {
        _devices = devices;
        _syncSignals = syncSignals;
    }

    public async Task<DeviceDto> RegisterAsync(int userId, RegisterDeviceRequest request)
    {
        var now = DateTime.UtcNow;
        var existing = await _devices.GetByIdentifierAsync(userId, request.DeviceIdentifier);
        if (existing is not null)
        {
            existing.DeviceName = request.DeviceName;
            existing.LastSeen = now;
            await _devices.UpdateAsync(existing);
            return existing.ToDto(now);
        }

        var device = new Device
        {
            UserId = userId,
            DeviceName = request.DeviceName,
            DeviceIdentifier = request.DeviceIdentifier,
            LastSeen = now,
            CreatedAt = now
        };
        device = await _devices.AddAsync(device);
        return device.ToDto(now);
    }

    public async Task HeartbeatAsync(int userId, HeartbeatRequest request)
    {
        var device = await _devices.GetByIdentifierAsync(userId, request.DeviceIdentifier)
            ?? throw new NotFoundException("Device not registered.");
        device.LastSeen = DateTime.UtcNow;
        await _devices.UpdateAsync(device);
    }

    public async Task<List<DeviceDto>> GetDevicesAsync(int userId)
    {
        var now = DateTime.UtcNow;
        var devices = await _devices.GetByUserAsync(userId);
        return devices.Select(d => d.ToDto(now)).ToList();
    }

    public async Task<int> RequestSyncAsync(int userId)
    {
        var devices = await _devices.GetByUserAsync(userId);
        foreach (var d in devices) _syncSignals.Request(d.Id);
        return devices.Count;
    }

    public async Task<bool> ConsumeSyncAsync(int userId, string deviceIdentifier)
    {
        var device = await _devices.GetByIdentifierAsync(userId, deviceIdentifier);
        return device is not null && _syncSignals.Consume(device.Id);
    }
}
