package com.alphnology.data.repository;

import com.alphnology.data.WorkshopParticipantRegistration;
import com.alphnology.data.enums.WorkshopRegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface WorkshopParticipantRegistrationRepository extends JpaRepository<WorkshopParticipantRegistration, Long>,
        JpaSpecificationExecutor<WorkshopParticipantRegistration> {

    @EntityGraph(attributePaths = {"session", "session.room", "session.track"})
    Optional<WorkshopParticipantRegistration> findByEventSlugAndTicketReference(String eventSlug, String ticketReference);

    long countBySession_CodeAndStatus(Long sessionCode, WorkshopRegistrationStatus status);

    long countBySession_CodeAndStatusAndCodeNot(Long sessionCode, WorkshopRegistrationStatus status, Long code);

    @Override
    @EntityGraph(attributePaths = {"session", "session.room", "session.track"})
    List<WorkshopParticipantRegistration> findAll();

    @Override
    @EntityGraph(attributePaths = {"session", "session.room", "session.track"})
    List<WorkshopParticipantRegistration> findAll(Specification<WorkshopParticipantRegistration> spec);
}
