package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;

public interface Command {
    void execute(String[] args, Repository repository) throws IOException;
}
