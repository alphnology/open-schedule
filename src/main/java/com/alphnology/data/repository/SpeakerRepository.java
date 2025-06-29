package com.alphnology.data.repository;

import com.alphnology.data.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SpeakerRepository extends JpaRepository<Speaker, Long>, JpaSpecificationExecutor<Speaker> {


    @Query("SELECT count(s) > 0 " +
           "FROM Session s JOIN s.speakers t " +
           "WHERE t.code = :#{#entity.code}")
    boolean hasSessionAssigned(@Param("entity") Speaker entity);
}
