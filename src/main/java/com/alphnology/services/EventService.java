package com.alphnology.services;

import com.alphnology.data.Event;
import com.alphnology.data.repository.EventRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Getter
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;


    public Optional<Event> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public Event save(Event entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Event> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Event> list(Pageable pageable, Specification<Event> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<Event> findAll() {
        return repository.findAll();
    }


}
