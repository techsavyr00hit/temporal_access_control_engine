package dev.tace.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {
    // ===== Sec 1: Validity window tests - start inclusive, expiry exclusive =====
    @Test void validityWindowIsStartInclusiveAndExpiryExclusive() {
        Instant start = Instant.parse("2026-10-09T10:00:00Z");
        Instant end = Instant.parse("2026-10-09T12:00:00Z");
        Permission p = new Permission("p1", "alice", null, "db", "WRITE", start, end, false);
        assertFalse(p.activeAt(start.minusNanos(1)));
        assertTrue(p.activeAt(start));
        assertTrue(p.activeAt(end.minusNanos(1)));
        assertFalse(p.activeAt(end));
    }

    // ===== Sec 2: Serialization tests - encode/decode round trip =====
    @Test void encodeDecodeRoundTrip() {
        Permission p = new Permission("p-1", "alice", null, "production-db", "WRITE",
                Instant.parse("2026-10-09T10:00:00Z"), Instant.parse("2026-10-09T12:00:00Z"), false);
        assertEquals(p, Permission.decode(p.encode()));
    }

    // ===== Sec 3: Revocation tests - revoked permissions are never active =====
    @Test void revokedPermissionIsNeverActive() {
        Instant now = Instant.now();
        Permission p = new Permission("p1", "alice", null, "db", "READ", now.minusSeconds(1), now.plusSeconds(60), false);
        assertFalse(p.revoke().activeAt(now));
    }
}
