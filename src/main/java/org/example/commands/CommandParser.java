package org.example.commands;

import java.util.Map;
import java.util.HashMap;

public class CommandParser {

    private final Map<String, Command> commands = new HashMap<>();

    public CommandParser() {
        commands.put("init", new InitCommand());
        commands.put("add", new AddCommand());
        commands.put("commit", new CommitCommand());
        commands.put("status", new StatusCommand());
        commands.put("log", new LogCommand());
        commands.put("branch", new BranchCommand());
        commands.put("checkout", new CheckoutCommand());
    }

    public Command parse(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("No command specified");
        }

        String commandName = args[0];
        Command command = commands.get(commandName);

        if (command == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }

        return command;
    }
}
