using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Api.Security;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;

namespace NotifySync.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/device")]
public class DeviceController : ControllerBase
{
    private readonly IDeviceService _devices;
    public DeviceController(IDeviceService devices) => _devices = devices;

    [HttpPost("register")]
    public async Task<ActionResult<DeviceDto>> Register([FromBody] RegisterDeviceRequest request)
        => Ok(await _devices.RegisterAsync(User.GetUserId(), request));

    [HttpPost("heartbeat")]
    public async Task<IActionResult> Heartbeat([FromBody] HeartbeatRequest request)
    {
        await _devices.HeartbeatAsync(User.GetUserId(), request);
        return NoContent();
    }

    [HttpGet]
    public async Task<ActionResult<List<DeviceDto>>> GetAll()
        => Ok(await _devices.GetDevicesAsync(User.GetUserId()));

    /// <summary>Dashboard → device: ask the user's device(s) to push their current notifications.</summary>
    [HttpPost("request-sync")]
    public async Task<IActionResult> RequestSync()
    {
        var count = await _devices.RequestSyncAsync(User.GetUserId());
        return Ok(new { devices = count });
    }

    /// <summary>Device polls this; returns (and clears) whether a sync was requested for it.</summary>
    [HttpGet("sync-pending")]
    public async Task<IActionResult> SyncPending([FromQuery] string identifier)
    {
        var pending = await _devices.ConsumeSyncAsync(User.GetUserId(), identifier ?? string.Empty);
        return Ok(new { pending });
    }
}
