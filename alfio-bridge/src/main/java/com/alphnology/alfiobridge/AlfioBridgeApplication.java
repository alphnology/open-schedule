package com.alphnology.alfiobridge;

import com.alphnology.alfiobridge.config.AlfioBridgeSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AlfioBridgeSecurityProperties.class)
public class AlfioBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlfioBridgeApplication.class, args);
    }
}
