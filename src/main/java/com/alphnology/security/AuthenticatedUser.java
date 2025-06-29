package com.alphnology.security;

import com.alphnology.data.User;
import com.alphnology.data.UserRepository;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticatedUser {

    private final UserRepository userRepository;
    private final AuthenticationContext authenticationContext;

    @Transactional
    public Optional<User> get() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(auth -> {

                    User user = userRepository.findByUsername(auth.getUsername()).orElseThrow(() -> new UsernameNotFoundException("No user present with username: " + auth.getUsername()));

                    if (VaadinSession.getCurrent().getAttribute(User.class) == null) {
                        user.setLastLoginTs(Instant.now());
                        userRepository.save(user);
                    }

                    VaadinSession.getCurrent().setAttribute(User.class, user);

                    return user;
                });
    }


    public void logout() {
        authenticationContext.logout();
        VaadinSession.getCurrent().setAttribute(User.class, null);
    }


}
