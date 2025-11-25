package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RemoveCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: git rm [--cached] [--force] <file>...");
            return;
        }

        boolean cached = false;
        boolean force = false;
        List<String> filePaths = new ArrayList<>();

        for (String arg : args) {
            if ("--cached".equals(arg)) {
                cached = true;
            } else if ("--force".equals(arg) || "-f".equals(arg)) {
                force = true;
            } else {
                filePaths.add(arg);
            }
        }

        if (filePaths.isEmpty()) {
            System.err.println("Usage: git rm [--cached] [--force] <file>...");
            return;
        }

        for (String filePath : filePaths) {
            repository.remove(filePath, cached, force);
        }
    }
}
