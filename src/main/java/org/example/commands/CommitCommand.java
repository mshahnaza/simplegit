package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public class CommitCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        String message = extractMessage(args);
        if (message == null || message.isEmpty()) {
            System.err.println("Usage: git commit -m \"message\"");
            return;
        }
        String author = System.getProperty("user.name", "Unknown") +
                " <" + System.getProperty("user.name", "unknown") + "@localhost>";
        repository.commit(message, author);
    }

    private String extractMessage(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("-m".equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

}
