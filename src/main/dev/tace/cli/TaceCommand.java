package dev.tace.cli;

import dev.tace.model.Permission;
import dev.tace.service.TaceService;
import dev.tace.store.ValkeyStore;
import picocli.CommandLine;
import picocli.CommandLine.*;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.Callable;

// ===== Sec 1: Root command - top-level CLI setup and service factory =====
@Command(name="tace", mixinStandardHelpOptions=true, version="TACE 1.0.0",
 description="Temporal Access-Control Engine CLI",
 subcommands={TaceCommand.User.class, TaceCommand.Role.class, TaceCommand.PermissionCmd.class,
              TaceCommand.Authorize.class, TaceCommand.Audit.class})
public class TaceCommand implements Runnable {
    @Override public void run() { new CommandLine(this).usage(System.out); }
    static TaceService service() { return new TaceService(new ValkeyStore()); }

    // ===== Sec 2: User commands - create and list users =====
    @Command(name="user", description="Manage users", subcommands={UserCreate.class, UserList.class})
    static class User implements Runnable { public void run() { new CommandLine(this).usage(System.out); } }
    @Command(name="create", description="Create a user")
    static class UserCreate implements Callable<Integer> {
        @Option(names="--name", required=true) String name;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { new TaceService(s).createUser(name); s.sadd("tace:users", name); System.out.println("Created user " + name); return 0; } }
    }
    @Command(name="list", description="List users")
    static class UserList implements Callable<Integer> {
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { System.out.println(String.join(System.lineSeparator(), new TaceService(s).users())); return 0; } }
    }

    // ===== Sec 3: Role commands - create roles and assign them to users =====
    @Command(name="role", description="Manage roles", subcommands={RoleCreate.class, RoleAssign.class})
    static class Role implements Runnable { public void run() { new CommandLine(this).usage(System.out); } }
    @Command(name="create", description="Create a role")
    static class RoleCreate implements Callable<Integer> {
        @Option(names="--name", required=true) String name;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { new TaceService(s).createRole(name); System.out.println("Created role " + name); return 0; } }
    }
    @Command(name="assign", description="Assign a role to a user")
    static class RoleAssign implements Callable<Integer> {
        @Option(names="--user", required=true) String user;
        @Option(names="--role", required=true) String role;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { new TaceService(s).assignRole(user, role); System.out.println("Assigned " + role + " to " + user); return 0; } }
    }

    // ===== Sec 4: Permission commands - grant, revoke and list permissions =====
    @Command(name="permission", description="Manage permissions", subcommands={Grant.class, Revoke.class, ListPermissions.class})
    static class PermissionCmd implements Runnable { public void run() { new CommandLine(this).usage(System.out); } }
    @Command(name="grant", description="Grant a time-bounded permission")
    static class Grant implements Callable<Integer> {
        @Option(names="--user", required=true) String user;
        @Option(names="--resource", required=true) String resource;
        @Option(names="--action", required=true) String action;
        @Option(names="--duration", description="e.g. 30m, 2h, 1d") String duration;
        @Option(names="--starts-at", description="ISO-8601 offset datetime") String startsAt;
        @Option(names="--expires-at", description="ISO-8601 offset datetime") String expiresAt;
        public Integer call() {
            if ((duration == null) == (expiresAt == null)) throw new ParameterException(new CommandLine(this), "Specify exactly one of --duration or --expires-at");
            Instant start = startsAt == null ? Instant.now() : OffsetDateTime.parse(startsAt).toInstant();
            Instant end = expiresAt == null ? null : OffsetDateTime.parse(expiresAt).toInstant();
            Duration d = duration == null ? null : parseDuration(duration);
            try (ValkeyStore s=new ValkeyStore()) {
                Permission p = new TaceService(s).grant(user, resource, action, d, start, end);
                System.out.println("Permission granted: " + p.id());
                System.out.println("Valid from: " + p.startsAt() + " until: " + p.expiresAt());
                return 0;
            }
        }
        private static Duration parseDuration(String s) {
            if (!s.matches("[1-9][0-9]*(s|m|h|d)")) throw new IllegalArgumentException("Duration must look like 30m, 2h, or 1d");
            long n = Long.parseLong(s.substring(0, s.length()-1));
            return switch (s.charAt(s.length()-1)) { case 's' -> Duration.ofSeconds(n); case 'm' -> Duration.ofMinutes(n); case 'h' -> Duration.ofHours(n); default -> Duration.ofDays(n); };
        }
    }
    @Command(name="revoke", description="Revoke a permission")
    static class Revoke implements Callable<Integer> {
        @Option(names="--id", required=true) String id;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { new TaceService(s).revoke(id); System.out.println("Revoked " + id); return 0; } }
    }
    @Command(name="list", description="List a user's grants")
    static class ListPermissions implements Callable<Integer> {
        @Option(names="--user", required=true) String user;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { new TaceService(s).permissions(user).forEach(System.out::println); return 0; } }
    }

    // ===== Sec 5: Authorization and audit commands - check access and view audit log =====
    @Command(name="authorize", description="Evaluate an authorization request")
    static class Authorize implements Callable<Integer> {
        @Option(names="--user", required=true) String user;
        @Option(names="--resource", required=true) String resource;
        @Option(names="--action", required=true) String action;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { System.out.println(new TaceService(s).authorize(user, resource, action)); return 0; } }
    }
    @Command(name="audit", description="Show recent audit events")
    static class Audit implements Callable<Integer> {
        @Option(names="--user") String user;
        public Integer call() { try (ValkeyStore s=new ValkeyStore()) { new TaceService(s).audit(user).forEach(System.out::println); return 0; } }
    }
}