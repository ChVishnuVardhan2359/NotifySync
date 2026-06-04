namespace NotifySync.Application.DTOs;

public record UserSummaryDto(
    int Id,
    string Email,
    string FirstName,
    string LastName,
    string Role,
    DateTime CreatedAt,
    int DeviceCount,
    int NotificationCount);

public record CreateUserRequest(
    string Email,
    string Password,
    string FirstName,
    string LastName,
    string Role);
