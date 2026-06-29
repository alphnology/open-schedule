package com.alphnology.data.repository;

import com.alphnology.data.WorkshopRegistrationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkshopRegistrationSettingsRepository extends JpaRepository<WorkshopRegistrationSettings, Long> {

    Optional<WorkshopRegistrationSettings> findBySingletonKey(String singletonKey);
}
