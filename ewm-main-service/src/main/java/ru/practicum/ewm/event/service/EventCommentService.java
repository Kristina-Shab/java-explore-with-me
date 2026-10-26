package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.repository.CommentRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventCommentService {
    private final CommentRepository commentRepository;

    public Long getCommentCount(Long eventId) {
        return commentRepository.countByEventId(eventId);
    }

    public Map<Long, Long> getCommentCountsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countCommentsForEvents(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
