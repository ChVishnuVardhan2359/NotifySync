namespace NotifySync.Infrastructure.Auth;

public class JwtSettings
{
    public const string SectionName = "Jwt";

    public string Key { get; set; } = string.Empty;
    public string Issuer { get; set; } = "NotifySync";
    public string Audience { get; set; } = "NotifySyncClients";
    public int ExpiryMinutes { get; set; } = 1440;
}
