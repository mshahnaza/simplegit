package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public class StatusCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        repository.status();
    }
}
