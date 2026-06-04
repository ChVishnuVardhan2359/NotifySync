using System.Collections.Concurrent;
using NotifySync.Application.Interfaces;

namespace NotifySync.Infrastructure.Services;

/// <summary>In-memory, per-device "sync requested" flag. Singleton.</summary>
public class SyncSignalStore : ISyncSignalStore
{
    private readonly ConcurrentDictionary<int, bool> _pending = new();

    public void Request(int deviceId) => _pending[deviceId] = true;

    public bool Consume(int deviceId) => _pending.TryRemove(deviceId, out _);
}
