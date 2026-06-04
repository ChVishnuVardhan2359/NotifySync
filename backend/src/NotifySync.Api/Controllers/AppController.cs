using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace NotifySync.Api.Controllers;

/// <summary>Serves the Android app binary so users can download it from the dashboard.</summary>
[ApiController]
[AllowAnonymous]
[Route("api/app")]
public class AppController : ControllerBase
{
    private const string ApkContentType = "application/vnd.android.package-archive";
    private readonly IWebHostEnvironment _env;
    private readonly IConfiguration _config;

    public AppController(IWebHostEnvironment env, IConfiguration config)
    {
        _env = env;
        _config = config;
    }

    [HttpGet("info")]
    public IActionResult Info()
    {
        var path = ResolveApkPath();
        var exists = System.IO.File.Exists(path);
        return Ok(new
        {
            available = exists,
            fileName = "NotifySync.apk",
            version = _config["AppDownload:Version"] ?? "1.0.0",
            sizeBytes = exists ? new FileInfo(path).Length : 0,
        });
    }

    [HttpGet("download")]
    public IActionResult Download()
    {
        var path = ResolveApkPath();
        if (!System.IO.File.Exists(path))
            return NotFound(new { message = "The app build is not available on this server yet." });

        return PhysicalFile(path, ApkContentType, "NotifySync.apk", enableRangeProcessing: true);
    }

    private string ResolveApkPath()
    {
        var configured = _config["AppDownload:ApkPath"];
        if (!string.IsNullOrWhiteSpace(configured))
        {
            return Path.IsPathRooted(configured)
                ? configured
                : Path.Combine(_env.ContentRootPath, configured);
        }
        return Path.Combine(_env.ContentRootPath, "wwwroot", "downloads", "NotifySync.apk");
    }
}
