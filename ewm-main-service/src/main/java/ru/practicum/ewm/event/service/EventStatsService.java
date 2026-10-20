package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.ewm.util.Constants.statsRangeEnd;
import static ru.practicum.ewm.util.Constants.statsRangeStart;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStatsService {
    private static final String EVENT_URI_PREFIX = "/events/";

    private final StatsClient statsClient;
    private final ParticipationRequestRepository requestRepository;

    public Long getViews(Long eventId, boolean unique) {
        try {
            List<ViewStats> stats = statsClient.getStats(
                    statsRangeStart(),
                    statsRangeEnd(),
                    List.of(EVENT_URI_PREFIX + eventId),
                    unique);
            return stats.isEmpty() ? 0L : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Не удалось получить статистику по событию id={}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    public Map<Long, Long> getViewsForEvents(List<Long> eventIds, boolean unique) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> uris = eventIds.stream()
                    .map(id -> EVENT_URI_PREFIX + id)
                    .toList();

            List<ViewStats> stats = statsClient.getStats(
                    statsRangeStart(),
                    statsRangeEnd(),
                    uris,
                    unique);

            return stats.stream()
                    .collect(Collectors.toMap(
                            s -> extractEventId(s.getUri()),
                            ViewStats::getHits));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            return Map.of();
        }
    }

    public Long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    public Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
    }

    private Long extractEventId(String uri) {
        return Long.parseLong(uri.substring(EVENT_URI_PREFIX.length()));
    }
}
