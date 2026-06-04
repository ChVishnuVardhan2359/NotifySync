using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using NotifySync.Application.DTOs;
using NotifySync.Application.Services;

namespace NotifySync.Api.Controllers;

/// <summary>Admin-only user management: list users and create new accounts.</summary>
[ApiController]
[Authorize(Policy = "AdminOnly")]
[Route("api/users")]
public class UsersController : ControllerBase
{
    private readonly IUserAdminService _users;
    public UsersController(IUserAdminService users) => _users = users;

    [HttpGet]
    public async Task<ActionResult<List<UserSummaryDto>>> GetAll()
        => Ok(await _users.ListAsync());

    [HttpPost]
    public async Task<ActionResult<UserSummaryDto>> Create([FromBody] CreateUserRequest request)
    {
        var created = await _users.CreateAsync(request);
        return CreatedAtAction(nameof(GetAll), new { }, created);
    }
}
