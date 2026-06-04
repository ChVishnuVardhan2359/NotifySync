using Microsoft.AspNetCore.SignalR;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;

namespace NotifySync.Api.Hubs;

public class SignalRNotificationBroadcaster : INotificationBroadcaster
{
    private readonly IHubContext<NotificationHub> _hub;
    public SignalRNotificationBroadcaster(IHubContext<NotificationHub> hub) => _hub = hub;

    public Task BroadcastAsync(int userId, NotificationDto notification) =>
        _hub.Clients.Group(NotificationHub.GroupFor(userId))
            .SendAsync("ReceiveNotification", notification);
}
