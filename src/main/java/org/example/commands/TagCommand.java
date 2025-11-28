package org.example.commands;

import org.example.repository.Repository;

import java.io.IOException;
import java.util.List;

public class TagCommand implements Command {
    @Override
    public void execute(String[] args, Repository repository) throws IOException {
        if (args.length == 0) {
            List<String> tags = repository.listTags();
            if (tags.isEmpty()) {
                System.out.println("No tags yet");
            } else {
                tags.forEach(System.out::println);
            }
        } else if ("-d".equals(args[0]) && args.length > 1) {
            repository.deleteTag(args[1]);
        } else if (args.length == 1 && !args[0].equals("-d") && !args[0].equals("show")) {
            repository.createTag(args[0]);
        } else if (args.length == 2 && "show".equals(args[0])) {
            repository.showTag(args[1]);
        } else {
            System.err.println("Usage: git tag [-d] <tagname>");
            System.err.println("       git tag show <tagname>");
        }
    }
}