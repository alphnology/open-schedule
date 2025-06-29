package com.alphnology.services;

import com.alphnology.data.Track;
import com.alphnology.data.repository.TrackRepository;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import jakarta.persistence.EntityNotFoundException;
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
public class TrackService {

    private final TrackRepository repository;


    public Optional<Track> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public Track save(Track entity) {
        return repository.save(entity);
    }

    public void delete(Long id) throws DeleteConstraintViolationException {
        Track entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Track not found."));

        if (repository.hasSessionAssigned(entity)) {
            throw new DeleteConstraintViolationException("The record cannot be deleted because it has assigned sessions.");
        }

        repository.deleteById(id);
    }

    public Page<Track> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Track> list(Pageable pageable, Specification<Track> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<Track> findAll() {
        return repository.findAll();
    }


}
