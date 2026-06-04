using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Api.Security;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;

namespace NotifySync.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/notifications")]
public class NotificationsController : ControllerBase
{
    private readonly INotificationService _notifications;
    public NotificationsController(INotificationService notifications) => _notifications = notifications;

    [HttpPost]
    public async Task<ActionResult<NotificationDto>> Create([FromBody] CreateNotificationRequest request)
    {
        var created = await _notifications.CreateAsync(User.GetUserId(), request);
        return CreatedAtAction(nameof(GetPaged), new { }, created);
    }

    [HttpGet]
    public async Task<ActionResult<PagedResult<NotificationDto>>> GetPaged(
        [FromQuery] int page = 1, [FromQuery] int pageSize = 50)
        => Ok(await _notifications.GetPagedAsync(User.GetUserId(), page, pageSize));

    [HttpGet("search")]
    public async Task<ActionResult<PagedResult<NotificationDto>>> Search(
        [FromQuery] string query, [FromQuery] int page = 1, [FromQuery] int pageSize = 50)
        => Ok(await _notifications.SearchAsync(User.GetUserId(), query ?? string.Empty, page, pageSize));

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Delete(int id)
    {
        var deleted = await _notifications.DeleteAsync(User.GetUserId(), id);
        return deleted ? NoContent() : NotFound();
    }
}
