package org.example.commands;

import org.example.objects.Commit;
import org.example.repository.Repository;

import java.io.IOException;
import java.util.List;
import static org.example.utils.Colors.*;

public class LogCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        List<Commit> commits = repository.log();

        if (commits.isEmpty()) {
            System.out.println("No commits yet");
            return;
        }

        for (Commit commit : commits) {
            System.out.println(YELLOW + "commit " + commit.getHexhash() + RESET);
            System.out.println("Author: " + commit.getAuthor());
            System.out.println("Date:   " + formatDate(commit.getCommitter()));
            System.out.println();
            System.out.println("    " + commit.getMessage());
            System.out.println();
        }
    }

    private String formatDate(String committer) {
        if (committer == null) return "Unknown";

        String[] parts = committer.split(" ");
        if (parts.length < 2) return "Unknown";

        try {
            long timestamp = Long.parseLong(parts[parts.length - 2]);
            return new java.util.Date(timestamp * 1000).toString();
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }
}
