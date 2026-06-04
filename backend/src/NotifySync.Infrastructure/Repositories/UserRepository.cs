using Microsoft.EntityFrameworkCore;
using NotifySync.Application.DTOs;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;
using NotifySync.Infrastructure.Data;

namespace NotifySync.Infrastructure.Repositories;

public class UserRepository : IUserRepository
{
    private readonly AppDbContext _db;
    public UserRepository(AppDbContext db) => _db = db;

    public Task<User?> GetByIdAsync(int id) =>
        _db.Users.FirstOrDefaultAsync(u => u.Id == id);

    public Task<User?> GetByEmailAsync(string email) =>
        _db.Users.FirstOrDefaultAsync(u => u.Email == email);

    public Task<bool> EmailExistsAsync(string email) =>
        _db.Users.AnyAsync(u => u.Email == email);

    public async Task<User> AddAsync(User user)
    {
        _db.Users.Add(user);
        await _db.SaveChangesAsync();
        return user;
    }

    public async Task UpdateAsync(User user)
    {
        _db.Users.Update(user);
        await _db.SaveChangesAsync();
    }

    public async Task<List<UserSummaryDto>> GetAllSummariesAsync()
    {
        var rows = await _db.Users
            .OrderBy(u => u.Id)
            .Select(u => new
            {
                u.Id, u.Email, u.FirstName, u.LastName, u.Role, u.CreatedAt,
                DeviceCount = u.Devices.Count(),
                NotificationCount = u.Notifications.Count(),
            })
            .ToListAsync();

        return rows.Select(r => new UserSummaryDto(
            r.Id, r.Email, r.FirstName, r.LastName, r.Role, r.CreatedAt,
            r.DeviceCount, r.NotificationCount)).ToList();
    }
}
