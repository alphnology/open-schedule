package com.alphnology.services.template;

import java.util.Map;

public interface TemplateService {

    String render(String templateId, Map<String, Object> model);

}
