using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Api.Security;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;

namespace NotifySync.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/dashboard")]
public class DashboardController : ControllerBase
{
    private readonly IDashboardService _dashboard;
    public DashboardController(IDashboardService dashboard) => _dashboard = dashboard;

    [HttpGet("stats")]
    public async Task<ActionResult<DashboardStatsDto>> Stats()
        => Ok(await _dashboard.GetStatsAsync(User.GetUserId()));
}
