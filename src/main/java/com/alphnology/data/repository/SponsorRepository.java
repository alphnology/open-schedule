package com.alphnology.data.repository;

import com.alphnology.data.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface SponsorRepository extends JpaRepository<Sponsor, Long>, JpaSpecificationExecutor<Sponsor> {

}
