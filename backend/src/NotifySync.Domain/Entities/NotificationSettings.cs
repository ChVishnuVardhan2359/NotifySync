namespace NotifySync.Domain.Entities;

public class NotificationSettings
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public bool IsSyncEnabled { get; set; } = true;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
}
