package org.example.commands;

import org.example.repository.Repository;
import static org.example.utils.Colors.*;

import java.io.IOException;
import java.util.List;

public class BranchCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        if (args.length == 0) {
            List<String> branches = repository.listBranches();
            String currentBranch = repository.getCurrentBranch();

            for (String branch : branches) {
                if (branch.equals(currentBranch)) {
                    System.out.println(GREEN_BOLD + "* " + branch + RESET);
                } else {
                    System.out.println("  " + branch);
                }
            }
            return;
        }

        if (args.length >= 2 && "-d".equals(args[0])) {
            repository.deleteBranch(args[1]);
        } else if (args.length == 1) {
            repository.createBranch(args[0]);
        } else {
            System.err.println("Usage: git branch [-d] [branch-name]");
        }
    }
}
