using NotifySync.Application.DTOs;
using NotifySync.Domain.Entities;

namespace NotifySync.Application.Interfaces;

public interface IPasswordHasher
{
    string Hash(string password);
    bool Verify(string password, string hash);
}

public interface IJwtTokenGenerator
{
    (string Token, DateTime ExpiresAt) Generate(User user);
}

/// <summary>Implemented in the API layer over SignalR; keeps the service layer transport-agnostic.</summary>
public interface INotificationBroadcaster
{
    Task BroadcastAsync(int userId, NotificationDto notification);
}

/// <summary>
/// Transient "sync now" signals from the dashboard to a device. The phone polls and consumes
/// the flag, then captures and uploads its currently-visible notifications.
/// </summary>
public interface ISyncSignalStore
{
    void Request(int deviceId);
    bool Consume(int deviceId);
}
