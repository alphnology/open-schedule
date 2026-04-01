package com.alphnology.data.repository;

import com.alphnology.data.Session;
import com.alphnology.data.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface SessionRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {

    @Override
    @EntityGraph(attributePaths = {"room", "track", "speakers", "tags", "ratings"})
    Optional<Session> findById(Long id);

    @Override
    @EntityGraph("Session.withDisplayAssociations")
    List<Session> findAll(Specification<Session> spec);

    @Override
    @EntityGraph("Session.withDisplayAssociations")
    Page<Session> findAll(Specification<Session> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"speakers", "room"})
    @Query("SELECT s FROM Session s WHERE s.code NOT IN " +
           "(SELECT sr.session.code FROM SessionRating sr WHERE sr.users = :user)")
    List<Session> findUnratedSessionsForUser(@Param("user") User user);
}
