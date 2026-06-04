using Microsoft.EntityFrameworkCore;
using NotifySync.Application.Interfaces;
using NotifySync.Domain.Entities;
using NotifySync.Infrastructure.Data;

namespace NotifySync.Infrastructure.Repositories;

public class CallLogRepository : ICallLogRepository
{
    private readonly AppDbContext _db;
    public CallLogRepository(AppDbContext db) => _db = db;

    public async Task<HashSet<string>> ExistingSourceKeysAsync(int userId, IEnumerable<string> sourceKeys)
    {
        var keys = sourceKeys.ToList();
        var found = await _db.CallLogs
            .Where(c => c.UserId == userId && keys.Contains(c.SourceKey))
            .Select(c => c.SourceKey)
            .ToListAsync();
        return found.ToHashSet();
    }

    public async Task AddRangeAsync(IEnumerable<CallLogEntry> entries)
    {
        _db.CallLogs.AddRange(entries);
        await _db.SaveChangesAsync();
    }

    public async Task<(List<CallLogEntry> Items, int Total)> GetPagedAsync(
        int userId, int page, int pageSize, string? search)
    {
        var query = _db.CallLogs.Include(c => c.Device).Where(c => c.UserId == userId);
        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = search.Trim();
            query = query.Where(c =>
                EF.Functions.ILike(c.Number, $"%{term}%") ||
                (c.Name != null && EF.Functions.ILike(c.Name, $"%{term}%")));
        }

        var total = await query.CountAsync();
        var items = await query
            .OrderByDescending(c => c.CallTime)
            .Skip((page - 1) * pageSize).Take(pageSize)
            .ToListAsync();
        return (items, total);
    }

    public Task<int> CountAsync(int userId) => _db.CallLogs.CountAsync(c => c.UserId == userId);
}
