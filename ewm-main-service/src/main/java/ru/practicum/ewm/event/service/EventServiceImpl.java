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
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.filter.EventAdminFilter;
import ru.practicum.ewm.event.filter.EventPublicFilter;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.UserStateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.validation.EventValidatorRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventValidatorRequest validator;
    private final EventStatsService eventStatsService;

    @Override
    public List<EventShortDto> getPublishedEvents(EventPublicFilter filter) {
        EventPublicFilter normalizedFilter = normalizePublicFilter(filter);
        if (validator.isSortByViews(normalizedFilter.getSort())) {
            return searchAndSortByViews(normalizedFilter);
        }
        Pageable pageable = PageRequest.of(
                normalizedFilter.getFrom() / normalizedFilter.getSize(), normalizedFilter.getSize(),
                Sort.by("eventDate").ascending());
        Page<Event> page = searchPublishedEventsWithPaging(normalizedFilter.getText(), normalizedFilter.getCategories(),
                normalizedFilter.getPaid(), normalizedFilter.getRangeStart(), normalizedFilter.getRangeEnd(),
                normalizedFilter.getOnlyAvailable(), pageable);
        return mapToShortDtosWithStats(page.getContent());
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId) {
        Event event = validator.findEventById(eventId);
        validator.checkEventPublished(event);
        return EventMapper.toFullDto(event, eventStatsService.getConfirmedRequests(eventId),
                eventStatsService.getViews(eventId, true));
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = validator.findUserById(userId);
        Category category = validator.findCategoryById(dto.getCategory());
        validator.checkEventDate(dto.getEventDate(), 2);
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
        validator.checkUserIdExists(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageable);
        return mapToShortDtosWithStats(page.getContent());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        validator.checkUserIdExists(userId);
        Event event = validator.findEventById(eventId);
        validator.checkEventInitiator(event, userId);
        return EventMapper.toFullDto(event, eventStatsService.getConfirmedRequests(eventId),
                eventStatsService.getViews(eventId, false));
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        validator.checkUserIdExists(userId);
        Event event = validator.findEventById(eventId);
        validator.checkEventInitiator(event, userId);
        validator.checkEventUpdatable(event);
        validator.checkEventDateIfPresent(dto.getEventDate(), 2);
        applyEventUpdate(event, dto);
        applyUserStateAction(event, dto.getStateAction());
        log.info("Обновлено событие: id={}, state={}", event.getId(), event.getState());
        return EventMapper.toFullDto(event, eventStatsService.getConfirmedRequests(eventId),
                eventStatsService.getViews(eventId, false));
    }

    @Override
    public List<EventFullDto> searchEventsAdmin(EventAdminFilter filter) {
        EventAdminFilter normalizedFilter = normalizeAdminFilter(filter);
        Pageable pageable = PageRequest.of(normalizedFilter.getFrom() / normalizedFilter.getSize(),
                normalizedFilter.getSize());
        Page<Event> page = eventRepository.searchEventsAdmin(normalizedFilter.getUsers(), normalizedFilter.getStates(),
                normalizedFilter.getCategories(), normalizedFilter.getRangeStart(), normalizedFilter.getRangeEnd(),
                pageable);
        return mapToFullDtosWithStats(page.getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = validator.findEventById(eventId);
        validator.checkEventDateIfPresent(dto.getEventDate(), 1);
        applyAdminUpdate(event, dto);
        applyAdminStateAction(event, dto.getStateAction());
        log.info("Админ обновил событие: id={}, state={}", event.getId(), event.getState());
        return EventMapper.toFullDto(event, eventStatsService.getConfirmedRequests(eventId),
                eventStatsService.getViews(eventId, false));
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
            Category category = validator.findCategoryById(dto.getCategory());
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

    private LocalDateTime applyDefaultRangeStart(LocalDateTime rangeStart) {
        return rangeStart != null ? rangeStart : LocalDateTime.now();
    }

    private <T> List<T> normalizeList(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }

    private List<EventShortDto> searchAndSortByViews(EventPublicFilter filter) {
        List<Event> events = searchPublishedEventsNoPaging(filter.getText(), filter.getCategories(), filter.getPaid(),
                filter.getRangeStart(), filter.getRangeEnd(), filter.getOnlyAvailable());
        List<EventShortDto> dtos = mapToShortDtosWithStats(events).stream()
                .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                .toList();
        return paginate(dtos, filter.getFrom(), filter.getSize());
    }

    private <T> List<T> paginate(List<T> list, int from, int size) {
        if (from >= list.size()) {
            return List.of();
        }
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
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
            Category category = validator.findCategoryById(dto.getCategory());
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
        validator.checkEventDate(event.getEventDate(), 1);
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

    private List<EventShortDto> mapToShortDtosWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequests = eventStatsService.getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> views = eventStatsService.getViewsForEvents(eventIds, false);
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
        Map<Long, Long> confirmedRequests = eventStatsService.getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> views = eventStatsService.getViewsForEvents(eventIds, false);
        return events.stream()
                .map(event -> EventMapper.toFullDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private Page<Event> searchPublishedEventsWithPaging(String text,
                                                        List<Long> categories,
                                                        Boolean paid,
                                                        LocalDateTime rangeStart,
                                                        LocalDateTime rangeEnd,
                                                        Boolean onlyAvailable,
                                                        Pageable pageable) {
        if (text == null) {
            return eventRepository.searchEventsWithoutText(
                    EventState.PUBLISHED, categories, paid,
                    rangeStart, rangeEnd, onlyAvailable, pageable);
        }
        return eventRepository.searchEvents(
                EventState.PUBLISHED, text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, pageable);
    }

    private List<Event> searchPublishedEventsNoPaging(String text,
                                                      List<Long> categories,
                                                      Boolean paid,
                                                      LocalDateTime rangeStart,
                                                      LocalDateTime rangeEnd,
                                                      Boolean onlyAvailable) {
        if (text == null) {
            return eventRepository.searchEventsNoPagingWithoutText(
                    EventState.PUBLISHED, categories, paid,
                    rangeStart, rangeEnd, onlyAvailable);
        }
        return eventRepository.searchEventsNoPaging(
                EventState.PUBLISHED, text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable);
    }

    private EventPublicFilter normalizePublicFilter(EventPublicFilter filter) {
        validator.checkDateRange(filter.getRangeStart(), filter.getRangeEnd());
        LocalDateTime rangeStart = applyDefaultRangeStart(filter.getRangeStart());
        List<Long> categories = normalizeList(filter.getCategories());
        Boolean onlyAvailable = filter.getOnlyAvailable() != null && filter.getOnlyAvailable();
        return filter.toBuilder()
                .rangeStart(rangeStart)
                .categories(categories)
                .onlyAvailable(onlyAvailable)
                .build();
    }

    private EventAdminFilter normalizeAdminFilter(EventAdminFilter filter) {
        List<Long> users = normalizeList(filter.getUsers());
        List<EventState> states = normalizeList(filter.getStates());
        List<Long> categories = normalizeList(filter.getCategories());
        validator.checkDateRange(filter.getRangeStart(), filter.getRangeEnd());
        return filter.toBuilder()
                .users(users)
                .states(states)
                .categories(categories)
                .build();
    }
}
