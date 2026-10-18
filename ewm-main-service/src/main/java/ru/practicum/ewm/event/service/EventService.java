package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.filter.EventAdminFilter;
import ru.practicum.ewm.event.filter.EventPublicFilter;

import java.util.List;

public interface EventService {
    List<EventShortDto> getPublishedEvents(EventPublicFilter filter);

    EventFullDto getPublishedEventById(Long eventId);

    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<EventFullDto> searchEventsAdmin(EventAdminFilter filter);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto);
}
