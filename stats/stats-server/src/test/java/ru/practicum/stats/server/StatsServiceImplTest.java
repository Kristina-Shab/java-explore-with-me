package ru.practicum.stats.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.repository.EndpointHitRepository;
import ru.practicum.stats.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class StatsServiceImplTest {

    @Autowired
    private StatsService service;

    @Autowired
    private EndpointHitRepository repository;

    @Test
    void testStatsServiceSaveHit() {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
                .build();

        service.saveHit(hit);

        var all = repository.findAll();
        assertThat(all).hasSize(1);
        var saved = all.getFirst();
        assertThat(saved.getApp()).isEqualTo("ewm-main-service");
        assertThat(saved.getUri()).isEqualTo("/events");
        assertThat(saved.getIp()).isEqualTo("192.168.0.1");
        assertThat(saved.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 6, 1, 12, 0, 0));
    }

    @Test
    void testStatsServiceGetStatsAll() {
        saveHit("/events", "192.168.0.1", "2024-06-01T12:00:00");
        saveHit("/events", "192.168.0.2", "2024-06-01T13:00:00");
        saveHit("/events/42", "192.168.0.1", "2024-06-01T14:00:00");

        List<ViewStats> result = service.getStats(
                LocalDateTime.of(2024, 6, 1, 0, 0),
                LocalDateTime.of(2024, 6, 2, 0, 0),
                null,
                false
        );

        assertThat(result).hasSize(2);
        ViewStats events = result.stream()
                .filter(s -> s.getUri().equals("/events"))
                .findFirst().orElseThrow();
        assertThat(events.getHits()).isEqualTo(2L);
    }

    @Test
    void testStatsServiceGetStatsUnique() {
        saveHit("/events", "192.168.0.1", "2024-06-01T12:00:00");
        saveHit("/events", "192.168.0.1", "2024-06-01T13:00:00");
        saveHit("/events", "192.168.0.2", "2024-06-01T14:00:00");

        List<ViewStats> result = service.getStats(
                LocalDateTime.of(2024, 6, 1, 0, 0),
                LocalDateTime.of(2024, 6, 2, 0, 0),
                null,
                true
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getHits()).isEqualTo(2L);
    }

    @Test
    void testStatsServiceGetStatsInvalidRange() {
        LocalDateTime start = LocalDateTime.of(2024, 6, 2, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 1, 0, 0);

        assertThatThrownBy(() -> service.getStats(start, end, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testStatsServiceGetStatsEmptyUris() {
        saveHit("/events", "192.168.0.1", "2024-06-01T12:00:00");
        saveHit("/other", "192.168.0.1", "2024-06-01T13:00:00");

        List<ViewStats> result = service.getStats(
                LocalDateTime.of(2024, 6, 1, 0, 0),
                LocalDateTime.of(2024, 6, 2, 0, 0),
                List.of(),
                false
        );

        assertThat(result).hasSize(2);
    }

    @Test
    void testStatsServiceGetStatsFilterByUris() {
        saveHit("/events", "192.168.0.1", "2024-06-01T12:00:00");
        saveHit("/events/42", "192.168.0.1", "2024-06-01T13:00:00");
        saveHit("/other", "192.168.0.1", "2024-06-01T14:00:00");

        List<ViewStats> result = service.getStats(
                LocalDateTime.of(2024, 6, 1, 0, 0),
                LocalDateTime.of(2024, 6, 2, 0, 0),
                List.of("/events", "/events/42"),
                false
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ViewStats::getUri)
                .containsExactlyInAnyOrder("/events", "/events/42");
    }

    @Test
    void testStatsServiceGetStatsEmptyRange() {
        saveHit("/events", "192.168.0.1", "2024-06-01T12:00:00");

        List<ViewStats> result = service.getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                null,
                false
        );

        assertThat(result).isEmpty();
    }

    private void saveHit(String uri, String ip, String time) {
        service.saveHit(EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.parse(time))
                .build());
    }
}

