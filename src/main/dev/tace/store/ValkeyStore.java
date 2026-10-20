package dev.tace.store;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Set;

/**
 * Valkey storage adapter using Jedis, a Redis-protocol Java client compatible with Valkey.
 */
public final class ValkeyStore implements AutoCloseable {
    // ===== Sec 1: Connection setup - pool creation from environment settings =====
    private final JedisPool pool;

    public ValkeyStore() {
        String host = System.getenv().getOrDefault("TACE_VALKEY_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("TACE_VALKEY_PORT", "6379"));
        this.pool = new JedisPool(host, port);
    }

    // ===== Sec 2: String operations - get, set and delete keys =====
    public String get(String key) {
        try (Jedis jedis = pool.getResource()) { return jedis.get(key); }
    }
    public void set(String key, String value) {
        try (Jedis jedis = pool.getResource()) { jedis.set(key, value); }
    }
    public void del(String key) {
        try (Jedis jedis = pool.getResource()) { jedis.del(key); }
    }

    // ===== Sec 3: Set and list operations - indexes (sets) and audit log (lists) =====
    public void sadd(String key, String value) {
        try (Jedis jedis = pool.getResource()) { jedis.sadd(key, value); }
    }
    public Set<String> smembers(String key) {
        try (Jedis jedis = pool.getResource()) { return jedis.smembers(key); }
    }
    public void lpush(String key, String value) {
        try (Jedis jedis = pool.getResource()) { jedis.lpush(key, value); }
    }
    public List<String> lrange(String key, long start, long stop) {
        try (Jedis jedis = pool.getResource()) { return jedis.lrange(key, start, stop); }
    }

    // ===== Sec 4: Key expiry - schedule cleanup of keys =====
    public void expireAt(String key, long unixSeconds) {
        try (Jedis jedis = pool.getResource()) { jedis.expireAt(key, unixSeconds); }
    }

    // ===== Sec 5: Atomic transactions - multi-command writes with audit events =====
    public void transactionSetAndAdd(String recordKey, String record, String indexKey,
                                     String id, String auditEvent) {
        try (Jedis jedis = pool.getResource()) {
            Transaction tx = jedis.multi();
            tx.set(recordKey, record);
            tx.sadd(indexKey, id);
            tx.lpush("tace:audit", auditEvent);
            tx.exec();
        }
    }

    public void transactionSet(String key, String value, String auditEvent) {
        try (Jedis jedis = pool.getResource()) {
            Transaction tx = jedis.multi();
            tx.set(key, value);
            tx.lpush("tace:audit", auditEvent);
            tx.exec();
        }
    }

    public void transactionDeleteAndAudit(String key, String auditEvent) {
        try (Jedis jedis = pool.getResource()) {
            Transaction tx = jedis.multi();
            tx.del(key);
            tx.lpush("tace:audit", auditEvent);
            tx.exec();
        }
    }

    // ===== Sec 6: Lifecycle - close the connection pool =====
    @Override
    public void close() { pool.close(); }
}