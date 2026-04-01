package com.alphnology.security;

import com.alphnology.views.login.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/images/*.png", "/icons/**", "/line-awesome/**")
                        .permitAll()
                        .requestMatchers("/share/**")
                        .anonymous())
                .with(VaadinSecurityConfigurer.vaadin(), configurer ->
                        configurer.loginView(LoginView.class))
                .build();
    }
}
