package ru.practicum.ewm.comment.model;

import ru.practicum.ewm.exception.ValidationException;

public enum CommentSort {
    NEWEST,
    OLDEST,
    RATING,
    POPULARITY;

    public static CommentSort fromString(String sort) {
        if (sort == null || sort.isBlank()) {
            return NEWEST;
        }
        try {
            return CommentSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неизвестный вариант сортировки комментариев: " + sort);
        }
    }
}
