package com.alphnology.data.repository;

import com.alphnology.data.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    @Query("SELECT count(s) > 0 FROM Session s WHERE s.room = :entity")
    boolean hasSessionAssigned(@Param("entity") Room entity);
}
