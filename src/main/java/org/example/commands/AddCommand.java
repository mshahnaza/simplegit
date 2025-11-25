package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public class AddCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: git add <file> or git add .");
            return;
        }
        String filename = args[0];
        if(filename.equals(".")) {
            repository.addAll();
        } else {
            repository.add(filename);
        }
    }
}
