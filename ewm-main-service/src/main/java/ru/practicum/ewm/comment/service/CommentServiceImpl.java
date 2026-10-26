package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.CommentRequestDto;
import ru.practicum.ewm.comment.dto.ReactionDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.CommentSort;
import ru.practicum.ewm.comment.model.Reaction;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.comment.repository.ReactionRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.validation.EventValidatorRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final EventValidatorRequest validator;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, CommentRequestDto dto) {
        User author = validator.findUserById(userId);
        Event event = validator.findEventById(eventId);
        if (event.getState() != EventState.PUBLISHED
                && !event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие, eventId = " + eventId);
        }
        Comment comment = CommentMapper.toEntity(dto, author, event);
        Comment saved = commentRepository.save(comment);
        log.info("Создан комментарий: id={}, eventId={}, authorId={}", saved.getId(), eventId, userId);
        return CommentMapper.toDto(saved, 0L, 0L);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, CommentRequestDto dto) {
        Comment comment = findCommentById(commentId);
        checkAuthorComment(comment, userId);
        CommentMapper.updateEntity(comment, dto);
        log.info("Обновлен комментарий: id={}", commentId);
        return toDtoWithReactions(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = findCommentById(commentId);
        checkAuthorComment(comment, userId);
        commentRepository.delete(comment);
        log.info("Удален комментарий: id={}", commentId);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        validator.checkUserIdExists(userId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        List<Comment> comments = commentRepository.findAllByAuthorId(userId, pageable);
        return mapToDtosWithReactions(comments);
    }

    @Override
    @Transactional
    public CommentDto addReaction(Long userId, Long commentId, ReactionDto dto) {
        User user = validator.findUserById(userId);
        Comment comment = findCommentById(commentId);
        processReaction(user, comment, dto.getUseful());
        return toDtoWithReactions(comment);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, CommentSort sort, int from, int size) {
        validator.findEventById(eventId);
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Object[]> page = commentRepository.findEventCommentsWithReactions(eventId, sort.name(), pageable);
        return page.getContent().stream()
                .map(row -> {
                    Comment comment = (Comment) row[0];
                    Long useful = ((Number) row[1]).longValue();
                    Long useless = ((Number) row[2]).longValue();
                    return CommentMapper.toDto(comment, useful, useless);
                })
                .toList();
    }

    @Override
    public List<CommentDto> searchComments(List<Long> users, List<Long> events,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           int from, int size) {
        List<Long> safeUsers = (users == null || users.isEmpty()) ? null : users;
        List<Long> safeEvents = (events == null || events.isEmpty()) ? null : events;
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        List<Comment> comments = commentRepository.searchComments(
                safeUsers, safeEvents, rangeStart, rangeEnd, pageable);
        return mapToDtosWithReactions(comments);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = findCommentById(commentId);
        commentRepository.delete(comment);
        log.info("Админ удалил комментарий: id={}", commentId);
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
    }

    private void checkAuthorComment(Comment comment, Long userId) {
        if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException(
                    "Только автор может изменять или удалять комментарий, commentId = " + comment.getId()
            );
        }
    }

    private CommentDto toDtoWithReactions(Comment comment) {
        long useful = reactionRepository.countByCommentIdAndUseful(comment.getId(), true);
        long useless = reactionRepository.countByCommentIdAndUseful(comment.getId(), false);
        return CommentMapper.toDto(comment, useful, useless);
    }

    private List<CommentDto> mapToDtosWithReactions(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, Map<Boolean, Long>> countsMap = getReactionCounts(commentIds);
        return comments.stream()
                .map(comment -> {
                    Map<Boolean, Long> counts = countsMap.getOrDefault(comment.getId(), Map.of());
                    long useful = counts.getOrDefault(true, 0L);
                    long useless = counts.getOrDefault(false, 0L);
                    return CommentMapper.toDto(comment, useful, useless);
                })
                .toList();
    }

    private void processReaction(User user, Comment comment, Boolean useful) {
        Optional<Reaction> existingOpt = reactionRepository.findByCommentIdAndUserId(comment.getId(), user.getId());
        if (existingOpt.isPresent()) {
            Reaction existing = existingOpt.get();
            if (existing.getUseful().equals(useful)) {
                reactionRepository.delete(existing);
            } else {
                existing.setUseful(useful);
            }
        } else {
            Reaction reaction = Reaction.builder()
                    .comment(comment)
                    .user(user)
                    .useful(useful)
                    .build();
            reactionRepository.save(reaction);
        }
    }

    private Map<Long, Map<Boolean, Long>> getReactionCounts(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> reactionCounts = reactionRepository.countReactionsForComments(commentIds);
        return reactionCounts.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.toMap(
                                row -> (Boolean) row[1],
                                row -> (Long) row[2]
                        )
                ));
    }
}
