package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    String SEARCH_EVENTS_QUERY = """
            SELECT e FROM Event e
            JOIN FETCH e.initiator
            JOIN FETCH e.category
            WHERE e.state = :state
            AND (:text IS NULL
                OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', COALESCE(:text, ''), '%'))
                OR LOWER(e.description) LIKE LOWER(CONCAT('%', COALESCE(:text, ''), '%')))
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:paid IS NULL OR e.paid = :paid)
            AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            AND (:onlyAvailable = FALSE OR e.participantLimit = 0
                OR (SELECT COUNT(r) FROM ParticipationRequest r
                    WHERE r.event.id = e.id AND r.status = 'CONFIRMED') < e.participantLimit)
            """;

    String SEARCH_EVENTS_WITHOUT_TEXT_QUERY = """
            SELECT e FROM Event e
            JOIN FETCH e.initiator
            JOIN FETCH e.category
            WHERE e.state = :state
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (:paid IS NULL OR e.paid = :paid)
              AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
              AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
              AND (:onlyAvailable = FALSE OR e.participantLimit = 0
                   OR (SELECT COUNT(r) FROM ParticipationRequest r
                       WHERE r.event.id = e.id AND r.status = 'CONFIRMED') < e.participantLimit)
            """;

    boolean existsByCategoryId(Long catId);

    @Query("SELECT e FROM Event e JOIN FETCH e.initiator JOIN FETCH e.category WHERE e.initiator.id = :userId")
    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    @Query(SEARCH_EVENTS_QUERY)
    Page<Event> searchEvents(@Param("state") EventState state,
                             @Param("text") String text,
                             @Param("categories") List<Long> categories,
                             @Param("paid") Boolean paid,
                             @Param("rangeStart") LocalDateTime rangeStart,
                             @Param("rangeEnd") LocalDateTime rangeEnd,
                             @Param("onlyAvailable") Boolean onlyAvailable,
                             Pageable pageable);

    @Query(SEARCH_EVENTS_QUERY)
    List<Event> searchEventsNoPaging(@Param("state") EventState state,
                                     @Param("text") String text,
                                     @Param("categories") List<Long> categories,
                                     @Param("paid") Boolean paid,
                                     @Param("rangeStart") LocalDateTime rangeStart,
                                     @Param("rangeEnd") LocalDateTime rangeEnd,
                                     @Param("onlyAvailable") Boolean onlyAvailable);

    @Query(SEARCH_EVENTS_WITHOUT_TEXT_QUERY)
    Page<Event> searchEventsWithoutText(@Param("state") EventState state,
                                        @Param("categories") List<Long> categories,
                                        @Param("paid") Boolean paid,
                                        @Param("rangeStart") LocalDateTime rangeStart,
                                        @Param("rangeEnd") LocalDateTime rangeEnd,
                                        @Param("onlyAvailable") Boolean onlyAvailable,
                                        Pageable pageable);

    @Query(SEARCH_EVENTS_WITHOUT_TEXT_QUERY)
    List<Event> searchEventsNoPagingWithoutText(@Param("state") EventState state,
                                                @Param("categories") List<Long> categories,
                                                @Param("paid") Boolean paid,
                                                @Param("rangeStart") LocalDateTime rangeStart,
                                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                                @Param("onlyAvailable") Boolean onlyAvailable);

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.initiator
            JOIN FETCH e.category
            WHERE (:users IS NULL OR e.initiator.id IN :users)
              AND (:states IS NULL OR e.state IN :states)
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
              AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            """)
    Page<Event> searchEventsAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<EventState> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  Pageable pageable);

    boolean existsByInitiatorId(Long userId);
}
