namespace NotifySync.Application.DTOs;

/// <summary>
/// Payload sent by the Android app. <c>DeviceId</c> matches the example contract
/// ("deviceId":"123") and is resolved either as a numeric device id or as a
/// device identifier string belonging to the authenticated user.
/// </summary>
public record CreateNotificationRequest(
    string DeviceId,
    string AppName,
    string PackageName,
    string Title,
    string Message,
    DateTime NotificationTime);

public record NotificationDto(
    int Id,
    int DeviceId,
    string DeviceName,
    string AppName,
    string PackageName,
    string Title,
    string Message,
    DateTime NotificationTime,
    DateTime CreatedAt);

public record PagedResult<T>(
    IReadOnlyList<T> Items,
    int Page,
    int PageSize,
    int TotalCount)
{
    public int TotalPages => PageSize <= 0 ? 0 : (int)Math.Ceiling(TotalCount / (double)PageSize);
}
