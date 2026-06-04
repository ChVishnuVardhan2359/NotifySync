using Microsoft.EntityFrameworkCore;
using NotifySync.Domain.Entities;

namespace NotifySync.Infrastructure.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Device> Devices => Set<Device>();
    public DbSet<Notification> Notifications => Set<Notification>();
    public DbSet<NotificationSettings> NotificationSettings => Set<NotificationSettings>();
    public DbSet<CallLogEntry> CallLogs => Set<CallLogEntry>();
    public DbSet<SmsMessage> SmsMessages => Set<SmsMessage>();

    protected override void OnModelCreating(ModelBuilder b)
    {
        b.Entity<User>(e =>
        {
            e.ToTable("Users");
            e.HasKey(x => x.Id);
            e.Property(x => x.Email).IsRequired().HasMaxLength(256);
            e.HasIndex(x => x.Email).IsUnique();
            e.Property(x => x.PasswordHash).IsRequired().HasMaxLength(512);
            e.Property(x => x.FirstName).IsRequired().HasMaxLength(100);
            e.Property(x => x.LastName).IsRequired().HasMaxLength(100);
            e.Property(x => x.Role).IsRequired().HasMaxLength(50).HasDefaultValue("User");
            e.Property(x => x.CreatedAt).HasDefaultValueSql("now() at time zone 'utc'");
        });

        b.Entity<Device>(e =>
        {
            e.ToTable("Devices");
            e.HasKey(x => x.Id);
            e.Property(x => x.DeviceName).IsRequired().HasMaxLength(150);
            e.Property(x => x.DeviceIdentifier).IsRequired().HasMaxLength(200);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("now() at time zone 'utc'");
            e.HasIndex(x => new { x.UserId, x.DeviceIdentifier }).IsUnique();
            e.HasOne(x => x.User).WithMany(u => u.Devices)
                .HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
        });

        b.Entity<Notification>(e =>
        {
            e.ToTable("Notifications");
            e.HasKey(x => x.Id);
            e.Property(x => x.AppName).IsRequired().HasMaxLength(200);
            e.Property(x => x.PackageName).IsRequired().HasMaxLength(200);
            e.Property(x => x.Title).HasMaxLength(500);
            e.Property(x => x.Message).HasMaxLength(4000);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("now() at time zone 'utc'");
            e.HasIndex(x => new { x.UserId, x.CreatedAt });
            e.HasIndex(x => x.AppName);
            e.HasOne(x => x.User).WithMany(u => u.Notifications)
                .HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
            e.HasOne(x => x.Device).WithMany(d => d.Notifications)
                .HasForeignKey(x => x.DeviceId).OnDelete(DeleteBehavior.Restrict);
        });

        b.Entity<NotificationSettings>(e =>
        {
            e.ToTable("NotificationSettings");
            e.HasKey(x => x.Id);
            e.Property(x => x.IsSyncEnabled).HasDefaultValue(true);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("now() at time zone 'utc'");
            e.HasIndex(x => x.UserId).IsUnique();
            e.HasOne(x => x.User).WithOne(u => u.Settings)
                .HasForeignKey<NotificationSettings>(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
        });

        b.Entity<CallLogEntry>(e =>
        {
            e.ToTable("CallLogs");
            e.HasKey(x => x.Id);
            e.Property(x => x.SourceKey).IsRequired().HasMaxLength(200);
            e.Property(x => x.Number).IsRequired().HasMaxLength(50);
            e.Property(x => x.Name).HasMaxLength(200);
            e.Property(x => x.CallType).IsRequired().HasMaxLength(20);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("now() at time zone 'utc'");
            e.HasIndex(x => new { x.UserId, x.SourceKey });
            e.HasIndex(x => new { x.UserId, x.CallTime });
            e.HasOne(x => x.User).WithMany().HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
            e.HasOne(x => x.Device).WithMany().HasForeignKey(x => x.DeviceId).OnDelete(DeleteBehavior.Restrict);
        });

        b.Entity<SmsMessage>(e =>
        {
            e.ToTable("SmsMessages");
            e.HasKey(x => x.Id);
            e.Property(x => x.SourceKey).IsRequired().HasMaxLength(200);
            e.Property(x => x.Address).IsRequired().HasMaxLength(100);
            e.Property(x => x.Body).HasMaxLength(4000);
            e.Property(x => x.MessageType).IsRequired().HasMaxLength(20);
            e.Property(x => x.CreatedAt).HasDefaultValueSql("now() at time zone 'utc'");
            e.HasIndex(x => new { x.UserId, x.SourceKey });
            e.HasIndex(x => new { x.UserId, x.MessageTime });
            e.HasOne(x => x.User).WithMany().HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
            e.HasOne(x => x.Device).WithMany().HasForeignKey(x => x.DeviceId).OnDelete(DeleteBehavior.Restrict);
        });
    }
}
