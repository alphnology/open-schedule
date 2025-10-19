package com.alphnology.data.repository;

import com.alphnology.data.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author me@fredpena.dev
 * @created 19/10/2025  - 08:40
 */
public interface NewsRepository extends JpaRepository<News, Long>, JpaSpecificationExecutor<News> {

}