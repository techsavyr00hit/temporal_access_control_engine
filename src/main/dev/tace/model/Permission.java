package dev.tace.model;

import java.time.Instant;
import java.util.Base64;

// ===== Sec 1: Data model - the Permission record and its fields =====
public record Permission(String id, String user, String role, String resource, String action,
                         Instant startsAt, Instant expiresAt, boolean revoked) {
    // ===== Sec 2: Temporal rules - active-window check and revocation =====
    public boolean activeAt(Instant now) {
        return !revoked && !now.isBefore(startsAt) && now.isBefore(expiresAt);
    }

    public Permission revoke() {
        return new Permission(id, user, role, resource, action, startsAt, expiresAt, true);
    }

    // ===== Sec 3: Serialization - encode/decode for storage, with Base64 helpers =====
    // Base64 fields make the simple pipe-delimited representation safe for ordinary CLI input.
    public String encode() {
        return String.join("|", enc(id), enc(user), enc(role == null ? "" : role), enc(resource),
                enc(action), startsAt.toString(), expiresAt.toString(), Boolean.toString(revoked));
    }

    public static Permission decode(String s) {
        String[] p = s.split("\\|", -1);
        if (p.length != 8) throw new IllegalArgumentException("Corrupt permission record");
        return new Permission(dec(p[0]), dec(p[1]), dec(p[2]).isEmpty() ? null : dec(p[2]),
                dec(p[3]), dec(p[4]), Instant.parse(p[5]), Instant.parse(p[6]),
                Boolean.parseBoolean(p[7]));
    }
    private static String enc(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private static String dec(String s) { return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8); }
}