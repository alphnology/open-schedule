package com.alphnology.services.template.impl;

import com.alphnology.services.template.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class ThymeleafTemplateServiceImpl implements TemplateService {

    private final TemplateEngine templateEngine;

    @Override
    public String render(String templateId, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);

        return templateEngine.process(templateId, context);
    }

}
