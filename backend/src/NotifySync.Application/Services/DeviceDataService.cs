using NotifySync.Application.Common;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Services;

public interface IDeviceDataService
{
    Task<UploadResult> UploadCallsAsync(int userId, UploadCallsRequest request);
    Task<PagedResult<CallDto>> GetCallsAsync(int userId, int page, int pageSize, string? search);
    Task<UploadResult> UploadSmsAsync(int userId, UploadSmsRequest request);
    Task<PagedResult<SmsDto>> GetSmsAsync(int userId, int page, int pageSize, string? search);
}

public class DeviceDataService : IDeviceDataService
{
    private readonly IDeviceRepository _devices;
    private readonly ICallLogRepository _calls;
    private readonly ISmsRepository _sms;

    public DeviceDataService(IDeviceRepository devices, ICallLogRepository calls, ISmsRepository sms)
    {
        _devices = devices;
        _calls = calls;
        _sms = sms;
    }

    public async Task<UploadResult> UploadCallsAsync(int userId, UploadCallsRequest request)
    {
        var device = await ResolveDeviceAsync(userId, request.DeviceId)
            ?? throw new NotFoundException($"Device '{request.DeviceId}' is not registered for this user.");

        var keys = request.Items.Select(i => i.SourceKey).Distinct().ToList();
        var existing = await _calls.ExistingSourceKeysAsync(userId, keys);
        var now = DateTime.UtcNow;

        var fresh = request.Items
            .Where(i => !existing.Contains(i.SourceKey))
            .GroupBy(i => i.SourceKey).Select(g => g.First()) // de-dupe within batch
            .Select(i => new CallLogEntry
            {
                UserId = userId,
                DeviceId = device.Id,
                SourceKey = i.SourceKey,
                Number = i.Number,
                Name = i.Name,
                CallType = i.CallType,
                CallTime = i.CallTime,
                DurationSeconds = i.DurationSeconds,
                CreatedAt = now,
            })
            .ToList();

        if (fresh.Count > 0) await _calls.AddRangeAsync(fresh);
        return new UploadResult(fresh.Count, request.Items.Count - fresh.Count);
    }

    public async Task<PagedResult<CallDto>> GetCallsAsync(int userId, int page, int pageSize, string? search)
    {
        (page, pageSize) = Normalize(page, pageSize);
        var (items, total) = await _calls.GetPagedAsync(userId, page, pageSize, search);
        var dtos = items.Select(c => new CallDto(
            c.Id, c.Number, c.Name, c.CallType, c.CallTime, c.DurationSeconds,
            c.Device?.DeviceName ?? string.Empty, c.CreatedAt)).ToList();
        return new PagedResult<CallDto>(dtos, page, pageSize, total);
    }

    public async Task<UploadResult> UploadSmsAsync(int userId, UploadSmsRequest request)
    {
        var device = await ResolveDeviceAsync(userId, request.DeviceId)
            ?? throw new NotFoundException($"Device '{request.DeviceId}' is not registered for this user.");

        var keys = request.Items.Select(i => i.SourceKey).Distinct().ToList();
        var existing = await _sms.ExistingSourceKeysAsync(userId, keys);
        var now = DateTime.UtcNow;

        var fresh = request.Items
            .Where(i => !existing.Contains(i.SourceKey))
            .GroupBy(i => i.SourceKey).Select(g => g.First())
            .Select(i => new SmsMessage
            {
                UserId = userId,
                DeviceId = device.Id,
                SourceKey = i.SourceKey,
                Address = i.Address,
                Body = i.Body,
                MessageType = i.MessageType,
                MessageTime = i.MessageTime,
                CreatedAt = now,
            })
            .ToList();

        if (fresh.Count > 0) await _sms.AddRangeAsync(fresh);
        return new UploadResult(fresh.Count, request.Items.Count - fresh.Count);
    }

    public async Task<PagedResult<SmsDto>> GetSmsAsync(int userId, int page, int pageSize, string? search)
    {
        (page, pageSize) = Normalize(page, pageSize);
        var (items, total) = await _sms.GetPagedAsync(userId, page, pageSize, search);
        var dtos = items.Select(s => new SmsDto(
            s.Id, s.Address, s.Body, s.MessageType, s.MessageTime,
            s.Device?.DeviceName ?? string.Empty, s.CreatedAt)).ToList();
        return new PagedResult<SmsDto>(dtos, page, pageSize, total);
    }

    private static (int, int) Normalize(int page, int pageSize)
    {
        page = page < 1 ? 1 : page;
        pageSize = pageSize is < 1 or > 500 ? 50 : pageSize;
        return (page, pageSize);
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
