package com.alphnology.data.repository;

import com.alphnology.data.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface TrackRepository extends JpaRepository<Track, Long>, JpaSpecificationExecutor<Track> {

    @Query("SELECT count(s) > 0 FROM Session s WHERE s.track = :entity")
    boolean hasSessionAssigned(@Param("entity") Track entity);

}
