package ru.practicum.ewm.comment.model;

public interface CommentWithReactions {
    Comment getComment();

    Long getUsefulCount();

    Long getUselessCount();
}
