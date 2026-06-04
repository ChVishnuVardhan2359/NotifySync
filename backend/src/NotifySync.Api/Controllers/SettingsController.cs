using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Api.Security;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;

namespace NotifySync.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/settings")]
public class SettingsController : ControllerBase
{
    private readonly ISettingsService _settings;
    public SettingsController(ISettingsService settings) => _settings = settings;

    [HttpGet("profile")]
    public async Task<ActionResult<ProfileDto>> GetProfile()
        => Ok(await _settings.GetProfileAsync(User.GetUserId()));

    [HttpPut("profile")]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
    {
        await _settings.UpdateProfileAsync(User.GetUserId(), request);
        return NoContent();
    }

    [HttpPut("password")]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordRequest request)
    {
        await _settings.ChangePasswordAsync(User.GetUserId(), request);
        return NoContent();
    }

    [HttpGet("notifications")]
    public async Task<ActionResult<NotificationSettingsDto>> GetNotificationSettings()
        => Ok(await _settings.GetSettingsAsync(User.GetUserId()));

    [HttpPut("notifications")]
    public async Task<ActionResult<NotificationSettingsDto>> UpdateNotificationSettings(
        [FromBody] UpdateNotificationSettingsRequest request)
        => Ok(await _settings.UpdateSettingsAsync(User.GetUserId(), request));
}
