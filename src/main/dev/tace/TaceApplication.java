package dev.tace;

import picocli.CommandLine;
import dev.tace.cli.TaceCommand;

public final class TaceApplication {
    // ===== Sec 1: Application entry point - builds the CLI and exits with its status code =====
    public static void main(String[] args) {
        int exit = new CommandLine(new TaceCommand()).execute(args);
        System.exit(exit);
    }
}
