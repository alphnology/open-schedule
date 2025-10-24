package com.alphnology.services;

import com.alphnology.data.Attender;
import com.alphnology.data.repository.AttenderRepository;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Getter
@Service
@RequiredArgsConstructor
public class AttenderService {

    private final AttenderRepository repository;


    public Optional<Attender> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public Attender save(Attender entity) {
        return repository.save(entity);
    }

    public void delete(Long id) throws DeleteConstraintViolationException {
        repository.deleteById(id);
    }

    public Page<Attender> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Attender> list(Pageable pageable, Specification<Attender> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<Attender> findAll(@Nullable Specification<Attender> spec) {
        return repository.findAll(spec);
    }

    public List<Attender> findAll() {
        return repository.findAll();
    }


}
