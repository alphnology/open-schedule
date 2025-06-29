package com.alphnology.services.email;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class EmailMessage {

    private Set<String> to;
    private String from;
    private String subject;
    private String body;

    private Set<Path> attachments = new HashSet<>();

    public void attach(Path file) {
        if (attachments == null) {
            attachments = new HashSet<>();
        }
        attachments.add(file);
    }
}
