package com.alphnology.services.email;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class EmailMessageOpenSchedule {

    private Set<String> to;
    private String from;
    private String subject;
    private Map<String, Object> model;
    private Set<String> fileIds;
    private String templateId;

}
