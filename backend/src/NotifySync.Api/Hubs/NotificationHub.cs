using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using NotifySync.Api.Security;

namespace NotifySync.Api.Hubs;

/// <summary>
/// Real-time channel. Each connection joins a per-user group so notifications are
/// only pushed to the owning user's open dashboards.
/// </summary>
[Authorize]
public class NotificationHub : Hub
{
    public static string GroupFor(int userId) => $"user-{userId}";

    public override async Task OnConnectedAsync()
    {
        var userId = Context.User!.GetUserId();
        await Groups.AddToGroupAsync(Context.ConnectionId, GroupFor(userId));
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.User!.GetUserId();
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, GroupFor(userId));
        await base.OnDisconnectedAsync(exception);
    }
}
