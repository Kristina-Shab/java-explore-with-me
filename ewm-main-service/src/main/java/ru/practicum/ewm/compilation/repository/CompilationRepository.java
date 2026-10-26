package ru.practicum.ewm.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.compilation.model.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @Query("""
            SELECT DISTINCT c FROM Compilation c
            LEFT JOIN FETCH c.events
            WHERE (:pinned IS NULL OR c.pinned = :pinned)
            """)
    Page<Compilation> findAllByPinned(Boolean pinned, Pageable pageable);
}
