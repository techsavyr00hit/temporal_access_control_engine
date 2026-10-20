# Temporal Access-Control Engine (TACE)

A Java 21 CLI for time-aware role-based access control backed by Valkey. This is a runnable starter codebase intended for local development and extension, not a hardened production IAM service.

## Requirements
- JDK 21+
- Maven 3.9+
- Docker (recommended for local Valkey)

## Run
```bash
docker compose up -d
mvn clean test
mvn compile exec:java -Dexec.args="user create --name alice"
mvn compile exec:java -Dexec.args="permission grant --user alice --resource production-db --action WRITE --duration 30m"
mvn compile exec:java -Dexec.args="authorize --user alice --resource production-db --action WRITE"
mvn compile exec:java -Dexec.args="permission list --user alice"
mvn compile exec:java -Dexec.args="audit --user alice"
```

Or build a jar:
```bash
mvn package
java -jar target/temporal-access-control-engine-1.0.0.jar --help
```
(If you want a self-contained executable jar, add Maven Shade Plugin; the `exec:java` workflow works without it.)

## Commands
```text
user create --name alice
user list
role create --name developer
role assign --user alice --role developer
permission grant --user alice --resource production-db --action WRITE --duration 30m
permission grant --user alice --resource production-db --action WRITE --starts-at 2026-10-09T10:00:00+05:30 --expires-at 2026-10-09T12:00:00+05:30
permission revoke --id perm-...
permission list --user alice
authorize --user alice --resource production-db --action WRITE
audit [--user alice]
```

## Authorization model
- A user can receive a direct permission grant or inherit a permission from an assigned role.
- Grant validity is `[startsAt, expiresAt)`: the start instant is inclusive and expiry instant exclusive.
- Revocation is checked from the stored permission record; Valkey TTL is only cleanup, never the security decision.
- Authorization decisions and administrative changes are appended to an audit list.
- Permission mutations use Valkey transactions (`MULTI/EXEC`) for the record/index/audit writes.

## Data layout
- `tace:user:<name>` — user marker
- `tace:role:<name>` — role marker
- `tace:user-roles:<name>` — set of assigned role names
- `tace:role-grants:<role>` — set of permission IDs attached to a role
- `tace:permission:<id>` — serialized permission record
- `tace:user-grants:<user>` — set of direct permission IDs
- `tace:audit` — audit event list

## Limitations / next steps
- This starter stores records in Valkey; configure Valkey persistence/backups for durable state. For high-assurance deployments, keep authoritative grants and audit events in a durable database and use Valkey as a cache.
- CLI user/role management is intentionally minimal; no authentication of the administrator is implemented.
- Audit records are append-only at the application level but not tamper-proof against a Valkey administrator.
- Add policy migrations, pagination, structured output, command-level authorization, robust clock tests, and multi-process race tests before production use.
