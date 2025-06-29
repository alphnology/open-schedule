package com.alphnology.configurations;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
        Map<String, Object> envProperties = new HashMap<>();

        dotenv.entries().forEach(entry -> {
            envProperties.put(entry.getKey(), entry.getValue());
            envProperties.put(entry.getKey().replaceAll("[_-]", ".").toLowerCase(), entry.getValue());
        });

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, envProperties));
    }
}
