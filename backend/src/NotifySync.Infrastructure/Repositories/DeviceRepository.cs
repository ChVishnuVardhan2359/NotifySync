using Microsoft.EntityFrameworkCore;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;
using NotifySync.Infrastructure.Data;

namespace NotifySync.Infrastructure.Repositories;

public class DeviceRepository : IDeviceRepository
{
    private readonly AppDbContext _db;
    public DeviceRepository(AppDbContext db) => _db = db;

    public Task<Device?> GetByIdForUserAsync(int userId, int id) =>
        _db.Devices.FirstOrDefaultAsync(d => d.Id == id && d.UserId == userId);

    public Task<Device?> GetByIdentifierAsync(int userId, string identifier) =>
        _db.Devices.FirstOrDefaultAsync(d => d.UserId == userId && d.DeviceIdentifier == identifier);

    public Task<List<Device>> GetByUserAsync(int userId) =>
        _db.Devices.Where(d => d.UserId == userId)
            .OrderByDescending(d => d.LastSeen)
            .ToListAsync();

    public Task<int> CountActiveAsync(int userId, DateTime since) =>
        _db.Devices.CountAsync(d => d.UserId == userId && d.LastSeen != null && d.LastSeen >= since);

    public async Task<Device> AddAsync(Device device)
    {
        _db.Devices.Add(device);
        await _db.SaveChangesAsync();
        return device;
    }

    public async Task UpdateAsync(Device device)
    {
        _db.Devices.Update(device);
        await _db.SaveChangesAsync();
    }
}
