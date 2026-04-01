package com.alphnology.data.repository;

import com.alphnology.data.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author me@fredpena.dev
 * @created 19/10/2025  - 08:40
 */
public interface NewsRepository extends JpaRepository<News, Long>, JpaSpecificationExecutor<News> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    List<News> findAll(Specification<News> spec);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<News> findAll(Specification<News> spec, Pageable pageable);

}