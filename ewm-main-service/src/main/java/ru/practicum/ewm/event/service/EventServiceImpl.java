package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.UserStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public List<EventShortDto> getPublishedEvents(String text,
                                                  List<Long> categories,
                                                  Boolean paid,
                                                  LocalDateTime rangeStart,
                                                  LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable,
                                                  String sort,
                                                  int from,
                                                  int size) {
        rangeStart = applyDefaultRangeStart(rangeStart, rangeEnd);
        categories = normalizeList(categories);
        onlyAvailable = onlyAvailable != null && onlyAvailable;
        checkDateRange(rangeStart, rangeEnd);
        if (isSortByViews(sort)) {
            return searchAndSortByViews(text, categories, paid,
                    rangeStart, rangeEnd, onlyAvailable, from, size);
        }
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        Page<Event> page = eventRepository.searchEvents(
                EventState.PUBLISHED, text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, pageable);
        return mapToShortDtosWithStats(page.getContent());
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId) {
        Event event = findEventById(eventId);
        checkEventPublished(event);
        return EventMapper.toFullDto(event, getConfirmedRequests(eventId), getViews(eventId));
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = findUserById(userId);
        Category category = findCategoryById(dto.getCategory());

        checkEventDate(dto.getEventDate());

        Event event = EventMapper.toEntity(dto).toBuilder()
                .initiator(initiator)
                .category(category)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();

        Event saved = eventRepository.save(event);
        log.info("Создано событие: id={}, title={}, initiatorId={}",
                saved.getId(), saved.getTitle(), userId);

        return EventMapper.toFullDto(saved, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        checkUserIdExists(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageable);
        return mapToShortDtosWithStats(page.getContent());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        checkUserIdExists(userId);

        Event event = findEventById(eventId);
        checkEventInitiator(event, userId);

        return EventMapper.toFullDto(event, getConfirmedRequests(eventId), getViews(eventId));
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        checkUserIdExists(userId);
        Event event = findEventById(eventId);
        checkEventInitiator(event, userId);
        checkEventUpdatable(event);
        checkEventDateIfPresent(dto.getEventDate());
        applyEventUpdate(event, dto);
        applyUserStateAction(event, dto.getStateAction());
        log.info("Обновлено событие: id={}, state={}", event.getId(), event.getState());
        return EventMapper.toFullDto(event, getConfirmedRequests(eventId), getViews(eventId));
    }

    @Override
    public List<EventFullDto> searchEventsAdmin(List<Long> users,
                                                List<EventState> states,
                                                List<Long> categories,
                                                LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd,
                                                int from,
                                                int size) {
        users = normalizeList(users);
        states = normalizeList(states);
        categories = normalizeList(categories);

        checkDateRange(rangeStart, rangeEnd);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = eventRepository.searchEventsAdmin(
                users, states, categories, rangeStart, rangeEnd, pageable);

        return mapToFullDtosWithStats(page.getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = findEventById(eventId);
        checkEventDateIfPresent(dto.getEventDate());
        applyAdminUpdate(event, dto);
        applyAdminStateAction(event, dto.getStateAction());
        log.info("Админ обновил событие: id={}, state={}", event.getId(), event.getState());
        return EventMapper.toFullDto(event, getConfirmedRequests(eventId), getViews(eventId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException(
                    "Дата события должна быть не раньше, чем через два часа от текущего момента");
        }
    }

    private void checkEventInitiator(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + event.getId() + " не найдено");
        }
    }

    private void checkUserIdExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ValidationException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void checkEventUpdatable(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Только события в состоянии PENDING или CANCELED можно изменить");
        }
    }

    private void checkEventDateIfPresent(LocalDateTime eventDate) {
        if (eventDate != null) {
            checkEventDate(eventDate);
        }
    }

    private void applyEventUpdate(Event event, UpdateEventUserRequest dto) {
        setIfNotNull(dto.getAnnotation(), event::setAnnotation);
        setIfNotNull(dto.getDescription(), event::setDescription);
        setIfNotNull(dto.getTitle(), event::setTitle);
        setIfNotNull(dto.getEventDate(), event::setEventDate);
        setIfNotNull(dto.getPaid(), event::setPaid);
        setIfNotNull(dto.getParticipantLimit(), event::setParticipantLimit);
        setIfNotNull(dto.getRequestModeration(), event::setRequestModeration);
        setIfNotNull(dto.getLocation(), event::setLocation);

        if (dto.getCategory() != null) {
            Category category = findCategoryById(dto.getCategory());
            event.setCategory(category);
        }
    }

    private void applyUserStateAction(Event event, UserStateAction stateAction) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
        }
    }

    private <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private LocalDateTime applyDefaultRangeStart(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null && rangeEnd == null) {
            return LocalDateTime.now();
        }
        return rangeStart;
    }

    private <T> List<T> normalizeList(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }

    private void checkDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала диапазона не может быть позже даты конца");
        }
    }

    private List<EventShortDto> searchAndSortByViews(String text,
                                                     List<Long> categories,
                                                     Boolean paid,
                                                     LocalDateTime rangeStart,
                                                     LocalDateTime rangeEnd,
                                                     Boolean onlyAvailable,
                                                     int from,
                                                     int size) {
        List<Event> events = eventRepository.searchEventsNoPaging(
                EventState.PUBLISHED, text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable);

        List<EventShortDto> dtos = mapToShortDtosWithStats(events).stream()
                .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                .toList();

        return paginate(dtos, from, size);
    }

    private boolean isSortByViews(String sort) {
        if (sort == null || sort.isBlank() || "EVENT_DATE".equalsIgnoreCase(sort)) {
            return false;
        }
        if ("VIEWS".equalsIgnoreCase(sort)) {
            return true;
        }
        throw new ValidationException("Неизвестный вариант сортировки: " + sort);
    }

    private Long getViews(Long eventId) {
        try {
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.MIN,
                    LocalDateTime.now().plusYears(1),
                    List.of("/events/" + eventId),
                    false);
            return stats.isEmpty() ? 0L : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Не удалось получить статистику по событию id={}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private <T> List<T> paginate(List<T> list, int from, int size) {
        if (from >= list.size()) {
            return List.of();
        }
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    private void checkEventPublished(Event event) {
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + event.getId() + " не найдено");
        }
    }

    private void applyAdminUpdate(Event event, UpdateEventAdminRequest dto) {
        setIfNotNull(dto.getAnnotation(), event::setAnnotation);
        setIfNotNull(dto.getDescription(), event::setDescription);
        setIfNotNull(dto.getTitle(), event::setTitle);
        setIfNotNull(dto.getEventDate(), event::setEventDate);
        setIfNotNull(dto.getPaid(), event::setPaid);
        setIfNotNull(dto.getParticipantLimit(), event::setParticipantLimit);
        setIfNotNull(dto.getRequestModeration(), event::setRequestModeration);
        setIfNotNull(dto.getLocation(), event::setLocation);

        if (dto.getCategory() != null) {
            Category category = findCategoryById(dto.getCategory());
            event.setCategory(category);
        }
    }

    private void applyAdminStateAction(Event event, AdminStateAction stateAction) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case PUBLISH_EVENT -> publishEvent(event);
            case REJECT_EVENT -> rejectEvent(event);
        }
    }

    private void publishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException(
                    "Публиковать можно только события в состоянии PENDING");
        }
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException(
                    "Дата начала события должна быть не раньше чем через час от даты публикации");
        }
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
    }

    private void rejectEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException(
                    "Отклонить можно только неопубликованное событие");
        }
        event.setState(EventState.CANCELED);
    }

    private Long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .toList();

            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.MIN,
                    LocalDateTime.now().plusYears(1),
                    uris,
                    false);
            return stats.stream()
                    .collect(Collectors.toMap(
                            s -> extractEventId(s.getUri()),
                            ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            return Map.of();
        }
    }

    private Long extractEventId(String uri) {
        return Long.parseLong(uri.substring("/events/".length()));
    }

    private List<EventShortDto> mapToShortDtosWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> views = getViewsForEvents(eventIds);
        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private List<EventFullDto> mapToFullDtosWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> views = getViewsForEvents(eventIds);
        return events.stream()
                .map(event -> EventMapper.toFullDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }
}
