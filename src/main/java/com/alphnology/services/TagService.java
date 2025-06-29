package com.alphnology.services;

import com.alphnology.data.Tag;
import com.alphnology.data.repository.TagRepository;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Getter
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository repository;

    public Tag save(Tag entity) {
        return repository.save(entity);
    }


    public List<Tag> searchTags(String filter) {
        return repository.findByNameContainingIgnoreCase(filter);
    }

    public Tag getOrCreate(String name) {
        return repository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    return repository.save(tag);
                });
    }

    public List<Tag> searchTags(String filter, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return repository.findByNameContainingIgnoreCase(filter, pageable).getContent();
    }

    public int countTags(String filter) {
        return (int) repository.countByNameContainingIgnoreCase(filter);
    }

    public List<Tag> findAll() {
        return repository.findAll();
    }

    public void delete(Long id) throws DeleteConstraintViolationException {
        Tag entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found."));

        if (repository.hasSessionAssigned(entity)) {
            throw new DeleteConstraintViolationException("The record cannot be deleted because it has assigned sessions.");
        }

        repository.deleteById(id);
    }

    public Page<Tag> list(Pageable pageable, Specification<Tag> filter) {
        return repository.findAll(filter, pageable);
    }


}
