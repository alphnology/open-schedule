package com.alphnology.data.repository;

import com.alphnology.data.Speaker;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface SpeakerRepository extends JpaRepository<Speaker, Long>, JpaSpecificationExecutor<Speaker> {

    @Override
    @EntityGraph(attributePaths = {"sessions", "sessions.room", "sessions.tags"})
    Optional<Speaker> findById(Long id);


    @Query("SELECT count(s) > 0 " +
           "FROM Session s JOIN s.speakers t " +
           "WHERE t.code = :#{#entity.code}")
    boolean hasSessionAssigned(@Param("entity") Speaker entity);
}
