package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public class CheckoutCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: git checkout <branch|commit>");
            return;
        }

        if (args.length >= 2 && "-b".equals(args[0])) {
            String newBranch = args[1];
            repository.checkoutB(newBranch);
        } else {
            String target = args[0];
            repository.checkout(target);
        }
    }
}
