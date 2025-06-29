package com.alphnology.services;

import com.alphnology.data.Session;
import com.alphnology.data.User;
import com.alphnology.data.repository.SessionRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Getter
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository repository;


    public Optional<Session> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    @Transactional
    public Session save(Session entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Session> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Session> list(Pageable pageable, Specification<Session> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<Session> findAll(@Nullable Specification<Session> spec) {
        return repository.findAll(spec);
    }

    public List<Session> findUnratedSessionsForUser(User user) {
        return repository.findUnratedSessionsForUser(user);
    }


}
