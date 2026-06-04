namespace NotifySync.Application.Common;

/// <summary>Base class for expected, mapped-to-HTTP application errors.</summary>
public abstract class AppException : Exception
{
    public abstract int StatusCode { get; }
    protected AppException(string message) : base(message) { }
}

public sealed class NotFoundException : AppException
{
    public override int StatusCode => 404;
    public NotFoundException(string message) : base(message) { }
}

public sealed class ConflictException : AppException
{
    public override int StatusCode => 409;
    public ConflictException(string message) : base(message) { }
}

public sealed class UnauthorizedException : AppException
{
    public override int StatusCode => 401;
    public UnauthorizedException(string message) : base(message) { }
}

public sealed class ValidationFailedException : AppException
{
    public override int StatusCode => 400;
    public IReadOnlyDictionary<string, string[]> Errors { get; }
    public ValidationFailedException(IReadOnlyDictionary<string, string[]> errors)
        : base("One or more validation errors occurred.") => Errors = errors;
}
