using FluentValidation;
using Microsoft.AspNetCore.Mvc.Filters;
using NotifySync.Application.Common;

namespace NotifySync.Api.Middleware;

/// <summary>
/// Runs any registered FluentValidation <c>IValidator&lt;T&gt;</c> against incoming action
/// arguments and raises a <see cref="ValidationFailedException"/> (mapped to HTTP 400) on failure.
/// </summary>
public class ValidationActionFilter : IAsyncActionFilter
{
    private readonly IServiceProvider _services;
    public ValidationActionFilter(IServiceProvider services) => _services = services;

    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        foreach (var argument in context.ActionArguments.Values)
        {
            if (argument is null) continue;
            var validatorType = typeof(IValidator<>).MakeGenericType(argument.GetType());
            if (_services.GetService(validatorType) is not IValidator validator) continue;

            var validationContext = new ValidationContext<object>(argument);
            var result = await validator.ValidateAsync(validationContext);
            if (!result.IsValid)
            {
                var errors = result.Errors
                    .GroupBy(e => e.PropertyName)
                    .ToDictionary(g => g.Key, g => g.Select(e => e.ErrorMessage).ToArray());
                throw new ValidationFailedException(errors);
            }
        }

        await next();
    }
}
