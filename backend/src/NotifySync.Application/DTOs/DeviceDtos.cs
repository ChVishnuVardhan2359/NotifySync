namespace NotifySync.Application.DTOs;

public record RegisterDeviceRequest(string DeviceName, string DeviceIdentifier);

public record HeartbeatRequest(string DeviceIdentifier);

public record DeviceDto(
    int Id,
    string DeviceName,
    string DeviceIdentifier,
    DateTime? LastSeen,
    DateTime CreatedAt,
    bool IsOnline);
