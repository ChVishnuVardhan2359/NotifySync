using FluentValidation;
using Microsoft.Extensions.DependencyInjection;
using NotifySync.Application.Interfaces;
using NotifySync.Application.Services;
using NotifySync.Application.Validators;

namespace NotifySync.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<IAuthService, AuthService>();
        services.AddScoped<IDeviceService, DeviceService>();
        services.AddScoped<INotificationService, NotificationService>();
        services.AddScoped<IDashboardService, DashboardService>();
        services.AddScoped<ISettingsService, SettingsService>();
        services.AddScoped<IUserAdminService, UserAdminService>();
        services.AddScoped<IDeviceDataService, DeviceDataService>();

        services.AddValidatorsFromAssemblyContaining<RegisterRequestValidator>();
        return services;
    }
}
