namespace NotifySync.Domain.Entities;

public class CallLogEntry
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public int DeviceId { get; set; }

    /// <summary>Stable per-device key (timestamp+number+type) used to avoid duplicates on re-sync.</summary>
    public string SourceKey { get; set; } = string.Empty;
    public string Number { get; set; } = string.Empty;
    public string? Name { get; set; }
    /// <summary>incoming | outgoing | missed | rejected | voicemail | unknown</summary>
    public string CallType { get; set; } = "unknown";
    public DateTime CallTime { get; set; }
    public int DurationSeconds { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
    public Device? Device { get; set; }
}
