package com.alphnology.services;

import com.alphnology.data.Speaker;
import com.alphnology.data.repository.SpeakerRepository;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class SpeakerService {

    private final SpeakerRepository repository;
    private final ObjectStorageService storageService;


    public Optional<Speaker> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public Speaker save(Speaker entity) {
        return repository.save(entity);
    }

    public void delete(Long id) throws DeleteConstraintViolationException {
        Speaker entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found."));

        if (repository.hasSessionAssigned(entity)) {
            throw new DeleteConstraintViolationException("The record cannot be deleted because it has assigned sessions.");
        }

        String photoKey = entity.getPhotoKey();
        repository.deleteById(id);

        if (photoKey != null) {
            try {
                storageService.delete(photoKey);
            } catch (Exception e) {
                log.warn("Could not delete speaker photo from storage: {}", photoKey, e);
            }
        }
    }

    public Page<Speaker> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Speaker> list(Pageable pageable, Specification<Speaker> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<Speaker> findAll(@Nullable Specification<Speaker> spec) {
        return repository.findAll(spec);
    }

    public List<Speaker> findAll() {
        return repository.findAll();
    }

    public long count() {
        return repository.count();
    }

}
