using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Api.Security;
using NotifySync.Application.DTOs;
using NotifySync.Application.Services;

namespace NotifySync.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/messages")]
public class MessagesController : ControllerBase
{
    private readonly IDeviceDataService _data;
    public MessagesController(IDeviceDataService data) => _data = data;

    [HttpPost]
    public async Task<ActionResult<UploadResult>> Upload([FromBody] UploadSmsRequest request)
        => Ok(await _data.UploadSmsAsync(User.GetUserId(), request));

    [HttpGet]
    public async Task<ActionResult<PagedResult<SmsDto>>> GetPaged(
        [FromQuery] int page = 1, [FromQuery] int pageSize = 50, [FromQuery] string? query = null)
        => Ok(await _data.GetSmsAsync(User.GetUserId(), page, pageSize, query));
}
