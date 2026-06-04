namespace NotifySync.Domain.Entities;

public class Device
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public string DeviceName { get; set; } = string.Empty;
    public string DeviceIdentifier { get; set; } = string.Empty;
    public DateTime? LastSeen { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
    public ICollection<Notification> Notifications { get; set; } = new List<Notification>();
}
