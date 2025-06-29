package com.alphnology.services;

import com.alphnology.data.Room;
import com.alphnology.data.repository.RoomRepository;
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
public class RoomService {

    private final RoomRepository repository;


    public Optional<Room> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public Room save(Room entity) {
        return repository.save(entity);
    }

    public void delete(Long id) throws DeleteConstraintViolationException {
        Room entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found."));

        if (repository.hasSessionAssigned(entity)) {
            throw new DeleteConstraintViolationException("The tag cannot be deleted because it has assigned sessions.");
        }

        repository.deleteById(id);
    }

    public Page<Room> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Room> list(Pageable pageable, Specification<Room> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<Room> findAll() {
        return repository.findAll();
    }


}
