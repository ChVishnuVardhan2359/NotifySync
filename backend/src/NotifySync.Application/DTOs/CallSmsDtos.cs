namespace NotifySync.Application.DTOs;

// ---- Calls ----
public record CallItem(
    string SourceKey,
    string Number,
    string? Name,
    string CallType,
    DateTime CallTime,
    int DurationSeconds);

public record UploadCallsRequest(string DeviceId, IReadOnlyList<CallItem> Items);

public record CallDto(
    int Id,
    string Number,
    string? Name,
    string CallType,
    DateTime CallTime,
    int DurationSeconds,
    string DeviceName,
    DateTime CreatedAt);

// ---- SMS ----
public record SmsItem(
    string SourceKey,
    string Address,
    string Body,
    string MessageType,
    DateTime MessageTime);

public record UploadSmsRequest(string DeviceId, IReadOnlyList<SmsItem> Items);

public record SmsDto(
    int Id,
    string Address,
    string Body,
    string MessageType,
    DateTime MessageTime,
    string DeviceName,
    DateTime CreatedAt);

// ---- Shared ----
public record UploadResult(int Inserted, int Skipped);
