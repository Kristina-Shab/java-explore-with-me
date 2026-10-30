package ru.practicum.ewm.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.comment.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE (:users IS NULL OR c.author.id IN :users)
              AND (:events IS NULL OR c.event.id IN :events)
              AND (CAST(:rangeStart AS timestamp) IS NULL OR c.createdOn >= :rangeStart)
              AND (CAST(:rangeEnd AS timestamp) IS NULL OR c.createdOn <= :rangeEnd)
            """)
    List<Comment> searchComments(@Param("users") List<Long> users,
                                 @Param("events") List<Long> events,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 Pageable pageable);

    @Query("""
            SELECT c,
                   SUM(CASE WHEN r.useful = true THEN 1 ELSE 0 END) as usefulCount,
                   SUM(CASE WHEN r.useful = false THEN 1 ELSE 0 END) as uselessCount
            FROM Comment c
            LEFT JOIN c.reactions r
            WHERE c.event.id = :eventId
            GROUP BY c.id
            ORDER BY
                CASE WHEN :sort = 'RATING' THEN (SUM(CASE WHEN r.useful = true THEN 1 ELSE 0 END) -
                        SUM(CASE WHEN r.useful = false THEN 1 ELSE 0 END)) ELSE 0 END DESC,
                CASE WHEN :sort = 'POPULARITY' THEN COUNT(r) ELSE 0 END DESC,
                CASE WHEN :sort = 'OLDEST' THEN c.createdOn END ASC,
                CASE WHEN :sort != 'OLDEST' OR :sort IS NULL THEN c.createdOn END DESC
            """)
    Page<Object[]> findEventCommentsWithReactions(
            @Param("eventId") Long eventId,
            @Param("sort") String sort,
            Pageable pageable);

    @Query("SELECT c.event.id, COUNT(c) FROM Comment c WHERE c.event.id IN :eventIds GROUP BY c.event.id")
    List<Object[]> countCommentsForEvents(@Param("eventIds") List<Long> eventIds);

    long countByEventId(Long eventId);
}
