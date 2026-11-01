package ru.practicum.ewm.comment.model;

public interface CommentReactionCount {
    Long getCommentId();

    Boolean getUseful();

    Long getCount();
}
