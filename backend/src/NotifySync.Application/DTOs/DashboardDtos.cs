namespace NotifySync.Application.DTOs;

public record TopAppDto(string AppName, int Count);

public record DashboardStatsDto(
    int TotalNotifications,
    int NotificationsToday,
    int ActiveDevices,
    IReadOnlyList<TopAppDto> TopApps);
