package com.alphnology.data.repository;

import com.alphnology.data.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

}
