using System.Text.Json;
using NotifySync.Application.Common;

namespace NotifySync.Api.Middleware;

public class ExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionHandlingMiddleware> _logger;

    public ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await _next(context);
        }
        catch (ValidationFailedException ex)
        {
            await WriteAsync(context, ex.StatusCode, "Validation failed", ex.Errors);
        }
        catch (AppException ex)
        {
            _logger.LogWarning("Handled app exception: {Message}", ex.Message);
            await WriteAsync(context, ex.StatusCode, ex.Message, null);
        }
        catch (UnauthorizedAccessException ex)
        {
            await WriteAsync(context, 401, ex.Message, null);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unhandled exception");
            await WriteAsync(context, 500, "An unexpected error occurred.", null);
        }
    }

    private static async Task WriteAsync(HttpContext context, int status, string message,
        IReadOnlyDictionary<string, string[]>? errors)
    {
        if (context.Response.HasStarted) return;
        context.Response.Clear();
        context.Response.StatusCode = status;
        context.Response.ContentType = "application/json";
        var payload = JsonSerializer.Serialize(new
        {
            status,
            message,
            errors,
            traceId = context.TraceIdentifier
        }, new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase });
        await context.Response.WriteAsync(payload);
    }
}
