using NotifySync.Application.Common;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Services;

public class AuthService : IAuthService
{
    private readonly IUserRepository _users;
    private readonly INotificationSettingsRepository _settings;
    private readonly IPasswordHasher _hasher;
    private readonly IJwtTokenGenerator _jwt;

    public AuthService(
        IUserRepository users,
        INotificationSettingsRepository settings,
        IPasswordHasher hasher,
        IJwtTokenGenerator jwt)
    {
        _users = users;
        _settings = settings;
        _hasher = hasher;
        _jwt = jwt;
    }

    public async Task<AuthResponse> RegisterAsync(RegisterRequest request)
    {
        var email = request.Email.Trim().ToLowerInvariant();
        if (await _users.EmailExistsAsync(email))
            throw new ConflictException("An account with this email already exists.");

        var user = new User
        {
            Email = email,
            PasswordHash = _hasher.Hash(request.Password),
            FirstName = request.FirstName.Trim(),
            LastName = request.LastName.Trim(),
            Role = "User",
            CreatedAt = DateTime.UtcNow
        };
        user = await _users.AddAsync(user);

        await _settings.AddAsync(new NotificationSettings
        {
            UserId = user.Id,
            IsSyncEnabled = true,
            CreatedAt = DateTime.UtcNow
        });

        return BuildResponse(user);
    }

    public async Task<AuthResponse> LoginAsync(LoginRequest request)
    {
        var email = request.Email.Trim().ToLowerInvariant();
        var user = await _users.GetByEmailAsync(email);
        if (user is null || !_hasher.Verify(request.Password, user.PasswordHash))
            throw new UnauthorizedException("Invalid email or password.");

        return BuildResponse(user);
    }

    private AuthResponse BuildResponse(User user)
    {
        var (token, expires) = _jwt.Generate(user);
        return new AuthResponse(token, user.Email, user.FirstName, user.LastName, user.Role, expires);
    }
}
