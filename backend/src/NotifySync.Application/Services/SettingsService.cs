using NotifySync.Application.Common;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Services;

public class SettingsService : ISettingsService
{
    private readonly IUserRepository _users;
    private readonly INotificationSettingsRepository _settings;
    private readonly IPasswordHasher _hasher;

    public SettingsService(
        IUserRepository users,
        INotificationSettingsRepository settings,
        IPasswordHasher hasher)
    {
        _users = users;
        _settings = settings;
        _hasher = hasher;
    }

    public async Task<ProfileDto> GetProfileAsync(int userId)
    {
        var user = await _users.GetByIdAsync(userId) ?? throw new NotFoundException("User not found.");
        return new ProfileDto(user.Email, user.FirstName, user.LastName, user.Role);
    }

    public async Task UpdateProfileAsync(int userId, UpdateProfileRequest request)
    {
        var user = await _users.GetByIdAsync(userId) ?? throw new NotFoundException("User not found.");
        user.FirstName = request.FirstName.Trim();
        user.LastName = request.LastName.Trim();
        await _users.UpdateAsync(user);
    }

    public async Task<bool> ChangePasswordAsync(int userId, ChangePasswordRequest request)
    {
        var user = await _users.GetByIdAsync(userId) ?? throw new NotFoundException("User not found.");
        if (!_hasher.Verify(request.CurrentPassword, user.PasswordHash))
            throw new UnauthorizedException("Current password is incorrect.");
        user.PasswordHash = _hasher.Hash(request.NewPassword);
        await _users.UpdateAsync(user);
        return true;
    }

    public async Task<NotificationSettingsDto> GetSettingsAsync(int userId)
    {
        var settings = await _settings.GetByUserAsync(userId) ?? await EnsureAsync(userId);
        return new NotificationSettingsDto(settings.IsSyncEnabled);
    }

    public async Task<NotificationSettingsDto> UpdateSettingsAsync(int userId, UpdateNotificationSettingsRequest request)
    {
        var settings = await _settings.GetByUserAsync(userId) ?? await EnsureAsync(userId);
        settings.IsSyncEnabled = request.IsSyncEnabled;
        await _settings.UpdateAsync(settings);
        return new NotificationSettingsDto(settings.IsSyncEnabled);
    }

    private async Task<NotificationSettings> EnsureAsync(int userId)
        => await _settings.AddAsync(new NotificationSettings
        {
            UserId = userId,
            IsSyncEnabled = true,
            CreatedAt = DateTime.UtcNow
        });
}
