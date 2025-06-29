package com.alphnology.data.repository;

import com.alphnology.data.Session;
import com.alphnology.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface SessionRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {


    @Query("SELECT s FROM Session s WHERE s.code NOT IN " +
           "(SELECT sr.session.code FROM SessionRating sr WHERE sr.users = :user)")
    List<Session> findUnratedSessionsForUser(@Param("user") User user);
}
