namespace NotifySync.Domain.Entities;

public class SmsMessage
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public int DeviceId { get; set; }

    /// <summary>Stable per-device key used to avoid duplicates on re-sync.</summary>
    public string SourceKey { get; set; } = string.Empty;
    public string Address { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    /// <summary>inbox | sent</summary>
    public string MessageType { get; set; } = "inbox";
    public DateTime MessageTime { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
    public Device? Device { get; set; }
}
