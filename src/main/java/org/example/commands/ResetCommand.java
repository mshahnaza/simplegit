package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public class ResetCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        if (args.length == 0) {
            repository.reset("--mixed", "HEAD");
        } else if (args.length == 1) {
            if (args[0].startsWith("--")) {
                repository.reset(args[0], "HEAD");
            } else {
                repository.reset("--mixed", args[0]);
            }
        } else if (args.length == 2) {
            repository.reset(args[0], args[1]);
        } else {
            System.err.println("Usage: git reset [--soft | --mixed | --hard] [<commit>]");
            System.err.println("    --soft    reset only HEAD");
            System.err.println("    --mixed   reset HEAD and index (default)");
            System.err.println("    --hard    reset HEAD, index and working directory");
        }
    }
}