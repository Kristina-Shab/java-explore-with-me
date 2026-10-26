package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.CommentRequestDto;
import ru.practicum.ewm.comment.dto.ReactionDto;
import ru.practicum.ewm.comment.model.CommentSort;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, Long eventId, CommentRequestDto dto);

    CommentDto updateComment(Long userId, Long commentId, CommentRequestDto dto);

    void deleteComment(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    CommentDto addReaction(Long userId, Long commentId, ReactionDto dto);

    List<CommentDto> getEventComments(Long eventId, CommentSort sort, int from, int size);

    List<CommentDto> searchComments(List<Long> users,
                                    List<Long> events,
                                    LocalDateTime rangeStart,
                                    LocalDateTime rangeEnd,
                                    int from,
                                    int size);

    void deleteComment(Long commentId);
}
