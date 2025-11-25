package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public class InitCommand implements Command {

    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        repository.init();
    }
}
