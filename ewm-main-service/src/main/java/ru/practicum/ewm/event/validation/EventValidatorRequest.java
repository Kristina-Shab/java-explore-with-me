package ru.practicum.ewm.event.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EventValidatorRequest {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    public void checkUserIdExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ValidationException("Пользователь с id=" + userId + " не найден");
        }
    }

    public Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }

    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    public void checkEventInitiator(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + event.getId() + " не найдено");
        }
    }

    public void checkEventUpdatable(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Только события в состоянии PENDING или CANCELED можно изменить");
        }
    }

    public void checkEventPublished(Event event) {
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + event.getId() + " не найдено");
        }
    }

    public void checkEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new ValidationException(
                    "Дата события должна быть не раньше, чем через " + hours + " часа(ов) от текущего момента");
        }
    }

    public void checkEventDateIfPresent(LocalDateTime eventDate, int hours) {
        if (eventDate != null) {
            checkEventDate(eventDate, hours);
        }
    }

    public void checkDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала диапазона не может быть позже даты конца");
        }
    }

    public boolean isSortByViews(String sort) {
        if (sort == null || sort.isBlank() || "EVENT_DATE".equalsIgnoreCase(sort)) {
            return false;
        }
        if ("VIEWS".equalsIgnoreCase(sort)) {
            return true;
        }
        throw new ValidationException("Неизвестный вариант сортировки: " + sort);
    }
}
