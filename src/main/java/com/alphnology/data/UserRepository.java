package com.alphnology.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {"ratings"})
    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"ratings"})
    Optional<User> findById(Long id);
}
