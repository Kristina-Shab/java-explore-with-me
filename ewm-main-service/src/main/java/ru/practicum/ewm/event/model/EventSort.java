package ru.practicum.ewm.event.model;

import ru.practicum.ewm.exception.ValidationException;

public enum EventSort {
    EVENT_DATE,
    VIEWS;

    public static EventSort fromString(String sort) {
        if (sort == null || sort.isBlank()) {
            return EVENT_DATE;
        }
        try {
            return EventSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неизвестный вариант сортировки: " + sort);
        }
    }
}
