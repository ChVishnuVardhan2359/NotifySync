namespace NotifySync.Application.DTOs;

public record ProfileDto(string Email, string FirstName, string LastName, string Role);

public record UpdateProfileRequest(string FirstName, string LastName);

public record ChangePasswordRequest(string CurrentPassword, string NewPassword);

public record NotificationSettingsDto(bool IsSyncEnabled);

public record UpdateNotificationSettingsRequest(bool IsSyncEnabled);
