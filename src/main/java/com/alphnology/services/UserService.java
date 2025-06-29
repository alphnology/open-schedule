package com.alphnology.services;

import com.alphnology.data.User;
import com.alphnology.data.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Getter
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;


    public Optional<User> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    public User save(User entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<User> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<User> list(Pageable pageable, Specification<User> filter) {
        return repository.findAll(filter, pageable);
    }

    public boolean isThisUserNotAlreadyRegistered(String userCode) {
        return repository.findByUsername(userCode).isEmpty();
    }

}
