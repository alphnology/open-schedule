package com.alphnology.data.repository;

import com.alphnology.data.Session;
import com.alphnology.data.SessionRating;
import com.alphnology.data.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface SessionRatingRepository extends JpaRepository<SessionRating, Long> {

    @EntityGraph(attributePaths = {"session.speakers", "session.room"})
    List<SessionRating> findByUsers(User user);

    Optional<SessionRating> findByUsersAndSession(User users, Session session);
}
