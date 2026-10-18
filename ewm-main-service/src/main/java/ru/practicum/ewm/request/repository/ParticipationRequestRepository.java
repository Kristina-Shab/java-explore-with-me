package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByIdInAndEventId(Collection<Long> ids, Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("""
            SELECT r.event.id, COUNT(r)
            FROM ParticipationRequest r
            WHERE r.event.id IN :eventIds
              AND r.status = :status
            GROUP BY r.event.id
            """)
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") Collection<Long> eventIds,
                                            @Param("status") RequestStatus status);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterId(Long userId);
}
