using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Api.Security;
using NotifySync.Application.DTOs;
using NotifySync.Application.Services;

namespace NotifySync.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/calls")]
public class CallsController : ControllerBase
{
    private readonly IDeviceDataService _data;
    public CallsController(IDeviceDataService data) => _data = data;

    [HttpPost]
    public async Task<ActionResult<UploadResult>> Upload([FromBody] UploadCallsRequest request)
        => Ok(await _data.UploadCallsAsync(User.GetUserId(), request));

    [HttpGet]
    public async Task<ActionResult<PagedResult<CallDto>>> GetPaged(
        [FromQuery] int page = 1, [FromQuery] int pageSize = 50, [FromQuery] string? query = null)
        => Ok(await _data.GetCallsAsync(User.GetUserId(), page, pageSize, query));
}
