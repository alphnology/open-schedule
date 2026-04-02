package com.alphnology.data.repository;

import com.alphnology.data.MailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailSettingsRepository extends JpaRepository<MailSettings, Long> {

    Optional<MailSettings> findBySingletonKey(String singletonKey);
}
