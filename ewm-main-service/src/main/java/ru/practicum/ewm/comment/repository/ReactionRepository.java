package ru.practicum.ewm.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.comment.model.Reaction;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentIdAndUseful(Long commentId, Boolean useful);

    @Query("""
            SELECT r.comment.id, r.useful, COUNT(r)
            FROM Reaction r
            WHERE r.comment.id IN :commentIds
            GROUP BY r.comment.id, r.useful
            """)
    List<Object[]> countReactionsForComments(@Param("commentIds") List<Long> commentIds);
}
