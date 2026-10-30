package ru.practicum.ewm.comment.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.CommentRequestDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommentMapper {
    public static CommentDto toDto(Comment comment, Long usefulCount, Long uselessCount) {
        String authorName = comment.getAuthor() != null
                ? comment.getAuthor().getName()
                : "Неизвестный или удаленный пользователь";

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(authorName)
                .eventId(comment.getEvent().getId())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .usefulCount(usefulCount)
                .uselessCount(uselessCount)
                .build();
    }

    public static Comment toEntity(CommentRequestDto dto, User author, Event event) {
        return Comment.builder()
                .text(dto.getText())
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static void updateEntity(Comment comment, CommentRequestDto dto) {
        comment.setText(dto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
    }
}
