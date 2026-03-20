package com.alphnology.services;

import com.alphnology.data.News;
import com.alphnology.data.repository.NewsRepository;
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
public class NewsService {

    private final NewsRepository repository;
    private final ObjectStorageService storageService;


    public Optional<News> get(Long id) {
        if (id == null) return Optional.empty();

        return repository.findById(id);
    }


    public News save(News entity) {
        return repository.save(entity);
    }

    public void delete(Long id) throws DeleteConstraintViolationException {
        News entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("News not found."));

        String photoKey = entity.getPhotoKey();
        repository.deleteById(entity.getCode());

        if (photoKey != null) {
            try {
                storageService.delete(photoKey);
            } catch (Exception e) {
                log.warn("Could not delete news photo from storage: {}", photoKey, e);
            }
        }
    }

    public Page<News> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<News> list(Pageable pageable, Specification<News> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<News> findAll(@Nullable Specification<News> spec) {
        return repository.findAll(spec);
    }

    public List<News> findAll() {
        return repository.findAll();
    }


}
