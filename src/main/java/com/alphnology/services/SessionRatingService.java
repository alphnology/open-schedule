package com.alphnology.services;

import com.alphnology.data.Session;
import com.alphnology.data.SessionRating;
import com.alphnology.data.User;
import com.alphnology.data.repository.SessionRatingRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Getter
@Service
@RequiredArgsConstructor
public class SessionRatingService {

    private final SessionRatingRepository repository;


    public Optional<SessionRating> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    @Transactional
    public SessionRating save(SessionRating entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }


    public List<SessionRating> findByUser(User user) {
        return repository.findByUsers(user);
    }

    public Optional<SessionRating> findByUsersAndSession(User users, Session session) {
        return repository.findByUsersAndSession(users, session);
    }

}
