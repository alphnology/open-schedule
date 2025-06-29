package com.alphnology.data.repository;

import com.alphnology.data.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author me@fredpena.dev
 * @created 15/06/2025  - 19:05
 */
public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {

    Optional<Tag> findByNameIgnoreCase(String name);

    List<Tag> findByNameContainingIgnoreCase(String filter);

    Page<Tag> findByNameContainingIgnoreCase(String filter, Pageable pageable);

    long countByNameContainingIgnoreCase(String filter);

    @Query("SELECT count(s) > 0 " +
           "FROM Session s JOIN s.tags t " +
           "WHERE t.code = :#{#entity.code}")
    boolean hasSessionAssigned(@Param("entity") Tag entity);
}