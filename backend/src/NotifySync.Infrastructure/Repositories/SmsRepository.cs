using Microsoft.EntityFrameworkCore;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;
using NotifySync.Infrastructure.Data;

namespace NotifySync.Infrastructure.Repositories;

public class SmsRepository : ISmsRepository
{
    private readonly AppDbContext _db;
    public SmsRepository(AppDbContext db) => _db = db;

    public async Task<HashSet<string>> ExistingSourceKeysAsync(int userId, IEnumerable<string> sourceKeys)
    {
        var keys = sourceKeys.ToList();
        var found = await _db.SmsMessages
            .Where(s => s.UserId == userId && keys.Contains(s.SourceKey))
            .Select(s => s.SourceKey)
            .ToListAsync();
        return found.ToHashSet();
    }

    public async Task AddRangeAsync(IEnumerable<SmsMessage> messages)
    {
        _db.SmsMessages.AddRange(messages);
        await _db.SaveChangesAsync();
    }

    public async Task<(List<SmsMessage> Items, int Total)> GetPagedAsync(
        int userId, int page, int pageSize, string? search)
    {
        var query = _db.SmsMessages.Include(s => s.Device).Where(s => s.UserId == userId);
        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = search.Trim();
            query = query.Where(s =>
                EF.Functions.ILike(s.Address, $"%{term}%") ||
                EF.Functions.ILike(s.Body, $"%{term}%"));
        }

        var total = await query.CountAsync();
        var items = await query
            .OrderByDescending(s => s.MessageTime)
            .Skip((page - 1) * pageSize).Take(pageSize)
            .ToListAsync();
        return (items, total);
    }

    public Task<int> CountAsync(int userId) => _db.SmsMessages.CountAsync(s => s.UserId == userId);
}
