# Implementation notes

This codebase provides the CLI and core temporal grant flow. Review these points before describing it as production-ready:

1. `role assign` persists user-role membership, but this starter does not yet implement role-derived permission grants. Authorization currently uses direct user grants.
2. `user create` updates the user index used by `user list`.
3. Valkey Java client APIs can vary by released version; if your local Maven resolver reports a mismatch around transaction or client imports, use the Valkey Java client version compatible with the installed Valkey release and adjust `ValkeyStore` accordingly.
4. Authorization scans the user's grant IDs, which is suitable for a small demo. For large grant counts, maintain an index by `(user, resource, action)` and use a Lua script or transaction protocol to make revocation and evaluation semantics explicit.
5. A production implementation should define whether a revocation racing with an authorization check is allowed to complete that check. For strict linearizable decisions, move the complete read/evaluate/audit sequence into a server-side Lua script or another atomic coordination mechanism.
\n\n## Storage client correction\n\nThe original adapter imported unavailable `io.valkey.*` classes. This version uses Jedis 5.2.0, a Redis-protocol Java client compatible with Valkey, and a pooled connection per operation. Run `mvn clean test` to validate the patched project locally.\n