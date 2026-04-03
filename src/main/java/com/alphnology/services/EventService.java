package com.alphnology.services;

import com.alphnology.data.Event;
import com.alphnology.data.repository.EventRepository;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;
    private final ObjectStorageService storageService;


    public Optional<Event> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public Event save(Event entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        Event entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found."));

        String photoKey = entity.getPhotoKey();
        repository.deleteById(id);

        if (photoKey != null) {
            try {
                storageService.delete(photoKey);
            } catch (Exception e) {
                log.warn("Could not delete event image from storage: {}", photoKey, e);
            }
        }
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

    public List<Event> findAll(Specification<Event> spec) {
        return repository.findAll(spec);
    }

    public Optional<Event> findCurrentEvent() {
        return repository.findAll().stream().findFirst();
    }


}
