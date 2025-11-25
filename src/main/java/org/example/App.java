package org.example;

import org.example.commands.Command;
import org.example.commands.CommandParser;
import org.example.repository.Repository;

import java.nio.file.Paths;
import java.util.Arrays;

public class App 
{
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("Usage: git <command> [<args>]");
                System.err.println("Available commands: init, add, commit, status, log, branch, checkout");
                System.exit(1);
            }

            Repository repo = new Repository(Paths.get("."));

            if (args.length > 0 &&
                    !args[0].equals("init") &&
                    !args[0].equals("help") &&
                    !args[0].equals("--help") &&
                    !args[0].equals("-h")) {

                if (!repo.isRepository()) {
                    System.err.println("fatal: not a git repository (or any of the parent directories): .git");
                    System.exit(1);
                }
            }

            CommandParser parser = new CommandParser();
            Command command = parser.parse(args);

            String[] commandArgs = args.length > 0 ?
                    Arrays.copyOfRange(args, 1, args.length) : new String[0];

            command.execute(commandArgs, repo);

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (System.getenv("DEBUG") != null) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
