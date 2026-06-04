using NotifySync.Application.Common;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Services;

public interface IUserAdminService
{
    Task<List<UserSummaryDto>> ListAsync();
    Task<UserSummaryDto> CreateAsync(CreateUserRequest request);
}

public class UserAdminService : IUserAdminService
{
    private static readonly string[] AllowedRoles = { "User", "Admin" };

    private readonly IUserRepository _users;
    private readonly INotificationSettingsRepository _settings;
    private readonly IPasswordHasher _hasher;

    public UserAdminService(
        IUserRepository users,
        INotificationSettingsRepository settings,
        IPasswordHasher hasher)
    {
        _users = users;
        _settings = settings;
        _hasher = hasher;
    }

    public Task<List<UserSummaryDto>> ListAsync() => _users.GetAllSummariesAsync();

    public async Task<UserSummaryDto> CreateAsync(CreateUserRequest request)
    {
        var email = request.Email.Trim().ToLowerInvariant();
        if (await _users.EmailExistsAsync(email))
            throw new ConflictException("An account with this email already exists.");

        var role = AllowedRoles.Contains(request.Role, StringComparer.OrdinalIgnoreCase)
            ? request.Role
            : "User";

        var user = new User
        {
            Email = email,
            PasswordHash = _hasher.Hash(request.Password),
            FirstName = request.FirstName.Trim(),
            LastName = request.LastName.Trim(),
            Role = role,
            CreatedAt = DateTime.UtcNow,
        };
        user = await _users.AddAsync(user);

        await _settings.AddAsync(new NotificationSettings
        {
            UserId = user.Id,
            IsSyncEnabled = true,
            CreatedAt = DateTime.UtcNow,
        });

        return new UserSummaryDto(
            user.Id, user.Email, user.FirstName, user.LastName, user.Role, user.CreatedAt, 0, 0);
    }
}
