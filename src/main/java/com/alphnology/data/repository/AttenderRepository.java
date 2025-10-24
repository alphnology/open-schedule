package com.alphnology.data.repository;

import com.alphnology.data.Attender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface AttenderRepository extends JpaRepository<Attender, Long>, JpaSpecificationExecutor<Attender> {


}
