package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.filter.EventPublicFilter;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.ewm.util.Constants.DATE_TIME_PATTERN;

@Validated
@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventPublicController {
    private static final String APP_NAME = "ewm-main-service";
    private final EventService eventService;
    private final StatsClient statsClient;

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) @Size(max = 7000) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            HttpServletRequest request) {
        EventPublicFilter filter = EventPublicFilter.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();
        List<EventShortDto> response = eventService.getPublishedEvents(filter);
        sendHit(request);
        return response;
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        EventFullDto response = eventService.getPublishedEventById(id);
        sendHit(request);
        return response;
    }

    private void sendHit(HttpServletRequest request) {
        try {
            statsClient.hit(EndpointHitDto.builder()
                    .app(APP_NAME)
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Не удалось отправить hit в stats: {}", e.getMessage());
        }
    }
}
